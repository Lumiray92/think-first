package com.thinkfirst.vault;

import com.thinkfirst.auth.CurrentUser;
import com.thinkfirst.common.ApiResponse;
import com.thinkfirst.vault.dto.ApiKeyRequest;
import com.thinkfirst.vault.dto.ApiKeyView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user/api-key")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final CurrentUser currentUser;

    public ApiKeyController(ApiKeyService apiKeyService, CurrentUser currentUser) {
        this.apiKeyService = apiKeyService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<ApiKeyView> get() {
        return ApiResponse.ok(apiKeyService.view(currentUser.id()));
    }

    @PutMapping
    public ApiResponse<ApiKeyView> save(@Valid @RequestBody ApiKeyRequest request) {
        return ApiResponse.ok(apiKeyService.save(currentUser.id(), request.apiKey()));
    }

    @PostMapping("/validate")
    public ApiResponse<Map<String, Boolean>> validate(@Valid @RequestBody ApiKeyRequest request) {
        apiKeyService.validateOnly(request.apiKey());
        return ApiResponse.ok(Map.of("valid", true));
    }

    @DeleteMapping
    public ApiResponse<ApiKeyView> clear() {
        return ApiResponse.ok(apiKeyService.clear(currentUser.id()));
    }
}
