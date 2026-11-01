package com.thinkfirst.coach;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 把 12 步槽位映射成一份 Markdown PRD，移植自前端 prd.js。 */
@Component
public class CoachPrdBuilder {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/M/d");

    public String title(Map<String, Object> slots, String idea) {
        String title = CoachValues.text(slots == null ? null : slots.get("s1_title"));
        if (!title.isEmpty()) {
            return title;
        }
        String ideaText = CoachValues.text(idea);
        return ideaText.isEmpty() ? "需求文档"
                : ideaText.substring(0, Math.min(24, ideaText.length()));
    }

    public String build(Map<String, Object> slots, String idea) {
        Map<String, Object> s = slots == null ? Map.of() : slots;
        List<String> lines = new ArrayList<>();

        lines.add("# " + title(s, idea));
        lines.add("");
        lines.add("> 由 ThinkFirst 智能体按 course07-Prompt 全流程引导生成 · "
                + LocalDate.now().format(DATE));
        lines.add("");

        pushSection(lines, "一句话描述", List.of(
                "- " + CoachValues.filled(CoachValues.firstNonBlank(
                        s.get("s6_elevator"), s.get("s1_problem"), idea))));
        pushUser(lines, s);
        pushProblem(lines, s);
        pushTask(lines, s);
        pushFeatures(lines, s);
        pushNotDoing(lines, s);
        pushFlow(lines, s);
        pushConstraints(lines, s);
        pushSuccess(lines, s);
        pushRisks(lines, s);
        pushSelfCheck(lines, s);
        pushPrompt(lines, s);

        String markdown = String.join("\n", lines).replaceAll("\n{3,}", "\n\n").trim();
        return markdown + "\n";
    }

    private void pushUser(List<String> out, Map<String, Object> s) {
        Map<String, Object> profile = CoachValues.map(s, "s5_profile");
        List<String> lines = new ArrayList<>();
        lines.add("- " + CoachValues.filled(s.get("s2_user")));
        if (!CoachValues.str(profile, "surface").isEmpty()) {
            lines.add("- 表面属性：" + CoachValues.str(profile, "surface"));
        }
        if (!CoachValues.str(profile, "behavior").isEmpty()) {
            lines.add("- 行为习惯：" + CoachValues.str(profile, "behavior"));
        }
        if (!CoachValues.str(profile, "motive").isEmpty()) {
            lines.add("- 深层动机：" + CoachValues.str(profile, "motive"));
        }
        if (!CoachValues.str(s, "s5_lowest").isEmpty()) {
            lines.add("- 情绪最低点：" + CoachValues.str(s, "s5_lowest"));
        }
        if (!CoachValues.str(s, "s5_improvement").isEmpty()) {
            lines.add("- 对应改进：" + CoachValues.str(s, "s5_improvement"));
        }
        pushSection(out, "目标用户", lines);
    }

    private void pushProblem(List<String> out, Map<String, Object> s) {
        List<String> diary = CoachValues.asStringList(s.get("s1_diary"));
        List<String> lines = new ArrayList<>();
        lines.add("- " + CoachValues.filled(s.get("s1_problem")));
        if (!CoachValues.str(s, "s2_alternatives").isEmpty()) {
            lines.add("- 现有替代方案：" + CoachValues.str(s, "s2_alternatives"));
        }
        if (!diary.isEmpty()) {
            lines.add("- 烦恼日记：");
            diary.forEach(row -> lines.add("  - " + row));
        }
        pushSection(out, "要解决的问题", lines);
    }

    private void pushTask(List<String> out, Map<String, Object> s) {
        Map<String, Object> layers = CoachValues.map(s, "s2_layers");
        List<String> lines = new ArrayList<>();
        lines.add("- " + CoachValues.filled(s.get("s2_jtbd")));
        if (!CoachValues.str(layers, "functional").isEmpty()) {
            lines.add("- 功能任务：" + CoachValues.str(layers, "functional"));
        }
        if (!CoachValues.str(layers, "emotional").isEmpty()) {
            lines.add("- 情感任务：" + CoachValues.str(layers, "emotional"));
        }
        if (!CoachValues.str(layers, "social").isEmpty()) {
            lines.add("- 社会任务：" + CoachValues.str(layers, "social"));
        }
        pushSection(out, "用户想完成的事（JTBD）", lines);
    }

