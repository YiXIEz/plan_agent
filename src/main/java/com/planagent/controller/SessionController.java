package com.planagent.controller;

import com.planagent.repository.MessageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final MessageRepository repo;

    public SessionController(MessageRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> listSessions() {
        return repo.listSessions();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<Map<String, Object>> getMessages(@PathVariable String sessionId) {
        return repo.loadMessages(sessionId);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        repo.deleteSession(sessionId);
    }
}
