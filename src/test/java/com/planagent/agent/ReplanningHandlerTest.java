package com.planagent.agent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplanningHandlerTest {
    private final ReplanningHandler handler = new ReplanningHandler();

    @Test
    void fullyBookedDetected() {
        var type = handler.analyze("{\"status\":\"失败: 已约满\"}");
        assertEquals(ReplanningHandler.FailureType.FULLY_BOOKED, type);
    }

    @Test
    void successDetected() {
        var type = handler.analyze("{\"status\":\"已预订\",\"queueNumber\":\"A5\"}");
        assertEquals(ReplanningHandler.FailureType.SUCCESS, type);
    }

    @Test
    void successWithWaitDetected() {
        var type = handler.analyze("{\"status\":\"已预订\",\"queueNumber\":\"A10\",\"estimatedWaitMinutes\":35}");
        assertEquals(ReplanningHandler.FailureType.SUCCESS, type);
    }

    @Test
    void notAvailableDetected() {
        var type = handler.analyze("{\"status\":\"失败: 商品已下架\"}");
        assertEquals(ReplanningHandler.FailureType.NOT_AVAILABLE, type);
    }

    @Test
    void orderSuccessDetected() {
        var type = handler.analyze("{\"status\":\"已下单\"}");
        assertEquals(ReplanningHandler.FailureType.SUCCESS, type);
    }

    @Test
    void unknownDetected() {
        var type = handler.analyze("{\"status\":\"未知错误\"}");
        assertEquals(ReplanningHandler.FailureType.UNKNOWN, type);
    }

    @Test
    void generateReplanPromptIncludesContext() {
        var prompt = handler.generateReplanPromptWithExpandedSearch(
            "make_reservation", "绿野轻食", "已约满，无法预订",
            "14:00出发 → 14:30亲子农场 → 17:00绿野轻食", 1);
        assertTrue(prompt.contains("make_reservation"));
        assertTrue(prompt.contains("绿野轻食"));
        assertTrue(prompt.contains("已约满"));
        assertTrue(prompt.contains("亲子农场"));
        assertTrue(prompt.contains("第1次"));
    }
}