    private void pushFeatures(List<String> out, Map<String, Object> s) {
        List<Map<String, Object>> p0 = CoachValues.asMapList(s.get("s4_features")).stream()
                .filter(feature -> "P0".equals(CoachValues.str(feature, "priority")))
                .toList();
        List<String> lines = new ArrayList<>();
        if (p0.isEmpty()) {
            lines.add("- " + CoachValues.EMPTY);
        } else {
            p0.forEach(feature -> lines.add("- [ ] " + CoachValues.str(feature, "name")));
        }
        if (!CoachValues.str(s, "s4_hypothesis").isEmpty()) {
            lines.add("");
            lines.add("- 核心假设：" + CoachValues.str(s, "s4_hypothesis"));
        }
        if (!CoachValues.str(s, "s4_scope_note").isEmpty()) {
            lines.add("- 范围提醒：" + CoachValues.str(s, "s4_scope_note"));
        }
        pushSection(out, "核心功能（P0，必须有）", lines);
    }

    private void pushNotDoing(List<String> out, Map<String, Object> s) {
        Map<String, Object> doing = CoachValues.map(s, "s4_notdoing");
        pushSection(out, "不做的功能", List.of(
                "- 不做的功能：" + listValue(doing.get("features")),
                "- 不服务的用户：" + listValue(doing.get("users")),
                "- 不追的指标：" + listValue(doing.get("metrics"))));
    }

    private void pushFlow(List<String> out, Map<String, Object> s) {
        Map<String, Object> acts = CoachValues.map(s, "s5_acts");
        List<String> lines = new ArrayList<>();
        if (!CoachValues.str(acts, "open").isEmpty()) {
            lines.add("- 开端：" + CoachValues.str(acts, "open"));
        }
        if (!CoachValues.str(acts, "conflict").isEmpty()) {
            lines.add("- 冲突：" + CoachValues.str(acts, "conflict"));
        }
        if (!CoachValues.str(acts, "solve").isEmpty()) {
            lines.add("- 解决：" + CoachValues.str(acts, "solve"));
        }
        if (!CoachValues.str(acts, "ending").isEmpty()) {
            lines.add("- 结局：" + CoachValues.str(acts, "ending"));
        }

        List<Map<String, Object>> journey = CoachValues.asMapList(s.get("s5_journey"));
        if (!journey.isEmpty()) {
            lines.add("");
            lines.add("- 用户旅程：");
            for (Map<String, Object> step : journey) {
                List<String> parts = new ArrayList<>();
                for (String key : List.of("stage", "action", "emotion")) {
                    String part = CoachValues.str(step, key);
                    if (!part.isEmpty()) {
                        parts.add(part);
                    }
                }
                lines.add("  - " + String.join(" · ", parts));
            }
        }
        pushSection(out, "用户流程", lines.isEmpty() ? List.of("- " + CoachValues.EMPTY) : lines);
    }

