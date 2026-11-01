package com.thinkfirst.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.vault.ApiKeyCrypto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tf-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "coach.enabled=false",
        "coach.data-dir=missing-corpus",
        "security.api-key.validation-enabled=false",
        "security.jwt.secret=test-jwt-secret-with-at-least-32-bytes",
        "security.api-key.master-key=bmuad7ZwbJTQNUBWidravaQbR/xahc68kMb/Hd2zTXc="
})
@AutoConfigureMockMvc
@Transactional
class AuthAndVaultIntegrationTest {

    private static final String RAW_KEY = "sk-test-1234567890abcd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiKeyCrypto crypto;

    @Test
    void registerLoginAndMe() throws Exception {
        register("alice@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String token = login("alice@example.com", "password123");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.hasApiKey").value(false));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"short@example.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("密码长度需为 8-72 位"));
    }

    @Test
    void coachRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/coach/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idea\":\"做一个周报收集器\",\"mode\":\"quick\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String token = register("bob@example.com", "password123");
        mockMvc.perform(post("/api/v1/coach/session")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idea\":\"做一个周报收集器\",\"mode\":\"quick\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.turn.kind").value("question"))
                .andExpect(jsonPath("$.data.turn.llm").value("template"))
                .andExpect(jsonPath("$.data.turn.llmNote").isNotEmpty());
    }

    @Test
    void coachSessionCannotBeAccessedByAnotherUser() throws Exception {
        String tokenA = register("frank@example.com", "password123");
        MvcResult started = mockMvc.perform(post("/api/v1/coach/session")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idea\":\"做一个周报收集器\",\"mode\":\"quick\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String sessionId = mapper.readTree(started.getResponse().getContentAsString())
                .path("data").path("sessionId").asText();

        String tokenB = register("grace@example.com", "password123");
        mockMvc.perform(post("/api/v1/coach/session/" + sessionId + "/turn")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"index\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/v1/coach/session/" + sessionId + "/prd")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void apiKeyIsEncryptedAtRestAndMasked() throws Exception {
        String token = register("carol@example.com", "password123");

        MvcResult saved = mockMvc.perform(put("/api/v1/user/api-key")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + RAW_KEY + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyMask").value("sk-****abcd"))
                .andExpect(jsonPath("$.data.hasApiKey").value(true))
                .andReturn();

        String savedBody = saved.getResponse().getContentAsString();
        assertThat(savedBody).doesNotContain(RAW_KEY);

        MvcResult fetched = mockMvc.perform(get("/api/v1/user/api-key")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyMask").value("sk-****abcd"))
                .andReturn();
        assertThat(fetched.getResponse().getContentAsString()).doesNotContain(RAW_KEY);

        User user = userRepository.findByEmailIgnoreCase("carol@example.com").orElseThrow();
        assertThat(user.getApiKeyCipher()).isNotBlank();
        assertThat(user.getApiKeyCipher()).doesNotContain(RAW_KEY);
        assertThat(user.getApiKeyCipher()).doesNotContain("sk-test");
        assertThat(crypto.decrypt(user.getApiKeyCipher())).isEqualTo(RAW_KEY);
    }

    @Test
    void apiKeyFormatRejectedAndAuthRequired() throws Exception {
        String token = register("dave@example.com", "password123");

        mockMvc.perform(put("/api/v1/user/api-key")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"not-a-key\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/v1/user/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + RAW_KEY + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clearApiKeyRemovesCiphertext() throws Exception {
        String token = register("erin@example.com", "password123");

        mockMvc.perform(put("/api/v1/user/api-key")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + RAW_KEY + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/user/api-key")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasApiKey").value(false));

        User user = userRepository.findByEmailIgnoreCase("erin@example.com").orElseThrow();
        assertThat(user.getApiKeyCipher()).isNull();
        assertThat(user.getApiKeyMask()).isNull();
    }

    private String register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }
}
