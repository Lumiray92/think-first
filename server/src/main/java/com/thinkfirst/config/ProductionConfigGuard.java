package com.thinkfirst.config;

import com.thinkfirst.auth.JwtProperties;
import com.thinkfirst.vault.ApiKeyProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 生产环境启动自检：拒绝仓库里的开发默认密钥，避免把演示配置带上线。
 * 只在 prod profile 下生效；本地开发和测试不受影响。
 */
@Component
@Profile("prod")
public class ProductionConfigGuard implements InitializingBean {

    static final String DEV_JWT_SECRET = "thinkfirst-dev-jwt-secret-change-me-0123456789";
    static final String DEV_MASTER_KEY = "bmuad7ZwbJTQNUBWidravaQbR/xahc68kMb/Hd2zTXc=";

    private final JwtProperties jwt;
    private final ApiKeyProperties apiKey;

    public ProductionConfigGuard(JwtProperties jwt, ApiKeyProperties apiKey) {
        this.jwt = jwt;
        this.apiKey = apiKey;
    }

    @Override
    public void afterPropertiesSet() {
        String secret = jwt.getSecret();
        if (secret == null || secret.isBlank() || DEV_JWT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "生产环境必须设置 TF_JWT_SECRET，且不能使用开发默认值");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("TF_JWT_SECRET 至少需要 32 字节");
        }

        String masterKey = apiKey.getMasterKey();
        if (masterKey == null || masterKey.isBlank() || DEV_MASTER_KEY.equals(masterKey)) {
            throw new IllegalStateException(
                    "生产环境必须设置 TF_MASTER_KEY，且不能使用开发默认值");
        }
        try {
            if (Base64.getDecoder().decode(masterKey).length != 32) {
                throw new IllegalStateException("TF_MASTER_KEY 必须是 32 字节的 Base64");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("TF_MASTER_KEY 不是合法的 Base64", e);
        }
    }
}
