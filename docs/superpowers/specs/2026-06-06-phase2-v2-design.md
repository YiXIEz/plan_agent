# Phase 2: 下单重规划 & 多目标优化排序 — 设计文档

> **修订版** — 基于真实前端代码审查后精简。前端为 React SPA，不区分 goal/confirm/replan 结构化消息。

## 1. 目标

对 plan-agent 的两个核心决策环节进行升级：

1. **多目标优化排序**：替换单一评分排序，引入场景自适应的多因素加权评分引擎
2. **下单/预订失败智能重规划**：`make_reservation` 或 `place_order` 返回失败（满座/不可用）时，Agent 自动进行局部重规划

## 2. 架构概览

```
前端 (React SPA, 不变)
  │ WebSocket { content, deepThinking }  (无 type 字段)
  ▼
ChatWebSocketHandler  ← 新增: confirmedPlan!=null → confirm()
  │
  ├─ execute() → AgentLoop (ReAct 循环, 规划阶段)
  └─ confirm() → AgentLoop (while 循环, 执行+重规划阶段)
                    │
                    ├── ReplanningHandler  (失败分析 + 重规划 Prompt)
                    └── ToolRegistry → SearchTool  ← ThreadLocal SessionContext
                                          │
                                          ├── MockDataStore  (纯过滤, 不变)
                                          └── ScoringEngine   (归一化 + 加权排序)
```

## 3. 新增/修改文件

```
scoring/
  ├── WeightConfig.java          @ConfigurationProperties("scoring")
  ├── ScenarioWeights.java       权重数据结构
  ├── ScoringEngine.java         评分引擎（归一化 + 加权求和 + 排序）
  └── SessionContextHolder.java  ThreadLocal 传递 SessionContext

agent/
  └── ReplanningHandler.java     失败分析 + 重规划 Prompt 生成
```

**修改文件**：AgentLoop（confirm 重构，新增 content 参数）、ChatWebSocketHandler（新增 confirm 路由）、ChatService（confirm 透传 content）、SearchTool 实现类（注入 ScoringEngine + SessionContextHolder）、ToolRegistry（execute 时设置/清理 ThreadLocal）、application.yml（scoring 配置段）

## 4. 评分引擎

```
Score = Σ(weight_i × normalize_i(value))
```

### 权重矩阵

| 维度 | 家庭场景 | 朋友场景 | 说明 |
|------|---------|---------|------|
| rating | 0.20 | 0.30 | 归一化: rating/5.0 |
| distance | 0.15 | 0.15 | 1 - min(dist,10)/10 |
| price-match | 0.10 | 0.15 | 目标匹配→1.0, 偏差一档→0.6 |
| available | 0.15 | 0.20 | 无需排队→1.0, <10min→0.8, >30min→0.2 |
| kid-friendly | 0.30 | — | 家庭专用：年龄适配度 |
| vibe-match | — | 0.20 | 朋友专用：标签与场景匹配度 |
| time-match | 0.10 | — | 时长匹配目标 |

### 归一化规则

- **rating**: `score / 5.0`
- **distance**: `1 - Math.min(dist, 10) / 10`（越近分越高）
- **price-match**: 预算完全匹配 → 1.0，相差一档 → 0.6，相差两档+ → 0.2
- **available**: 从 queueTime 文本解析，"无需排队"→ 1.0
- **kid-friendly**: 年龄范围完全覆盖 → 1.0，部分覆盖 → 0.7，不覆盖 → 0.3
- **time-match**: 活动时长与目标时长匹配度
- **vibe-match**: 餐厅 tags 与场景偏好的重叠率

权重可通过 `application.yml` 的 `scoring.family.*` / `scoring.friends.*` 外部配置。

### ScoringEngine 集成方式：ThreadLocal（方案 B3）

- **SessionContextHolder**：提供 `set(SessionContext)` / `get()` / `clear()` 静态方法，基于 ThreadLocal
- **ToolRegistry.execute()**：调用 Tool 前 set(ctx)，调用后 clear()
- **SearchTool**（ActivitySearchTool, RestaurantSearchTool, RatingFilterTool）：注入 ScoringEngine，调用时通过 SessionContextHolder.get() 获取 ctx，先调 MockDataStore 过滤再调 ScoringEngine 排序
- **MockDataStore**：不做任何排序，保持纯数据查询

