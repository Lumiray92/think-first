package com.thinkfirst.coach;

import java.util.List;

/** 教练智能体的可选模型增强；任何失败都必须保留模板结果。 */
public interface CoachLlm {

    default void updateSlots(CoachSession session, java.util.Map<String,Object> applied, String answer) {}

    void enhanceQuestion(CoachSession session, CoachTurn turn, List<Integer> stages);

    default String polish(CoachSession session, String source, String instructions) { throw new IllegalStateException("暂不支持文档调整"); }

    void enhancePrd(CoachSession session, CoachTurn turn, String basePrd);
}
