# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot web application that implements a short-term activity planning AI agent. Users type natural language goals in Chinese, the agent produces executable plans via a ReAct loop using real LLMs (DeepSeek primary, Qwen fallback), and after user confirmation executes bookings/payments/notifications via mocked services.

## Commands

```bash
# Compile
mvn compile

# Run all tests
mvn test

# Run a single test
mvn test -Dtest=ModelJsonTest

# Package (skip tests)
mvn clean package -DskipTests

# Run server (requires API keys)
export DEEPSEEK_API_KEY=your_key
export QWEN_API_KEY=your_key
java -jar target/plan-agent-1.0.0-SNAPSHOT.jar
# Then open http://localhost:8080
```

## Architecture

```
Browser (index.html, single-file)  ──WebSocket──►  ChatWebSocketHandler
                                                         │
                                                    AgentLoop.java
                                                    (ReAct loop, max 10 rounds)
                                                         │
                                              ┌──────────┼──────────┐
                                              │          │          │
                                         LLM (via     ToolRegistry  MockDataStore
                                      LangChain4j)   (10 tools)   (simulated data)
```

**ReAct Loop:**
1. System prompt sets agent role + Chinese context
2. LLM receives user goal + tool specs and returns Thought + ToolCall (e.g., `check_weather`)
3. `ToolRegistry` dispatches to the appropriate tool, which reads from `MockDataStore`
4. Observation result is fed back into conversation
5. Loop repeats until LLM returns a final plan with no tool calls
6. User confirms → agent executes action tools (reserve, order, pay, notify)

**Streaming:** `AgentLoop` returns `Flux<AgentStep>` via Project Reactor. Each thought/action/observation is pushed to the browser over WebSocket in real time.

**Fallback model:** DeepSeek is primary; after configurable error threshold, switches to Qwen for the remainder of the session.

## Key Files

| File | Role |
|---|---|
| `AgentLoop.java` | Core ReAct loop, LLM interaction, plan confirmation/execution |
| `ChatWebSocketHandler.java` | WebSocket endpoint, session management, message routing |
| `ToolRegistry.java` | Registers all 10 tools by name, dispatches tool calls |
| `MockDataStore.java` | All simulated external data (7 activities, 8 restaurants, weather, routes) |
| `AgentStep.java` | DTO for 7 WebSocket message types (THOUGHT, ACTION, OBSERVATION, etc.) |
| `SessionContext.java` | Per-WebSocket-connection state (messages, confirmed, tool calls) |
| `LLMConfig.java` | Configures two `ChatLanguageModel` beans for DeepSeek and Qwen |

## Tools (10 total)

**Search tools:** `check_weather`, `search_activities`, `search_restaurants`, `plan_route`, `filter_by_rating`
**Action tools:** `check_queue`, `make_reservation`, `place_order`, `pay`, `send_notification`

All tools self-register in `ToolRegistry` via constructor injection. Tool specs use LangChain4j's `JsonObjectSchema.builder()` API.

## Dependencies

- Java 17, Spring Boot 3.4.1, Maven
- LangChain4j 1.0.0-beta3 (core + open-ai modules)
- DeepSeek and Qwen accessed via OpenAI-compatible API endpoints
- Project Reactor for reactive streaming (WebSocket → frontend)

## Conventions

- All UI text, prompts, mock data, and tool descriptions are in Simplified Chinese
- LLM API keys come from `DEEPSEEK_API_KEY` and `QWEN_API_KEY` environment variables
- WebSocket protocol: `ws://host/ws/chat`, message types defined by `AgentStep.Type` enum
