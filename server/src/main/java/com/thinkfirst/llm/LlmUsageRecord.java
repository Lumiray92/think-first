package com.thinkfirst.llm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 每次模型调用的 token 用量，用于成本追踪；不记录 Key 和提示词正文。 */
@Entity
@Table(name = "tf_llm_usage")
@Getter
@Setter
public class LlmUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 32)
    private String purpose;

    private Integer stageNo;

    @Column(nullable = false)
    private int promptTokens;

    @Column(nullable = false)
    private int completionTokens;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
