package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class QueueCheckTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public QueueCheckTool(ToolRegistry registry, MockDataStore store) {
        registry.register("check_queue",
            "查询餐厅当前排队或空位情况",
            Map.of(
                "restaurantId", Map.of("type", "string", "description", "餐厅ID"),
                "timeSlot", Map.of("type", "string", "description", "到店时间段，如17:00"),
                "partySize", Map.of("type", "integer", "description", "用餐人数")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String restId = node.get("restaurantId").asText();
                String time = node.has("timeSlot") ? node.get("timeSlot").asText() : "17:00";
                int size = node.has("partySize") ? node.get("partySize").asInt() : 3;
                return mapper.writeValueAsString(store.checkQueue(restId, time, size));
            });
    }
}
