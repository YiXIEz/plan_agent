package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class NotificationTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public NotificationTool(ToolRegistry registry, MockDataStore store) {
        registry.register("send_notification",
            "将计划发送给指定联系人",
            Map.of(
                "recipient", Map.of("type", "string", "description", "接收人，如老婆、朋友"),
                "message", Map.of("type", "string", "description", "通知消息内容")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String recipient = node.get("recipient").asText();
                String message = node.get("message").asText();
                return mapper.writeValueAsString(store.sendNotification(recipient, message));
            });
    }
}
