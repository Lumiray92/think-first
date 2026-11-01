package com.thinkfirst.vault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.api-key")
public class ApiKeyProperties {

    /** 32 字节 AES 主密钥的 Base64，生产环境必须由 TF_MASTER_KEY 覆盖。 */
    private String masterKey;
    private boolean validationEnabled = true;
    private String deepseekBaseUrl = "https://api.deepseek.com";

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    public String getDeepseekBaseUrl() {
        return deepseekBaseUrl;
    }

    public void setDeepseekBaseUrl(String deepseekBaseUrl) {
        this.deepseekBaseUrl = deepseekBaseUrl;
    }
}
