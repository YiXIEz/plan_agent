package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OrderTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderTool(ToolRegistry registry, MockDataStore store) {
        registry.register("place_order",
            "下单预订门票、蛋糕、鲜花等",
            Map.of(
                "productType", Map.of("type", "string", "description", "商品类型", "enum", java.util.List.of("门票", "蛋糕", "鲜花")),
                "merchantId", Map.of("type", "string", "description", "商户ID"),
                "quantity", Map.of("type", "integer", "description", "数量"),
                "note", Map.of("type", "string", "description", "备注，如送达时间、送达地址")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String type = node.get("productType").asText();
                String merchant = node.has("merchantId") ? node.get("merchantId").asText() : "";
                int qty = node.has("quantity") ? node.get("quantity").asInt() : 1;
                String note = node.has("note") ? node.get("note").asText() : "";
                return mapper.writeValueAsString(store.placeOrder(type, merchant, qty, note));
            });
    }
}
