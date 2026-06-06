package com.planagent.agent;

import com.planagent.amap.AmapClient;
import com.planagent.mock.MockDataStore;
import com.planagent.tools.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class AgentLoopTest {
    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        var store = new MockDataStore();
        var amapClient = new AmapClient("", RestClient.builder());
        registry = new ToolRegistry();
        new ActivitySearchTool(registry, store, amapClient);
        new RestaurantSearchTool(registry, store, amapClient);
        new WeatherTool(registry, store, amapClient);
        new RouteTool(registry, store, amapClient);
        new RatingFilterTool(registry, store, amapClient);
        new QueueCheckTool(registry, store);
        new ReservationTool(registry, store);
        new OrderTool(registry, store);
        new PaymentTool(registry, store);
        new NotificationTool(registry, store);
    }

    @Test
    void allTenToolsRegistered() {
        var specs = registry.getSpecifications();
        assertEquals(10, specs.size());
        var names = specs.stream().map(s -> s.name()).sorted().toList();
        assertTrue(names.contains("search_activities"));
        assertTrue(names.contains("search_restaurants"));
        assertTrue(names.contains("check_weather"));
        assertTrue(names.contains("plan_route"));
        assertTrue(names.contains("filter_by_rating"));
        assertTrue(names.contains("check_queue"));
        assertTrue(names.contains("make_reservation"));
        assertTrue(names.contains("place_order"));
        assertTrue(names.contains("pay"));
        assertTrue(names.contains("send_notification"));
    }
}
