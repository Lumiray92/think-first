package com.thinkfirst.coach;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 通过真实控制器、认证、数据库验证恢复；模型使用替身，避免测试产生费用。 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:conversation-test;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "coach.data-dir=missing-corpus", "security.api-key.validation-enabled=false"})
@AutoConfigureMockMvc
class ConversationPersistenceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CoachService service;
    @Autowired ConversationStore store;
    @MockBean CoachLlm llm;
    private String register(String email)throws Exception {
        String json=mapper.writeValueAsString(Map.of("email",email,"password","testPassword123"));
        return mapper.readTree(mvc.perform(post("/api/v1/auth/register").with(r->{r.setRemoteAddr(email);return r;}).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("token").asText();
    }
    @Test void recoverFromDatabaseAndDenyOtherAccount()throws Exception {
        String token=register("restore@example.com");
        JsonNode first=mapper.readTree(mvc.perform(post("/api/v1/coach/session").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"idea\":\"整理错题\",\"mode\":\"quick\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        String id=first.path("sessionId").asText();
        mvc.perform(post("/api/v1/coach/session/"+id+"/turn").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"index\":0}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.turn.kind").value("confirm"));
        assertThat(store.load(id).getHistory()).hasSize(1);
        // 清空进程缓存，强制从数据库反序列化，模拟后端重启后的第一次读取。
        ((Map<?,?>)ReflectionTestUtils.getField(service,"sessions")).clear();
        mvc.perform(get("/api/v1/coach/session/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.turn.kind").value("confirm"));
        mvc.perform(post("/api/v1/coach/session/"+id+"/turn").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"confirm\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.turn.kind").value("question"));
        mvc.perform(get("/api/v1/coach/session").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(id));
        String other=register("other-restore@example.com");
        mvc.perform(get("/api/v1/coach/session/"+id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/coach/session").header("Authorization","Bearer "+other)).andExpect(jsonPath("$.data.length()").value(0));
    }
    @Test void oversizedChinesePasswordAndLoginRateLimit()throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email","long@example.com","password","密".repeat(30))))).andExpect(status().isBadRequest());
        for(int i=0;i<10;i++) mvc.perform(post("/api/v1/auth/login").with(r->{r.setRemoteAddr("rate-test");return r;}).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"missing@example.com\",\"password\":\"wrongpassword\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").with(r->{r.setRemoteAddr("rate-test");return r;}).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"missing@example.com\",\"password\":\"wrongpassword\"}")).andExpect(status().isTooManyRequests());
    }
}
