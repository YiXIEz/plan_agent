# Phase 2 — 需手动应用的现有文件修改

沙箱限制无法编辑 existing files。以下是每个需要修改的文件的完整变更。

---

## 1. AgentLoop.java

**路径**: `src/main/java/com/planagent/agent/AgentLoop.java`

### 1a. 修改 import 和字段声明

替换文件顶部的 import 块和类声明开头：

```java
package com.planagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.agent.ReplanningHandler;
import com.planagent.agent.ReplanningHandler.FailureType;
import com.planagent.model.*;
import com.planagent.tools.ToolRegistry;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

@Component
public class AgentLoop {
    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final int MAX_ROUNDS = 15;
    private static final int MAX_REPLANS = 3;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ChatLanguageModel primaryModel;
    private final ChatLanguageModel fallbackModel;
    private final ToolRegistry toolRegistry;
    private final ReplanningHandler replanningHandler;
```

### 1b. 修改构造函数

将构造函数改为：

```java
    public AgentLoop(@Qualifier("primaryModel") ChatLanguageModel primaryModel,
                     @Qualifier("fallbackModel") ChatLanguageModel fallbackModel,
                     ToolRegistry toolRegistry,
                     ReplanningHandler replanningHandler) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.toolRegistry = toolRegistry;
        this.replanningHandler = replanningHandler;
    }
```

### 1c. 修改 execute() 中的 toolRegistry 调用

在 `execute()` 方法中，找到:
```java
String result = toolRegistry.execute(req);
```
替换为:
```java
String result = toolRegistry.execute(req, ctx);
```

### 1d. 替换整个 confirm() 方法

将方法签名从 `confirm(SessionContext session)` 改为 `confirm(SessionContext session, String userMessage)`，并用以下实现替换：

```java
    public Flux<AgentStep> confirm(SessionContext session, String userMessage) {
        return Flux.create(sink -> {
            String currentPrompt = "用户已确认以下计划，请执行所有需要的操作(预订、下单、支付、通知):\n\n"
                + session.confirmedPlan + "\n\n用户消息: " + userMessage;
            var specs = toolRegistry.getSpecifications();
            int replanCount = 0;
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(SYSTEM_PROMPT));
            messages.add(UserMessage.from(currentPrompt));

            while (replanCount <= MAX_REPLANS) {
                try {
                    ChatRequest request = ChatRequest.builder()
                        .messages(messages)
                        .toolSpecifications(specs)
                        .build();
                    ChatResponse response = primaryModel.chat(request);
                    AiMessage aiMessage = response.aiMessage();

                    if (aiMessage.hasToolExecutionRequests()) {
                        boolean allSuccess = true;
                        for (var req : aiMessage.toolExecutionRequests()) {
                            sink.next(AgentStep.confirming("正在执行: " + req.name()));
                            String result = toolRegistry.execute(req, session);
                            log.info("Execution result: {}", result);

                            messages.add(aiMessage);
                            messages.add(ToolExecutionResultMessage.from(req, result));

                            FailureType failure = replanningHandler.analyze(result);

                            if (replanningHandler.isFailure(failure)) {
                                allSuccess = false;
                                if (replanCount >= MAX_REPLANS) {
                                    sink.next(AgentStep.error(
                                        "自动调整" + MAX_REPLANS + "次仍失败，请手动修改方案"));
                                    sink.complete();
                                    return;
                                }
                                replanCount++;
                                String replanPrompt = replanningHandler
                                    .generateReplanPromptWithExpandedSearch(
                                        req.name(), "目标项", "失败: " + result,
                                        session.confirmedPlan, replanCount);
                                messages.add(UserMessage.from(replanPrompt));
                                break;
                            }
                        }
                        if (allSuccess) {
                            sink.next(AgentStep.done("全部搞定! 计划已安排好，通知已发送。"));
                            sink.complete();
                            return;
                        }
                    } else {
                        messages.add(aiMessage);
                        sink.next(AgentStep.done(aiMessage.text()));
                        sink.complete();
                        return;
                    }
                } catch (Exception e) {
                    log.error("Confirmation error: {}", e.getMessage());
                    sink.next(AgentStep.error("执行失败: " + e.getMessage()));
                    sink.complete();
                    return;
                }
            }
        });
    }
```

---

## 2. ChatService.java

**路径**: `src/main/java/com/planagent/service/ChatService.java`

### 2a. 添加 userMessage 参数到 confirm()

修改 `confirm` 方法签名并添加重载：

```java
    /**
     * Execute confirmed plan actions with user message, persisting to database.
     */
    public Flux<AgentStep> confirm(SessionContext ctx, String userMessage) {
        return agentLoop.confirm(ctx, userMessage)
            .doOnNext(step -> {
                if (step.type != null) {
                    repo.saveAgentStep(ctx.sessionId, nextSeq(ctx.sessionId), step);
                }
            })
            .doOnComplete(() -> log.info("Confirmation complete for session {}", ctx.sessionId));
    }

    /**
     * Execute confirmed plan actions (backward compatible).
     */
    public Flux<AgentStep> confirm(SessionContext ctx) {
        return confirm(ctx, "确认");
    }
```

---

## 3. ChatWebSocketHandler.java

**路径**: `src/main/java/com/planagent/controller/ChatWebSocketHandler.java`

### 3a. 添加 confirm 路由

