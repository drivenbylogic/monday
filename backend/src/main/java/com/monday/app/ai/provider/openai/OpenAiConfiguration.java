package com.monday.app.ai.provider.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenAiConfiguration {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Bean
    public OpenAIClient openAIClient() {
        if (!StringUtils.hasText(apiKey)) {
            // Optional: return a dummy client or let the context start and fail on use
            // For now, we instantiate it anyway. If apiKey is empty, it might fail at runtime.
            return OpenAIOkHttpClient.builder().apiKey("dummy").build();
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
