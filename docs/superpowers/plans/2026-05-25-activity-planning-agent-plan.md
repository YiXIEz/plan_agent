# Activity Planning Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local short-term activity planning and execution Agent — natural language goal in, executable plan out, with automated reservation/order/notification actions.

**Architecture:** Spring Boot MVC app with WebSocket streaming. ReAct loop via LangChain4j calling DeepSeek/Qwen with OpenAI-compatible API. 10 tools backed by Mock services. Single-file HTML/JS frontend.

**Tech Stack:** Java 17, Spring Boot 3.4, LangChain4j 1.0.0-beta3, Maven, WebSocket (spring-boot-starter-websocket)

---

### Task 1: Project scaffold

**Files:**
- Create: `pom.xml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/planagent/PlanAgentApplication.java`

- [ ] **Step 1: Write pom.xml with all dependencies**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
    </parent>

    <groupId>com.planagent</groupId>
    <artifactId>plan-agent</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
        <langchain4j.version>1.0.0-beta3</langchain4j.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-core</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write application.yml**

```yaml
server:
  port: 8080

llm:
  primary: deepseek
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: https://api.deepseek.com/v1
    model-name: deepseek-chat
  qwen:
    api-key: ${QWEN_API_KEY:}
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model-name: qwen-plus

logging:
  level:
    com.planagent: DEBUG
```

- [ ] **Step 3: Write PlanAgentApplication.java**

```java
package com.planagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PlanAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanAgentApplication.class, args);
    }
}
```

