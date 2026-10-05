package com.monday.app.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.ai.gateway.AiGateway;
import com.monday.app.ai.model.AiProvider;
import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;
import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleSummary;
import com.monday.app.intelligence.entity.DailyArticleSelection;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class IntelligenceSummaryService {

    private static final String PROMPT_VERSION = "INTELLIGENCE_SUMMARY_V1";
    private static final int MAX_WORDS = 249;
    
    private final AiGateway aiGateway;
    private final ArticleSummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;

    public IntelligenceSummaryService(AiGateway aiGateway, 
                                      ArticleSummaryRepository summaryRepository,
                                      ObjectMapper objectMapper) {
        this.aiGateway = aiGateway;
        this.summaryRepository = summaryRepository;
        this.objectMapper = objectMapper;
    }

    public ArticleSummary generateSummary(DailyArticleSelection selection) {
        Article article = selection.getArticle();

        Map<String, String> payload = new HashMap<>();
        payload.put("articleTitle", article.getTitle());
        payload.put("sourceName", "RSS Feed"); // Should be derived from ArticleSource if available
        payload.put("publishedAt", article.getPublishedAt() != null ? article.getPublishedAt().toString() : "");
        payload.put("articleUrl", article.getCanonicalUrl());
        payload.put("content", article.getContentText());

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return saveFailure(article, "Failed to construct JSON payload: " + e.getMessage());
        }

        String prompt = "Summarize the following article in 80 to 150 words. Do not exceed 249 words. " +
                        "Do not invent facts or speculate. Preserve important names and numbers. " +
                        "Article data:\n" + jsonPayload;

        AiRequest request = new AiRequest(AiProvider.OPENAI, "gpt-4", prompt); // model can be configurable

        try {
            AiResponse response = aiGateway.generate(request);
            String summaryText = response.content();
            
            int wordCount = summaryText.split("\\s+").length;
            if (wordCount > MAX_WORDS) {
                return saveFailure(article, "Summary exceeded maximum word count of 249. Actual: " + wordCount);
            }

            ArticleSummary summary = new ArticleSummary();
            summary.setArticle(article);
            summary.setSummary(summaryText);
            summary.setModel("gpt-4");
            summary.setPromptVersion(PROMPT_VERSION);
            summary.setWordCount(wordCount);
            summary.setGeneratedAt(OffsetDateTime.now());
            summary.setStatus("SUCCESS");
            return summaryRepository.save(summary);
            
        } catch (Exception e) {
            return saveFailure(article, "AI Gateway failed: " + e.getMessage());
        }
    }

    private ArticleSummary saveFailure(Article article, String reason) {
        ArticleSummary summary = new ArticleSummary();
        summary.setArticle(article);
        summary.setStatus("FAILED");
        summary.setFailureReason(reason);
        return summaryRepository.save(summary);
    }
}
