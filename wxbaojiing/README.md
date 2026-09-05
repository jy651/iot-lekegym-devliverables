# 设备闲置告警（WxbaoxuTask）

Quartz 定时任务：主库训练流水与从库设备数据做「时间窗内无记录」判定，器械 ID 取交集后推送企业微信；再用 Redis 门店白名单过滤。

## 文件

- `WxbaoxuTask.java`：时间窗、双源交集、门店过滤、告警文案
- `WeChatMarkdownSender.java`：企业微信 Markdown 发送

## Quartz 调用示例

```text
WxbaoxuTask.snedEquipmentMsg(-1,-1)
```

依赖实习项目中的 DataService、ITrainingDetailService，不能单独编译运行。
请勿把企业微信 Webhook 真实 key 提交进仓库。