- [ ] **Step 4: Verify project compiles**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.yml src/main/java/com/planagent/PlanAgentApplication.java
git commit -m "chore: scaffold Spring Boot project with LangChain4j dependencies"
```

---

### Task 2: Data model classes

**Files:**
- Create: `src/main/java/com/planagent/model/Activity.java`
- Create: `src/main/java/com/planagent/model/Restaurant.java`
- Create: `src/main/java/com/planagent/model/WeatherInfo.java`
- Create: `src/main/java/com/planagent/model/RouteInfo.java`
- Create: `src/main/java/com/planagent/model/QueueInfo.java`
- Create: `src/main/java/com/planagent/model/Reservation.java`
- Create: `src/main/java/com/planagent/model/OrderResult.java`
- Create: `src/main/java/com/planagent/model/PaymentResult.java`
- Create: `src/main/java/com/planagent/model/NotificationResult.java`
- Create: `src/test/java/com/planagent/model/ModelJsonTest.java`

- [ ] **Step 1: Write all model classes**

All models use Jackson annotations for JSON serialization (needed for Tool argument/result marshalling).

```java
// Activity.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Activity {
    @JsonProperty("id") public String id;
    @JsonProperty("name") public String name;
    @JsonProperty("type") public String type;
    @JsonProperty("rating") public double rating;
    @JsonProperty("distance") public String distance;
    @JsonProperty("duration") public String duration;
    @JsonProperty("price") public String price;
    @JsonProperty("highlights") public java.util.List<String> highlights;
    @JsonProperty("ageSuitable") public String ageSuitable;
    @JsonProperty("openTime") public String openTime;

    public Activity() {}
    public Activity(String id, String name, String type, double rating, String distance,
                    String duration, String price, java.util.List<String> highlights,
                    String ageSuitable, String openTime) {
        this.id = id; this.name = name; this.type = type; this.rating = rating;
        this.distance = distance; this.duration = duration; this.price = price;
        this.highlights = highlights; this.ageSuitable = ageSuitable; this.openTime = openTime;
    }
}
```

```java
// Restaurant.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Restaurant {
    @JsonProperty("id") public String id;
    @JsonProperty("name") public String name;
    @JsonProperty("cuisine") public String cuisine;
    @JsonProperty("rating") public double rating;
    @JsonProperty("distance") public String distance;
    @JsonProperty("avgPrice") public String avgPrice;
    @JsonProperty("tags") public java.util.List<String> tags;
    @JsonProperty("hasKidsMenu") public boolean hasKidsMenu;
    @JsonProperty("hasLowCalOption") public boolean hasLowCalOption;
    @JsonProperty("queueTime") public String queueTime;
    @JsonProperty("capacity") public String capacity;

    public Restaurant() {}
    public Restaurant(String id, String name, String cuisine, double rating, String distance,
                      String avgPrice, java.util.List<String> tags, boolean hasKidsMenu,
                      boolean hasLowCalOption, String queueTime, String capacity) {
        this.id = id; this.name = name; this.cuisine = cuisine; this.rating = rating;
        this.distance = distance; this.avgPrice = avgPrice; this.tags = tags;
        this.hasKidsMenu = hasKidsMenu; this.hasLowCalOption = hasLowCalOption;
        this.queueTime = queueTime; this.capacity = capacity;
    }
}
```

```java
// WeatherInfo.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherInfo {
    @JsonProperty("district") public String district;
    @JsonProperty("weather") public String weather;
    @JsonProperty("temperature") public String temperature;
    @JsonProperty("suitable") public boolean suitable;

    public WeatherInfo() {}
    public WeatherInfo(String district, String weather, String temperature, boolean suitable) {
        this.district = district; this.weather = weather;
        this.temperature = temperature; this.suitable = suitable;
    }
}
```

```java
// RouteInfo.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RouteInfo {
    @JsonProperty("from") public String from;
    @JsonProperty("to") public String to;
    @JsonProperty("mode") public String mode;
    @JsonProperty("distance") public String distance;
    @JsonProperty("duration") public String duration;

    public RouteInfo() {}
    public RouteInfo(String from, String to, String mode, String distance, String duration) {
        this.from = from; this.to = to; this.mode = mode;
        this.distance = distance; this.duration = duration;
    }
}
```

```java
// QueueInfo.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueueInfo {
    @JsonProperty("restaurantName") public String restaurantName;
    @JsonProperty("queueSize") public int queueSize;
    @JsonProperty("estimatedWaitMinutes") public int estimatedWaitMinutes;
    @JsonProperty("available") public boolean available;

    public QueueInfo() {}
    public QueueInfo(String restaurantName, int queueSize, int estimatedWaitMinutes, boolean available) {
        this.restaurantName = restaurantName; this.queueSize = queueSize;
        this.estimatedWaitMinutes = estimatedWaitMinutes; this.available = available;
    }
}
```

```java
// Reservation.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Reservation {
    @JsonProperty("reservationId") public String reservationId;
    @JsonProperty("restaurantName") public String restaurantName;
    @JsonProperty("timeSlot") public String timeSlot;
    @JsonProperty("partySize") public int partySize;
    @JsonProperty("queueNumber") public String queueNumber;
    @JsonProperty("status") public String status;

    public Reservation() {}
    public Reservation(String reservationId, String restaurantName, String timeSlot,
                       int partySize, String queueNumber, String status) {
        this.reservationId = reservationId; this.restaurantName = restaurantName;
        this.timeSlot = timeSlot; this.partySize = partySize;
        this.queueNumber = queueNumber; this.status = status;
    }
}
```

```java
// OrderResult.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderResult {
    @JsonProperty("orderId") public String orderId;
    @JsonProperty("productType") public String productType;
    @JsonProperty("productName") public String productName;
    @JsonProperty("quantity") public int quantity;
    @JsonProperty("totalPrice") public String totalPrice;
    @JsonProperty("deliveryTime") public String deliveryTime;
    @JsonProperty("status") public String status;

    public OrderResult() {}
    public OrderResult(String orderId, String productType, String productName, int quantity,
                       String totalPrice, String deliveryTime, String status) {
        this.orderId = orderId; this.productType = productType; this.productName = productName;
        this.quantity = quantity; this.totalPrice = totalPrice; this.deliveryTime = deliveryTime;
        this.status = status;
    }
}
```

```java
// PaymentResult.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentResult {
    @JsonProperty("paymentId") public String paymentId;
    @JsonProperty("orderId") public String orderId;
    @JsonProperty("amount") public String amount;
    @JsonProperty("status") public String status;

    public PaymentResult() {}
    public PaymentResult(String paymentId, String orderId, String amount, String status) {
        this.paymentId = paymentId; this.orderId = orderId;
        this.amount = amount; this.status = status;
    }
}
```

```java
// NotificationResult.java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationResult {
    @JsonProperty("recipient") public String recipient;
    @JsonProperty("channel") public String channel;
    @JsonProperty("status") public String status;
    @JsonProperty("preview") public String preview;

    public NotificationResult() {}
    public NotificationResult(String recipient, String channel, String status, String preview) {
        this.recipient = recipient; this.channel = channel;
        this.status = status; this.preview = preview;
    }
}
```

- [ ] **Step 2: Write JSON serialization test**

```java
// ModelJsonTest.java
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
```

- [ ] **Step 3: Run test**

```bash
mvn test -Dtest=ModelJsonTest
```

Expected: Tests pass

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/planagent/model/ src/test/java/com/planagent/model/
git commit -m "feat: add data model classes with Jackson support"
```

---

### Task 3: AgentStep and SessionContext

**Files:**
- Create: `src/main/java/com/planagent/model/AgentStep.java`
- Create: `src/main/java/com/planagent/model/SessionContext.java`

- [ ] **Step 1: Write AgentStep.java — WebSocket message DTO**

