package com.thinkfirst.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class HttpDeepSeekClient implements DeepSeekClient {

    private final LlmProperties properties;
    private final RestClient client;

    public HttpDeepSeekClient(LlmProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(Math.max(120, properties.getTimeoutSeconds()) * 1000);
        this.client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public Completion complete(String apiKey, String system, String user,
                               double temperature, int maxTokens) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", false
        );
        try {
            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new LlmException("模型返回为空");
            }
            if ("length".equals(response.path("choices").path(0).path("finish_reason").asText())) {
                throw new LlmException("生成内容超出长度限制，请精简回答后重试");
            }
            String content = response.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new LlmException("模型返回为空");
            }
            JsonNode usage = response.path("usage");
            return new Completion(content,
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0));
        } catch (HttpClientErrorException e) {
            throw new LlmException(messageFor(e.getStatusCode().value()));
        } catch (RestClientException e) {
            throw new LlmException("模型服务暂时不可用");
        }
    }

    private static String messageFor(int status) {
        return switch (status) {
            case 401, 403 -> "API Key 无效或没有权限";
            case 402 -> "API Key 余额不足";
            case 429 -> "模型请求过于频繁";
            default -> "模型服务暂时不可用（HTTP " + status + "）";
        };
    }
}
