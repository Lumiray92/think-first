package com.thinkfirst.coach;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 快速模式每组答完后的确认卡摘要。 */
@Component
public class CoachSummary {

    public List<CoachSummaryItem> build(Map<String, Object> stateSlots, int groupId,
                                        Map<String, Object> applied) {
        Map<String, Object> slots = new LinkedHashMap<>(stateSlots == null ? Map.of() : stateSlots);
        slots.putAll(applied);

        return switch (groupId) {
            case 1 -> List.of(
                    item("要解决的问题", CoachValues.str(slots, "s1_problem")),
                    item("目标用户", CoachValues.str(slots, "s2_user")),
                    item("用户想完成的事", CoachValues.str(slots, "s2_jtbd")));
            case 2 -> {
                List<String> p0 = new ArrayList<>();
                for (Map<String, Object> feature : CoachValues.asMapList(slots.get("s4_features"))) {
                    if ("P0".equals(CoachValues.str(feature, "priority"))) {
                        p0.add(CoachValues.str(feature, "name"));
                    }
                }
                List<String> risks = new ArrayList<>();
                for (Map<String, Object> risk : CoachValues.asMapList(slots.get("s3_top3"))) {
                    risks.add(CoachValues.str(risk, "reason"));
                }
                Map<String, Object> notDoing = CoachValues.map(slots, "s4_notdoing");
                yield List.of(
                        item("核心假设", CoachValues.str(slots, "s4_hypothesis")),
                        item("P0 功能", String.join("、", p0)),
                        item("最需要注意的三个问题", String.join("；", risks)),
                        item("不做的功能", String.join("、", CoachValues.strList(notDoing, "features"))));
            }
            case 3 -> {
                Map<String, Object> answers = CoachValues.map(slots, "s6_answers");
                Map<String, Object> profile = CoachValues.map(slots, "s5_profile");
                yield List.of(
                        item("用户画像", CoachValues.str(profile, "surface")),
                        lightItem(answers, "who", "用户是谁"),
                        lightItem(answers, "pain", "痛点在哪"),
                        lightItem(answers, "why", "为什么选你"));
            }
            case 4 -> {
                CoachSchema.Scene scene = CoachSchema.scene(CoachValues.str(slots, "s7_scene"));
                yield List.of(
                        item("场景", scene == null ? "" : scene.title()),
                        item("硬约束", CoachValues.str(slots, "s7_constraints")),
                        item("验收标准", CoachValues.str(slots, "s7_acceptance")),
                        item("是否可以开始", readiness(CoachValues.str(slots, "s8_gate"))));
            }
            default -> List.of(
                    item("提示词框架", CoachValues.str(slots, "s10_framework") + "：" + PromptFrameworks.definition(CoachValues.str(slots, "s10_framework"))),
                    item("使用技巧", String.join("、", CoachValues.strList(slots, "s11_techniques")).replace("few-shot", "提供示例").replace("self-critique", "生成后自查").replace("zero-shot", "直接说明任务")),
                    item("PRD 自检", String.join("；", CoachValues.strList(slots, "s12_selfcheck"))),
                    item("AI 复述", CoachValues.str(slots, "s12_restate")));
        };
    }

    private static String readiness(String value) {
        return switch(value) { case "Go" -> "可以开始做第一版"; case "No-Go" -> "还需补充信息，暂不开始"; case "有条件 Go" -> "先找人试用，确认有需要再开始"; default -> value; };
    }
    private static CoachSummaryItem item(String label, String value) {
        return new CoachSummaryItem(label, value == null ? "" : value);
    }

    private static CoachSummaryItem lightItem(Map<String, Object> answers, String key, String label) {
        Map<String, Object> answer = CoachValues.map(answers, key);
        String light = switch (CoachValues.str(answer, "light")) {
            case "green" -> "绿";
            case "yellow" -> "黄";
            case "red" -> "红";
            default -> "待判断";
        };
        return item(label, CoachValues.str(answer, "answer") + "（" + light + "）");
    }
}
