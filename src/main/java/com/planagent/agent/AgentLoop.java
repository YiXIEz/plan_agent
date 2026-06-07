package com.planagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planagent.agent.ReplanningHandler;
import com.planagent.agent.ReplanningHandler.FailureType;
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
    private static final int MAX_REPLANS = 3;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ChatLanguageModel primaryModel;
    private final ChatLanguageModel fallbackModel;
    private final ChatLanguageModel deepThinkingModel;
    private final ToolRegistry toolRegistry;
    private final ReplanningHandler replanningHandler;

    private static final String SYSTEM_PROMPT = """
        你是一个本地短时活动规划助手。用户在下午安排4-6小时的休闲活动。

        你需要:
        1. 理解用户场景(家庭/朋友、人数、偏好、年龄等)
        2. 按顺序规划: 查天气 → 找活动 → 找餐厅 → 查排队/路线 → 汇总方案
        3. 生成最终方案，方案需包含时间线、活动详情、餐厅选项、路线建议

        规则:
        - 每次只调用一个工具，等待结果后再决定下一步
        - 家庭场景优先考虑: 亲子友好、安全、适合孩子年龄、有轻食选项
        - 距离控制在5km以内，除非特别说明
        - 最终方案必须包含时间线和备选选项
        - 输出最终方案时，直接展示完整计划

        重要规则:
        - 如果用户没有明确指定城市或区域，必须先追问，不能自己假设地点
        - 只能用工具返回的真实数据来规划，不能凭空编造地点名称、距离、评分
        - 如果工具返回的数据不够好或不相关，如实告知用户并请求更具体的需求
        - 不要编造不存在的地点、餐厅名或活动名
        - 信息不足时先问清楚再规划，不要猜测
        """;

    public AgentLoop(@Qualifier("primaryModel") ChatLanguageModel primaryModel,
                     @Qualifier("fallbackModel") ChatLanguageModel fallbackModel,
                     @Qualifier("deepThinkingModel") ChatLanguageModel deepThinkingModel,
                     ToolRegistry toolRegistry,
                     ReplanningHandler replanningHandler) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.deepThinkingModel = deepThinkingModel;
        this.toolRegistry = toolRegistry;
        this.replanningHandler = replanningHandler;
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
                        messages.add(aiMessage);

                        for (var req : aiMessage.toolExecutionRequests()) {
                            sink.next(AgentStep.action(req.name(), req.arguments()));

                            String result = toolRegistry.execute(req, ctx);
                            sink.next(AgentStep.observation(result));

                            messages.add(ToolExecutionResultMessage.from(req, result));
                        }
                    } else {
                        String fullText = aiMessage.text();
                        messages.add(aiMessage);
                        streamText(sink, fullText);
                        sink.next(AgentStep.finalPlan(fullText));
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

    private void streamText(reactor.core.publisher.FluxSink<AgentStep> sink, String text) {
        if (text == null || text.isEmpty()) return;
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + 4, text.length());
            while (end < text.length() && end - i < 8 && !isBreakChar(text.charAt(end))) { end++; }
            if (end - i > 8) end = i + 4;
            sink.next(AgentStep.token(text.substring(i, end)));
            i = end;
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }

    private boolean isBreakChar(char c) {
        return c == '。' || c == '，' || c == '、' || c == '！' || c == '？' || c == '\n'
            || c == '.' || c == ',' || c == '!' || c == '?' || c == ' ' || c == '：' || c == '；';
    }

    public Flux<AgentStep> confirm(SessionContext session, String userMessage) {
        return Flux.create(sink -> {
            String currentPrompt = "用户已确认以下计划，请执行所有需要的操作(预订、下单、支付、通知):\n\n"
                + session.confirmedPlan + "\n\n用户消息: " + userMessage;
            var specs = toolRegistry.getSpecifications();
            int replanCount = 0;
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(SYSTEM_PROMPT));
            messages.add(UserMessage.from(currentPrompt));

            while (replanCount <= MAX_REPLANS) {
                try {
                    ChatRequest request = ChatRequest.builder()
                        .messages(messages)
                        .toolSpecifications(specs)
                        .build();
                    ChatResponse response = primaryModel.chat(request);
                    AiMessage aiMessage = response.aiMessage();

                    if (aiMessage.hasToolExecutionRequests()) {
                        boolean allSuccess = true;
                        messages.add(aiMessage);
                        for (var req : aiMessage.toolExecutionRequests()) {
                            sink.next(AgentStep.confirming("正在执行: " + req.name()));
                            String result = toolRegistry.execute(req, session);
                            log.info("Execution result: {}", result);

                            messages.add(ToolExecutionResultMessage.from(req, result));

                            FailureType failure = replanningHandler.analyze(result);

                            if (replanningHandler.isFailure(failure)) {
                                allSuccess = false;
                                if (replanCount >= MAX_REPLANS) {
                                    sink.next(AgentStep.error(
                                        "自动调整" + MAX_REPLANS + "次仍失败，请手动修改方案"));
                                    sink.complete();
                                    return;
                                }
                                replanCount++;
                                String replanPrompt = replanningHandler
                                    .generateReplanPromptWithExpandedSearch(
                                        req.name(), "目标项", "失败: " + result,
                                        session.confirmedPlan, replanCount);
                                messages.add(UserMessage.from(replanPrompt));
                                break;
                            }
                        }
                        if (allSuccess) {
                            sink.next(AgentStep.done("🎉 全部搞定! 计划已安排好，通知已发送。"));
                            sink.complete();
                            return;
                        }
                    } else {
                        messages.add(aiMessage);
                        sink.next(AgentStep.done(aiMessage.text()));
                        sink.complete();
                        return;
                    }
                } catch (Exception e) {
                    log.error("Confirmation error: {}", e.getMessage());
                    sink.next(AgentStep.error("执行失败: " + e.getMessage()));
                    sink.complete();
                    return;
                }
            }
        });
    }
}
