package com.thinkfirst.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LlmUsageService {

    private static final Logger log = LoggerFactory.getLogger(LlmUsageService.class);

    private final LlmUsageRepository repository;

    public LlmUsageService(LlmUsageRepository repository) {
        this.repository = repository;
    }

    /** 记账失败不影响本次生成。 */
    public void record(Long userId, String purpose, Integer stageNo,
                       DeepSeekClient.Completion completion) {
        if (userId == null || completion == null) {
            return;
        }
        try {
            LlmUsageRecord record = new LlmUsageRecord();
            record.setUserId(userId);
            record.setPurpose(purpose);
            record.setStageNo(stageNo);
            record.setPromptTokens(completion.promptTokens());
            record.setCompletionTokens(completion.completionTokens());
            repository.save(record);
        } catch (RuntimeException e) {
            log.warn("记录模型用量失败: {}", e.getClass().getSimpleName());
        }
    }
}
