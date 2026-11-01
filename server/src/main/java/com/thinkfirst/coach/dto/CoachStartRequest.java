package com.thinkfirst.coach.dto;

import jakarta.validation.constraints.Size;

/** 新建会话：一句想法 + quick / full。 */
public record CoachStartRequest(
        @Size(max = 2000) String idea,
        String mode
) {
}
