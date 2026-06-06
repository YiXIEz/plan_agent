package com.planagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.model.SessionContext;
import com.planagent.scoring.SessionContextHolder;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ToolRegistry {
    private final Map<String, ToolExecutor> tools = new LinkedHashMap<>();
    private final Map<String, ToolSpecification> specs = new LinkedHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String argumentsJson) throws Exception;
    }

    public void register(String name, String description, Map<String, Map<String, Object>> params, ToolExecutor executor) {
        var specBuilder = ToolSpecification.builder().name(name).description(description);
        if (params != null && !params.isEmpty()) {
            var schemaBuilder = JsonObjectSchema.builder();
            params.forEach((key, props) -> {
                String type = (String) props.getOrDefault("type", "string");
                String desc = (String) props.getOrDefault("description", "");
                List<String> enums = (List<String>) props.get("enum");
                if (enums != null && !enums.isEmpty()) {
                    schemaBuilder.addEnumProperty(key, enums, desc);
                } else {
                    switch (type) {
                        case "integer" -> schemaBuilder.addIntegerProperty(key, desc);
                        case "number" -> schemaBuilder.addNumberProperty(key, desc);
                        case "boolean" -> schemaBuilder.addBooleanProperty(key, desc);
                        case "array" -> schemaBuilder.addProperty(key, JsonArraySchema.builder().items(new JsonStringSchema()).description(desc).build());
                        default -> schemaBuilder.addStringProperty(key, desc);
                    }
                }
            });
            specBuilder.parameters(schemaBuilder.build());
        }
        ToolSpecification spec = specBuilder.build();
        specs.put(name, spec);
        tools.put(name, executor);
    }

    public List<ToolSpecification> getSpecifications() {
        return new ArrayList<>(specs.values());
    }

    public String execute(ToolExecutionRequest request) {
        ToolExecutor executor = tools.get(request.name());
        if (executor == null) return "{\"error\": \"Unknown tool: " + request.name() + "\"}";
        try {
            return executor.execute(request.arguments());
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Execute a tool with SessionContext. The SessionContext is set in a ThreadLocal
     * before execution so that search tools can access it for scoring.
     */
    public String execute(ToolExecutionRequest req, SessionContext ctx) {
        SessionContextHolder.set(ctx);
        try {
            ToolExecutor executor = tools.get(req.name());
            if (executor == null) {
                return "{\"error\": \"Unknown tool: " + req.name() + "\"}";
            }
            return executor.execute(req.arguments());
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } finally {
            SessionContextHolder.clear();
        }
    }
}
