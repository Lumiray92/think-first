package com.thinkfirst.vault;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加解密。每次加密使用随机 12 字节 IV，存储格式为 base64(iv):base64(cipher+tag)。
 */
@Component
public class ApiKeyCrypto {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyCrypto(ApiKeyProperties properties) {
        byte[] raw;
        try {
            if (properties.getMasterKey() == null || properties.getMasterKey().isBlank()) {
                throw new IllegalStateException("请设置 TF_MASTER_KEY（32 字节 Base64）");
            }
            raw = Base64.getDecoder().decode(properties.getMasterKey());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("security.api-key.master-key 不是合法的 Base64");
        }
        if (raw.length != 32) {
            throw new IllegalStateException("security.api-key.master-key 必须是 32 字节，请设置 TF_MASTER_KEY");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new KeyVaultUnavailableException("API Key 加密失败");
        }
    }

    public String decrypt(String stored) {
        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("bad format");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new KeyVaultUnavailableException("API Key 解密失败，请重新保存");
        }
    }
}
