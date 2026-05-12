package com.webox.webox.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.webox.entity.MenuItem;
import com.webox.webox.support.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QwenClient {

    private static final Logger log = LoggerFactory.getLogger(QwenClient.class);

    private final RestClient restClient;
    private final QwenProperties props;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper mapper = new ObjectMapper();

    public QwenClient(RestClient aiRestClient, QwenProperties props, PromptBuilder promptBuilder) {
        this.restClient = aiRestClient;
        this.props = props;
        this.promptBuilder = promptBuilder;
    }

    public List<Recommendation> recommend(String query, List<String> allergens, List<MenuItem> menu) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new AiUnavailableException("API Key not configured");
        }

        List<Map<String, String>> messages = promptBuilder.build(query, allergens, menu);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        Map<String, String> fmt = new LinkedHashMap<>();
        fmt.put("type", "json_object");
        body.put("response_format", fmt);

        long t0 = System.currentTimeMillis();
        try {
            ChatResponse resp = restClient.post()
                    .uri(props.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);

            long cost = System.currentTimeMillis() - t0;
            log.info("qwen call ok, cost={}ms, model={}", cost, props.getModel());

            if (resp == null || resp.choices == null || resp.choices.isEmpty()) {
                throw new AiUnavailableException("Empty response from Qwen");
            }
            String content = resp.choices.get(0).message == null ? null : resp.choices.get(0).message.content;
            if (content == null || content.isBlank()) {
                throw new AiUnavailableException("Empty content from Qwen");
            }
            RecommendationEnvelope env = mapper.readValue(content, RecommendationEnvelope.class);
            return env.recommendations == null ? new ArrayList<>() : env.recommendations;
        } catch (RestClientException e) {
            log.warn("qwen call failed: {}", e.getMessage());
            throw new AiUnavailableException("Qwen request failed", e);
        } catch (Exception e) {
            log.warn("qwen parse failed: {}", e.getMessage());
            throw new AiUnavailableException("Qwen response parse failed", e);
        }
    }

    public static class Recommendation {
        public String code;
        public String reason;

        public Recommendation() {
        }

        public Recommendation(String code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        public String getCode() {
            return code;
        }

        public String getReason() {
            return reason;
        }
    }

    public static class RecommendationEnvelope {
        public List<Recommendation> recommendations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Message {
        public String role;
        public String content;
    }
}