```java
package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentStep {
    public enum Type { THOUGHT, ACTION, OBSERVATION, FINAL_PLAN, CONFIRMING, DONE, ERROR }

    @JsonProperty("type") public Type type;
    @JsonProperty("content") public String content;
    @JsonProperty("tool") public String tool;
    @JsonProperty("params") public String params;

    public AgentStep() {}

    public static AgentStep thought(String content) {
        var s = new AgentStep(); s.type = Type.THOUGHT; s.content = content; return s;
    }
    public static AgentStep action(String tool, String params) {
        var s = new AgentStep(); s.type = Type.ACTION; s.tool = tool; s.params = params; return s;
    }
    public static AgentStep observation(String content) {
        var s = new AgentStep(); s.type = Type.OBSERVATION; s.content = content; return s;
    }
    public static AgentStep finalPlan(String content) {
        var s = new AgentStep(); s.type = Type.FINAL_PLAN; s.content = content; return s;
    }
    public static AgentStep confirming(String content) {
        var s = new AgentStep(); s.type = Type.CONFIRMING; s.content = content; return s;
    }
    public static AgentStep done(String content) {
        var s = new AgentStep(); s.type = Type.DONE; s.content = content; return s;
    }
    public static AgentStep error(String content) {
        var s = new AgentStep(); s.type = Type.ERROR; s.content = content; return s;
    }
}
```

- [ ] **Step 2: Write SessionContext.java**

```java
package com.planagent.model;

import java.util.*;

public class SessionContext {
    public enum Scenario { FAMILY, FRIENDS, UNKNOWN }

    public String sessionId;
    public Scenario scenario = Scenario.UNKNOWN;
    public int partySize;
    public List<String> preferences = new ArrayList<>();
    public List<Activity> candidateActivities = new ArrayList<>();
    public List<Restaurant> candidateRestaurants = new ArrayList<>();
    public String confirmedPlan;
    public Map<String, OrderResult> executedOrders = new LinkedHashMap<>();
    public Map<String, Reservation> reservations = new LinkedHashMap<>();
    public String startTime = "14:00";

    public SessionContext(String sessionId) {
        this.sessionId = sessionId;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/planagent/model/AgentStep.java src/main/java/com/planagent/model/SessionContext.java
git commit -m "feat: add AgentStep DTO and SessionContext"
```

---

### Task 4: MockDataStore — simulated database with realistic data

**Files:**
- Create: `src/main/java/com/planagent/mock/MockDataStore.java`
- Create: `src/test/java/com/planagent/mock/MockDataStoreTest.java`

- [ ] **Step 1: Write MockDataStore.java**

```java
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
        // Simulate changing queue time
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
```

- [ ] **Step 2: Write MockDataStoreTest.java**

```java
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
```

- [ ] **Step 3: Run tests**

```bash
mvn test -Dtest=MockDataStoreTest
```

