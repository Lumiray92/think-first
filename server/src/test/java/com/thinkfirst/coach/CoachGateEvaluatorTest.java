package com.thinkfirst.coach;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoachGateEvaluatorTest {

    private final CoachGateEvaluator evaluator = new CoachGateEvaluator();

    @Test
    void soulGateReturnsGreenYellowRed() {
        assertEquals("green", evaluator.evaluate(soul("green", "green", "green"), 6).status());
        CoachGate yellow = evaluator.evaluate(soul("green", "green", "yellow"), 6);
        assertEquals("yellow", yellow.status());
        assertFalse(yellow.blocking());
        assertEquals(5, yellow.returnTo());

        CoachGate red = evaluator.evaluate(soul("red", "green", "yellow"), 6);
        assertEquals("red", red.status());
        assertTrue(red.blocking());
        assertEquals(2, red.returnTo());
    }

    @Test
    void checklistGateReturnsGoConditionalGoAndNoGo() {
        assertEquals("go", evaluator.evaluate(checklist(true, true, true, true, true, true), 8).status());

        CoachGate conditional = evaluator.evaluate(
                checklist(true, true, true, true, "yellow", true), 8);
        assertEquals("yellow", conditional.status());
        assertFalse(conditional.blocking());
        assertEquals(6, conditional.returnTo());

        CoachGate noGo = evaluator.evaluate(
                checklist(true, false, true, true, true, true), 8);
        assertEquals("no-go", noGo.status());
        assertTrue(noGo.blocking());
        assertEquals(2, noGo.returnTo());
    }

    @Test
    void nonGateStageReturnsNull() {
        assertNull(evaluator.evaluate(Map.of(), 4));
    }

    private static Map<String, Object> soul(String who, String pain, String why) {
        return Map.of("s6_answers", Map.of(
                "who", Map.of("answer", "用户", "light", who),
                "pain", Map.of("answer", "痛点", "light", pain),
                "why", Map.of("answer", "差异", "light", why)));
    }

    private static Map<String, Object> checklist(Object problem, Object user, Object risk,
                                                 Object scope, Object soul, Object scene) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("problem", problem);
        values.put("user", user);
        values.put("risk", risk);
        values.put("scope", scope);
        values.put("soul", soul);
        values.put("scene", scene);
        return Map.of("s8_checklist", values);
    }
}
