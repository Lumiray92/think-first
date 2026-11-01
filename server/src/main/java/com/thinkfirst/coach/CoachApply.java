package com.thinkfirst.coach;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 把用户回答 + 固定画像填成各步槽位，移植自前端 mockAgent.js。 */
@Component
public class CoachApply {

    static final List<String> RISKS = List.of(
            "功能太多做不完，第一版拖成第三版",
            "用起来比现有工具还麻烦，用户试一次就放弃",
            "做完只有自己用，没有真实用户",
            "核心假设没验证就开写",
            "用户群判断错，服务了不会用的人",
            "替代方案已经够用，用户没有迁移理由",
            "关键提醒不准，用户失去信任",
            "数据丢一次，用户再也不来了",
            "高估使用意愿，低估学习成本",
            "没有推广，做完没人知道"
    );

    private static final List<Map<String, Object>> TOP3 = List.of(
            CoachValues.entryMap(
                    "reason", "操作太麻烦，用户试一次就放弃",
                    "likelihood", "高",
                    "severity", "高",
                    "prevention", "首屏只留一个输入框，打开即可用"),
            CoachValues.entryMap(
                    "reason", "功能太多做不完",
                    "likelihood", "高",
                    "severity", "中",
                    "prevention", "P0 不超过 3 个，有不做清单"),
            CoachValues.entryMap(
                    "reason", "做完只有自己用",
                    "likelihood", "中",
                    "severity", "高",
                    "prevention", "第一周找 3 个真实用户试用")
    );

    private final CoachProfileLibrary profiles;

    public CoachApply(CoachProfileLibrary profiles) {
        this.profiles = profiles;
    }

