package com.planagent.repository;

import com.planagent.model.AgentStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class MessageRepository {
    private static final Logger log = LoggerFactory.getLogger(MessageRepository.class);
    private final JdbcTemplate jdbc;

    public MessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createSession(String sessionId, String title) {
        jdbc.update(
            "MERGE INTO sessions (session_id, title, updated_at) KEY(session_id) VALUES (?, ?, CURRENT_TIMESTAMP)",
            sessionId, title);
    }

    public void saveUserMessage(String sessionId, int seq, String content) {
        jdbc.update(
            "INSERT INTO messages (session_id, seq, role, content) VALUES (?, ?, 'user', ?)",
            sessionId, seq, content);
    }

    public void saveAgentStep(String sessionId, int seq, AgentStep step) {
        jdbc.update(
            "INSERT INTO messages (session_id, seq, role, content, step_type, tool_name, tool_params) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            sessionId, seq,
            roleOf(step),
            contentOf(step),
            stepTypeOf(step),
            toolNameOf(step),
            toolParamsOf(step)
        );
    }

    public List<Map<String, Object>> loadMessages(String sessionId) {
        return jdbc.queryForList(
            "SELECT seq, role, content, step_type, tool_name, tool_params, created_at " +
            "FROM messages WHERE session_id = ? ORDER BY seq", sessionId).stream()
            .map(this::lowerKeys).toList();
    }

    public List<Map<String, Object>> listSessions() {
        return jdbc.queryForList(
            "SELECT session_id, title, created_at, updated_at FROM sessions ORDER BY updated_at DESC LIMIT 50").stream()
            .map(this::lowerKeys).toList();
    }

    private Map<String, Object> lowerKeys(Map<String, Object> row) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        row.forEach((k, v) -> m.put(k.toLowerCase(), v));
        return m;
    }

    public void deleteSession(String sessionId) {
        jdbc.update("DELETE FROM messages WHERE session_id = ?", sessionId);
        jdbc.update("DELETE FROM sessions WHERE session_id = ?", sessionId);
    }

    // ── helpers ──

    private String roleOf(AgentStep step) {
        return switch (step.type) {
            case ACTION, OBSERVATION -> "tool";
            default -> "assistant";
        };
    }

    private String contentOf(AgentStep step) {
        return step.content != null && step.content.length() > 8000
            ? step.content.substring(0, 8000)
            : step.content;
    }

    private String stepTypeOf(AgentStep step) {
        return step.type != null ? step.type.name() : null;
    }

    private String toolNameOf(AgentStep step) {
        return step.tool;
    }

    private String toolParamsOf(AgentStep step) {
        if (step.params != null && step.params.length() > 2000)
            return step.params.substring(0, 2000);
        return step.params;
    }
}
