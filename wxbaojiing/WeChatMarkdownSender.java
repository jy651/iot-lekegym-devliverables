/**
 * 企业微信群机器人 Markdown 推送。
 * 上传 GitHub 时请把 WEBHOOK_URL 换成自己的 key，不要提交真实密钥。
 */
public class WeChatMarkdownSender {

    private static final String WEBHOOK_URL =
            "lllllll1111111111111";

    private WeChatMarkdownSender() {
    }

    public static void sendMsgByMk(String msg) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "markdown");
        Map<String, String> content = new HashMap<>();
        content.put("content", msg);
        body.put("markdown", content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(WEBHOOK_URL, request, String.class);
        System.out.println(response.getBody());
    }
}
