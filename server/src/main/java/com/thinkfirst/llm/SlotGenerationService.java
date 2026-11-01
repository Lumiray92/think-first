package com.thinkfirst.llm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.coach.CoachSession;
import com.thinkfirst.vault.ApiKeyService;
import org.springframework.stereotype.Service;
@Service
public class SlotGenerationService {
    private final LlmProperties properties;
    private final ApiKeyService apiKeys;
    private final DeepSeekClient client;
    private final ObjectMapper mapper;
    private final LlmUsageService usage;
    public SlotGenerationService(LlmProperties properties, ApiKeyService apiKeys, DeepSeekClient client, ObjectMapper mapper, LlmUsageService usage) {
        this.properties=properties; this.apiKeys=apiKeys; this.client=client; this.mapper=mapper; this.usage=usage;
    }
    public void updateSlots(CoachSession session, java.util.Map<String,Object> applied, String answer) {
        if (!properties.isEnabled()) throw new IllegalStateException("请启用模型后生成需求");
        String key = apiKeys.requirePlaintext(session.getOwnerId());
        if (key == null) throw new IllegalStateException("请在设置中配置 DeepSeek Key");
        try {
            String prompt = "原始想法：" + session.getIdea()
                + "\n历史回答：" + mapper.writeValueAsString(session.getHistory())
                + "\n已确认信息：" + mapper.writeValueAsString(session.getSlots())
                + "\n最新回答：" + answer
                + "\n待填写字段结构：" + mapper.writeValueAsString(applied);
            DeepSeekClient.Completion result = client.complete(key,
                "你是需求分析Agent。面向普通用户写作，用日常中文，不用产品开发术语。根据用户想法和最新回答重新生成待填写字段的所有值。"
                + "只输出JSON，顶层键与提供结构完全一致，保持数据类型及嵌套结构。"
                + "结构中的示例不是用户事实，禁止沿用无关人物、吃药等案例。"
                + "框架字段 s10_framework 只能选择 S.C.A.F.F.、R.G.C.、故事化 Prompt、万能模板之一。复杂开发默认S.C.A.F.F.，简单任务用R.G.C.，表达用户处境用故事化，通用任务用万能模板。s10_prompt必须按所选框架分段填写本项目具体内容，并展开各组成部分；s11_techniques只记录提供示例、自查等技巧，不可把zero-shot、few-shot、CoT当成框架。"
                + "最新回答优先，所有用户、任务、功能、约束围绕当前项目动态更新。"
                + "未知事实写待确认，不能编造年龄、验证结果或使用经历。s1_scores 的 repeat/rule/verify/sensitive/fault 为0至5分，总分为五项之和，增加对应的 *_reason 字段逐项解释依据。敏感性越不敏感分越高，容错性越易纠错分越高。",
                prompt, 0.3, properties.getSlotsMaxTokens());
            JsonNode node = mapper.readTree(result.content());
            if (!node.isObject()) throw new IllegalArgumentException();
            java.util.Map<String,Object> updated = mapper.convertValue(node,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>() {});
            if (!updated.keySet().equals(applied.keySet())) throw new IllegalArgumentException();
            for (String field : applied.keySet()) {
                Object old = applied.get(field), value = updated.get(field);
                if (value == null || (old instanceof String && !(value instanceof String))
                    || (old instanceof java.util.Map && !(value instanceof java.util.Map))
                    || (old instanceof java.util.List && !(value instanceof java.util.List))) throw new IllegalArgumentException();
            }
            // 兼容模型偶尔沿用旧分类，禁止将技巧保存为框架。
            if (updated.containsKey("s10_framework")) {
                String framework = String.valueOf(updated.get("s10_framework"));
                if (!java.util.Arrays.asList(com.thinkfirst.coach.PromptFrameworks.NAMES).contains(framework)) {
                    throw new IllegalArgumentException("模型返回了不支持的提示词框架，请重试");
                }
            }
            applied.clear(); applied.putAll(updated);
            usage.record(session.getOwnerId(), "slots", session.getStage(), result);
        } catch (Exception e) {
            throw new IllegalStateException("需求更新失败，请重试，本次未保存", e);
        }
    }

}
