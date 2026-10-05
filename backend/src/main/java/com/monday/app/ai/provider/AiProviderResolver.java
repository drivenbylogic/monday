package com.monday.app.ai.provider;

import com.monday.app.ai.exception.AiException;
import com.monday.app.ai.model.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiProviderResolver {

    private final Map<AiProvider, AiProviderClient> providers;
    private final AiProvider defaultProvider;

    public AiProviderResolver(
            List<AiProviderClient> clientList,
            @Value("${ai.default-provider:OPENAI}") AiProvider defaultProvider) {
        this.providers = clientList.stream()
                .collect(Collectors.toMap(AiProviderClient::getProvider, Function.identity()));
        this.defaultProvider = defaultProvider;
    }

    public AiProviderClient resolve(AiProvider requestedProvider) {
        AiProvider providerToUse = requestedProvider != null ? requestedProvider : defaultProvider;
        
        AiProviderClient client = providers.get(providerToUse);
        if (client == null) {
            throw new AiException("Unsupported or unconfigured AI provider: " + providerToUse);
        }
        
        return client;
    }
}
