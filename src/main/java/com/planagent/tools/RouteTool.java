package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class RouteTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public RouteTool(ToolRegistry registry, MockDataStore store) {
        registry.register("plan_route",
            "规划两点间路线和时间",
            Map.of(
                "from", Map.of("type", "string", "description", "起点"),
                "to", Map.of("type", "string", "description", "终点"),
                "mode", Map.of("type", "string", "description", "出行方式", "enum", java.util.List.of("驾车", "公交", "步行"))
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String from = node.get("from").asText();
                String to = node.get("to").asText();
                String mode = node.has("mode") ? node.get("mode").asText() : "驾车";
                return mapper.writeValueAsString(store.planRoute(from, to, mode));
            });
    }
}
