package com.thinkfirst.auth;

import com.thinkfirst.auth.dto.AuthResponse;
import com.thinkfirst.auth.dto.LoginRequest;
import com.thinkfirst.auth.dto.RegisterRequest;
import com.thinkfirst.auth.dto.UserView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    public AuthResponse register(RegisterRequest request) {
        checkPasswordBytes(request.password());
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        checkPasswordBytes(request.password());
        String email = normalize(request.email());
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        // 不存在的邮箱也执行一次哈希校验，减少凭响应耗时枚举账号的机会。
        boolean matches = passwordEncoder.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }
        return response(user);
    }

    public UserView me(Long userId) {
        return toView(userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("账号不存在")));
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.issue(user.getId(), user.getEmail()), toView(user));
    }

    public static UserView toView(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getApiKeyCipher() != null,
                user.getApiKeyMask(), user.getApiKeyUpdatedAt());
    }

    private void checkPasswordBytes(String password) {
        // BCrypt 的上限是字节数，中文密码不能简单按字符数量判断。
        if (password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("密码不能超过72字节，中文字符通常占3字节");
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
