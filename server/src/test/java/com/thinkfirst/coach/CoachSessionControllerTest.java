package com.thinkfirst.coach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.coach.rag.CoachCorpusLoader;
import com.thinkfirst.coach.rag.CoachProperties;
import com.thinkfirst.auth.CurrentUser;
import com.thinkfirst.coach.rag.CoachRetriever;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoachSessionControllerTest {

    private static MockMvc mockMvc;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper();
        CoachProfileLibrary profiles = new CoachProfileLibrary(mapper);
        CoachProperties properties = new CoachProperties();
        properties.setDataDir("missing-corpus");
        CoachCorpusLoader loader = new CoachCorpusLoader(properties, mapper);
        CoachRetriever retriever = new CoachRetriever(loader);
        CoachService service = new CoachService(
                new CoachQuestionFactory(profiles, retriever, null),
                new CoachApply(profiles),
                new CoachSummary(),
                new CoachGateEvaluator(),
                new CoachPrdBuilder(),
                null,
                List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CoachSessionController(service, new CurrentUser()))
                .setControllerAdvice(new CoachExceptionHandler())
                .build();
    }

    @AfterAll
    static void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startsSessionAnswersAndExportsPrd() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/v1/coach/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idea\":\"做一个周报收集器\",\"mode\":\"quick\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.turn.kind").value("question"))
                .andExpect(jsonPath("$.data.turn.candidates.length()").value(3))
                .andReturn();

        String sessionId = mapper.readTree(started.getResponse().getContentAsString())
                .path("data").path("sessionId").asText();

        mockMvc.perform(post("/api/v1/coach/session/" + sessionId + "/turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"index\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.turn.kind").value("confirm"))
                .andExpect(jsonPath("$.data.turn.summary.length()").value(3))
                .andExpect(jsonPath("$.data.slots.s1_problem").isNotEmpty());

        mockMvc.perform(get("/api/v1/coach/session/" + sessionId + "/prd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markdown").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("做一个周报收集器"));
    }

    @Test
    void missingSessionReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/coach/session/missing/turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"index\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
