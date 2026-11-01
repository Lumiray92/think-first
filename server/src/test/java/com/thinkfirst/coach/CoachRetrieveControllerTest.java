package com.thinkfirst.coach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.coach.rag.CoachCorpus;
import com.thinkfirst.coach.rag.CoachCorpusLoader;
import com.thinkfirst.coach.rag.CoachProperties;
import com.thinkfirst.coach.rag.CoachRetriever;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 不启动完整 Spring 上下文，单独验证检索接口的请求/响应契约。 */
class CoachRetrieveControllerTest {

    private static MockMvc mockMvc;

    @BeforeAll
    static void setUp() throws Exception {
        CoachProperties properties = new CoachProperties();
        properties.setDataDir("../app/rag/course07");
        CoachCorpusLoader loader = new CoachCorpusLoader(properties, new ObjectMapper());
        CoachCorpus corpus = loader.load(Path.of(properties.getDataDir()));
        CoachRetriever retriever = new CoachRetriever(loader);
        retriever.rebuild(corpus);

        mockMvc = MockMvcBuilders.standaloneSetup(new CoachRetrieveController(retriever)).build();
    }

    @Test
    void retrievesStepSix() throws Exception {
        mockMvc.perform(post("/api/v1/coach/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"step\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.step").value(6))
                .andExpect(jsonPath("$.data.hits.length()").value(6))
                .andExpect(jsonPath("$.data.coverage.missing.length()").value(0));
    }

    @Test
    void rejectsCustomQueryUntilEmbeddingIsWired() throws Exception {
        mockMvc.perform(post("/api/v1/coach/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"step\":6,\"query\":\"自定义问题\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsUnknownStep() throws Exception {
        mockMvc.perform(post("/api/v1/coach/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"step\":99}"))
                .andExpect(status().isBadRequest());
    }
}
