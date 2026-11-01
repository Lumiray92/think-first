package com.thinkfirst.vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiKeyRequest(
        @NotBlank(message = "API Key 不能为空")
        @Size(max = 200, message = "API Key 过长")
        String apiKey
) {
    @Override public String toString() { return "敏感请求（内容已隐藏）"; }
}