    private void pushConstraints(List<String> out, Map<String, Object> s) {
        List<String> lines = new ArrayList<>();
        CoachSchema.Scene scene = CoachSchema.scene(CoachValues.str(s, "s7_scene"));
        if (scene != null) {
            lines.add("- 场景类型：" + scene.title());
        }
        if (!CoachValues.str(s, "s7_constraints").isEmpty()) {
            lines.add("- 硬约束：" + CoachValues.str(s, "s7_constraints"));
        }
        if (!CoachValues.str(s, "s7_redlines").isEmpty()) {
            lines.add("- 红线：" + CoachValues.str(s, "s7_redlines"));
        }
        if (!CoachValues.str(s, "s1_redline").isEmpty()) {
            lines.add("- 问题阶段红线：" + CoachValues.str(s, "s1_redline"));
        }
        if (s.get("s1_scores") != null) {
            Map<String, Object> score = CoachValues.asMap(s.get("s1_scores"));
            lines.add("五维评分用于初步判断任务是否适合自动化，每项 0–5 分，共 25 分。这是 AI 初步估计，不代表用户调研结果。");
            lines.add("| 维度 | 含义与评分方向 | 分数 | 依据 |");
            lines.add("|---|---|---|---|");
            String[][] dimensions = {{"repeat","重复性","任务发生的频率，越频繁分越高"},{"rule","规则性","步骤是否固定，越明确分越高"},{"verify","可验证性","能否检查结果对错，越容易检查分越高"},{"sensitive","敏感性","是否涉及敏感数据，越不敏感分越高"},{"fault","容错性","出错后果是否可承受，越容易纠正分越高"}};
            for (String[] d : dimensions) lines.add("| " + d[1] + " | " + d[2] + " | " + CoachValues.filled(score.get(d[0])) + "/5 | " + CoachValues.filled(score.get(d[0] + "_reason")) + " |");
            lines.add("评分参考：20–25 分可优先考虑；15–19 分先改进低分项；10–14 分需谨慎或专业支持；低于 10 分不建议自动执行。缺乏依据的项目应先确认，不能仅凭总分决定开发。");

            lines.add("- 五维评分：" + CoachValues.filled(score.get("total")) + " / 25（重复性 "
                    + CoachValues.filled(score.get("repeat")) + "，规则性 "
                    + CoachValues.filled(score.get("rule")) + "，可验证性 "
                    + CoachValues.filled(score.get("verify")) + "，敏感性 "
                    + CoachValues.filled(score.get("sensitive")) + "，容错性 "
                    + CoachValues.filled(score.get("fault")) + "）");
        }
        pushSection(out, "技术约束", lines.isEmpty() ? List.of("- " + CoachValues.EMPTY) : lines);
    }

    private void pushSuccess(List<String> out, Map<String, Object> s) {
        List<String> lines = new ArrayList<>();
        if (!CoachValues.str(s, "s4_validation").isEmpty()) {
            lines.add("- 验证标准：" + CoachValues.str(s, "s4_validation"));
        }
        if (!CoachValues.str(s, "s7_acceptance").isEmpty()) {
            lines.add("- 验收标准：" + CoachValues.str(s, "s7_acceptance"));
        }
        if (!CoachValues.str(s, "s6_elevator").isEmpty()) {
            lines.add("- 电梯演讲：" + CoachValues.str(s, "s6_elevator"));
        }
        pushSection(out, "成功标准", lines.isEmpty() ? List.of("- " + CoachValues.EMPTY) : lines);
    }

    private void pushRisks(List<String> out, Map<String, Object> s) {
        List<Map<String, Object>> top3 = CoachValues.asMapList(s.get("s3_top3"));
        List<String> lines = new ArrayList<>();
        if (!top3.isEmpty()) {
            lines.add("- 最需要注意的三个问题与对策：");
            for (Map<String, Object> risk : top3) {
                lines.add("  - " + CoachValues.str(risk, "reason")
                        + "（可能性 " + CoachValues.filled(risk.get("likelihood"))
                        + " / 严重度 " + CoachValues.filled(risk.get("severity"))
                        + "）→ " + CoachValues.filled(risk.get("prevention")));
            }
        }
        if (!CoachValues.str(s, "s3_signals").isEmpty()) {
            lines.add("- 失败信号：" + CoachValues.str(s, "s3_signals"));
        }
        if (!CoachValues.str(s, "s3_adjust").isEmpty()) {
            lines.add("- 调整方案：" + CoachValues.str(s, "s3_adjust"));
        }
        pushSection(out, "风险与对策", lines.isEmpty() ? List.of("- " + CoachValues.EMPTY) : lines);
    }

