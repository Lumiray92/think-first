package com.thinkfirst.vault;

import com.thinkfirst.auth.InvalidCredentialsException;
import com.thinkfirst.auth.User;
import com.thinkfirst.auth.UserRepository;
import com.thinkfirst.vault.dto.ApiKeyView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Pattern;

@Service
public class ApiKeyService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^sk-[A-Za-z0-9_-]{8,200}$");

    private final UserRepository userRepository;
    private final ApiKeyCrypto crypto;
    private final DeepSeekKeyValidator validator;

    public ApiKeyService(UserRepository userRepository, ApiKeyCrypto crypto,
                         DeepSeekKeyValidator validator) {
        this.userRepository = userRepository;
        this.crypto = crypto;
        this.validator = validator;
    }

    @Transactional
    public ApiKeyView save(Long userId, String rawKey) {
        String apiKey = checkFormat(rawKey);
        validator.validate(apiKey);
        User user = requireUser(userId);
        user.setApiKeyCipher(crypto.encrypt(apiKey));
        user.setApiKeyMask(mask(apiKey));
        user.setApiKeyUpdatedAt(Instant.now());
        userRepository.save(user);
        return view(userId);
    }

    @Transactional(readOnly = true)
    public ApiKeyView view(Long userId) {
        User user = requireUser(userId);
        return new ApiKeyView(user.getApiKeyMask(), user.getApiKeyCipher() != null,
                user.getApiKeyUpdatedAt());
    }

    /** 供后续 LLM 调用使用；调用方不得回传或记录返回值。 */
    @Transactional(readOnly = true)
    public String requirePlaintext(Long userId) {
        User user = requireUser(userId);
        if (user.getApiKeyCipher() == null) {
            throw new ApiKeyException("请先在设置中保存 DeepSeek API Key");
        }
        return crypto.decrypt(user.getApiKeyCipher());
    }

    public void validateOnly(String rawKey) {
        validator.validate(checkFormat(rawKey));
    }

    @Transactional
    public ApiKeyView clear(Long userId) {
        User user = requireUser(userId);
        user.setApiKeyCipher(null);
        user.setApiKeyMask(null);
        user.setApiKeyUpdatedAt(null);
        userRepository.save(user);
        return view(userId);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("账号不存在"));
    }

    private String checkFormat(String rawKey) {
        String apiKey = rawKey == null ? "" : rawKey.trim();
        if (!KEY_PATTERN.matcher(apiKey).matches()) {
            throw new ApiKeyException("Key 格式不正确，应以 sk- 开头");
        }
        return apiKey;
    }

    static String mask(String apiKey) {
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 3) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
