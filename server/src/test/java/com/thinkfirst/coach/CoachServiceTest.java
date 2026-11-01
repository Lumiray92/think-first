package com.thinkfirst.coach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.coach.dto.CoachActionRequest;
import com.thinkfirst.coach.rag.CoachCorpusLoader;
import com.thinkfirst.coach.rag.CoachProperties;
import com.thinkfirst.coach.rag.CoachRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoachServiceTest {

    private CoachService service;

    @BeforeEach
    void setUp() {
        service = buildService(List.of());
    }

    private static CoachService buildService(List<CoachNextPhase> phases) {
        ObjectMapper mapper = new ObjectMapper();
        CoachProfileLibrary profiles = new CoachProfileLibrary(mapper);
        CoachProperties properties = new CoachProperties();
        properties.setDataDir("missing-corpus");
        CoachCorpusLoader loader = new CoachCorpusLoader(properties, mapper);
        CoachRetriever retriever = new CoachRetriever(loader);

        return new CoachService(
                new CoachQuestionFactory(profiles, retriever, null),
                new CoachApply(profiles),
                new CoachSummary(),
                new CoachGateEvaluator(),
                new CoachPrdBuilder(),
                null,
                phases);
    }

    @Test
    void quickModeWalksFiveGroupsWithoutSkippingStages() {
        CoachSessionView view = service.start(1L, "做一个待办清单", "quick");
        assertEquals("question", view.turn().getKind());
        assertEquals(1, view.turn().getGroup());

        int guard = 0;
        while (!"done".equals(view.turn().getKind()) && guard++ < 30) {
            CoachTurn turn = view.turn();
            if ("question".equals(turn.getKind())) {
                assertFalse(turn.getCandidates().isEmpty(), "每次提问至少给 3 个候选");
                view = service.act(view.sessionId(), 1L, action(null, 0));
            } else if ("confirm".equals(turn.getKind())) {
                assertFalse(turn.getSummary().isEmpty(), "确认卡不能为空");
                view = service.act(view.sessionId(), 1L, action("confirm", null));
            } else if ("gate".equals(turn.getKind())) {
                assertTrue(turn.getCanContinue(), "黄灯允许继续");
                view = service.act(view.sessionId(), 1L, action("confirm", null));
            } else {
                throw new AssertionError("意外回合: " + turn.getKind());
            }
        }

        assertEquals("done", view.turn().getKind());
        assertEquals(5, view.turn().getProgress().group());

        CoachSession session = service.find(view.sessionId());
        for (String key : List.of("s1_problem", "s2_jtbd", "s3_top3", "s4_features",
                "s5_journey", "s6_answers", "s7_scene", "s8_gate", "s9_context",
                "s10_prompt", "s11_prompt", "s12_selfcheck", "s12_restate")) {
            assertTrue(session.getSlots().containsKey(key), "快速模式不应省略槽位: " + key);
        }

        String prd = service.getPrd(view.sessionId(), 1L).markdown();
        assertEquals(12, prd.split("## ", -1).length - 1, "PRD 应有 12 个章节");
        assertTrue(prd.contains("## 用户流程"));
        assertTrue(prd.contains("## 技术约束"));
        assertTrue(prd.contains("## 给 AI 的 Prompt"));
        assertTrue(prd.contains("## 最终自检"));
        assertTrue(prd.contains("项目上下文"));
        assertTrue(prd.contains("使用技巧"));
        assertTrue(prd.contains("P0"));
    }

    @Test
    void stageSixRedGateBlocksAndBackClearsLaterSlots() {
        CoachSessionView view = service.start(1L, "做一个待办清单", "full");
        for (int stage = 1; stage <= 5; stage++) {
            assertEquals(stage, view.turn().getStage());
            view = service.act(view.sessionId(), 1L, action(null, 0));
        }

        assertEquals(6, view.turn().getStage());
        view = service.act(view.sessionId(), 1L, action(null, 2));
        assertEquals("gate", view.turn().getKind());
        assertEquals("red", view.turn().getGate().status());
        assertFalse(view.turn().getCanContinue());

        view = service.act(view.sessionId(), 1L, action("confirm", null));
        assertEquals("gate", view.turn().getKind());
        assertEquals("red", view.turn().getGate().status(), "红灯确认后仍应停在闸门");

        view = service.act(view.sessionId(), 1L, action("back", null, 2));
        assertEquals("question", view.turn().getKind());
        assertEquals(2, view.turn().getStage());
        assertFalse(service.find(view.sessionId()).getSlots().containsKey("s6_answers"),
                "回炉应清空第 6 步及之后的槽位");
    }

    @Test
    void stageEightNoGoReturnsToChecklistItem() {
        CoachSessionView view = service.start(1L, "做一个待办清单", "full");
        for (int stage = 1; stage <= 7; stage++) {
            view = service.act(view.sessionId(), 1L, action(null, 0));
        }

        assertEquals(8, view.turn().getStage());
        view = service.act(view.sessionId(), 1L, action(null, 2));
        assertEquals("gate", view.turn().getKind());
        assertEquals("no-go", view.turn().getGate().status());
        assertTrue(view.turn().getGate().blocking());
        assertEquals(2, view.turn().getReturnTo(), "用户项未过应回到第 2 步");
    }

    @Test
    void unknownSessionIsRejected() {
        assertThrows(CoachSessionNotFoundException.class,
                () -> service.act("missing", 1L, action(null, 0)));
    }

    @Test
    void rotatedCandidatesKeepTheirOriginalBranch() {
        CoachSessionView view = service.start(1L, "做一个待办清单", "full");
        String sessionId = view.sessionId();

        view = service.act(sessionId, 1L, action("regenerate", null));
        assertEquals("a", view.turn().getCandidates().get(0).id());
        String selectedText = view.turn().getCandidates().get(0).text();

        service.act(sessionId, 1L, action(null, 0));
        assertEquals(selectedText, service.find(sessionId).getSlots().get("s1_problem"));
    }

    @Test
    void sessionIsBoundToItsOwner() {
        CoachSessionView view = service.start(1L, "做一个待办清单", "quick");

        assertThrows(CoachSessionNotFoundException.class,
                () -> service.act(view.sessionId(), 2L, action(null, 0)));
        assertThrows(CoachSessionNotFoundException.class,
                () -> service.getPrd(view.sessionId(), 2L));
        assertThrows(CoachSessionNotFoundException.class,
                () -> service.find(view.sessionId(), 2L));
    }

    @Test
    void nextPhaseCanTakeOverAfterPrd() {
        CoachNextPhase phase = new CoachNextPhase() {
            @Override
            public boolean supports(CoachSession session) {
                return true;
            }

            @Override
            public CoachTurn start(CoachSession session) {
                return CoachTurn.builder().kind("next").title("第 13 步").build();
            }
        };
        CoachService withPhase = buildService(List.of(phase));

        CoachSessionView view = withPhase.start(1L, "做一个待办清单", "quick");
        int guard = 0;
        while (!"next".equals(view.turn().getKind()) && guard++ < 30) {
            CoachTurn turn = view.turn();
            CoachActionRequest next = switch (turn.getKind()) {
                case "question" -> action(null, 0);
                case "confirm", "gate" -> action("confirm", null);
                default -> throw new AssertionError("意外回合: " + turn.getKind());
            };
            view = withPhase.act(view.sessionId(), 1L, next);
        }

        assertEquals("next", view.turn().getKind(), "注册扩展点后应由它接管 PRD 之后的流程");
    }

    @Test
    void cannotSkipAheadAfterReturningInEitherMode() {
        for (String mode : List.of("quick", "full")) {
            CoachSessionView view = service.start(1L, "整理错题", mode);
            final String id = view.sessionId();
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.act(id, 1L, action("back", null, 12)));
            service.act(id, 1L, action(null, 0));
            if ("quick".equals(mode)) service.act(id, 1L, action("confirm", null));
            CoachSessionView returned = service.act(id, 1L, action("back", null, 1));
            assertEquals(1, returned.turn().getStage());
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.act(id, 1L, action("back", null, 3)));
        }
    }

    private static CoachActionRequest action(String type, Integer index) {
        return new CoachActionRequest(type, index, null, null);
    }

    private static CoachActionRequest action(String type, Integer index, Integer stage) {
        return new CoachActionRequest(type, index, null, stage);
    }
}
