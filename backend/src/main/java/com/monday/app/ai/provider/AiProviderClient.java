package com.monday.app.ai.provider;

import com.monday.app.ai.model.AiProvider;
import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;

public interface AiProviderClient {
    AiProvider getProvider();
    AiResponse generate(AiRequest request);
}
