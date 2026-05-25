package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaymentTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public PaymentTool(ToolRegistry registry, MockDataStore store) {
        registry.register("pay",
            "完成订单支付",
            Map.of("orderId", Map.of("type", "string", "description", "订单ID")),
            (args) -> {
                var node = mapper.readTree(args);
                String orderId = node.get("orderId").asText();
                return mapper.writeValueAsString(store.pay(orderId));
            });
    }
}
