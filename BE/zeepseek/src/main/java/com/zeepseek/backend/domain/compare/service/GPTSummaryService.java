package com.zeepseek.backend.domain.compare.service;

import com.zeepseek.backend.domain.compare.entity.DongInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GPTSummaryService {

    private final WebClient webClient;

    // application.properties 또는 환경변수로부터 API 키를 주입받습니다.
    @Value("${openai.api.key}")
    private String openAiApiKey;

    public GPTSummaryService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public String getSummaryForDong(DongInfo dong) {
        String prompt = String.format("동네 %s(%s)의 안전 점수는 %.1f, 여가 점수는 %.1f입니다. 이 동네를 한 문장으로 요약해줘.",
                dong.getName(), dong.getGuName(), dong.getSafe(), dong.getLeisure());

        // OpenAI API 호출을 위한 요청 본문 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "text-davinci-003");  // 사용할 모델 선택
        requestBody.put("prompt", prompt);
        requestBody.put("max_tokens", 50);
        requestBody.put("temperature", 0.7);

        try {
            OpenAiResponse response = webClient.post()
                    .uri("/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getText().trim();
            }
        } catch (Exception e) {
            // 로깅 등 에러 처리를 진행합니다.
            e.printStackTrace();
        }
        // API 호출에 실패할 경우 기본 요약문 반환
        return "예시 요약문: " + dong.getName() + " 동네는 안전하고 여가 시설이 잘 갖추어져 있습니다.";
    }

    // OpenAI API 응답 매핑용 내부 클래스 (필요한 필드만 포함)
    public static class OpenAiResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }

        public static class Choice {
            private String text;

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }
    }
}
