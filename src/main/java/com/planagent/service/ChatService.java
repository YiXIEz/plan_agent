package com.planagent.service;

import com.planagent.agent.AgentLoop;
import com.planagent.model.AgentStep;
import com.planagent.model.SessionContext;
import com.planagent.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final AgentLoop agentLoop;
    private final MessageRepository repo;
    private final Map<String, AtomicInteger> sessionSeqs = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionCreated = new ConcurrentHashMap<>();

    public ChatService(AgentLoop agentLoop, MessageRepository repo) {
        this.agentLoop = agentLoop;
        this.repo = repo;
    }

    /** Called when WebSocket disconnects */
    public void onSessionEnd(String sessionId) {
        sessionSeqs.remove(sessionId);
        sessionCreated.remove(sessionId);
        log.info("Session ended: {}", sessionId);
    }

    /**
     * Execute agent planning, persisting each step to the database.
     * AgentLoop business logic is unchanged - this is a thin persistence wrapper.
     */
    public Flux<AgentStep> execute(SessionContext ctx, String userGoal) {
        return execute(ctx, userGoal, false);
    }

    public Flux<AgentStep> execute(SessionContext ctx, String userGoal, boolean deepThinking) {
        // Lazy session creation on first message
        if (sessionCreated.putIfAbsent(ctx.sessionId, true) == null) {
            String title = userGoal.length() > 100 ? userGoal.substring(0, 100) : userGoal;
            repo.createSession(ctx.sessionId, title);
            sessionSeqs.put(ctx.sessionId, new AtomicInteger(0));
        }

        // Save user message
        repo.saveUserMessage(ctx.sessionId, nextSeq(ctx.sessionId), userGoal);

        return agentLoop.execute(ctx, userGoal, deepThinking)
            .doOnNext(step -> {
                if (step.type != null) {
                    repo.saveAgentStep(ctx.sessionId, nextSeq(ctx.sessionId), step);
                }
            })
            .doOnError(e -> log.error("Agent execution error for session {}: {}", ctx.sessionId, e.getMessage()))
            .doOnComplete(() -> log.info("Execution complete for session {}", ctx.sessionId));
    }

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

    private int nextSeq(String sessionId) {
        return sessionSeqs.computeIfAbsent(sessionId, k -> new AtomicInteger(0))
            .getAndIncrement();
    }
}
