package com.thinkfirst.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 60;
    private int slotsMaxTokens = 4000;
    public int getSlotsMaxTokens() { return slotsMaxTokens; }
    public void setSlotsMaxTokens(int value) { slotsMaxTokens = value; }
    private int questionMaxTokens = 1000;
    private int polishMaxTokens = 10000;
    public int getPolishMaxTokens() { return polishMaxTokens; }
    public void setPolishMaxTokens(int value) { polishMaxTokens = value; }
    private int prdMaxTokens = 3000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getQuestionMaxTokens() {
        return questionMaxTokens;
    }

    public void setQuestionMaxTokens(int questionMaxTokens) {
        this.questionMaxTokens = questionMaxTokens;
    }

    public int getPrdMaxTokens() {
        return prdMaxTokens;
    }

    public void setPrdMaxTokens(int prdMaxTokens) {
        this.prdMaxTokens = prdMaxTokens;
    }
}
