package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.amap.AmapClient;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WeatherTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public WeatherTool(ToolRegistry registry, MockDataStore store, AmapClient amapClient) {
        registry.register("check_weather",
            "查询指定区域下午天气",
            Map.of("district", Map.of("type", "string", "description", "区域名称，如朝阳区")),
            (args) -> {
                var node = mapper.readTree(args);
                String district = node.has("district") ? node.get("district").asText() : "北京";
                if (amapClient.isEnabled()) {
                    var result = amapClient.checkWeather(district);
                    if (result != null) return mapper.writeValueAsString(result);
                }
                return mapper.writeValueAsString(store.checkWeather(district));
            });
    }
}
