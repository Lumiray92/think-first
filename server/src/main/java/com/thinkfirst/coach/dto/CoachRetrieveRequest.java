package com.thinkfirst.coach.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 检索请求。当前只支持每步固定 query，自定义 query 待接入 EmbeddingClient。 */
public record CoachRetrieveRequest(
        @NotNull @Min(1) @Max(12) Integer step,
        @Min(1) @Max(8) Integer topK,
        String query
) {
}
