package com.thinkfirst.coach;

import java.util.List;

/** 与前端 schema.js 对齐的 12 步、5 组快速模式、场景与检查项。 */
public final class CoachSchema {

    public static final int TOTAL_STAGES = 12;

    public record Stage(int no, String name, String agent, int group, String goal,
                        String driver, boolean gate) {
    }

    public record Group(int id, List<Integer> stages, String title, String driver, String hint) {
    }

    public record Scene(String key, String title, String desc, String constraint) {
    }

    public record CheckItem(String key, String label, int returnTo) {
    }

    public static final List<Scene> SCENES = List.of(
            new Scene("data", "数据分析", "先有业务问题，再找数据",
                    "图表少而准、结论先行；3～5 张图讲完"),
            new Scene("auto", "自动化脚本", "先手动跑通，再自动化",
                    "先自动化最痛的一步；保留原始文件；失败必须提示"),
            new Scene("personal", "个人工具", "解决自己本周真实痛点",
                    "打开到完成 ≤3 步；免登录；离线可用"),
            new Scene("family", "家人工具", "老人、小孩、非技术用户",
                    "字 ≥20px；按钮 ≥屏幕宽 40%；≤3 步；不登录")
    );

    public static final List<CheckItem> CHECK_ITEMS = List.of(
            new CheckItem("problem", "问题定义：真实、可量化、频率够高", 1),
            new CheckItem("user", "用户理解：能说出一个具体的人", 2),
            new CheckItem("risk", "风险预判：最需要注意的三个问题都有对策", 3),
            new CheckItem("scope", "MVP 范围：P0 ≤ 5 个，有不做清单", 4),
            new CheckItem("soul", "用户需求确认：至少两绿，无红灯", 6),
            new CheckItem("scene", "场景约束：硬约束、红线、验收标准都写了", 7)
    );

    public static final List<Stage> STAGES = List.of(
            new Stage(1, "问题发现", "提问官", 1,
                    "找出够频繁、够痛、够安全的问题",
                    "先说说你观察到的具体麻烦：多久发生一次？最痛的是什么？", false),
            new Stage(2, "JTBD 任务定义", "提问官", 1,
                    "说清谁、在什么情境、要完成什么任务",
                    "谁，在什么情境下，想完成什么任务？", false),
            new Stage(3, "逆向思维 / Pre-mortem", "反方", 2,
                    "先假设失败，找出最致命的三个原因",
                    "假设 3 个月后这个项目已经失败了，你觉得最可能是什么原因？", false),
            new Stage(4, "减法思维 / MVP", "减法官", 2,
                    "第一版只做什么，明确不做什么",
                    "第一版你只想保留哪个功能？", false),
            new Stage(5, "用户的使用过程", "提问官", 3,
                    "把用户写成一个具体的人，找到情绪最低点",
                    "用户是谁？他在什么情绪下使用？", false),
            new Stage(6, "用户需求确认", "提问官", 3,
                    "用户是谁、痛点在哪、为什么选你，三句话能说清吗",
                    "用一句话回答三个问题：用户是谁、痛点在哪、为什么选你？", true),
            new Stage(7, "在哪里使用", "提问官", 4,
                    "套用场景模板，写出硬约束、红线和验收标准",
                    "这个产品最像哪类场景？", false),
            new Stage(8, "开发前检查", "PRD 写手", 4,
                    "确认是否准备好开始做第一版",
                    "现在可以开始做第一版了吗？", true),
            new Stage(9, "补充项目背景", "PRD 写手", 5,
                    "给 AI 三层上下文：项目、领域、任务",
                    "AI 需要知道哪三层背景？", false),
            new Stage(10, "告诉 AI 要做什么", "PRD 写手", 5,
                    "按决策树选框架，写出第一版 Prompt",
                    "你希望 AI 怎么回答？", false),
            new Stage(11, "补充回答要求", "PRD 写手", 5,
                    "按需加示例、分步推理、多方案或自查",
                    "需要给例子、分步推理、多方案对比，还是让 AI 自查？", false),
            new Stage(12, "写 PRD", "PRD 写手", 5,
                    "把前 11 步的产出映射成一份 AI 能执行的任务书",
                    "确认一下：这份 PRD 准确吗？", false)
    );

    public static final List<Group> GROUPS = List.of(
            new Group(1, List.of(1, 2), "给谁用，解决什么问题",
                    "谁会用这个产品？你想帮他解决什么问题？",
                    "选一个接近的，也可以自己写。"),
            new Group(2, List.of(3, 4), "先做什么，担心什么",
                    "你最担心这个项目遇到什么问题？",
                    "说一个原因就行，我会按需求、技术、场景、习惯四层补全。"),
            new Group(3, List.of(5, 6), "确认用户需求",
                    "用一句话说说：用户是谁、他最痛的是什么、为什么用你的？",
                    "说清楚给谁用、解决什么问题、为什么会用。"),
            new Group(4, List.of(7, 8), "使用方式与准备检查",
                    "这个产品最像哪类场景？",
                    "选完场景，我会自动套硬约束、红线和验收标准，再过开发前检查。"),
            new Group(5, List.of(9, 10, 11, 12), "整理需求文档",
                    "第一版想先跑通哪条流程？",
                    "我会把上下文、Prompt 框架和 PRD 一次成稿。")
    );

    /** 每一步写入的槽位键，回炉时按步骤清理。下标 0 对应第 1 步。 */
    private static final List<List<String>> STAGE_SLOTS = List.of(
            List.of("s1_title", "s1_problem", "s1_diary", "s1_scores", "s1_redline"),
            List.of("s2_user", "s2_jtbd", "s2_layers", "s2_alternatives"),
            List.of("s3_failures", "s3_top3", "s3_signals", "s3_adjust"),
            List.of("s4_hypothesis", "s4_validation", "s4_features", "s4_notdoing", "s4_scope_note"),
            List.of("s5_profile", "s5_acts", "s5_journey", "s5_lowest", "s5_improvement", "s5_prompt4"),
            List.of("s6_answers", "s6_elevator"),
            List.of("s7_scene", "s7_constraints", "s7_redlines", "s7_acceptance"),
            List.of("s8_checklist", "s8_gate"),
            List.of("s9_context"),
            List.of("s10_framework", "s10_prompt"),
            List.of("s11_techniques", "s11_prompt"),
            List.of("s12_selfcheck", "s12_restate")
    );

    private CoachSchema() {
    }

    public static Stage stage(int no) {
        return STAGES.stream().filter(stage -> stage.no() == no).findFirst().orElse(STAGES.get(0));
    }

    public static Group group(int id) {
        return GROUPS.stream().filter(group -> group.id() == id).findFirst().orElse(GROUPS.get(0));
    }

    public static Group groupOfStage(int no) {
        return group(stage(no).group());
    }

    public static Integer nextGroupId(int id) {
        int next = id + 1;
        return next <= GROUPS.size() ? next : null;
    }

    public static Integer nextStageNo(int stageNo) {
        int next = stageNo + 1;
        return next <= STAGES.size() ? next : null;
    }

    public static Scene scene(String key) {
        return SCENES.stream().filter(scene -> scene.key().equals(key)).findFirst().orElse(null);
    }

    public static List<String> slotsOf(int stageNo) {
        if (stageNo < 1 || stageNo > STAGE_SLOTS.size()) {
            return List.of();
        }
        return STAGE_SLOTS.get(stageNo - 1);
    }
}
