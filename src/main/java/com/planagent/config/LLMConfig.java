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

    @Value("${llm.primary:deepseek}")
    private String primary;

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
        if ("qwen".equalsIgnoreCase(primary)) {
            return buildModel(qwenApiKey, "QWEN_API_KEY", qwenBaseUrl, qwenModel);
        }
        return buildModel(deepseekApiKey, "DEEPSEEK_API_KEY", deepseekBaseUrl, deepseekModel);
    }

    @Bean
    public ChatLanguageModel fallbackModel() {
        if ("qwen".equalsIgnoreCase(primary)) {
            return buildModel(deepseekApiKey, "DEEPSEEK_API_KEY", deepseekBaseUrl, deepseekModel);
        }
        return buildModel(qwenApiKey, "QWEN_API_KEY", qwenBaseUrl, qwenModel);
    }

    private ChatLanguageModel buildModel(String apiKey, String envVar, String baseUrl, String model) {
        String key = (apiKey != null && !apiKey.isEmpty()) ? apiKey : System.getenv(envVar);
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(key != null ? key : "")
            .modelName(model)
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
