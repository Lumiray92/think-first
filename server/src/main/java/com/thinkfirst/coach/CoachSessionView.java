package com.thinkfirst.coach;

/** 会话接口返回：会话元信息 + 当前回合 + 槽位快照（供前端刷新后恢复）。 */
public record CoachSessionView(String sessionId, String mode, String status,
                               CoachTurn turn, java.util.Map<String, Object> slots) {
}
