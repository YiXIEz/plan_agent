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
