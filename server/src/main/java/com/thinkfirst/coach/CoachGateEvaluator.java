package com.thinkfirst.coach;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 第 6 步红黄绿自检、第 8 步开发前检查。 */
@Component
public class CoachGateEvaluator {

    public CoachGate evaluate(Map<String, Object> slots, int stageNo) {
        if (stageNo == 6) {
            return evaluateSoul(slots);
        }
        if (stageNo == 8) {
            return evaluateChecklist(slots);
        }
        return null;
    }

    public boolean isBlocking(CoachGate gate) {
        return gate != null && ("red".equals(gate.status()) || "no-go".equals(gate.status()));
    }

    private CoachGate evaluateSoul(Map<String, Object> slots) {
        Map<String, Object> answers = CoachValues.map(slots, "s6_answers");
        String who = CoachValues.str(CoachValues.map(answers, "who"), "light");
        String pain = CoachValues.str(CoachValues.map(answers, "pain"), "light");
        String why = CoachValues.str(CoachValues.map(answers, "why"), "light");
        List<String> lights = List.of(who, pain, why);

        long reds = lights.stream().filter("red"::equals).count();
        long greens = lights.stream().filter("green"::equals).count();

        if (reds > 0) {
            return new CoachGate("red", "有红灯，先回炉",
                    "用户是谁还没说清，先别往下走。回到 JTBD 把具体的人补出来。",
                    List.of("用户 / 痛点 / 差异化至少有一项说不清"), 2, true);
        }
        if (greens == lights.size()) {
            return new CoachGate("green", "已经想清楚，可以继续",
                    "三个问题都能一句话说清，有依据。", List.of(), null, false);
        }
        return new CoachGate("yellow", "两绿一黄，先做最小实验",
                "方向对了，但差异化还没被验证。可以继续，但要带着风险开工。",
                List.of("为什么选你：建议先找 3 个真实用户验证"), 5, false);
    }

    private CoachGate evaluateChecklist(Map<String, Object> slots) {
        Map<String, Object> checklist = CoachValues.map(slots, "s8_checklist");
        List<ChecklistFailure> failures = new ArrayList<>();
        for (CoachSchema.CheckItem item : CoachSchema.CHECK_ITEMS) {
            Object value = checklist.get(item.key());
            if (Boolean.FALSE.equals(value) || "yellow".equals(value)) {
                failures.add(new ChecklistFailure(item, value));
            }
        }

        List<ChecklistFailure> hard = failures.stream()
                .filter(failure -> Boolean.FALSE.equals(failure.value()))
                .toList();

        if (!hard.isEmpty()) {
            return new CoachGate("no-go", "暂时别开始，先补充信息",
                    "还有硬性检查没过，不能进入写 PRD。",
                    hard.stream().map(failure -> failure.item().label()).toList(),
                    hard.get(0).item().returnTo(), true);
        }
        if (!failures.isEmpty()) {
            return new CoachGate("yellow", "有条件 Go",
                    "可以继续，但要带着风险开工。建议先做最小实验。",
                    failures.stream().map(failure -> failure.item().label()).toList(),
                    failures.get(0).item().returnTo(), false);
        }
        return new CoachGate("go", "Go，可以写 PRD", "六项检查全部通过。",
                List.of(), null, false);
    }

    private record ChecklistFailure(CoachSchema.CheckItem item, Object value) {
    }
}