Expected: All 7 tests pass

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/planagent/mock/ src/test/java/com/planagent/mock/
git commit -m "feat: add MockDataStore with realistic activity and restaurant data"
```

---

### Task 5: LLM configuration

**Files:**
- Create: `src/main/java/com/planagent/config/LLMConfig.java`

- [ ] **Step 1: Write LLMConfig.java**

```java
package com.planagent.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class LLMConfig {

    @Value("${llm.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${llm.deepseek.base-url}")
    private String deepseekBaseUrl;

    @Value("${llm.deepseek.model-name}")
    private String deepseekModel;

    @Value("${llm.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${llm.qwen.base-url}")
    private String qwenBaseUrl;

    @Value("${llm.qwen.model-name}")
    private String qwenModel;

    @Bean
    @Primary
    public ChatLanguageModel primaryModel() {
        String apiKey = !deepseekApiKey.isEmpty() ? deepseekApiKey : System.getenv("DEEPSEEK_API_KEY");
        return OpenAiChatModel.builder()
            .baseUrl(deepseekBaseUrl)
            .apiKey(apiKey)
            .modelName(deepseekModel)
            .timeout(Duration.ofSeconds(60))
            .maxRetries(2)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Bean
    public ChatLanguageModel fallbackModel() {
        String apiKey = !qwenApiKey.isEmpty() ? qwenApiKey : System.getenv("QWEN_API_KEY");
        return OpenAiChatModel.builder()
            .baseUrl(qwenBaseUrl)
            .apiKey(apiKey)
            .modelName(qwenModel)
            .timeout(Duration.ofSeconds(60))
            .maxRetries(2)
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/planagent/config/LLMConfig.java
git commit -m "feat: add LLM config for DeepSeek and Qwen via OpenAI-compatible API"
```

---

### Task 6: Tool implementations — search tools

**Files:**
- Create: `src/main/java/com/planagent/tools/ToolRegistry.java` (skeleton)
- Create: `src/main/java/com/planagent/tools/ActivitySearchTool.java`
- Create: `src/main/java/com/planagent/tools/RestaurantSearchTool.java`
- Create: `src/main/java/com/planagent/tools/WeatherTool.java`
- Create: `src/main/java/com/planagent/tools/RouteTool.java`
- Create: `src/main/java/com/planagent/tools/RatingFilterTool.java`

- [ ] **Step 1: Write ToolRegistry skeleton**

```java
package com.planagent.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ToolRegistry {
    private final Map<String, ToolExecutor> tools = new LinkedHashMap<>();
    private final Map<String, ToolSpecification> specs = new LinkedHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String argumentsJson) throws Exception;
    }

    public void register(String name, String description, Map<String, Map<String, Object>> params, ToolExecutor executor) {
        var builder = ToolSpecification.builder().name(name).description(description);
        if (params != null && !params.isEmpty()) {
            params.forEach((key, props) -> {
                String type = (String) props.getOrDefault("type", "string");
                String desc = (String) props.getOrDefault("description", "");
                java.util.List<String> enums = (java.util.List<String>) props.get("enum");
                builder.addParameter(key, type, desc, enums != null ? enums : List.of());
            });
        }
        ToolSpecification spec = builder.build();
        specs.put(name, spec);
        tools.put(name, executor);
    }

    public List<ToolSpecification> getSpecifications() {
        return new ArrayList<>(specs.values());
    }

    public String execute(ToolExecutionRequest request) {
        ToolExecutor executor = tools.get(request.name());
        if (executor == null) return "{\"error\": \"Unknown tool: " + request.name() + "\"}";
        try {
            return executor.execute(request.arguments());
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
```

- [ ] **Step 2: Write ActivitySearchTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class ActivitySearchTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public ActivitySearchTool(ToolRegistry registry, MockDataStore store) {
        registry.register("search_activities",
            "搜索附近的亲子活动/公园/展览/手工坊",
            Map.of(
                "type", Map.of("type", "string", "description", "活动类型: 亲子乐园, 展览, 公园, 手工坊, 运动, 电影"),
                "childAge", Map.of("type", "integer", "description", "孩子年龄"),
                "duration", Map.of("type", "string", "description", "预计停留时长，如2-3小时"),
                "maxDistance", Map.of("type", "string", "description", "距离上限，如5.0km")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String type = node.has("type") ? node.get("type").asText() : null;
                int age = node.has("childAge") ? node.get("childAge").asInt() : 5;
                String duration = node.has("duration") ? node.get("duration").asText() : "";
                String dist = node.has("maxDistance") ? node.get("maxDistance").asText() : "10.0km";
                var results = store.searchActivities(type, age, duration, dist);
                return mapper.writeValueAsString(results);
            });
    }
}
```

- [ ] **Step 3: Write RestaurantSearchTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class RestaurantSearchTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public RestaurantSearchTool(ToolRegistry registry, MockDataStore store) {
        registry.register("search_restaurants",
            "搜索符合条件的餐厅",
            Map.of(
                "keyword", Map.of("type", "string", "description", "搜索关键词，如粤菜、轻食"),
                "tags", Map.of("type", "array", "description", "菜系标签，如轻食、粤菜、亲子友好"),
                "budget", Map.of("type", "string", "description", "预算档次", "enum", List.of("经济", "中等", "高端")),
                "kidFriendly", Map.of("type", "boolean", "description", "是否需要亲子友好")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String keyword = node.has("keyword") ? node.get("keyword").asText() : null;
                List<String> tags = node.has("tags") ? mapper.convertValue(node.get("tags"), List.class) : null;
                String budget = node.has("budget") ? node.get("budget").asText() : null;
                boolean kidFriendly = node.has("kidFriendly") && node.get("kidFriendly").asBoolean();
                var results = store.searchRestaurants(keyword, tags, budget, kidFriendly);
                return mapper.writeValueAsString(results);
            });
    }
}
```

- [ ] **Step 4: Write WeatherTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WeatherTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public WeatherTool(ToolRegistry registry, MockDataStore store) {
        registry.register("check_weather",
            "查询指定区域下午天气",
            Map.of("district", Map.of("type", "string", "description", "区域名称，如朝阳区")),
            (args) -> {
                var node = mapper.readTree(args);
                String district = node.has("district") ? node.get("district").asText() : "北京";
                return mapper.writeValueAsString(store.checkWeather(district));
            });
    }
}
```

- [ ] **Step 5: Write RouteTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class RouteTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public RouteTool(ToolRegistry registry, MockDataStore store) {
        registry.register("plan_route",
            "规划两点间路线和时间",
            Map.of(
                "from", Map.of("type", "string", "description", "起点"),
                "to", Map.of("type", "string", "description", "终点"),
                "mode", Map.of("type", "string", "description", "出行方式", "enum", java.util.List.of("驾车", "公交", "步行"))
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String from = node.get("from").asText();
                String to = node.get("to").asText();
                String mode = node.has("mode") ? node.get("mode").asText() : "驾车";
                return mapper.writeValueAsString(store.planRoute(from, to, mode));
            });
    }
}
```

- [ ] **Step 6: Write RatingFilterTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class RatingFilterTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public RatingFilterTool(ToolRegistry registry, MockDataStore store) {
        registry.register("filter_by_rating",
            "按评分筛选商户",
            Map.of(
                "ids", Map.of("type", "array", "description", "商户ID列表"),
                "minRating", Map.of("type", "number", "description", "最低评分，如4.5")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                List<String> ids = mapper.convertValue(node.get("ids"), List.class);
                double minRating = node.get("minRating").asDouble();
                return mapper.writeValueAsString(store.filterByRating(ids, minRating));
            });
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/planagent/tools/
git commit -m "feat: add search tools (activity, restaurant, weather, route, rating)"
```

---

### Task 7: Tool implementations — action tools

**Files:**
- Create: `src/main/java/com/planagent/tools/QueueCheckTool.java`
- Create: `src/main/java/com/planagent/tools/ReservationTool.java`
- Create: `src/main/java/com/planagent/tools/OrderTool.java`
- Create: `src/main/java/com/planagent/tools/PaymentTool.java`
- Create: `src/main/java/com/planagent/tools/NotificationTool.java`

- [ ] **Step 1: Write QueueCheckTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class QueueCheckTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public QueueCheckTool(ToolRegistry registry, MockDataStore store) {
        registry.register("check_queue",
            "查询餐厅当前排队或空位情况",
            Map.of(
                "restaurantId", Map.of("type", "string", "description", "餐厅ID"),
                "timeSlot", Map.of("type", "string", "description", "到店时间段，如17:00"),
                "partySize", Map.of("type", "integer", "description", "用餐人数")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String restId = node.get("restaurantId").asText();
                String time = node.has("timeSlot") ? node.get("timeSlot").asText() : "17:00";
                int size = node.has("partySize") ? node.get("partySize").asInt() : 3;
                return mapper.writeValueAsString(store.checkQueue(restId, time, size));
            });
    }
}
```

- [ ] **Step 2: Write ReservationTool**

```java
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
```

- [ ] **Step 3: Write OrderTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OrderTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderTool(ToolRegistry registry, MockDataStore store) {
        registry.register("place_order",
            "下单预订门票、蛋糕、鲜花等",
            Map.of(
                "productType", Map.of("type", "string", "description", "商品类型", "enum", java.util.List.of("门票", "蛋糕", "鲜花")),
                "merchantId", Map.of("type", "string", "description", "商户ID"),
                "quantity", Map.of("type", "integer", "description", "数量"),
                "note", Map.of("type", "string", "description", "备注，如送达时间、送达地址")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String type = node.get("productType").asText();
                String merchant = node.has("merchantId") ? node.get("merchantId").asText() : "";
                int qty = node.has("quantity") ? node.get("quantity").asInt() : 1;
                String note = node.has("note") ? node.get("note").asText() : "";
                return mapper.writeValueAsString(store.placeOrder(type, merchant, qty, note));
            });
    }
}
```

- [ ] **Step 4: Write PaymentTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaymentTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public PaymentTool(ToolRegistry registry, MockDataStore store) {
        registry.register("pay",
            "完成订单支付",
            Map.of("orderId", Map.of("type", "string", "description", "订单ID")),
            (args) -> {
                var node = mapper.readTree(args);
                String orderId = node.get("orderId").asText();
                return mapper.writeValueAsString(store.pay(orderId));
            });
    }
}
```

- [ ] **Step 5: Write NotificationTool**

```java
package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.mock.MockDataStore;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class NotificationTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public NotificationTool(ToolRegistry registry, MockDataStore store) {
        registry.register("send_notification",
            "将计划发送给指定联系人",
            Map.of(
                "recipient", Map.of("type", "string", "description", "接收人，如老婆、朋友"),
                "message", Map.of("type", "string", "description", "通知消息内容")
            ),
            (args) -> {
                var node = mapper.readTree(args);
                String recipient = node.get("recipient").asText();
                String message = node.get("message").asText();
                return mapper.writeValueAsString(store.sendNotification(recipient, message));
            });
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/planagent/tools/
git commit -m "feat: add action tools (queue, reservation, order, payment, notification)"
```

---

### Task 8: AgentLoop — ReAct core

**Files:**
- Create: `src/main/java/com/planagent/agent/AgentLoop.java`
- Create: `src/test/java/com/planagent/agent/AgentLoopTest.java`

- [ ] **Step 1: Write AgentLoop.java**

```java
package com.planagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.model.*;
import com.planagent.tools.ToolRegistry;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

@Component
public class AgentLoop {
    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final int MAX_ROUNDS = 10;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ChatLanguageModel primaryModel;
    private final ChatLanguageModel fallbackModel;
    private final ToolRegistry toolRegistry;

    private static final String SYSTEM_PROMPT = """
        你是一个本地短时活动规划助手。用户想在下午安排4-6小时的休闲活动。
        
        你需要:
        1. 理解用户场景(家庭/朋友、人数、偏好、年龄等)
        2. 按顺序规划: 查天气 → 找活动 → 找餐厅 → 查排队/路线 → 汇总方案
        3. 生成最终方案给用户确认，方案需包含时间线、活动详情、餐厅选项、路线建议
        4. 用户确认后，执行下单/预订/通知等操作
        
        规则:
        - 每次只调用一个工具，等待结果后再决定下一步
        - 家庭场景优先考虑: 亲子友好、安全、适合孩子年龄、有轻食选项
        - 距离控制在5km以内，除非特别说明
        - 最终方案必须包含时间线和备选选项
        - 输出最终方案时，不要说"需要我确认"，直接展示完整计划
        """;

    public AgentLoop(@Qualifier("primaryModel") ChatLanguageModel primaryModel,
                     @Qualifier("fallbackModel") ChatLanguageModel fallbackModel,
                     ToolRegistry toolRegistry) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.toolRegistry = toolRegistry;
    }

    public Flux<AgentStep> execute(String userGoal) {
        return Flux.create(sink -> {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(SYSTEM_PROMPT));
            messages.add(UserMessage.from(userGoal));

            var specs = toolRegistry.getSpecifications();
            ChatLanguageModel model = primaryModel;

            for (int round = 0; round < MAX_ROUNDS; round++) {
                try {
                    var response = model.generate(messages, specs);
                    AiMessage aiMessage = response.content();

                    if (aiMessage.hasToolExecutionRequests()) {
                        String thought = aiMessage.text();
                        if (thought != null && !thought.isBlank()) {
                            sink.next(AgentStep.thought(thought));
                        }

                        for (var req : aiMessage.toolExecutionRequests()) {
                            sink.next(AgentStep.action(req.name(), req.arguments()));

                            String result = toolRegistry.execute(req);
                            sink.next(AgentStep.observation(result));

                            messages.add(aiMessage);
                            messages.add(ToolExecutionResultMessage.from(req, result));
                        }
                    } else {
                        sink.next(AgentStep.finalPlan(aiMessage.text()));
                        sink.complete();
                        return;
                    }
                } catch (Exception e) {
                    log.error("Agent loop error at round {}: {}", round, e.getMessage());
                    if (model == primaryModel) {
                        log.info("Switching to fallback model");
                        model = fallbackModel;
                        sink.next(AgentStep.error("主模型异常，已切换到备用模型"));
                        round--; // retry with fallback
                    } else {
                        sink.next(AgentStep.error("规划失败: " + e.getMessage()));
                        sink.complete();
                        return;
                    }
                }
            }
            sink.next(AgentStep.error("超过最大规划轮数"));
            sink.complete();
        });
    }

    public Flux<AgentStep> confirm(SessionContext session) {
        return Flux.create(sink -> {
            String plan = session.confirmedPlan;
            var specs = toolRegistry.getSpecifications();
            String confirmPrompt = "用户已确认以下计划，请执行所有需要的操作(预订、下单、支付、通知):\n\n" + plan;

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(SYSTEM_PROMPT));
            messages.add(UserMessage.from(confirmPrompt));

            try {
                var response = primaryModel.generate(messages, specs);
                AiMessage aiMessage = response.content();

                if (aiMessage.hasToolExecutionRequests()) {
                    for (var req : aiMessage.toolExecutionRequests()) {
                        sink.next(AgentStep.confirming("正在执行: " + req.name()));
                        String result = toolRegistry.execute(req);
                        log.info("Execution result: {}", result);
                    }
                }
                sink.next(AgentStep.done("🎉 全部搞定! 计划已安排好，通知已发送。"));
                sink.complete();
            } catch (Exception e) {
                sink.next(AgentStep.error("执行失败: " + e.getMessage()));
                sink.complete();
            }
        });
    }
}
```

- [ ] **Step 2: Write AgentLoopTest.java**

```java
package com.planagent.agent;

