# Plan Agent — 周末活动规划助手

基于 Spring Boot + LangChain4j 的 AI 活动规划助手。ReAct 循环推理，高德地图真实 API，Vercel Chatbot 前端。

## 架构

```
Vercel Chatbot (Next.js) ──WebSocket──► Spring Boot
  frontend/                              │
  ├─ app/(chat)/layout.tsx               ChatWebSocketHandler
  ├─ hooks/use-websocket-chat.ts         │
  └─ components/chat/               ChatService (H2 持久化)
                                         │
                                    AgentLoop (ReAct 循环)
                                    /         \
                             ToolRegistry    LLM (DeepSeek v4-pro)
                            /    |    \
                      AmapClient MockDataStore
```

## 前置条件

- Java 17+
- Maven 3.9+
- Node.js 18+
- DeepSeek API Key（[获取](https://platform.deepseek.com/api_keys)）
- 高德地图 Web服务 API Key（[获取](https://lbs.amap.com/)）

## 快速开始

```bash
# 1. 克隆项目
git clone git@github.com:MSSHHH/plan_agent.git
cd plan-agent

# 2. 设置环境变量
export DEEPSEEK_API_KEY=你的DeepSeek密钥
export AMAP_API_KEY=你的高德地图密钥

# 3. 启动后端（Spring Boot :8080）
mvn clean package -DskipTests
java -jar target/plan-agent-1.0.0-SNAPSHOT.jar

# 4. 启动前端（另一个终端，Next.js :3001）
cd frontend
npm install
npm run dev

# 5. 打开浏览器
# http://localhost:3001
```

## 配置说明

核心配置在 `src/main/resources/application.yml`：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DEEPSEEK_API_KEY` | - | DeepSeek API 密钥（必填） |
| `AMAP_API_KEY` | - | 高德地图密钥，不填则回退 Mock 数据 |
| `QWEN_API_KEY` | - | Qwen 备用模型密钥（可选） |

数据库使用 **H2 嵌入式文件数据库**，零配置，数据自动存 `./data/conversation.mv.db`，表自动创建。H2 控制台：http://localhost:8080/h2-console（URL: `jdbc:h2:file:./data/conversation`，用户名 `sa`，密码空）。

## 工具列表

| 工具 | 功能 | 数据来源 |
|------|------|----------|
| `check_weather` | 查询天气 | 高德 API |
| `search_activities` | 搜索亲子活动 | 高德 POI |
| `search_restaurants` | 搜索餐厅 | 高德 POI |
| `plan_route` | 规划路线 | 高德路径 |
| `filter_by_rating` | 评分筛选 | 高德详情 |
| `check_queue` | 排队查询 | Mock |
| `make_reservation` | 预订 | Mock |
| `place_order` | 下单 | Mock |
| `pay` | 支付 | Mock |
| `send_notification` | 通知 | Mock |

## 项目结构

```
src/main/java/com/planagent/
├── agent/AgentLoop.java              # ReAct 循环（流式输出）
├── amap/AmapClient.java              # 高德地图 API
├── config/LLMConfig.java             # LLM 配置
├── controller/
│   ├── ChatWebSocketHandler.java     # WebSocket 端点
│   └── SessionController.java        # REST API（历史对话）
├── mock/MockDataStore.java           # Mock 数据
├── model/                            # 数据模型
├── repository/MessageRepository.java # H2 持久化
├── service/ChatService.java          # 对话服务
└── tools/                            # 10 个工具

frontend/                             # Vercel Chatbot 前端
├── app/(chat)/                       # 聊天页面
├── components/chat/                  # UI 组件
│   ├── app-sidebar.tsx               # 侧边栏
│   ├── messages.tsx                  # 消息列表
│   ├── multimodal-input.tsx          # 输入框
│   ├── greeting.tsx                  # Hero 标题
│   └── suggested-actions.tsx         # 推荐问题
├── hooks/
│   ├── use-websocket-chat.ts         # WebSocket 通信
│   └── use-active-chat.tsx           # 聊天状态管理
└── app/(chat)/api/history/route.ts   # 历史对话 API 代理
```

## 开发命令

```bash
# 后端
mvn compile
mvn test              # 27 个测试
mvn clean package -DskipTests

# 前端
cd frontend
npm run dev           # 开发模式（热更新，:3001）
npm run build         # 生产构建
```

## WebSocket 协议

前端通过 `ws://localhost:8080/ws/chat` 与后端通信：

```json
// 发送
{ "content": "用户的输入文本" }

// 接收（流式）
{ "type": "TOKEN", "content": "逐" }
{ "type": "TOKEN", "content": "段" }
{ "type": "TOKEN", "content": "输" }
{ "type": "FINAL_PLAN", "content": "完整方案 Markdown..." }
{ "type": "DONE", "content": "执行完成" }
{ "type": "ERROR", "content": "错误信息" }
```
