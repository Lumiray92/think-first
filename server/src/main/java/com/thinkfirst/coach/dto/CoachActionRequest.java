package com.thinkfirst.coach.dto;

/** 回合动作：默认回答，也支持 confirm / regenerate / back。 */
public record CoachActionRequest(
        String type,
        Integer index,
        String text,
        Integer stage
) {
}
