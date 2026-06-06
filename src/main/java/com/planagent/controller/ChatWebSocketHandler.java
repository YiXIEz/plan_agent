package com.planagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.model.AgentStep;
import com.planagent.model.SessionContext;
import com.planagent.service.ChatService;
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
    private final ChatService chatService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService) {
        this.chatService = chatService;
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
        boolean deepThinking = node.has("deepThinking") && node.get("deepThinking").asBoolean();
        String sessionId = wsSession.getId();
        SessionContext ctx = sessions.get(sessionId);

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
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String id = session.getId();
        sessions.remove(id);
        wsSessions.remove(id);
        chatService.onSessionEnd(id);
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
