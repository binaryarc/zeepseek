package com.zeepseek.backend.domain.dong.service;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.entity.DongInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GPTSummaryService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final WebClient webClient;

    public GPTSummaryService(WebClient.Builder webClientBuilder) {
        // Use the base OpenAI API URL
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
    }

    public String getSummaryForDong(DongInfo dong) {
        String prompt = String.format("%s(%s)의 특징과 장점을 3줄 요약해줘. 마무리를 반드시 지어야 해.",
                dong.getName(), dong.getGuName());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo"); // Updated model
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 200);
        requestBody.put("temperature", 0.7);

        try {
            // Use the newer chat completions endpoint
            OpenAiResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // API 호출 실패 시 기본 요약문 반환
        return dong.getName() + " 동은 안전하고 여가 시설이 잘 갖추어져 있습니다.";
    }

    public String getSummaryForDongCompare(DongInfoDocs dong1, DongInfoDocs dong2) {
        String prompt = String.format("%s(%s)의 특징과 장점을 3줄 요약해줘. 마무리를 반드시 지어야 해.",
                dong1.getName(), dong1.getGuName());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo"); // Updated model
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 200);
        requestBody.put("temperature", 0.7);

        try {
            // Use the newer chat completions endpoint
            OpenAiResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // API 호출 실패 시 기본 요약문 반환
        return dong1.getName() + " 동은 안전하고 여가 시설이 잘 갖추어져 있습니다.";
    }

    // Updated OpenAI API response mapping class
    public static class OpenAiResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }
        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }
        public static class Choice {
            private Message message;
            public Message getMessage() {
                return message;
            }
            public void setMessage(Message message) {
                this.message = message;
            }
        }
        public static class Message {
            private String content;
            public String getContent() {
                return content;
            }
            public void setContent(String content) {
                this.content = content;
            }
        }
    }
}