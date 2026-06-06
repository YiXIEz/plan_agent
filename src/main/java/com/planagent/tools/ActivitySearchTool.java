package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.amap.AmapClient;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ActivitySearchTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public ActivitySearchTool(ToolRegistry registry, MockDataStore store, AmapClient amapClient) {
        registry.register("search_activities",
            "搜索附近的亲子活动/公园/展览/手工坊",
            Map.of(
                "type", Map.of("type", "string", "description", "活动类型: 亲子乐园, 展览, 公园, 手工坊, 运动, 电影"),
                "childAge", Map.of("type", "integer", "description", "孩子年龄"),
                "duration", Map.of("type", "string", "description", "预计停留时长，如2-3小时"),
                "maxDistance", Map.of("type", "string", "description", "距离上限，如5.0km")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String type = node.has("type") ? node.get("type").asText() : null;
                int age = node.has("childAge") ? node.get("childAge").asInt() : 5;
                String duration = node.has("duration") ? node.get("duration").asText() : "";
                String dist = node.has("maxDistance") ? node.get("maxDistance").asText() : "10.0km";
                if (amapClient.isEnabled()) {
                    var result = amapClient.searchActivities(type, age, duration, dist);
                    if (result != null) return mapper.writeValueAsString(result);
                }
                var results = store.searchActivities(type, age, duration, dist);
                return mapper.writeValueAsString(results);
            });
    }
}