    public Map<String, Object> applyStage(Map<String, Object> slotsView, String idea,
                                          int stageNo, CoachAnswer answer) {
        Map<String, Object> profile = profiles.pick(idea);
        int idx = answer == null ? 0 : answer.index();
        String text = answer == null ? "" : CoachValues.text(answer.text());
        Map<String, Object> slots = new LinkedHashMap<>();

        switch (stageNo) {
            case 1 -> {
                slots.put("s1_title", CoachValues.str(profile, "name"));
                slots.put("s1_problem", !text.isEmpty() ? text
                        : CoachValues.str(profile, "problem"));
                slots.put("s1_diary", List.of(
                        "周一：" + CoachValues.str(profile, "problem") + "（频率 5 / 痛苦 4）",
                        "周三：又遇到一次，" + CoachValues.str(profile, "pain") + "（频率 4 / 痛苦 4）",
                        "周五：" + CoachValues.str(profile, "alternativePain") + "（频率 3 / 痛苦 3）"));
                slots.put("s1_scores", CoachValues.entryMap(
                        "repeat", 5, "rule", 4, "verify", 4, "sensitive", 4, "fault", 3, "total", 20));
                slots.put("s1_redline", "不碰资金、不存他人隐私、不做需要长期维护的同步服务。");
            }
            case 2 -> {
                slots.put("s2_user", CoachValues.str(profile, "user"));
                String jtbd = "当" + CoachValues.str(profile, "user") + "在"
                        + whenClause(profile) + "时，想要" + CoachValues.str(profile, "task")
                        + "，以便" + CoachValues.str(profile, "outcome") + "。";
                slots.put("s2_jtbd", !text.isEmpty() ? text : jtbd);
                slots.put("s2_layers", CoachValues.entryMap(
                        "functional", CoachValues.str(profile, "functional"),
                        "emotional", CoachValues.str(profile, "emotional"),
                        "social", CoachValues.str(profile, "social")));
                slots.put("s2_alternatives", CoachValues.str(profile, "alternatives")
                        + "；问题是" + CoachValues.str(profile, "alternativePain") + "。");
            }
            case 3 -> {
                slots.put("s3_failures", RISKS);
                slots.put("s3_top3", TOP3);
                slots.put("s3_signals", "连续 3 天没有新增使用；试用者第二次不再打开；"
                        + CoachValues.str(profile, "pain") + "再次发生。");
                slots.put("s3_adjust", "先访谈 3 个试用者；操作问题就简化入口；需求问题就回到第 1 步重选。");
            }
            case 4 -> {
                slots.put("s4_hypothesis", CoachValues.str(profile, "hypothesis"));
                slots.put("s4_validation", CoachValues.str(profile, "validation"));

                List<Map<String, Object>> features = new ArrayList<>();
                for (String name : CoachValues.strList(profile, "p0")) {
                    features.add(CoachValues.entryMap(
                            "name", name, "q1", 1, "q2", 1, "q3", 1, "priority", "P0"));
                }
                for (String name : CoachValues.strList(profile, "p1")) {
                    features.add(CoachValues.entryMap(
                            "name", name, "q1", 0, "q2", 1, "q3", 0, "priority", "P1"));
                }
                for (String name : CoachValues.strList(profile, "p2")) {
                    features.add(CoachValues.entryMap(
                            "name", name, "q1", 0, "q2", 0, "q3", 0, "priority", "P2"));
                }
                slots.put("s4_features", features);
                slots.put("s4_notdoing", CoachValues.entryMap(
                        "features", CoachValues.strList(profile, "p2"),
                        "users", List.of("团队用户", "非目标场景用户"),
                        "metrics", List.of("日活", "下载量")));
                slots.put("s4_scope_note", idx == 2
                        ? "用户选择了完整方案，风险：第一版范围过大，建议回到减法再砍一次。" : "");
            }
            case 5 -> {
                slots.put("s5_profile", CoachValues.entryMap(
                        "surface", CoachValues.str(profile, "user") + "，手机为主",
                        "behavior", "在" + CoachValues.str(profile, "situation") + "处理这件事",
                        "motive", "怕" + CoachValues.str(profile, "pain") + "，想要掌控感"));
                slots.put("s5_acts", CoachValues.entryMap(
                        "open", "早上，" + CoachValues.str(profile, "user") + "又遇到："
                                + CoachValues.str(profile, "problem"),
                        "conflict", "试过" + CoachValues.str(profile, "alternatives")
                                + "，但" + CoachValues.str(profile, "alternativePain"),
                        "solve", "打开工具，" + String.join("，", CoachValues.strList(profile, "p0")),
                        "ending", CoachValues.str(profile, "outcome")));
                slots.put("s5_journey", List.of(
                        CoachValues.entryMap(
                                "stage", "发现", "action", "同事推荐或搜索",
                                "touchpoint", "群聊 / 搜索", "emotion", "好奇",
                                "pain", "不确定好不好用"),
                        CoachValues.entryMap(
                                "stage", "首次使用", "action", "打开即用，不用注册",
                                "touchpoint", "首屏", "emotion", "紧张",
                                "pain", "步骤一多就放弃"),
                        CoachValues.entryMap(
                                "stage", "核心流程",
                                "action", "完成「" + first(CoachValues.strList(profile, "p0")) + "」",
                                "touchpoint", "主界面", "emotion", "顺畅",
                                "pain", "失败没有提示"),
                        CoachValues.entryMap(
                                "stage", "长期使用", "action", "每天固定时间使用",
                                "touchpoint", "提醒", "emotion", "安心",
                                "pain", "提醒不准会失去信任")));
                slots.put("s5_lowest", "首次使用——如果要注册或配置，用户可能直接关掉。");
                slots.put("s5_improvement", "免注册、打开即用、默认焦点在输入框，三步内完成。");
                slots.put("s5_prompt4", CoachValues.entryMap(
                        "who", CoachValues.str(profile, "user"),
                        "status", "现在用" + CoachValues.str(profile, "alternatives")
                                + "，" + CoachValues.str(profile, "alternativePain"),
                        "pain", CoachValues.str(profile, "pain"),
                        "expect", CoachValues.str(profile, "outcome")));
            }
            case 6 -> {
                String light = idx == 0 ? "green" : idx == 1 ? "yellow" : "red";
                slots.put("s6_answers", CoachValues.entryMap(
                        "who", CoachValues.entryMap(
                                "answer", CoachValues.str(profile, "user"),
                                "light", idx == 2 ? "red" : "green"),
                        "pain", CoachValues.entryMap(
                                "answer", CoachValues.str(profile, "problem")
                                        + "；深层是" + CoachValues.str(profile, "pain") + "。",
                                "light", idx == 2 ? "yellow" : "green"),
                        "why", CoachValues.entryMap(
                                "answer", String.join("、", CoachValues.strList(profile, "p0"))
                                        + "，比" + CoachValues.str(profile, "alternatives") + "更省步骤。",
                                "light", light)));
                slots.put("s6_elevator", CoachValues.str(profile, "elevator"));
            }
            case 7 -> {
                CoachSchema.Scene scene = idx >= 0 && idx < CoachSchema.SCENES.size()
                        ? CoachSchema.SCENES.get(idx)
                        : CoachSchema.scene(CoachValues.str(profile, "scene"));
                if (scene == null) {
                    scene = CoachSchema.SCENES.get(2);
                }
                slots.put("s7_scene", scene.key());
                slots.put("s7_constraints", CoachValues.str(profile, "constraints") + "；" + scene.constraint());
                slots.put("s7_redlines", CoachValues.str(profile, "redlines"));
                slots.put("s7_acceptance", CoachValues.str(profile, "acceptance"));
            }
            case 8 -> {
                Map<String, Object> checklist = checklistFromLights(slotsView);
                if (idx == 1) {
                    checklist.put("soul", "yellow");
                }
                if (idx == 2) {
                    checklist.put("user", false);
                    checklist.put("risk", false);
                }
                slots.put("s8_checklist", checklist);
                Object soul = checklist.get("soul");
                slots.put("s8_gate", Boolean.FALSE.equals(soul) ? "No-Go"
                        : "yellow".equals(soul) ? "有条件 Go" : "Go");
            }
            case 9 -> slots.put("s9_context", CoachValues.map(profile, "context"));
            case 10 -> {
                String framework = PromptFrameworks.NAMES[idx >= 0 && idx < 4 ? idx : 0];
                slots.put("s10_framework", framework);
                slots.put("s10_prompt", PromptFrameworks.prompt(framework, slotsView, idea));
            }
            case 11 -> {
                List<String> techniques = switch (idx) {
                    case 0 -> List.of("few-shot", "self-critique");
                    case 1 -> List.of("self-critique");
                    default -> List.of();
                };
                slots.put("s11_techniques", techniques);
                String base = CoachValues.str(slotsView, "s10_prompt");
                if (base.isEmpty()) {
                    base = "你是一个资深产品工程师。请用 Vue 3 实现「"
                            + CoachValues.str(profile, "name") + "」。";
                }
                slots.put("s11_prompt", techniques.isEmpty() ? base
                        : base + "\n\n要求：每个功能先给 2～3 个示例，完成后自查：是否有遗漏、是否有边界情况没写。");
            }
            case 12 -> {
                slots.put("s12_selfcheck", List.of(
                        "PRD 是否足够清晰，AI 能准确理解？",
                        "有没有哪里 AI 不确定该怎么做？",
                        "有没有遗漏的边界情况？"));
                slots.put("s12_restate", "你要做的是「" + CoachValues.str(profile, "name") + "」，给"
                        + CoachValues.str(profile, "user") + "用，解决" + CoachValues.str(profile, "problem")
                        + "。第一版只做" + String.join("、", CoachValues.strList(profile, "p0"))
                        + "，不做" + String.join("、", CoachValues.strList(profile, "p2")) + "。");
            }
            default -> {
            }
        }

        return slots;
    }

