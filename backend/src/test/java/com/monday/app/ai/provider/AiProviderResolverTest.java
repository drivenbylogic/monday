package com.monday.app.ai.provider;

import com.monday.app.ai.exception.AiException;
import com.monday.app.ai.model.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderResolverTest {

    @Test
    void resolve_returnsConfiguredProvider() {
        AiProviderClient client = mock(AiProviderClient.class);
        when(client.getProvider()).thenReturn(AiProvider.OPENAI);

        AiProviderResolver resolver = new AiProviderResolver(List.of(client), AiProvider.OPENAI);

        AiProviderClient resolved = resolver.resolve(AiProvider.OPENAI);
        assertEquals(client, resolved);
    }

    @Test
    void resolve_returnsDefaultWhenRequestedIsNull() {
        AiProviderClient client = mock(AiProviderClient.class);
        when(client.getProvider()).thenReturn(AiProvider.OPENAI);

        AiProviderResolver resolver = new AiProviderResolver(List.of(client), AiProvider.OPENAI);

        AiProviderClient resolved = resolver.resolve(null);
        assertEquals(client, resolved);
    }

    @Test
    void resolve_throwsExceptionForUnsupportedProvider() {
        AiProviderClient client = mock(AiProviderClient.class);
        when(client.getProvider()).thenReturn(AiProvider.OPENAI);

        AiProviderResolver resolver = new AiProviderResolver(List.of(client), AiProvider.OPENAI);

        // We can't actually pass a missing provider easily with a single enum value OPENAI,
        // but if we simulate a scenario where the map doesn't contain it, e.g. empty list:
        AiProviderResolver emptyResolver = new AiProviderResolver(List.of(), AiProvider.OPENAI);

        AiException exception = assertThrows(AiException.class, () -> emptyResolver.resolve(AiProvider.OPENAI));
        assertTrue(exception.getMessage().contains("Unsupported or unconfigured AI provider"));
    }
}
