/**
 * 设备闲置告警定时任务。
 * Quartz 调用：WxbaoxuTask.snedEquipmentMsg(-1,-1)
 *
 * 主库训练流水、从库设备数据各自找出时间窗内无记录的器械 ID，
 * 取交集后再按 Redis 门店白名单过滤，降低单侧误报。
 * 从库查询走 DataService（@DataSource(SLAVE)）。
 */
@Component("WxbaoxuTask")
public class WxbaoxuTask {

    private static final String USED_STORE_REDIS_KEY = "usedStore";

    private static final DateTimeFormatter RANGE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Autowired
    private ITrainingDetailService trainingDetailService;

    @Autowired
    private DataService dataService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * startTime/endTime 均为 -1：统计前一自然日全天。
     * 否则按毫秒区间 [start, end] 统计（停机回补）。
     */
    public List<DeviceDataVO> snedEquipmentMsg(Long startTime, Long endTime) {
        boolean previousFullDay =
                startTime != null && endTime != null && startTime == -1L && endTime == -1L;

        long startMillis;
        long endMillis;
        if (previousFullDay) {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate yesterday = LocalDate.now(zone).minusDays(1);
            startMillis = yesterday.atStartOfDay(zone).toInstant().toEpochMilli();
            endMillis = yesterday.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;
        } else {
            if (startTime == null || endTime == null) {
                return Collections.emptyList();
            }
            startMillis = startTime;
            endMillis = endTime;
        }

        List<String> fromTraining = trainingDetailService.unusedEquipmentId(startMillis, endMillis);
        List<String> fromDevice = dataService.getUnusedEquipmentId(startMillis, endMillis);
        if (fromTraining.isEmpty() || fromDevice.isEmpty()) {
            return Collections.emptyList();
        }

        HashSet<String> deviceIds = new HashSet<>(fromDevice);
        List<String> intersection = fromTraining.stream()
                .filter(deviceIds::contains)
                .collect(Collectors.toList());
        if (intersection.isEmpty()) {
            return Collections.emptyList();
        }

        List<DeviceDataVO> details = trainingDetailService.getDetails(intersection);
        List<DeviceDataVO> filteredDetails = filterByStatStoreIds(details);
        if (!filteredDetails.isEmpty()) {
            WeChatMarkdownSender.sendMsgByMk(
                    buildUnusedEquipmentMarkdown(previousFullDay, startMillis, endMillis, filteredDetails));
        }
        return filteredDetails;
    }

    /** Redis usedStore 为逗号分隔门店 ID，只保留白名单内器械。 */
    private List<DeviceDataVO> filterByStatStoreIds(List<DeviceDataVO> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }
        String storeIdsStr = stringRedisTemplate.opsForValue().get(USED_STORE_REDIS_KEY);
        if (storeIdsStr == null || storeIdsStr.trim().isEmpty()) {
            return details;
        }
        Set<Long> allowedStoreIds = Arrays.stream(storeIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        if (allowedStoreIds.isEmpty()) {
            return Collections.emptyList();
        }
        return details.stream()
                .filter(d -> d.getStoreId() != null && allowedStoreIds.contains(d.getStoreId()))
                .collect(Collectors.toList());
    }

    private static String buildUnusedEquipmentMarkdown(
            boolean previousFullDay, long startMillis, long endMillis, List<DeviceDataVO> equipmentList) {
        String mode = previousFullDay ? "前一自然日（昨天 0 点～24 点）" : "指定时段";
        String range = RANGE_FMT.format(Instant.ofEpochMilli(startMillis))
                + " ~ "
                + RANGE_FMT.format(Instant.ofEpochMilli(endMillis));
        String detailLine = equipmentList.stream()
                .map(e -> "> "
                        + (e.getStoreName() != null ? e.getStoreName() : "-")
                        + " | "
                        + (e.getEquipmentName() != null ? e.getEquipmentName() : "-")
                        + " | ID:"
                        + e.getEquipmentId())
                .collect(Collectors.joining("\n"));
        return "## 设备未使用告警\n> **统计方式：** "
                + mode
                + "\n> **时间范围：** "
                + range
                + "\n> **无使用记录器械（训练与设备数据交集）：**\n"
                + detailLine;
    }
}
