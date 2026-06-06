package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.amap.AmapClient;
import com.planagent.mock.MockDataStore;
import com.planagent.model.Restaurant;
import com.planagent.model.SessionContext;
import com.planagent.scoring.ScoringEngine;
import com.planagent.scoring.SessionContextHolder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class RestaurantSearchTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public RestaurantSearchTool(ToolRegistry registry, MockDataStore store, AmapClient amapClient,
                                ScoringEngine scoringEngine) {
        registry.register("search_restaurants",
            "搜索符合条件的餐厅",
            Map.of(
                "keyword", Map.of("type", "string", "description", "搜索关键词，如粤菜、轻食"),
                "tags", Map.of("type", "array", "description", "菜系标签，如轻食、粤菜、亲子友好"),
                "budget", Map.of("type", "string", "description", "预算档次", "enum", List.of("经济", "中等", "高端")),
                "kidFriendly", Map.of("type", "boolean", "description", "是否需要亲子友好")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String keyword = node.has("keyword") ? node.get("keyword").asText() : null;
                List<String> tags = node.has("tags") ? mapper.convertValue(node.get("tags"), List.class) : null;
                String budget = node.has("budget") ? node.get("budget").asText() : null;
                boolean kidFriendly = node.has("kidFriendly") && node.get("kidFriendly").asBoolean();
                if (amapClient.isEnabled()) {
                    var result = amapClient.searchRestaurants(keyword, tags, budget, kidFriendly);
                    if (result != null) return mapper.writeValueAsString(result);
                }
                List<Restaurant> results = store.searchRestaurants(keyword, tags, budget, kidFriendly);
                SessionContext ctx = SessionContextHolder.get();
                if (ctx != null) {
                    results = scoringEngine.rankRestaurants(results, ctx);
                }
                return mapper.writeValueAsString(results);
            });
    }
}
