package com.monday.app.ai.gateway;

import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;
import com.monday.app.ai.provider.AiProviderClient;
import com.monday.app.ai.provider.AiProviderResolver;
import org.springframework.stereotype.Service;

@Service
public class AiGatewayImpl implements AiGateway {

    private final AiProviderResolver providerResolver;

    public AiGatewayImpl(AiProviderResolver providerResolver) {
        this.providerResolver = providerResolver;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        AiProviderClient client = providerResolver.resolve(request.provider());
        return client.generate(request);
    }
}
