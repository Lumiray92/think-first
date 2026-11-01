package com.thinkfirst.coach.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 12 步固定 query 的 must_cover 必须全部命中。 */
class CoachRetrieverTest {

    private static CoachRetriever retriever;

    @BeforeAll
    static void setUp() throws Exception {
        CoachProperties properties = new CoachProperties();
        properties.setDataDir("../app/rag/course07");
        CoachCorpusLoader loader = new CoachCorpusLoader(properties, new ObjectMapper());
        CoachCorpus corpus = loader.load(Path.of(properties.getDataDir()));
        assertFalse(corpus.isEmpty(), "语料应已加载");

        retriever = new CoachRetriever(loader);
        retriever.rebuild(corpus);
    }

    @Test
    void allTwelveStepsCoverMustCoverTerms() {
        for (int step = 1; step <= 12; step++) {
            CoachRetrievalResult result = retriever.retrieve(step, null);
            assertEquals(0, result.coverage().missing().size(),
                    "第 " + step + " 步缺少关键词: " + result.coverage().missing());
            assertEquals(1.0, result.coverage().rate(), "第 " + step + " 步覆盖率应为 100%");
        }
    }

    @Test
    void stepSixReturnsSoulQuestionsAndChecklist() {
        CoachRetrievalResult result = retriever.retrieve(6, 6);
        String text = result.hits().stream()
                .map(hit -> hit.title() + " " + hit.heading() + " " + hit.text())
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(text.contains("用户是谁"), "应命中灵魂三问");
        assertTrue(text.contains("痛点在哪"));
        assertTrue(text.contains("为什么选你"));
        assertTrue(text.contains("红黄绿") || text.contains("红绿灯"), "应命中红黄绿自检");
    }

    @Test
    void stepTwelveKeepsPrdTemplate() {
        CoachRetrievalResult result = retriever.retrieve(12, 8);
        assertNotNull(result.coverage());
        assertTrue(result.hits().stream().anyMatch(hit -> "template".equals(hit.docType())),
                "第 12 步应保留一条 PRD 模板");
        String text = result.hits().stream()
                .map(hit -> hit.title() + " " + hit.heading() + " " + hit.text())
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(text.contains("成功标准"), "应命中 PRD 成功标准字段");
        assertTrue(text.contains("P0"), "应命中 P0 功能");
    }

    @Test
    void rejectsUnknownStep() {
        try {
            retriever.retrieve(99, null);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("99"));
            return;
        }
        throw new AssertionError("不支持的步骤应抛出 IllegalArgumentException");
    }
}