Spring WebSocket 单线程模型下 ThreadLocal 安全。

## 5. 重规划处理器

### 失败类型枚举

| 类型 | 触发条件 | 行为 |
|------|----------|------|
| SUCCESS | status="已预订"/"已下单"（含 LONG_WAIT） | 继续执行下一条 Tool |
| FULLY_BOOKED | status 含 "已满"/"已约满" | 自动局部重规划 |
| NOT_AVAILABLE | status 含 "不可用"/"已下架" | 自动局部重规划 |
| UNKNOWN | 其他失败 | 自动局部重规划 |

注：LONG_WAIT 不再单独处理。等待时间信息在最终 DONE 消息中汇总展示。

### 重规划策略

- **半径**：局部——仅替换失败项，保留计划中其他环节
- **次数上限**：每个确认流程最多 3 次局部重规划
- **扩大策略**：首次失败搜索 5km，第二次放宽到 8km，第三次 11km
- **死循环保护**：超过上限后返回 ERROR "自动调整失败，请手动修改方案"

## 6. AgentLoop 变更

### confirm() 流程（重构）

```
while (replanCount <= 3):
    LLM 调用 → 获取 ToolCall 列表
    for each Tool:
        result = toolRegistry.execute(req)
        failureType = replanningHandler.analyze(result)
        
        switch:
          SUCCESS（含 LONG_WAIT）→ 继续下一个 Tool
          FULLY_BOOKED / NOT_AVAILABLE / UNKNOWN:
            if replanCount >= 3 → ERROR, return
            replanCount++
            生成 replanPrompt → break（重新调 LLM）
    
    if 全部 Tool 成功 → DONE（汇总等待信息）, return
```

### 方法签名变更

- `AgentLoop.confirm(SessionContext session)` → `AgentLoop.confirm(SessionContext session, String userMessage)` — 新增 userMessage 参数，传入用户确认时的消息内容（如"确认"、"可以"），LLM 据此理解用户意图
- `ChatService.confirm(SessionContext ctx)` → `ChatService.confirm(SessionContext ctx, String userMessage)` — 透传到 AgentLoop

### ChatWebSocketHandler 路由

```java
if ("confirm".equals(type)) {
    // 保留兼容，但当前前端不使用
    chatService.confirm(ctx, content)...
} else if (ctx.confirmedPlan != null) {
    // 新增：已确认计划后，后续消息路由到 confirm
    chatService.confirm(ctx, content)...
} else {
    // 首次消息走规划流程
    chatService.execute(ctx, content, deepThinking)...
}
```

注：用户想开始全新规划时，点击前端"新对话"按钮（page reload = 新 WebSocket 会话 = confirmedPlan 为 null）。

## 7. 前端

**不做任何修改**。现有 React SPA 已能：
- 渲染所有 AgentStep 类型（未知类型 fallback 为 text 消息）
- 用户通过自然语言与 Agent 交互（LLM 从对话历史理解意图）
- DONE 消息中的等待汇总自然展示给用户

## 8. 不变更项（相比初版设计去掉的）

- ❌ `AgentStep.Type.NOTIFY` — LONG_WAIT 归入 SUCCESS
- ❌ `AgentLoop.replan()` — 不需要独立的重规划方法
- ❌ WebSocket "replan" 消息路由
- ❌ 前端 NOTIFY 卡片 / 按钮
- ❌ MockDataStore 注入 ScoringEngine — 排序移至 Tool 层

## 9. 技术要点

- **ScoringEngine** 通过 ThreadLocal SessionContextHolder 被 Tool 访问，不改变 Tool 接口签名
- **ReplanningHandler** 无状态（replanCount 由 AgentLoop 管理）
- **向后兼容**：Tool 接口不变，前端不变，WebSocket 协议不变
- **测试**：ScoringEngine 和 ReplanningHandler 各自独立单元测试
