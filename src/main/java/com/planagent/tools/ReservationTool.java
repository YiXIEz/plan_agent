package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ReservationTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public ReservationTool(ToolRegistry registry, MockDataStore store) {
        registry.register("make_reservation",
            "预订餐厅座位或购买门票",
            Map.of(
                "restaurantId", Map.of("type", "string", "description", "商户ID"),
                "timeSlot", Map.of("type", "string", "description", "时间段"),
                "partySize", Map.of("type", "integer", "description", "人数"),
                "note", Map.of("type", "string", "description", "备注，如儿童座椅、蛋糕送到")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String restId = node.get("restaurantId").asText();
                String time = node.has("timeSlot") ? node.get("timeSlot").asText() : "17:00";
                int size = node.has("partySize") ? node.get("partySize").asInt() : 3;
                String note = node.has("note") ? node.get("note").asText() : "";
                return mapper.writeValueAsString(store.makeReservation(restId, time, size, note));
            });
    }
}
