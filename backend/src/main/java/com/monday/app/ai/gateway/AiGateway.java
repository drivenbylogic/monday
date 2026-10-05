package com.monday.app.ai.gateway;

import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;

public interface AiGateway {
    AiResponse generate(AiRequest request);
}
