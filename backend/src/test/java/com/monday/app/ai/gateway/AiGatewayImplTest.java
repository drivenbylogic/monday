package com.monday.app.ai.gateway;

import com.monday.app.ai.model.AiProvider;
import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;
import com.monday.app.ai.provider.AiProviderClient;
import com.monday.app.ai.provider.AiProviderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AiGatewayImplTest {

    private AiProviderResolver resolver;
    private AiProviderClient client;
    private AiGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        resolver = mock(AiProviderResolver.class);
        client = mock(AiProviderClient.class);
        gateway = new AiGatewayImpl(resolver);
    }

    @Test
    void generate_delegatesToResolvedProvider() {
        AiRequest request = new AiRequest(AiProvider.OPENAI, "model", "prompt");
        AiResponse expectedResponse = new AiResponse("response content");

        when(resolver.resolve(AiProvider.OPENAI)).thenReturn(client);
        when(client.generate(request)).thenReturn(expectedResponse);

        AiResponse actualResponse = gateway.generate(request);

        assertEquals(expectedResponse, actualResponse);
        verify(resolver).resolve(AiProvider.OPENAI);
        verify(client).generate(request);
    }
}
