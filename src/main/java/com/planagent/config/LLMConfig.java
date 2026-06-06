package com.planagent.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class LLMConfig {

    @Value("${llm.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${llm.deepseek.base-url}")
    private String deepseekBaseUrl;

    @Value("${llm.deepseek.model-name}")
    private String deepseekModel;

    @Value("${llm.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${llm.qwen.base-url}")
    private String qwenBaseUrl;

    @Value("${llm.qwen.model-name}")
    private String qwenModel;

    @Bean
    @Primary
    public ChatLanguageModel primaryModel() {
        String apiKey = !deepseekApiKey.isEmpty() ? deepseekApiKey : System.getenv("DEEPSEEK_API_KEY");
        return OpenAiChatModel.builder()
            .baseUrl(deepseekBaseUrl)
            .apiKey(apiKey)
            .modelName(deepseekModel)
            .timeout(Duration.ofSeconds(60))
            .maxRetries(2)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Bean
    public ChatLanguageModel fallbackModel() {
        String apiKey = !qwenApiKey.isEmpty() ? qwenApiKey : System.getenv("QWEN_API_KEY");
        return OpenAiChatModel.builder()
            .baseUrl(qwenBaseUrl)
            .apiKey(apiKey)
            .modelName(qwenModel)
            .timeout(Duration.ofSeconds(60))
            .maxRetries(2)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Bean
    public ChatLanguageModel deepThinkingModel() {
        String apiKey = !deepseekApiKey.isEmpty() ? deepseekApiKey : System.getenv("DEEPSEEK_API_KEY");
        return OpenAiChatModel.builder()
            .baseUrl(deepseekBaseUrl)
            .apiKey(apiKey)
            .modelName("deepseek-v4-pro")
            .timeout(Duration.ofSeconds(120))
            .maxRetries(2)
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
