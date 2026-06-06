# Plan Agent — 周末活动规划助手

基于 Spring Boot + LangChain4j 的 AI 活动规划助手，使用 ReAct 循环通过 DeepSeek LLM 推理，调用高德地图真实 API 获取天气/地点/路线数据，自动生成完整的亲子下午活动方案。

## 架构

```
浏览器 (index.html) ──WebSocket──► ChatWebSocketHandler
                                        │
                                   ChatService (持久化)
                                        │
                                   AgentLoop (ReAct 循环)
                                   /         \
                            ToolRegistry    LLM (DeepSeek/Qwen)
                           /    |    \
                     AmapClient MockDataStore (Action tools)
```

## 前置条件

- Java 17+
- Maven 3.9+
- MySQL 8.0+
- DeepSeek API Key（[获取](https://platform.deepseek.com/api_keys)）
- 高德地图 Web服务 API Key（[获取](https://lbs.amap.com/)）

## 快速开始

```bash
# 1. 克隆项目
git clone git@gitee.com:YIXie777/plan-agent.git
cd plan-agent

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS plan_agent DEFAULT CHARACTER SET utf8mb4"

# 3. 设置环境变量
export DEEPSEEK_API_KEY=你的DeepSeek密钥
export AMAP_API_KEY=你的高德地图密钥
export MYSQL_PASSWORD=你的MySQL密码

# 4. 编译运行
mvn clean package -DskipTests
java -jar target/plan-agent-1.0.0-SNAPSHOT.jar

# 5. 打开浏览器
# http://localhost:8080
```

首次启动后表会自动创建（通过 `schema.sql`）。

## 配置说明

所有配置在 `src/main/resources/application.yml`，支持环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DEEPSEEK_API_KEY` | - | DeepSeek API 密钥 |
| `AMAP_API_KEY` | - | 高德地图 Web 服务 API 密钥 |
| `MYSQL_HOST` | localhost | MySQL 地址 |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_USER` | root | MySQL 用户名 |
| `MYSQL_PASSWORD` | 10011001 | MySQL 密码 |

无 `AMAP_API_KEY` 时自动回退到 Mock 数据。

## 工具列表

**搜索工具（Amap 真实 API）：**

| 工具 | 功能 | 数据来源 |
|------|------|----------|
| `check_weather` | 查询指定区域天气 | 高德天气 API |
| `search_activities` | 搜索附近亲子活动 | 高德 POI 搜索 |
| `search_restaurants` | 搜索附近餐厅 | 高德 POI 搜索 |
| `plan_route` | 规划两点间路线 | 高德路径规划 API |
| `filter_by_rating` | 按评分筛选商户 | 高德 POI 详情 API |

**行动工具（Mock 数据）：**

| 工具 | 功能 |
|------|------|
| `check_queue` | 查询餐厅排队情况 |
| `make_reservation` | 预订餐厅 |
| `place_order` | 下单（门票/蛋糕/鲜花） |
| `pay` | 支付 |
| `send_notification` | 发送通知 |

## 项目结构

```
src/main/java/com/planagent/
├── agent/AgentLoop.java          # ReAct 循环核心
├── amap/AmapClient.java          # 高德地图 API 客户端
├── config/LLMConfig.java         # LLM 配置（DeepSeek + Qwen 备用）
├── controller/ChatWebSocketHandler.java  # WebSocket 端点
├── mock/MockDataStore.java       # Mock 数据存储
├── model/                        # 数据模型
├── repository/MessageRepository.java  # MySQL 持久化
├── service/ChatService.java      # 对话服务（持久化封装）
└── tools/                        # 10 个工具实现
```

## 开发命令

```bash
mvn compile            # 编译
mvn test               # 运行测试（27 个）
mvn test -Dtest=AmapClientTest  # 运行单个测试类
mvn clean package -DskipTests   # 打包
```
