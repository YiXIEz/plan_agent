package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentStep {
    public enum Type { THOUGHT, ACTION, OBSERVATION, TOKEN, FINAL_PLAN, CONFIRMING, DONE, ERROR }

    @JsonProperty("type") public Type type;
    @JsonProperty("content") public String content;
    @JsonProperty("tool") public String tool;
    @JsonProperty("params") public String params;

    public AgentStep() {}

    public static AgentStep thought(String content) {
        var s = new AgentStep(); s.type = Type.THOUGHT; s.content = content; return s;
    }
    public static AgentStep action(String tool, String params) {
        var s = new AgentStep(); s.type = Type.ACTION; s.tool = tool; s.params = params; return s;
    }
    public static AgentStep observation(String content) {
        var s = new AgentStep(); s.type = Type.OBSERVATION; s.content = content; return s;
    }
    public static AgentStep token(String content) {
        var s = new AgentStep(); s.type = Type.TOKEN; s.content = content; return s;
    }
    public static AgentStep finalPlan(String content) {
        var s = new AgentStep(); s.type = Type.FINAL_PLAN; s.content = content; return s;
    }
    public static AgentStep confirming(String content) {
        var s = new AgentStep(); s.type = Type.CONFIRMING; s.content = content; return s;
    }
    public static AgentStep done(String content) {
        var s = new AgentStep(); s.type = Type.DONE; s.content = content; return s;
    }
    public static AgentStep error(String content) {
        var s = new AgentStep(); s.type = Type.ERROR; s.content = content; return s;
    }
}
