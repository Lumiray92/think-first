package com.thinkfirst.coach;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 一次引导会话。M2 阶段先放内存，后续再落库。 */
@Data
public class CoachSession {

    private final String id;
    private final Long ownerId;
    private String idea;
    private String mode = "quick";
    private int stage = 1;
    private int group = 1;
    private String status = "idle";      // idle | asking | confirming | blocked | done
    private Map<String, Object> slots = new HashMap<>();
    private CoachGate gate;
    private CoachTurn pending;
    /** LLM 优化后的最终 PRD；为空时使用模板生成。 */
    private String finalPrd;
    private List<Map<String, Object>> history = new ArrayList<>();
    /** 完整对话与文档版本随会话落库，换浏览器也能查看。 */
    private List<Map<String,Object>> messages = new ArrayList<>();
    private List<Map<String,Object>> versions = new ArrayList<>();
    private long updatedAt = System.currentTimeMillis();

    @com.fasterxml.jackson.annotation.JsonCreator
    public CoachSession(@com.fasterxml.jackson.annotation.JsonProperty("id") String id,
                        @com.fasterxml.jackson.annotation.JsonProperty("ownerId") Long ownerId,
                        @com.fasterxml.jackson.annotation.JsonProperty("idea") String idea,
                        @com.fasterxml.jackson.annotation.JsonProperty("mode") String mode) {
        this.id = id;
        this.ownerId = ownerId;
        this.idea = idea == null ? "" : idea;
        this.mode = "full".equals(mode) ? "full" : "quick";
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}