    private void pushSelfCheck(List<String> out, Map<String, Object> s) {
        Map<String, Object> answers = CoachValues.map(s, "s6_answers");
        Map<String, Object> who = CoachValues.map(answers, "who");
        Map<String, Object> pain = CoachValues.map(answers, "pain");
        Map<String, Object> why = CoachValues.map(answers, "why");

        List<String> lines = new ArrayList<>();
        lines.add("- 用户是谁：" + CoachValues.filled(who.get("answer"))
                + "（" + lightText(CoachValues.str(who, "light")) + "）");
        lines.add("- 痛点在哪：" + CoachValues.filled(pain.get("answer"))
                + "（" + lightText(CoachValues.str(pain, "light")) + "）");
        lines.add("- 为什么选你：" + CoachValues.filled(why.get("answer"))
                + "（" + lightText(CoachValues.str(why, "light")) + "）");

        Map<String, Object> checklist = CoachValues.map(s, "s8_checklist");
        lines.add("");
        lines.add("- 开发前检查：");
        for (CoachSchema.CheckItem item : CoachSchema.CHECK_ITEMS) {
            lines.add("  - " + item.label() + "：" + checkText(checklist.get(item.key())));
        }
        if (!CoachValues.str(s, "s8_gate").isEmpty()) {
            lines.add("- 结论：" + CoachValues.str(s, "s8_gate"));
        }
        if (!CoachValues.str(s, "s12_restate").isEmpty()) {
            lines.add("- AI 复述：" + CoachValues.str(s, "s12_restate"));
        }
        pushSection(out, "最终自检", lines);
    }

    private void pushPrompt(List<String> out, Map<String, Object> s) {
        String prompt = CoachValues.text(s.get("s11_prompt"));
        if (prompt.isEmpty()) {
            prompt = CoachValues.text(s.get("s10_prompt"));
        }
        Map<String, Object> context = CoachValues.map(s, "s9_context");
        List<String> lines = new ArrayList<>();
        String framework = CoachValues.str(s, "s10_framework");
        lines.add("提示词框架：" + framework);
        lines.add("框架含义：" + PromptFrameworks.definition(framework));
        lines.add("使用技巧与框架不同：提供示例用于说明预期结果；生成后自查用于检查遗漏和错误。");

        if (!CoachValues.str(context, "project").isEmpty()) {
            lines.add("- 项目上下文：" + CoachValues.str(context, "project"));
        }
        if (!CoachValues.str(context, "domain").isEmpty()) {
            lines.add("- 领域上下文：" + CoachValues.str(context, "domain"));
        }
        if (!CoachValues.str(context, "task").isEmpty()) {
            lines.add("- 任务上下文：" + CoachValues.str(context, "task"));
        }
        List<String> techniques = CoachValues.strList(s, "s11_techniques");
        if (!techniques.isEmpty()) {
            lines.add("- 使用技巧：" + String.join("、", techniques));
        }
        if (!lines.isEmpty()) {
            lines.add("");
        }
        lines.add(prompt.isEmpty() ? CoachValues.EMPTY : prompt);
        pushSection(out, "给 AI 的 Prompt", lines);
    }

    private static void pushSection(List<String> out, String title, List<String> lines) {
        out.add("## " + title);
        out.add("");
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                out.add(line);
            }
        }
        out.add("");
    }

    private static String listValue(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return CoachValues.EMPTY;
        }
        List<String> parts = new ArrayList<>();
        for (Object item : list) {
            String text;
            if (item instanceof String str) {
                text = str;
            } else {
                Map<String, Object> map = CoachValues.asMap(item);
                text = CoachValues.str(map, "name");
                if (text.isEmpty()) {
                    text = CoachValues.str(map, "reason");
                }
            }
            if (!text.isEmpty()) {
                parts.add(text);
            }
        }
        return parts.isEmpty() ? CoachValues.EMPTY : String.join("、", parts);
    }

    private static String lightText(String light) {
        return switch (light) {
            case "green" -> "🟢 清晰";
            case "yellow" -> "🟡 说不清";
            case "red" -> "🔴 答不出";
            default -> "⚪ 待判断";
        };
    }

    private static String checkText(Object value) {
        if (Boolean.TRUE.equals(value)) {
            return "✅ 通过";
        }
        if ("yellow".equals(value)) {
            return "🟡 有风险";
        }
        if (Boolean.FALSE.equals(value)) {
            return "❌ 未通过";
        }
        return "⚪ 待检查";
    }
}
