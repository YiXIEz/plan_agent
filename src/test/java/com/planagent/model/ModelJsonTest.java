package com.planagent.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModelJsonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void activityRoundTrip() throws Exception {
        var a = new Activity("a1", "奇乐亲子农场", "亲子乐园", 4.8, "2.3km",
            "2-3小时", "￥128/人", List.of("萌宠喂养", "采摘体验"), "3-10岁", "09:00-18:00");
        String json = mapper.writeValueAsString(a);
        var restored = mapper.readValue(json, Activity.class);
        assertEquals("a1", restored.id);
        assertEquals(4.8, restored.rating);
        assertEquals(2, restored.highlights.size());
    }

    @Test
    void restaurantRoundTrip() throws Exception {
        var r = new Restaurant("r1", "绿野轻食", "轻食/简餐", 4.6, "0.5km",
            "￥65/人", List.of("轻食", "亲子友好"), true, true, "15分钟", "可容纳4人");
        String json = mapper.writeValueAsString(r);
        var restored = mapper.readValue(json, Restaurant.class);
        assertEquals("r1", restored.id);
        assertEquals(2, restored.tags.size());
    }
}