    public Map<String, Object> applyGroup(Map<String, Object> slotsView, String idea,
                                          int groupId, CoachAnswer answer) {
        int idx = answer == null ? 0 : answer.index();
        String text = answer == null ? "" : CoachValues.text(answer.text());
        Map<String, Object> profile = profiles.pick(idea);
        Map<String, Object> result = new LinkedHashMap<>();

        switch (groupId) {
            case 1 -> {
                result.putAll(applyStage(slotsView, idea, 1, answer));
                result.putAll(applyStage(slotsView, idea, 2, new CoachAnswer(0, "")));
                if (idx != 0) {
                    result.put("s1_problem", !text.isEmpty() ? text : CoachValues.str(profile, "problem"));
                }
            }
            case 2 -> {
                result.putAll(applyStage(slotsView, idea, 3, answer));
                result.putAll(applyStage(slotsView, idea, 4, new CoachAnswer(idx == 2 ? 2 : 0, "")));
            }
            case 3 -> {
                result.putAll(applyStage(slotsView, idea, 5, new CoachAnswer(0, "")));
                result.putAll(applyStage(slotsView, idea, 6, answer));
            }
            case 4 -> {
                result.putAll(applyStage(slotsView, idea, 7, answer));
                result.putAll(applyStage(slotsView, idea, 8, new CoachAnswer(0, "")));
            }
            case 5 -> {
                Map<String, Object> s9 = applyStage(slotsView, idea, 9, new CoachAnswer(0, ""));
                Map<String, Object> s10 = applyStage(slotsView, idea, 10, new CoachAnswer(0, text));
                Map<String, Object> staged = merge(slotsView, s9, s10);
                Map<String, Object> s11 = applyStage(staged, idea, 11,
                        new CoachAnswer(idx == 2 ? 2 : 0, ""));
                Map<String, Object> staged2 = merge(staged, s11);
                Map<String, Object> s12 = applyStage(staged2, idea, 12, new CoachAnswer(0, ""));
                result.putAll(s9);
                result.putAll(s10);
                result.putAll(s11);
                result.putAll(s12);
            }
            default -> {
            }
        }
        return result;
    }

    private Map<String, Object> checklistFromLights(Map<String, Object> slots) {
        Map<String, Object> answers = CoachValues.map(slots, "s6_answers");
        Object who = CoachValues.map(answers, "who").get("light");
        Object pain = CoachValues.map(answers, "pain").get("light");
        Object why = CoachValues.map(answers, "why").get("light");
        boolean hasRed = "red".equals(who) || "red".equals(pain) || "red".equals(why);
        boolean hasYellow = "yellow".equals(who) || "yellow".equals(pain) || "yellow".equals(why);

        Map<String, Object> checklist = new LinkedHashMap<>();
        checklist.put("problem", true);
        checklist.put("user", !"red".equals(who));
        checklist.put("risk", true);
        checklist.put("scope", true);
        checklist.put("soul", hasRed ? false : (hasYellow ? "yellow" : true));
        checklist.put("scene", true);
        return checklist;
    }

    private static String whenClause(Map<String, Object> profile) {
        return CoachValues.str(profile, "situation").replaceAll("的时候$", "");
    }

    private static String first(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }

    private static Map<String, Object> merge(Map<String, Object> base, Map<String, Object>... updates) {
        Map<String, Object> merged = new LinkedHashMap<>(base == null ? Map.of() : base);
        for (Map<String, Object> update : updates) {
            merged.putAll(update);
        }
        return merged;
    }
}