import com.planagent.config.LLMConfig;
import com.planagent.mock.MockDataStore;
import com.planagent.model.AgentStep;
import com.planagent.tools.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class AgentLoopTest {
    private AgentLoop agentLoop;
    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        var store = new MockDataStore();
        registry = new ToolRegistry();
        // Register all tools
        new ActivitySearchTool(registry, store);
        new RestaurantSearchTool(registry, store);
        new WeatherTool(registry, store);
        new RouteTool(registry, store);
        new RatingFilterTool(registry, store);
        new QueueCheckTool(registry, store);
        new ReservationTool(registry, store);
        new OrderTool(registry, store);
        new PaymentTool(registry, store);
        new NotificationTool(registry, store);
        // Verify all 10 tools registered
        assert registry.getSpecifications().size() == 10;
    }

    @Test
    void allTenToolsRegistered() {
        var specs = registry.getSpecifications();
        assertEquals(10, specs.size());
        var names = specs.stream().map(s -> s.name()).sorted().toList();
        assertEquals(List.of("check_queue", "check_weather", "filter_by_rating",
            "make_reservation", "pay", "place_order", "plan_route",
            "search_activities", "search_restaurants", "send_notification"), names.stream().sorted().toList());
    }
}
```

Note: The full AgentLoop integration test requires a real LLM — it will be validated manually via the Web UI.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/planagent/agent/ src/test/java/com/planagent/agent/
git commit -m "feat: add AgentLoop ReAct implementation with tool dispatch"
```

