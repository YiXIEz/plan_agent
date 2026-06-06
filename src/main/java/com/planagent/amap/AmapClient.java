package com.planagent.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.model.Activity;
import com.planagent.model.Restaurant;
import com.planagent.model.RouteInfo;
import com.planagent.model.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AmapClient {

    private static final Logger log = LoggerFactory.getLogger(AmapClient.class);
    private static final String AMAP_BASE = "https://restapi.amap.com";

    // Beijing center as default location for POI searches
    private static final double DEFAULT_LNG = 116.397428;
    private static final double DEFAULT_LAT = 39.908722;

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final ConcurrentHashMap<String, GeoResult> geocodeCache = new ConcurrentHashMap<>();

    public AmapClient(@Value("${amap.api-key:}") String apiKey, RestClient.Builder builder) {
        this.apiKey = apiKey;
        this.restClient = builder.build();
        this.mapper = new ObjectMapper();
        if (isEnabled()) {
            log.info("AmapClient initialized with API key");
        } else {
            log.info("AmapClient disabled — no API key configured, will use MockDataStore");
        }
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Clears the geocode cache. Exposed for testing.
     */
    void clearCache() {
        geocodeCache.clear();
    }

    // ── Weather ──────────────────────────────────────────────────────

    public WeatherInfo checkWeather(String district) {
        try {
            GeoResult geo = geocode(district);
            if (geo == null) return null;

            String json = restClient.get()
                .uri(AMAP_BASE + "/v3/weather/weatherInfo?key={key}&city={city}&extensions=base",
                    apiKey, geo.adcode)
                .retrieve()
                .body(String.class);

            JsonNode root = mapper.readTree(json);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap weather API returned status={}", root.path("status").asText());
                return null;
            }
            JsonNode live = root.path("lives").get(0);
            if (live == null) return null;

            String weather = live.path("weather").asText();
            String temp = live.path("temperature").asText() + "°C";
            boolean suitable = !weather.contains("雨") && !weather.contains("雪")
                && !weather.contains("霾") && !weather.contains("沙尘");
            return new WeatherInfo(district, weather, temp, suitable);
        } catch (Exception e) {
            log.warn("Amap weather call failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Activity Search ──────────────────────────────────────────────

    public List<Activity> searchActivities(String type, int childAge, String duration, String maxDistance) {
        try {
            String location = getDefaultLocation();
            if (location == null) return null;

            String keywords = mapActivityKeyword(type);
            int radius = parseDistanceMeters(maxDistance);

            String uri = new StringBuilder(AMAP_BASE)
                .append("/v5/place/around?key=").append(apiKey)
                .append("&location=").append(location)
                .append("&keywords=").append(keywords)
                .append("&radius=").append(radius)
                .append("&offset=20&page=1")
                .toString();

            String json = restClient.get().uri(uri).retrieve().body(String.class);

            JsonNode root = mapper.readTree(json);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap POI search returned status={}", root.path("status").asText());
                return null;
            }

            JsonNode pois = root.path("pois");
            if (pois.size() == 0) return null;

            List<Activity> results = new ArrayList<>();
            for (JsonNode poi : pois) {
                results.add(poiToActivity(poi, type));
            }
            return results;
        } catch (Exception e) {
            log.warn("Amap activity search failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Restaurant Search ────────────────────────────────────────────

    public List<Restaurant> searchRestaurants(String keyword, List<String> tags,
                                               String budget, boolean kidFriendly) {
        try {
            String location = getDefaultLocation();
            if (location == null) return null;

            int radius = 5000;
            String keywords = (keyword != null && !keyword.isEmpty()) ? keyword : null;

            StringBuilder uri = new StringBuilder(AMAP_BASE)
                .append("/v5/place/around?key=").append(apiKey)
                .append("&location=").append(location)
                .append("&types=050000")
                .append("&radius=").append(radius)
                .append("&offset=20&page=1");
            if (keywords != null) {
                uri.append("&keywords=").append(keywords);
            }

            String json = restClient.get()
                .uri(uri.toString())
                .retrieve()
                .body(String.class);

            JsonNode root = mapper.readTree(json);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap restaurant search returned status={}", root.path("status").asText());
                return null;
            }

            JsonNode pois = root.path("pois");
            if (pois.size() == 0) return null;

            List<Restaurant> results = new ArrayList<>();
            for (JsonNode poi : pois) {
                results.add(poiToRestaurant(poi));
            }
            return results;
        } catch (Exception e) {
            log.warn("Amap restaurant search failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Route Planning ───────────────────────────────────────────────

    public RouteInfo planRoute(String from, String to, String mode) {
        try {
            GeoResult fromGeo = geocode(from);
            if (fromGeo == null) return null;
            GeoResult toGeo = geocode(to);
            if (toGeo == null) return null;

            String origin = fromGeo.lng + "," + fromGeo.lat;
            String dest = toGeo.lng + "," + toGeo.lat;

            String endpoint = switch (mode) {
                case "公交" -> "/v3/direction/transit/integrated";
                case "步行" -> "/v3/direction/walking";
                default -> "/v3/direction/driving";
            };

            StringBuilder urlBuilder = new StringBuilder(AMAP_BASE)
                .append(endpoint)
                .append("?key=").append(apiKey)
                .append("&origin=").append(origin)
                .append("&destination=").append(dest);
            if ("公交".equals(mode)) {
                urlBuilder.append("&city=北京&cityd=北京");
            }

            String json = restClient.get()
                .uri(urlBuilder.toString())
                .retrieve()
                .body(String.class);

            JsonNode root = mapper.readTree(json);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap route API returned status={}", root.path("status").asText());
                return null;
            }
            JsonNode path = root.path("route").path("paths").get(0);
            if (path == null) return null;

            int distMeters = path.path("distance").asInt();
            int durationSec = path.path("duration").asInt();

            String distStr = distMeters >= 1000
                ? String.format("%.1fkm", distMeters / 1000.0)
                : distMeters + "m";
            String durationStr;
            if (durationSec >= 3600) {
                int h = durationSec / 3600;
                int m = (durationSec % 3600) / 60;
                durationStr = h + "h" + m + "min";
            } else if (durationSec >= 60) {
                durationStr = (durationSec / 60) + "分钟";
            } else {
                durationStr = durationSec + "秒";
            }

            return new RouteInfo(from, to, mode, distStr, durationStr);
        } catch (Exception e) {
            log.warn("Amap route call failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Rating Filter ────────────────────────────────────────────────

    public List<Map<String, Object>> filterByRating(List<String> ids, double minRating) {
        if (ids == null || ids.isEmpty()) return null;

        List<Map<String, Object>> results = new ArrayList<>();
        for (String id : ids) {
            try {
                String json = restClient.get()
                    .uri(AMAP_BASE + "/v3/place/detail?key={key}&id={id}", apiKey, id)
                    .retrieve()
                    .body(String.class);

                JsonNode root = mapper.readTree(json);
                if (!"1".equals(root.path("status").asText())) continue;

                JsonNode poi = root.path("pois").get(0);
                if (poi == null) continue;

                String ratingStr = poi.path("biz_ext").path("rating").asText();
                if (ratingStr.isEmpty()) continue;
                double rating = Double.parseDouble(ratingStr);
                if (rating < minRating) continue;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", poi.path("id").asText());
                item.put("name", poi.path("name").asText());
                item.put("rating", rating);
                item.put("type", poi.path("type").asText());
                results.add(item);
            } catch (Exception e) {
                // individual POI detail call failed, skip this one
            }
        }
        if (results.isEmpty()) return null;
        return results;
    }

    // ── Geocode ──────────────────────────────────────────────────────

    GeoResult geocode(String district) {
        if (district == null || district.isBlank()) return null;

        var cached = geocodeCache.get(district);
        if (cached != null) return cached;

        try {
            String json = restClient.get()
                .uri(AMAP_BASE + "/v3/geocode/geo?key={key}&address={addr}",
                    apiKey, district)
                .retrieve()
                .body(String.class);

            JsonNode root = mapper.readTree(json);
            if (!"1".equals(root.path("status").asText())
                || root.path("geocodes").size() == 0) {
                log.warn("Amap geocode failed for district={}", district);
                return null;
            }
            JsonNode geo = root.path("geocodes").get(0);
            String adcode = geo.path("adcode").asText();
            String[] loc = geo.path("location").asText().split(",");
            double lng = Double.parseDouble(loc[0]);
            double lat = Double.parseDouble(loc[1]);
            GeoResult result = new GeoResult(lng, lat, adcode);
            geocodeCache.put(district, result);
            return result;
        } catch (Exception e) {
            log.warn("Amap geocode failed for district={}: {}", district, e.getMessage());
            return null;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String getDefaultLocation() {
        // Use cached district coordinates if available, otherwise no location
        if (!geocodeCache.isEmpty()) {
            var entry = geocodeCache.values().iterator().next();
            return entry.lng + "," + entry.lat;
        }
        return null;
    }

    private String mapActivityKeyword(String type) {
        if (type == null || type.isEmpty()) {
            return "游乐园|公园|科技馆|体育馆|电影院";
        }
        if (type.contains("亲子乐园") || type.contains("手工")) return "游乐园|儿童乐园";
        if (type.contains("展览") || type.contains("科技馆")) return "科技馆|展览馆";
        if (type.contains("公园")) return "公园";
        if (type.contains("运动")) return "体育馆|运动场";
        if (type.contains("电影")) return "电影院";
        if (type.contains("户外") || type.contains("自然")) return "公园|植物园";
        return "游乐园|公园|科技馆|体育馆|电影院";
    }

    private int parseDistanceMeters(String maxDistance) {
        if (maxDistance == null || maxDistance.isEmpty()) return 5000;
        try {
            double km = Double.parseDouble(maxDistance.replaceAll("[^0-9.]", ""));
            return (int) (km * 1000);
        } catch (NumberFormatException e) {
            return 5000;
        }
    }

    private Activity poiToActivity(JsonNode poi, String searchType) {
        String name = poi.path("name").asText();
        String type = poi.path("type").asText();
        if (type.isEmpty() && searchType != null) type = searchType;

        String ratingStr = poi.path("biz_ext").path("rating").asText();
        double rating = ratingStr.isEmpty() ? 4.0 : Double.parseDouble(ratingStr);

        String distanceStr = poi.path("distance").asText();
        String distance = distanceStr.isEmpty() ? "未知"
            : (Integer.parseInt(distanceStr) >= 1000
                ? String.format("%.1fkm", Integer.parseInt(distanceStr) / 1000.0)
                : distanceStr + "m");

        String costStr = poi.path("biz_ext").path("cost").asText();
        String price = costStr.isEmpty() ? "暂无报价" : "¥" + costStr + "/人";

        String openTime = poi.path("deep_info").path("opentime").asText();
        if (openTime.isEmpty()) openTime = "未知";

        return new Activity(
            poi.path("id").asText(),
            name,
            type,
            rating,
            distance,
            "2-3小时",
            price,
            List.of(),
            "全年龄",
            openTime
        );
    }

    private Restaurant poiToRestaurant(JsonNode poi) {
        String name = poi.path("name").asText();
        String type = poi.path("type").asText();

        String ratingStr = poi.path("biz_ext").path("rating").asText();
        double rating = ratingStr.isEmpty() ? 4.0 : Double.parseDouble(ratingStr);

        String distanceStr = poi.path("distance").asText();
        String distance = distanceStr.isEmpty() ? "未知"
            : (Integer.parseInt(distanceStr) >= 1000
                ? String.format("%.1fkm", Integer.parseInt(distanceStr) / 1000.0)
                : distanceStr + "m");

        String costStr = poi.path("biz_ext").path("cost").asText();
        String avgPrice = costStr.isEmpty() ? "暂无报价" : "¥" + costStr + "/人";

        List<String> tags = new ArrayList<>();
        if (!type.isEmpty()) tags.add(type);

        return new Restaurant(
            poi.path("id").asText(),
            name,
            type.isEmpty() ? "未知菜系" : type,
            rating,
            distance,
            avgPrice,
            tags,
            false,
            false,
            "未知",
            "未知"
        );
    }

    // ── Inner class for geocode cache ────────────────────────────────

    static class GeoResult {
        final double lng;
        final double lat;
        final String adcode;

        GeoResult(double lng, double lat, String adcode) {
            this.lng = lng;
            this.lat = lat;
            this.adcode = adcode;
        }
    }
}