在 `handleTextMessage` 方法中，将当前的 if-else 块替换为：

```java
        if ("confirm".equals(type)) {
            ctx.confirmedPlan = content;
            chatService.confirm(ctx, content)
                .doOnNext(step -> send(wsSession, step))
                .doOnError(e -> send(wsSession, AgentStep.error(e.getMessage())))
                .doFinally(s -> log.info("Confirmation done: {}", sessionId))
                .subscribe();
        } else if (ctx.confirmedPlan != null) {
            // Post-plan message: route to confirm (user is confirming the plan)
            chatService.confirm(ctx, content)
                .doOnNext(step -> send(wsSession, step))
                .doOnError(e -> send(wsSession, AgentStep.error(e.getMessage())))
                .doFinally(s -> log.info("Post-plan confirm done: {}", sessionId))
                .subscribe();
        } else {
            chatService.execute(ctx, content, deepThinking)
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
```

---

## 4. ToolRegistry.java

**路径**: `src/main/java/com/planagent/tools/ToolRegistry.java`

### 4a. 修改 execute() 签名，添加 ThreadLocal 管理

将 `execute(ToolExecutionRequest req)` 改为 `execute(ToolExecutionRequest req, SessionContext ctx)`：

```java
import com.planagent.model.SessionContext;
import com.planagent.scoring.SessionContextHolder;

public String execute(ToolExecutionRequest req, SessionContext ctx) {
    SessionContextHolder.set(ctx);
    try {
        Tool tool = tools.get(req.name());
        if (tool == null) {
            return "{\"error\": \"Unknown tool: " + req.name() + "\"}";
        }
        return tool.execute(req);
    } finally {
        SessionContextHolder.clear();
    }
}
```

（删除旧的 `execute(ToolExecutionRequest req)` 单参数版本）

---

## 5. ActivitySearchTool.java

**路径**: `src/main/java/com/planagent/tools/ActivitySearchTool.java`

### 5a. 添加 ScoringEngine 注入，在搜索后进行排序

添加 import 和字段：
```java
import com.planagent.scoring.ScoringEngine;
import com.planagent.scoring.SessionContextHolder;
import com.planagent.model.SessionContext;

private final ScoringEngine scoringEngine;
```

修改构造函数，注入 ScoringEngine：
```java
public ActivitySearchTool(MockDataStore store, ScoringEngine scoringEngine) {
    this.store = store;
    this.scoringEngine = scoringEngine;
}
```

在 execute() 方法中，MockDataStore.searchActivities() 返回后，插入排序：
```java
List<Activity> results = store.searchActivities(type, childAge, duration, maxDistance);
// 通过 ThreadLocal 获取 SessionContext 进行排序
SessionContext ctx = SessionContextHolder.get();
if (ctx != null) {
    results = scoringEngine.rankActivities(results, ctx, childAge);
}
return toJson(results);
```

---

## 6. RestaurantSearchTool.java

**路径**: `src/main/java/com/planagent/tools/RestaurantSearchTool.java`

### 6a. 添加 ScoringEngine 注入，在搜索后进行排序

同上模式：
```java
import com.planagent.scoring.ScoringEngine;
import com.planagent.scoring.SessionContextHolder;
import com.planagent.model.SessionContext;

private final ScoringEngine scoringEngine;

public RestaurantSearchTool(MockDataStore store, ScoringEngine scoringEngine) {
    this.store = store;
    this.scoringEngine = scoringEngine;
}
```

在 execute() 方法中插入排序：
```java
List<Restaurant> results = store.searchRestaurants(keyword, tags, budget, kidFriendly);
SessionContext ctx = SessionContextHolder.get();
if (ctx != null) {
    results = scoringEngine.rankRestaurants(results, ctx);
}
return toJson(results);
```

---

## 7. RatingFilterTool.java (可选)

**路径**: `src/main/java/com/planagent/tools/RatingFilterTool.java`

如果该工具也做搜索排序，同样注入 ScoringEngine 并在返回前排序。

---

## 8. application.yml

**路径**: `src/main/resources/application.yml`

追加以下配置（或确认 `application-scoring.yml` 已被 Spring 加载）：

```yaml
scoring:
  family:
    rating: 0.20
    distance: 0.15
    price-match: 0.10
    available: 0.15
    kid-friendly: 0.30
    time-match: 0.10
  friends:
    rating: 0.30
    distance: 0.15
    price-match: 0.15
    available: 0.20
    vibe-match: 0.20
```

如果使用独立的 `application-scoring.yml`，需在 `application.yml` 中添加：
```yaml
spring:
  config:
    import: optional:classpath:application-scoring.yml
```

---

## 9. AgentLoopTest.java (如存在)

**路径**: `src/test/java/com/planagent/agent/AgentLoopTest.java`

将 `agentLoop.confirm(ctx)` 调用改为 `agentLoop.confirm(ctx, "确认")`。

---

## 验证步骤

全部修改完成后运行：

```bash
# 编译
mvn compile

# 如果编译通过，运行所有测试
mvn test

# 预期: 所有测试通过
# - ModelJsonTest
# - MockDataStoreTest
# - AgentLoopTest (可能需要小幅调整)
# - ScoringEngineTest (4 tests)
# - ReplanningHandlerTest (7 tests)

# 构建
mvn clean package -DskipTests
```
