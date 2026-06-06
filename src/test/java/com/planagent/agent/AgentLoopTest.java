package com.planagent.agent;

import com.planagent.amap.AmapClient;
import com.planagent.mock.MockDataStore;
import com.planagent.scoring.ScoringEngine;
import com.planagent.scoring.WeightConfig;
import com.planagent.tools.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentLoopTest {
    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        var store = new MockDataStore();
        var amapClient = new AmapClient("", RestClient.builder());
        var weightConfig = new WeightConfig();
        weightConfig.setFamily(Map.of("rating", 0.20, "distance", 0.15, "price-match", 0.10,
            "available", 0.15, "kid-friendly", 0.30, "time-match", 0.10));
        weightConfig.setFriends(Map.of("rating", 0.30, "distance", 0.15, "price-match", 0.15,
            "available", 0.20, "vibe-match", 0.20));
        var scoringEngine = new ScoringEngine(weightConfig);
        registry = new ToolRegistry();
        new ActivitySearchTool(registry, store, amapClient, scoringEngine);
        new RestaurantSearchTool(registry, store, amapClient, scoringEngine);
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
