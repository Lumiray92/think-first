package com.thinkfirst.vault;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** 用一次 max_tokens=1 的请求确认 Key 可用；任何情况下都不记录 Key 明文。 */
@Component
public class DeepSeekKeyValidator {

    private final ApiKeyProperties properties;
    private final RestClient client;

    public DeepSeekKeyValidator(ApiKeyProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.client = RestClient.builder()
                .baseUrl(properties.getDeepseekBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public void validate(String apiKey) {
        if (!properties.isValidationEnabled()) {
            return;
        }
        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(Map.of("role", "user", "content", "ping")),
                "max_tokens", 1,
                "stream", false
        );
        try {
            client.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new ApiKeyException("Key 无效或没有权限，请检查后重试");
            }
            if (status == 402) {
                throw new ApiKeyException("Key 有效，但账户余额不足，请先充值");
            }
            if (status == 429) {
                throw new ApiKeyException("校验请求过于频繁，请稍后再试");
            }
            throw new ApiKeyException("Key 校验未通过（HTTP " + status + "）");
        } catch (RestClientException e) {
            throw new KeyVaultUnavailableException("暂时无法连接 DeepSeek 校验服务，请稍后再试");
        }
    }
}
