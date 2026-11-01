package com.thinkfirst.coach;

import java.util.List;

/** 第 6、8 步的闸门结果。 */
public record CoachGate(
        String status,
        String label,
        String message,
        List<String> issues,
        Integer returnTo,
        boolean blocking
) {
}
