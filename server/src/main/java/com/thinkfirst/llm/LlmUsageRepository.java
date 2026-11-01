package com.thinkfirst.llm;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmUsageRepository extends JpaRepository<LlmUsageRecord, Long> {
}
