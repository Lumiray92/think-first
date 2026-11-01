package com.thinkfirst.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 190, message = "邮箱过长")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度需为 8-72 位")
        String password
) {
    @Override public String toString() { return "敏感请求（内容已隐藏）"; }
}
