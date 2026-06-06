package com.planagent.scoring;

import com.planagent.model.*;
import com.planagent.model.SessionContext.Scenario;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {
    private final WeightConfig config;
    private final ScoringEngine engine;

    public ScoringEngineTest() {
        config = new WeightConfig();
        config.setFamily(Map.of("rating", 0.20, "distance", 0.15, "price-match", 0.10,
            "available", 0.15, "kid-friendly", 0.30, "time-match", 0.10));
        config.setFriends(Map.of("rating", 0.30, "distance", 0.15, "price-match", 0.15,
            "available", 0.20, "vibe-match", 0.20));
        engine = new ScoringEngine(config);
    }

    @Test
    void familyScenarioRanksKidFriendlyHigher() {
        var ctx = new SessionContext("test");
        ctx.scenario = Scenario.FAMILY;
        ctx.partySize = 3;

        var a1 = new Activity("a1", "亲子农场", "亲子乐园", 4.0, "2.3km",
            "2-3小时", "￥128/人", List.of("萌宠"), "3-10岁", "09:00-18:00");
        var a2 = new Activity("a2", "蹦床公园", "运动", 5.0, "2.3km",
            "2小时", "￥98/人", List.of("蹦床"), "12-18岁", "09:00-21:00");

        var list = new ArrayList<>(List.of(a2, a1));
        var ranked = engine.rankActivities(list, ctx, 5);

        assertEquals("亲子农场", ranked.get(0).name);
    }

    @Test
    void friendsScenarioRatesRatingHigher() {
        var ctx = new SessionContext("test");
        ctx.scenario = Scenario.FRIENDS;
        ctx.partySize = 4;

        var r1 = new Restaurant("r1", "高评分餐厅", "粤菜", 4.9, "5.0km",
            "￥80/人", List.of("粤菜"), false, false, "无需排队", "可容纳4人");
        var r2 = new Restaurant("r2", "普通餐厅", "川菜", 3.5, "5.0km",
            "￥80/人", List.of("川菜"), false, false, "无需排队", "可容纳4人");

        var list = new ArrayList<>(List.of(r2, r1));
        var ranked = engine.rankRestaurants(list, ctx);

        assertEquals("高评分餐厅", ranked.get(0).name);
    }

    @Test
    void closerRestaurantScoresHigher_allElseEqual() {
        var ctx = new SessionContext("test");
        ctx.scenario = Scenario.FAMILY;
        ctx.partySize = 3;

        var r1 = new Restaurant("r1", "近餐厅", "粤菜", 4.5, "0.5km",
            "￥80/人", List.of("粤菜"), true, true, "无需排队", "可容纳4人");
        var r2 = new Restaurant("r2", "远餐厅", "粤菜", 4.5, "8.0km",
            "￥80/人", List.of("粤菜"), true, true, "无需排队", "可容纳4人");

        var list = new ArrayList<>(List.of(r2, r1));
        var ranked = engine.rankRestaurants(list, ctx);

        assertEquals("近餐厅", ranked.get(0).name);
    }

    @Test
    void noWaitScoresHigherThanLongWait() {
        var ctx = new SessionContext("test");
        ctx.scenario = Scenario.FAMILY;
        ctx.partySize = 3;

        var r1 = new Restaurant("r1", "快餐厅", "粤菜", 4.5, "2.0km",
            "￥80/人", List.of("粤菜"), true, true, "无需排队", "可容纳4人");
        var r2 = new Restaurant("r2", "慢餐厅", "粤菜", 4.5, "2.0km",
            "￥80/人", List.of("粤菜"), true, true, "需提前订座", "可容纳4人");

        var list = new ArrayList<>(List.of(r2, r1));
        var ranked = engine.rankRestaurants(list, ctx);

        assertEquals("快餐厅", ranked.get(0).name);
    }
}
