package com.monday.app.ai.provider.openai;

import com.monday.app.ai.exception.AiException;
import com.monday.app.ai.model.AiProvider;
import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;
import com.monday.app.ai.provider.AiProviderClient;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiProviderClient implements AiProviderClient {

    private final OpenAIClient openAIClient;

    public OpenAiProviderClient(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(request.model())
                    .messages(List.of(
                            ChatCompletionMessageParam.ofUser(
                                    ChatCompletionUserMessageParam.builder()
                                            .content(request.prompt())
                                            .build()
                            )
                    ))
                    .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);
            
            String content = completion.choices().get(0).message().content().orElse("");
            return new AiResponse(content);
        } catch (Exception e) {
            throw new AiException("Failed to generate response from OpenAI: " + e.getMessage(), e);
        }
    }
}
