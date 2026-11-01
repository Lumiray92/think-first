package com.thinkfirst.coach;

/** 框架用于组织信息；示例和自查属于技巧，不能写入框架字段。 */
public final class PromptFrameworks {
    private PromptFrameworks() {}
    public static final String[] NAMES = {"S.C.A.F.F.", "R.G.C.", "故事化 Prompt", "万能模板"};
    public static String definition(String name) {
        return switch (name) {
            case "R.G.C." -> "Role（角色）：由谁来完成；Goal（目标）：要完成什么；Constraints（约束）：必须遵守什么。";
            case "故事化 Prompt" -> "我是谁、目前的处境、遇到的问题、期望的结果。";
            case "万能模板" -> "角色、背景、任务、输入资料、约束、输出格式、验收要求。";
            default -> "Situation（情境）：项目背景和现状；Challenge（挑战）：具体任务；Audience（受众）：使用者和维护者；Format（格式）：交付内容和形式；Foundations（基础约束）：技术、性能、安全及范围边界。";
        };
    }
    public static String prompt(String name, java.util.Map<String,Object> slots, String idea) {
        // 不推测用户技术栈，缺失信息留待确认；最终提示词必须能独立阅读。
        return "采用框架：" + name + "\n框架说明：" + definition(name)
            + "\n项目背景：" + idea
            + "\n当前任务：" + CoachValues.str(slots, "s1_problem")
            + "\n目标用户：" + CoachValues.str(slots, "s2_user")
            + "\n用户目标：" + CoachValues.str(slots, "s2_jtbd")
            + "\n功能范围：" + String.valueOf(slots.getOrDefault("s4_features", "待确认"))
            + "\n交付格式：可运行实现、必要注释、运行说明和测试结果。"
            + "\n约束：" + CoachValues.str(slots, "s7_constraints")
            + "\n验收条件：" + CoachValues.str(slots, "s7_acceptance")
            + "\n缺少的信息先列为待确认，不编造已有代码或调研结果。";
    }
}
