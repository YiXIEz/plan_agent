package com.planagent.scoring;

import com.planagent.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ScoringEngine {

    private static final double MAX_DISTANCE_KM = 10.0;

    private final WeightConfig config;

    public ScoringEngine(WeightConfig config) {
        this.config = config;
    }

    public List<Activity> rankActivities(List<Activity> activities, SessionContext ctx, int childAge) {
        var weights = config.forScenario(ctx.scenario);
        return activities.stream()
            .sorted(Comparator.comparingDouble(a -> -scoreActivity(a, childAge, weights)))
            .collect(Collectors.toList());
    }

    public List<Restaurant> rankRestaurants(List<Restaurant> restaurants, SessionContext ctx) {
        var weights = config.forScenario(ctx.scenario);
        return restaurants.stream()
            .sorted(Comparator.comparingDouble(r -> -scoreRestaurant(r, ctx, weights)))
            .collect(Collectors.toList());
    }

    double scoreActivity(Activity a, int childAge, ScenarioWeights w) {
        double score = 0;
        score += w.get("rating")      * normalizeRating(a.rating);
        score += w.get("distance")    * distanceScore(a.distance);
        score += w.get("price-match") * priceScore(a.price, "中等");
        score += w.get("time-match")  * timeScore(a.duration, "2-3小时");
        if (w.containsKey("kid-friendly")) {
            score += w.get("kid-friendly") * kidFriendlyScore(a.ageSuitable, childAge);
        }
        return score;
    }

    double scoreRestaurant(Restaurant r, SessionContext ctx, ScenarioWeights w) {
        double score = 0;
        score += w.get("rating")      * normalizeRating(r.rating);
        score += w.get("distance")    * distanceScore(parseRefDistance(r.distance));
        score += w.get("price-match") * priceScore(r.avgPrice, "中等");
        score += w.get("available")   * availableScore(r.queueTime);
        if (w.containsKey("kid-friendly")) {
            score += w.get("kid-friendly") * (r.hasKidsMenu ? 1.0 : 0.3);
        }
        if (w.containsKey("vibe-match")) {
            score += w.get("vibe-match") * vibeScore(r.tags, ctx);
        }
        return score;
    }

    // ---- normalization helpers ----

    double normalizeRating(double rating) { return rating / 5.0; }

    double distanceScore(String distStr) {
        return distanceScore(parseDistance(distStr));
    }

    double distanceScore(double distKm) {
        return 1.0 - Math.min(distKm, MAX_DISTANCE_KM) / MAX_DISTANCE_KM;
    }

    double priceScore(String priceStr, String targetBudget) {
        int price = parsePrice(priceStr);
        int target = targetBudget.equals("经济") ? 60 : targetBudget.equals("中等") ? 100 : 150;
        int diff = Math.abs(price - target);
        if (diff <= 20) return 1.0;
        if (diff <= 50) return 0.6;
        return 0.2;
    }

    double availableScore(String queueTime) {
        if (queueTime == null) return 0.5;
        if (queueTime.contains("无需排队")) return 1.0;
        if (queueTime.contains("0-5分钟")) return 0.9;
        if (queueTime.contains("5-10分钟")) return 0.8;
        if (queueTime.contains("10-15分钟")) return 0.7;
        if (queueTime.contains("15-20分钟")) return 0.5;
        if (queueTime.contains("需提前订座")) return 0.2;
        return 0.3;
    }

    double kidFriendlyScore(String ageSuitable, int childAge) {
        if (ageSuitable == null) return 0.5;
        if (ageSuitable.contains("全年龄")) return 1.0;
        try {
            String[] parts = ageSuitable.replaceAll("[^0-9-]", "").split("-");
            int min = Integer.parseInt(parts[0]);
            int max = parts.length > 1 ? Integer.parseInt(parts[1]) : 99;
            if (childAge >= min && childAge <= max) return 1.0;
            if (Math.abs(childAge - min) <= 3 || (parts.length > 1 && Math.abs(childAge - max) <= 3))
                return 0.7;
            return 0.3;
        } catch (Exception e) { return 0.5; }
    }

    double timeScore(String duration, String target) {
        int durMin = parseDurationMinutes(duration);
        int targetMin = parseDurationMinutes(target);
        if (targetMin == 0 || durMin == 0) return 0.5;
        double ratio = (double) durMin / targetMin;
        if (ratio >= 0.7 && ratio <= 1.3) return 1.0;
        if (ratio >= 0.5 && ratio <= 1.5) return 0.7;
        return 0.3;
    }

    double vibeScore(List<String> tags, SessionContext ctx) {
        if (tags == null || tags.isEmpty()) return 0.3;
        List<String> prefs = ctx.preferences != null ? ctx.preferences : List.of();
        if (prefs.isEmpty()) return 0.5;
        long matches = tags.stream()
            .filter(tag -> prefs.stream().anyMatch(p -> p.contains(tag) || tag.contains(p)))
            .count();
        return Math.min(1.0, 0.3 + 0.7 * (double) matches / prefs.size());
    }

    // ---- internal parsers ----

    double parseDistance(String s) {
        if (s == null || s.isEmpty()) return MAX_DISTANCE_KM;
        try { return Double.parseDouble(s.replaceAll("[^0-9.]", "")); }
        catch (NumberFormatException e) { return MAX_DISTANCE_KM; }
    }

    double parseRefDistance(String refDist) {
        if (refDist == null || refDist.isEmpty()) return 2.0;
        try {
            String num = refDist.replaceAll(".*?([0-9.]+).*", "$1");
            return Double.parseDouble(num);
        } catch (Exception e) { return 2.0; }
    }

    int parsePrice(String priceStr) {
        try { return Integer.parseInt(priceStr.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 100; }
    }

    int parseDurationMinutes(String dur) {
        if (dur == null || dur.isEmpty()) return 0;
        try {
            String[] parts = dur.split("[-h]");
            int total = 0;
            for (String p : parts) {
                p = p.trim();
                if (p.isEmpty()) continue;
                if (p.contains("min")) total += Integer.parseInt(p.replaceAll("[^0-9]", ""));
                else total += Integer.parseInt(p.replaceAll("[^0-9]", "")) * 60;
            }
            return total;
        } catch (Exception e) { return 0; }
    }
}
