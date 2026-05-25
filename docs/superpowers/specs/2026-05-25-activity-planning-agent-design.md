# 本地短时活动规划与执行 Agent — 设计文档

## 1. 目标

构建一个本地场景短时活动规划与执行 Agent，接受一句自然语言目标（如"下午带老婆孩子出去玩4-6小时"），输出可执行的完整方案并自动完成关键下单/预订动作。

**场景聚焦**：家庭场景（5岁孩子 + 减肥中的老婆），朋友场景逻辑类似可后续扩展。

**交付形式**：Spring Boot Web UI Demo，全 Mock 外部服务，接入 DeepSeek/Qwen 真实 LLM。

## 2. 架构概览

```
用户输入 (Web UI)
    │  WebSocket 双向流式通信
    ▼
AgentLoop (ReAct)
    │  Thought → Action → Observation 循环，最多10轮
    ├── ToolRegistry (10个 @Tool)
    │       ├── 搜索类: searchActivities, searchRestaurants, checkWeather, planRoute, filterByRating
    │       └── 执行类: checkQueue, makeReservation, placeOrder, pay, sendNotification
    ├── Mock Services (模拟美团/高德/天气 API，返回真实感数据)
    └── LLM Provider (DeepSeek / Qwen，LangChain4j ChatLanguageModel 统一接口)
```

关键组件：`AgentLoop`（管理 ReAct 循环）、`ToolRegistry`（LangChain4j `@Tool` 注册）、`SessionContext`（对话状态）、`ChatWebSocketHandler`（流式推送）。

## 3. ReAct 规划策略

**循环流程**：每轮 LLM 返回 Thought + ToolCall → 执行 Tool → 结果注入回对话 → 下一轮，直到 LLM 输出最终计划。

**System Prompt 要点**：角色定义（活动规划助手）、约束（下午4-6小时、离家近）、输出要求（去哪玩→去哪吃→额外活动）、确认机制（方案需用户确认后才执行下单）。

**场景识别**：从自然语言中提取人数、关系、年龄、饮食约束，存入 SessionContext。

**确认与执行**：用户确认计划后，Agent 逐一执行 makeReservation / placeOrder / pay / sendNotification，每步状态实时推送前端。

## 4. 工具调用链路

以家庭场景典型链路为例：

```
checkWeather → searchActivities → filterByRating
    → searchRestaurants(tags=轻食+亲子友好)
    → checkQueue → planRoute
    → [最终计划输出，用户确认]
    → makeReservation + placeOrder(蛋糕) + pay + sendNotification
```

Tool 执行层面处理三类异常：
- **无结果**：自动扩大搜索参数重试一次，仍无结果反馈 LLM 调整方案
- **排队久/已满**：反馈 LLM，由 LLM 决定替换或调整时间
- **执行异常**：重试3次（1s间隔），失败后跳过非关键 Tool，记录到 AgentStep.error

## 5. LLM 调用与降级

- 主模型 DeepSeek，备用模型 Qwen
- API 超时/Rate Limit：指数退避重试（1s/2s/4s），3次后切换备用模型
- 返回格式不可解析：将错误信息 + 格式说明追加到消息，请求 LLM 重新生成
- 单次规划最多10轮 ReAct 循环，防止无限循环

## 6. WebSocket 消息协议

| 类型 | 含义 | 前端表现 |
|------|------|----------|
| THOUGHT | LLM 推理过程 | 灰色文字，实时流式 |
| ACTION | Tool 调用 | 蓝色，显示工具名+参数 |
| OBSERVATION | Tool 返回结果 | 绿色，结构化展示 |
| FINAL_PLAN | 最终方案 | 卡片样式，含确认/调整按钮 |
| CONFIRMING | 执行中 | 加载状态 |
| DONE | 全部完成 | 绿色卡片，含订单/预订汇总 |
| ERROR | 异常 | 红色提示 |

## 7. Mock 服务设计

所有外部服务（美团、高德、天气）均 Mock，但数据模拟真实场景：
- 活动库：5-8个亲子活动，含评分、距离、价格、适合年龄
- 餐厅库：8-10家，含菜系、人均、标签（轻食/亲子友好）、排队时间
- 天气：随机返回晴/多云/小雨 + 温度
- 预订/支付：始终成功（通过），偶尔模拟排队时间变化

## 8. 技术选型

| 层 | 技术 |
|----|------|
| 框架 | Spring Boot 3.4 + WebFlux |
| LLM | LangChain4j + DeepSeek/Qwen |
| 通信 | WebSocket (spring-boot-starter-websocket) |
| 前端 | 单文件 HTML/CSS/JS，内嵌于 static/ |
| 构建 | Maven |
