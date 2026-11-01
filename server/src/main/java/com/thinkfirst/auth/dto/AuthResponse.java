package com.thinkfirst.auth.dto;

public record AuthResponse(String token, UserView user) {
}
