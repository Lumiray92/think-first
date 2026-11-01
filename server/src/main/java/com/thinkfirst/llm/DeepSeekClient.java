package com.thinkfirst.llm;

public interface DeepSeekClient {

    Completion complete(String apiKey, String system, String user, double temperature, int maxTokens);

    record Completion(String content, int promptTokens, int completionTokens) {
    }
}
