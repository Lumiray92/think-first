package com.thinkfirst.coach;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 一次完整的智能体回合，字段与前端 turn 契约一致。 */
@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoachTurn {

    private String kind;                 // question | confirm | gate | done
    private Integer stage;
    private Integer group;
    private String stageName;
    private String agent;
    private String title;
    private String question;
    private String hint;
    private String message;
    @Builder.Default
    private List<CoachCandidate> candidates = List.of();
    private List<CoachSummaryItem> summary;
    private CoachGate gate;
    private List<String> issues;
    private Integer returnTo;
    private Boolean canContinue;
    private CoachProgress progress;
    private String prd;
    private List<CoachCitation> citations;
    private Boolean regenerated;
    /** deepseek = 模型生成；template = 内置模板回退。 */
    private String llm;
    private String llmNote;
}