---

### Task 9: WebSocket configuration and handler

**Files:**
- Create: `src/main/java/com/planagent/config/WebSocketConfig.java`
- Create: `src/main/java/com/planagent/controller/ChatWebSocketHandler.java`

- [ ] **Step 1: Write WebSocketConfig.java**

```java
package com.planagent.config;

import com.planagent.controller.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler handler;

    public WebSocketConfig(ChatWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat").setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: Write ChatWebSocketHandler.java**

```java
package com.planagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.agent.AgentLoop;
import com.planagent.model.AgentStep;
import com.planagent.model.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final AgentLoop agentLoop;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, new SessionContext(sessionId));
        wsSessions.put(sessionId, session);
        log.info("WebSocket connected: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String payload = message.getPayload();
        var node = mapper.readTree(payload);
        String type = node.has("type") ? node.get("type").asText() : "goal";
        String content = node.has("content") ? node.get("content").asText() : "";
        String sessionId = wsSession.getId();
        SessionContext ctx = sessions.get(sessionId);

        if ("confirm".equals(type)) {
            ctx.confirmedPlan = content;
            agentLoop.confirm(ctx)
                .doOnNext(step -> send(wsSession, step))
                .doOnError(e -> send(wsSession, AgentStep.error(e.getMessage())))
                .doFinally(s -> log.info("Confirmation done: {}", sessionId))
                .subscribe();
        } else {
            agentLoop.execute(content)
                .doOnNext(step -> {
                    if (step.type == AgentStep.Type.FINAL_PLAN) {
                        ctx.confirmedPlan = step.content;
                    }
                    send(wsSession, step);
                })
                .doOnError(e -> send(wsSession, AgentStep.error(e.getMessage())))
                .doFinally(s -> log.info("Planning done: {}", sessionId))
                .subscribe();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String id = session.getId();
        sessions.remove(id);
        wsSessions.remove(id);
    }

    private void send(WebSocketSession wsSession, AgentStep step) {
        try {
            String json = mapper.writeValueAsString(step);
            wsSession.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send WebSocket message", e);
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/planagent/config/WebSocketConfig.java src/main/java/com/planagent/controller/ChatWebSocketHandler.java
git commit -m "feat: add WebSocket handler with planning and confirmation flows"
```

---

### Task 10: REST controller

**Files:**
- Create: `src/main/java/com/planagent/controller/ChatController.java`

- [ ] **Step 1: Write ChatController.java**

```java
package com.planagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "plan-agent");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/planagent/controller/ChatController.java
git commit -m "feat: add health check REST endpoint"
```

---

### Task 11: Web UI — single-file frontend

**Files:**
- Create: `src/main/resources/static/index.html`

- [ ] **Step 1: Write index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>周末活动规划助手</title>
<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f5f5f5; height: 100vh; display: flex; justify-content: center; align-items: center; }
#app { width: 100%; max-width: 700px; height: 90vh; background: #fff; border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,0.1); display: flex; flex-direction: column; overflow: hidden; }
#header { padding: 16px 20px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; background: #fff; }
#header h1 { font-size: 18px; color: #333; }
#header button { padding: 6px 14px; border: 1px solid #ddd; border-radius: 6px; background: #fff; cursor: pointer; font-size: 13px; }
#messages { flex: 1; overflow-y: auto; padding: 16px 20px; }
.msg { margin-bottom: 12px; max-width: 85%; line-height: 1.6; }
.msg.user { margin-left: auto; background: #1677ff; color: #fff; padding: 10px 14px; border-radius: 10px 10px 0 10px; }
.msg.thought { color: #888; font-size: 13px; font-style: italic; padding: 4px 0; }
.msg.action { color: #1677ff; font-size: 13px; font-family: monospace; background: #f0f5ff; padding: 6px 10px; border-radius: 6px; }
.msg.observation { color: #52c41a; font-size: 13px; background: #f6ffed; padding: 8px 12px; border-radius: 6px; white-space: pre-wrap; }
.msg.plan { background: #fffbe6; border: 1px solid #ffe58f; padding: 14px 16px; border-radius: 8px; }
.msg.plan h3 { margin-bottom: 8px; }
.msg.plan .actions { margin-top: 10px; display: flex; gap: 8px; }
.msg.plan .actions button { padding: 6px 16px; border-radius: 6px; cursor: pointer; border: none; font-size: 14px; }
.btn-confirm { background: #1677ff; color: #fff; }
.btn-adjust { background: #fff; border: 1px solid #d9d9d9 !important; color: #333; }
.msg.done { background: #f6ffed; border: 1px solid #b7eb8f; padding: 12px 14px; border-radius: 8px; }
.msg.error { color: #ff4d4f; background: #fff2f0; padding: 8px 12px; border-radius: 6px; }
.msg.confirming { color: #faad14; font-size: 13px; }
.msg.loading { color: #bbb; font-size: 13px; }
#input-area { padding: 12px 20px; border-top: 1px solid #eee; display: flex; gap: 8px; }
#input-area input { flex: 1; padding: 10px 14px; border: 1px solid #d9d9d9; border-radius: 8px; font-size: 14px; outline: none; }
#input-area input:focus { border-color: #1677ff; }
#input-area button { padding: 10px 20px; background: #1677ff; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; }
#input-area button:disabled { background: #b3d4ff; cursor: not-allowed; }
</style>
</head>
<body>
<div id="app">
  <div id="header">
    <h1>🎯 周末活动规划助手</h1>
    <button onclick="location.reload()">新对话</button>
  </div>
  <div id="messages"></div>
  <div id="input-area">
    <input type="text" id="userInput" placeholder="输入你想安排的活动...">
    <button id="sendBtn" onclick="sendMessage()">发送</button>
  </div>
</div>

<script>
let ws;
let planningInProgress = false;

function connect() {
  ws = new WebSocket("ws://" + location.host + "/ws/chat");
  ws.onmessage = (event) => {
    const step = JSON.parse(event.data);
    render(step);
  };
  ws.onclose = () => { planningInProgress = false; };
  ws.onerror = () => { planningInProgress = false; };
}

function sendMessage() {
  const input = document.getElementById("userInput");
  const text = input.value.trim();
  if (!text || planningInProgress) return;

  planningInProgress = true;
  document.getElementById("sendBtn").disabled = true;
  addMessage("user", text);
  input.value = "";

  // Determine if this is a confirmation
  if (text.includes("确认") || text.includes("可以") || text.includes("好的") || text === "是") {
    ws.send(JSON.stringify({ type: "confirm", content: text }));
  } else {
    ws.send(JSON.stringify({ content: text }));
  }
}

function render(step) {
  const msgs = document.getElementById("messages");
  switch (step.type) {
    case "THOUGHT":
      addMessage("thought", "🧠 " + step.content); break;
    case "ACTION":
      addMessage("action", "🔧 " + step.tool + "(" + step.params + ")"); break;
    case "OBSERVATION":
      addMessage("observation", "👁️ " + formatJson(step.content)); break;
    case "FINAL_PLAN":
      addPlanCard(step.content); break;
    case "CONFIRMING":
      addMessage("confirming", "⏳ " + step.content); break;
    case "DONE":
      addMessage("done", step.content); planningInProgress = false;
      document.getElementById("sendBtn").disabled = false; break;
    case "ERROR":
      addMessage("error", "❌ " + step.content);
      planningInProgress = false;
      document.getElementById("sendBtn").disabled = false; break;
  }
  msgs.scrollTop = msgs.scrollHeight;
}

function addMessage(cls, text) {
  const div = document.createElement("div");
  div.className = "msg " + cls;
  div.textContent = text;
  document.getElementById("messages").appendChild(div);
}

function addPlanCard(content) {
  const div = document.createElement("div");
  div.className = "msg plan";
  div.innerHTML = '<h3>📋 方案</h3><pre style="white-space:pre-wrap;font-family:inherit;margin-bottom:8px">' + content + '</pre>' +
    '<div class="actions"><button class="btn-confirm" onclick="confirmPlan()">✅ 确认方案</button>' +
    '<button class="btn-adjust" onclick="adjustPlan()">🔄 调整方案</button></div>';
  document.getElementById("messages").appendChild(div);
  planningInProgress = false;
  document.getElementById("sendBtn").disabled = false;
}

function confirmPlan() {
  ws.send(JSON.stringify({ type: "confirm", content: "确认执行此方案" }));
  planningInProgress = true;
  document.getElementById("sendBtn").disabled = true;
}

function adjustPlan() {
  document.getElementById("userInput").value = "请调整方案：";
  document.getElementById("userInput").focus();
}

function formatJson(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2); } catch(e) { return str; }
}

document.getElementById("userInput").addEventListener("keydown", (e) => {
  if (e.key === "Enter") sendMessage();
});

connect();
</script>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add single-file chat Web UI with WebSocket streaming"
```

---

### Task 12: End-to-end verification

**Files:** No new files — verification only.

- [ ] **Step 1: Build the project**

```bash
mvn clean package -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Set API keys and start the server**

```bash
export DEEPSEEK_API_KEY=your_key_here
export QWEN_API_KEY=your_key_here
java -jar target/plan-agent-1.0.0-SNAPSHOT.jar
```

Expected: Server starts on port 8080

- [ ] **Step 3: Open browser at http://localhost:8080**

- Verify the chat UI loads
- Type: "下午带老婆孩子出去玩，孩子5岁，老婆在减肥，4-6小时，离家近"
- Verify: real-time Thought → Action → Observation streaming
- Verify: final plan card with confirm/adjust buttons
- Click confirm and verify: execution steps + DONE message

- [ ] **Step 4: Commit final state**

```bash
git add -A
git commit -m "chore: finalize project structure after e2e verification"
```

---
