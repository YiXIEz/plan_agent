package com.planagent.mock;

import com.planagent.model.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class MockDataStore {

    private final List<Activity> activities = new ArrayList<>();
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final Random rng = ThreadLocalRandom.current();

    public MockDataStore() {
        initActivities();
        initRestaurants();
    }

    private void initActivities() {
        activities.add(new Activity("act-001", "奇乐亲子农场", "亲子乐园", 4.8, "2.3km",
            "2-3小时", "￥128/人（儿童半价）",
            List.of("萌宠喂养", "采摘体验", "儿童沙池", "家长休息区"), "3-10岁", "09:00-18:00"));
        activities.add(new Activity("act-002", "阳光科技馆", "展览/科技馆", 4.6, "4.1km",
            "2-3小时", "￥60/人（1.2m以下免费）",
            List.of("儿童科学区", "互动实验", "球幕影院", "天文观测"), "4-12岁", "09:30-17:30"));
        activities.add(new Activity("act-003", "绿洲公园", "公园/户外", 4.5, "1.8km",
            "1.5-2小时", "免费",
            List.of("人工湖", "儿童游乐区", "自行车道", "野餐草坪"), "全年龄", "全天"));
        activities.add(new Activity("act-004", "彩虹手工坊", "手工/亲子DIY", 4.7, "3.0km",
            "1.5-2小时", "￥88/组",
            List.of("陶艺体验", "绘画课堂", "烘焙DIY", "作品可带走"), "4-12岁", "10:00-18:00"));
        activities.add(new Activity("act-005", "欢乐蹦床公园", "运动/室内", 4.4, "5.2km",
            "1.5-2小时", "￥98/人",
            List.of("自由蹦床区", "海绵池", "攀岩墙", "亲子互动区"), "3岁以上", "09:00-21:00"));
        activities.add(new Activity("act-006", "小森林植物园", "自然/户外", 4.6, "4.8km",
            "2-3小时", "￥30/人",
            List.of("热带温室", "蝴蝶馆", "亲子种植体验", "科普讲解"), "全年龄", "08:00-17:00"));
        activities.add(new Activity("act-007", "星光影城亲子厅", "电影/室内", 4.3, "1.5km",
            "2小时", "￥50/人",
            List.of("亲子影厅", "儿童座椅", "低音量播放", "亲子套餐"), "3岁以上", "10:00-22:00"));
    }

    private void initRestaurants() {
        restaurants.add(new Restaurant("rest-001", "绿野轻食餐厅", "轻食/简餐", 4.6, "距奇乐亲子农场0.5km",
            "￥65/人", List.of("轻食", "亲子友好", "有机"), true, true, "10-15分钟", "可容纳4人"));
        restaurants.add(new Restaurant("rest-002", "粤味轩", "粤菜", 4.7, "距绿洲公园0.8km",
            "￥80/人", List.of("粤菜", "清淡", "有包间"), true, true, "0-5分钟", "可容纳4-8人"));
        restaurants.add(new Restaurant("rest-003", "元气亲子餐厅", "融合菜", 4.5, "距阳光科技馆0.3km",
            "￥100/人", List.of("亲子餐厅", "儿童游乐角", "低卡菜单"), true, true, "5-10分钟", "可容纳4人"));
        restaurants.add(new Restaurant("rest-004", "花园日料", "日料", 4.8, "距彩虹手工坊1.0km",
            "￥120/人", List.of("日料", "精致", "安静"), false, true, "需提前订座", "可容纳2-4人"));
        restaurants.add(new Restaurant("rest-005", "必胜客欢乐餐厅", "西式简餐", 4.2, "距欢乐蹦床公园0.5km",
            "￥55/人", List.of("披萨", "亲子套餐", "快速"), true, false, "无需排队", "可容纳4-6人"));
        restaurants.add(new Restaurant("rest-006", "天府小馆", "川菜", 4.4, "距科技馆1.2km",
            "￥70/人", List.of("川菜", "麻辣", "热闹"), false, false, "15-20分钟", "可容纳4人"));
        restaurants.add(new Restaurant("rest-007", "和风拉面屋", "日式拉面", 4.3, "距植物园0.6km",
            "￥45/人", List.of("拉面", "快捷", "一人食友好"), false, false, "0-5分钟", "可容纳4人"));
        restaurants.add(new Restaurant("rest-008", "素心素食坊", "素食/轻食", 4.6, "距绿洲公园1.0km",
            "￥60/人", List.of("素食", "轻食", "有机", "低卡"), true, true, "无需排队", "可容纳4人"));
    }

    public List<Activity> searchActivities(String type, int childAge, String duration, String maxDistance) {
        double maxDistKm = parseDistance(maxDistance);
        return activities.stream()
            .filter(a -> type == null || type.isEmpty() || a.type.contains(type))
            .filter(a -> isAgeSuitable(a.ageSuitable, childAge))
            .filter(a -> parseDistance(a.distance) <= maxDistKm)
            .sorted(Comparator.comparingDouble(a -> -a.rating))
            .collect(Collectors.toList());
    }

    public List<Restaurant> searchRestaurants(String keyword, List<String> tags, String budget, boolean kidFriendly) {
        return restaurants.stream()
            .filter(r -> keyword == null || keyword.isEmpty() || r.name.contains(keyword) || r.cuisine.contains(keyword))
            .filter(r -> tags == null || tags.isEmpty() || r.tags.stream().anyMatch(tags::contains))
            .filter(r -> !kidFriendly || r.hasKidsMenu)
            .filter(r -> budget == null || matchesBudget(r.avgPrice, budget))
            .sorted(Comparator.comparingDouble(r -> -r.rating))
            .collect(Collectors.toList());
    }

    public Activity getActivity(String id) {
        return activities.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
    }

    public Restaurant getRestaurant(String id) {
        return restaurants.stream().filter(r -> r.id.equals(id)).findFirst().orElse(null);
    }

    public QueueInfo checkQueue(String restaurantId, String timeSlot, int partySize) {
        Restaurant r = getRestaurant(restaurantId);
        if (r == null) return new QueueInfo("未知", 0, 0, false);
        int wait = "无需排队".equals(r.queueTime) ? rng.nextInt(5)
            : r.queueTime.contains("需提前订座") ? 45 + rng.nextInt(30)
            : Integer.parseInt(r.queueTime.replaceAll("[^0-9]", "").split("-")[0]) + rng.nextInt(15);
        return new QueueInfo(r.name, wait > 0 ? rng.nextInt(5) + 1 : 0, wait, wait < 30);
    }

    public WeatherInfo checkWeather(String district) {
        String[] weathers = {"晴", "晴转多云", "多云", "阴", "小雨"};
        String weather = weathers[rng.nextInt(weathers.length)];
        int temp = 20 + rng.nextInt(15);
        boolean suitable = !weather.contains("雨") && !weather.equals("阴") || rng.nextDouble() > 0.5;
        return new WeatherInfo(district, weather, temp + "°C", suitable);
    }

    public RouteInfo planRoute(String from, String to, String mode) {
        double dist = 1.0 + rng.nextDouble() * 10;
        int durationMin;
        switch (mode) {
            case "驾车": durationMin = (int)(dist * 3 + rng.nextInt(10)); break;
            case "公交": durationMin = (int)(dist * 5 + rng.nextInt(20)); break;
            default: durationMin = (int)(dist * 10 + rng.nextInt(5)); break;
        }
        return new RouteInfo(from, to, mode,
            String.format("%.1fkm", dist),
            durationMin > 60 ? (durationMin / 60 + "h" + durationMin % 60 + "min") : durationMin + "分钟");
    }

    public List<?> filterByRating(List<String> ids, double minRating) {
        var result = new ArrayList<>();
        for (String id : ids) {
            Activity a = getActivity(id);
            if (a != null && a.rating >= minRating) { result.add(a); continue; }
            Restaurant r = getRestaurant(id);
            if (r != null && r.rating >= minRating) result.add(r);
        }
        return result;
    }

    public Reservation makeReservation(String restaurantId, String timeSlot, int partySize, String note) {
        Restaurant r = getRestaurant(restaurantId);
        if (r == null) return new Reservation("", "未知", timeSlot, partySize, "", "失败: 餐厅不存在");
        String id = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new Reservation(id, r.name, timeSlot, partySize, "A" + (rng.nextInt(50) + 1), "已预订");
    }

    public OrderResult placeOrder(String productType, String merchantId, int quantity, String note) {
        String id = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String name;
        String price;
        switch (productType) {
            case "门票": name = "亲子门票"; price = "￥" + (128 * quantity); break;
            case "蛋糕": name = "生日蛋糕"; price = "￥" + (198 * quantity); break;
            case "鲜花": name = "鲜花束"; price = "￥" + (168 * quantity); break;
            default: name = productType; price = "￥100";
        }
        return new OrderResult(id, productType, name, quantity, price, "预计送达: 17:15", "已下单");
    }

    public PaymentResult pay(String orderId) {
        return new PaymentResult("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            orderId, "已支付", "支付成功");
    }

    public NotificationResult sendNotification(String recipient, String message) {
        return new NotificationResult(recipient, "微信/短信", "已发送",
            message.length() > 60 ? message.substring(0, 60) + "..." : message);
    }

    private double parseDistance(String s) {
        if (s == null || s.isEmpty()) return 10.0;
        try { return Double.parseDouble(s.replaceAll("[^0-9.]", "")); } catch (NumberFormatException e) { return 10.0; }
    }

    private boolean isAgeSuitable(String range, int age) {
        if (range.contains("全年龄")) return true;
        try {
            String[] parts = range.replaceAll("[^0-9-]", "").split("-");
            int min = Integer.parseInt(parts[0]), max = Integer.parseInt(parts[1]);
            return age >= min && age <= max;
        } catch (Exception e) { return true; }
    }

    private boolean matchesBudget(String priceStr, String budget) {
        int price = Integer.parseInt(priceStr.replaceAll("[^0-9]", ""));
        return switch (budget) {
            case "经济" -> price <= 60;
            case "中等" -> price <= 100;
            case "高端" -> price > 100;
            default -> true;
        };
    }
}
