package com.thinkfirst.coach;

/**
 * 第 13–16 步（三轮开发、迭代、调试、复盘）的扩展点。
 * 本期不提供实现；未来新增实现并注册为 Spring Bean 后，PRD 完成时会自动接管。
 */
public interface CoachNextPhase {

    boolean supports(CoachSession session);

    /** 返回下一阶段的回合；返回 null 表示不接管，仍以 PRD 完成回合结束。 */
    CoachTurn start(CoachSession session);
}
