package com.planagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.model.*;
import com.planagent.tools.ToolRegistry;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

@Component
public class AgentLoop {
    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final int MAX_ROUNDS = 15;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ChatLanguageModel primaryModel;
    private final ChatLanguageModel fallbackModel;
    private final ChatLanguageModel deepThinkingModel;
    private final ToolRegistry toolRegistry;

    private static final String SYSTEM_PROMPT = """
        你是一个本地短时活动规划助手。用户想在下午安排4-6小时的休闲活动。

        你需要:
        1. 理解用户场景(家庭/朋友、人数、偏好、年龄等)
        2. 按顺序规划: 查天气 → 找活动 → 找餐厅 → 查排队/路线 → 汇总方案
        3. 生成最终方案给用户确认，方案需包含时间线、活动详情、餐厅选项、路线建议
        4. 用户确认后，执行下单/预订/通知等操作

        规则:
        - 每次只调用一个工具，等待结果后再决定下一步
        - 家庭场景优先考虑: 亲子友好、安全、适合孩子年龄、有轻食选项
        - 距离控制在5km以内，除非特别说明
        - 最终方案必须包含时间线和备选选项
        - 输出最终方案时，不要说"需要我确认"，直接展示完整计划
        """;

    public AgentLoop(@Qualifier("primaryModel") ChatLanguageModel primaryModel,
                     @Qualifier("fallbackModel") ChatLanguageModel fallbackModel,
                     @Qualifier("deepThinkingModel") ChatLanguageModel deepThinkingModel,
                     ToolRegistry toolRegistry) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.deepThinkingModel = deepThinkingModel;
        this.toolRegistry = toolRegistry;
    }

    public Flux<AgentStep> execute(SessionContext ctx, String userGoal) {
        return execute(ctx, userGoal, false);
    }

    public Flux<AgentStep> execute(SessionContext ctx, String userGoal, boolean deepThinking) {
        return Flux.create(sink -> {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(SYSTEM_PROMPT));
            for (ChatMessage msg : ctx.conversationHistory) {
                if (!(msg instanceof SystemMessage)) {
                    messages.add(msg);
                }
            }
            messages.add(UserMessage.from(userGoal));

            var specs = toolRegistry.getSpecifications();
            ChatLanguageModel model = deepThinking ? deepThinkingModel : primaryModel;

            for (int round = 0; round < MAX_ROUNDS; round++) {
                try {
                    ChatRequest request = ChatRequest.builder()
                        .messages(messages)
                        .toolSpecifications(specs)
                        .build();
                    ChatResponse response = model.chat(request);
                    AiMessage aiMessage = response.aiMessage();

                    if (aiMessage.hasToolExecutionRequests()) {
                        String thought = aiMessage.text();
                        if (thought != null && !thought.isBlank()) {
                            sink.next(AgentStep.thought(thought));
                        }

                        for (var req : aiMessage.toolExecutionRequests()) {
                            sink.next(AgentStep.action(req.name(), req.arguments()));

                            String result = toolRegistry.execute(req);
                            sink.next(AgentStep.observation(result));

                            messages.add(aiMessage);
                            messages.add(ToolExecutionResultMessage.from(req, result));
                        }
                    } else {
                        messages.add(aiMessage);
                        sink.next(AgentStep.finalPlan(aiMessage.text()));
                        ctx.conversationHistory = new ArrayList<>(messages);
                        sink.complete();
                        return;
                    }
                } catch (Exception e) {
                    log.error("Agent loop error at round {}: {}", round, e.getMessage());
                    if (model == primaryModel) {
                        log.info("Switching to fallback model");
                        model = fallbackModel;
                        sink.next(AgentStep.error("主模型异常，已切换到备用模型"));
                        round--;
                    } else {
                        ctx.conversationHistory = new ArrayList<>(messages);
                        sink.next(AgentStep.error("规划失败: " + e.getMessage()));
                        sink.complete();
                        return;
                    }
                }
            }
            ctx.conversationHistory = new ArrayList<>(messages);
            sink.next(AgentStep.error("超过最大规划轮数"));
            sink.complete();
        });
    }

    public Flux<AgentStep> confirm(SessionContext session) {
        return Flux.create(sink -> {
            String plan = session.confirmedPlan;
            var specs = toolRegistry.getSpecifications();
            String confirmPrompt = "用户已确认以下计划，请执行所有需要的操作(预订、下单、支付、通知):\n\n" + plan;

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(SYSTEM_PROMPT));
            messages.add(UserMessage.from(confirmPrompt));

            try {
                ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(specs)
                    .build();
                ChatResponse response = primaryModel.chat(request);
                AiMessage aiMessage = response.aiMessage();

                if (aiMessage.hasToolExecutionRequests()) {
                    for (var req : aiMessage.toolExecutionRequests()) {
                        sink.next(AgentStep.confirming("正在执行: " + req.name()));
                        String result = toolRegistry.execute(req);
                        log.info("Execution result: {}", result);
                    }
                }
                sink.next(AgentStep.done("🎉 全部搞定! 计划已安排好，通知已发送。"));
                sink.complete();
            } catch (Exception e) {
                sink.next(AgentStep.error("执行失败: " + e.getMessage()));
                sink.complete();
            }
        });
    }
}
