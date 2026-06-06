package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.amap.AmapClient;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class RatingFilterTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public RatingFilterTool(ToolRegistry registry, MockDataStore store, AmapClient amapClient) {
        registry.register("filter_by_rating",
            "按评分筛选商户",
            Map.of(
                "ids", Map.of("type", "array", "description", "商户ID列表"),
                "minRating", Map.of("type", "number", "description", "最低评分，如4.5")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                List<String> ids = mapper.convertValue(node.get("ids"), List.class);
                double minRating = node.get("minRating").asDouble();
                if (amapClient.isEnabled()) {
                    var result = amapClient.filterByRating(ids, minRating);
                    if (result != null) return mapper.writeValueAsString(result);
                }
                return mapper.writeValueAsString(store.filterByRating(ids, minRating));
            });
    }
}
