package com.planagent.mock;

import com.planagent.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MockDataStoreTest {
    private final MockDataStore store = new MockDataStore();

    @Test
    void searchActivitiesByType() {
        var results = store.searchActivities("亲子乐园", 5, "2-3小时", "5.0km");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(a -> a.type.contains("亲子乐园")));
    }

    @Test
    void searchRestaurantsByTags() {
        var results = store.searchRestaurants("", List.of("轻食"), null, true);
        assertFalse(results.isEmpty());
        results.forEach(r -> {
            assertTrue(r.tags.contains("轻食") || r.hasLowCalOption);
            assertTrue(r.hasKidsMenu);
        });
    }

    @Test
    void checkQueueReturnsInfo() {
        var info = store.checkQueue("rest-001", "17:00", 3);
        assertNotNull(info.restaurantName);
    }

    @Test
    void makeReservationReturnsReservation() {
        var res = store.makeReservation("rest-001", "17:00", 3, "儿童座椅");
        assertEquals("已预订", res.status);
        assertNotNull(res.reservationId);
    }

    @Test
    void placeOrderReturnsOrder() {
        var order = store.placeOrder("蛋糕", "rest-001", 1, "送到餐厅");
        assertEquals("已下单", order.status);
        assertNotNull(order.orderId);
    }

    @Test
    void weatherReturnsInfo() {
        var w = store.checkWeather("朝阳区");
        assertNotNull(w.weather);
        assertTrue(w.temperature.contains("°C"));
    }

    @Test
    void routeReturnsInfo() {
        var r = store.planRoute("家", "奇乐亲子农场", "驾车");
        assertNotNull(r.distance);
        assertNotNull(r.duration);
    }
}
