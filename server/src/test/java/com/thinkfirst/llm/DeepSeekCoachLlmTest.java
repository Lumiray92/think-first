package com.thinkfirst.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.coach.CoachCandidate;
import com.thinkfirst.coach.CoachCitation;
import com.thinkfirst.coach.CoachSession;
import com.thinkfirst.coach.CoachTurn;
import com.thinkfirst.vault.ApiKeyException;
import com.thinkfirst.vault.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeepSeekCoachLlmTest {

    private static final String KEY = "sk-test-1234567890abcd";

    private ApiKeyService apiKeys;
    private DeepSeekClient client;
    private LlmUsageService usage;
    private DeepSeekCoachLlm llm;
    private CoachSession session;

    @BeforeEach
    void setUp() {
        apiKeys = mock(ApiKeyService.class);
        client = mock(DeepSeekClient.class);
        usage = mock(LlmUsageService.class);
        LlmProperties properties = new LlmProperties();
        properties.setEnabled(true);
        llm = new DeepSeekCoachLlm(properties, apiKeys, client, usage, new ObjectMapper());
        session = new CoachSession("s1", 1L, "做一个提醒妈妈吃药的小程序", "quick");
        when(apiKeys.requirePlaintext(1L)).thenReturn(KEY);
    }

    @Test
    void rewritesCandidatesWhenModelReturnsValidJson() {
        CoachTurn turn = questionTurn(1, null);
        String json = """
                {"question":"妈妈最近一周忘吃药几次？","hint":"说具体次数",
                 "candidates":[
                   {"id":"p","text":"新 p","why":"新 p 理由"},
                   {"id":"a","text":"新 a","why":"新 a 理由"},
                   {"id":"b","text":"新 b","why":"新 b 理由"}]}
                """;
        when(client.complete(eq(KEY), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(new DeepSeekClient.Completion(json, 12, 34));

        llm.enhanceQuestion(session, turn, List.of(1));

        assertEquals("deepseek", turn.getLlm());
        assertEquals("妈妈最近一周忘吃药几次？", turn.getQuestion());
        assertEquals(3, turn.getCandidates().size());
        assertEquals("p", turn.getCandidates().get(0).id());
        assertEquals("新 p", turn.getCandidates().get(0).text());
        verify(usage).record(eq(1L), eq("question"), eq(1), any());
    }

    @Test
    void keepsTemplateWhenModelFails() {
        CoachTurn turn = questionTurn(1, null);
        when(client.complete(eq(KEY), anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new LlmException("API Key 余额不足"));

        llm.enhanceQuestion(session, turn, List.of(1));

        assertEquals("template", turn.getLlm());
        assertEquals("API Key 余额不足", turn.getLlmNote());
        assertEquals("原 p", turn.getCandidates().get(0).text());
        verify(usage, never()).record(any(), anyString(), any(), any());
    }

    @Test
    void keepsTemplateWhenCandidateIdsChange() {
        CoachTurn turn = questionTurn(1, null);
        String json = """
                {"question":"换个问法","candidates":[
                   {"id":"x","text":"x","why":"x"},
                   {"id":"y","text":"y","why":"y"},
                   {"id":"z","text":"z","why":"z"}]}
                """;
        when(client.complete(eq(KEY), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(new DeepSeekClient.Completion(json, 10, 10));

        llm.enhanceQuestion(session, turn, List.of(1));

        assertEquals("template", turn.getLlm());
        assertEquals("原问题", turn.getQuestion());
        assertEquals("原 p", turn.getCandidates().get(0).text());
    }

    @Test
    void skipsGateSensitiveStage() {
        CoachTurn turn = questionTurn(6, null);
        llm.enhanceQuestion(session, turn, List.of(6));

        assertNull(turn.getLlm());
        assertEquals("原 p", turn.getCandidates().get(0).text());
        verify(client, never()).complete(anyString(), anyString(), anyString(), anyDouble(), anyInt());
    }

    @Test
    void usesTemplateWhenKeyIsMissing() {
        when(apiKeys.requirePlaintext(1L)).thenThrow(new ApiKeyException("请先设置 Key"));
        CoachTurn turn = questionTurn(1, null);

        llm.enhanceQuestion(session, turn, List.of(1));

        assertEquals("template", turn.getLlm());
        assertTrue(turn.getLlmNote().contains("API Key"));
        verify(client, never()).complete(anyString(), anyString(), anyString(), anyDouble(), anyInt());
    }

    @Test
    void enhancesPrdOnlyWhenAllSectionsSurvive() {
        CoachTurn turn = CoachTurn.builder().kind("done").stage(12).build();
        String base = prd("# PRD");
        String enhanced = prd("# 优化后的 PRD");
        when(client.complete(eq(KEY), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(new DeepSeekClient.Completion(enhanced, 100, 200));

        llm.enhancePrd(session, turn, base);

        assertEquals("deepseek", turn.getLlm());
        assertTrue(turn.getPrd().contains("# 优化后的 PRD"));
        assertEquals(turn.getPrd(), session.getFinalPrd());
        verify(usage).record(eq(1L), eq("prd"), eq(12), any());
    }

    @Test
    void keepsTemplatePrdWhenSectionsAreMissing() {
        CoachTurn turn = CoachTurn.builder().kind("done").stage(12).build();
        String base = prd("# PRD");
        when(client.complete(eq(KEY), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(new DeepSeekClient.Completion("# 只有标题\n", 100, 5));

        llm.enhancePrd(session, turn, base);

        assertEquals("template", turn.getLlm());
        assertNull(turn.getPrd());
        assertNull(session.getFinalPrd());
    }

    private CoachTurn questionTurn(int stage, Integer group) {
        return CoachTurn.builder()
                .kind("question")
                .stage(stage)
                .group(group)
                .stageName("问题发现")
                .question("原问题")
                .hint("原提示")
                .candidates(List.of(
                        new CoachCandidate("p", "原 p", "p 理由"),
                        new CoachCandidate("a", "原 a", "a 理由"),
                        new CoachCandidate("b", "原 b", "b 理由")))
                .citations(List.of(new CoachCitation("course07-Prompt", 1, "标题", "片段")))
                .build();
    }

    private static String prd(String title) {
        StringBuilder builder = new StringBuilder(title).append("\n\n");
        for (String section : List.of("一句话描述", "目标用户", "要解决的问题", "用户任务（JTBD）",
                "核心功能（P0，必须有）", "不做的功能", "用户流程", "技术约束",
                "成功标准", "风险与对策", "最终自检", "给 AI 的 Prompt")) {
            builder.append("## ").append(section).append("\n\n内容\n\n");
        }
        return builder.toString();
    }
}
