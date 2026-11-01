package com.thinkfirst.coach;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 原型阶段的固定画像库。接 LLM 后这一层替换为模型生成，
 * 但 profile 的字段契约保持不变。
 */
@Component
public class CoachProfileLibrary {

    private static final Logger log = LoggerFactory.getLogger(CoachProfileLibrary.class);

    private final List<Map<String, Object>> profiles;

    public CoachProfileLibrary(ObjectMapper mapper) {
        List<Map<String, Object>> loaded = new ArrayList<>();
        try (InputStream input = getClass().getResourceAsStream("/coach/profiles.json")) {
            if (input == null) {
                log.warn("未找到 coach/profiles.json，将只使用通用画像");
            } else {
                loaded = mapper.readValue(input, new TypeReference<>() {
                });
            }
        } catch (Exception e) {
            log.error("加载 coach/profiles.json 失败，将只使用通用画像", e);
        }
        this.profiles = List.copyOf(loaded);
    }

    public Map<String, Object> pick(String idea) {
        return generic(idea);
    }

    public Map<String, Object> generic(String idea) {
        String raw = CoachValues.text(idea);
        String name = raw.isEmpty()
                ? "一个还没想清楚的小工具"
                : (raw.length() > 16 ? raw.substring(0, 16) + "…" : raw);
        String problem = raw.isEmpty() ? "每天都要重复处理同一件麻烦事，步骤多、容易漏" : raw;

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "generic");
        profile.put("keywords", List.of());
        profile.put("name", name);
        profile.put("user", "一个正在被这个问题困扰的具体用户");
        profile.put("situation", "每天都会遇到这个问题的时候");
        profile.put("problem", problem);
        profile.put("frequency", "每天 2–3 次");
        profile.put("duration", "10 分钟");
        profile.put("pain", "重复、易错、心里不踏实");
        profile.put("alternatives", "手动处理、现成工具、先忍着");
        profile.put("alternativePain", "分散、不稳定、不够贴合");
        profile.put("task", "用更少的步骤把这件事做完");
        profile.put("outcome", "省下时间，少出错");
        profile.put("functional", "核心动作、结果保存、失败提示");
        profile.put("emotional", "省心，不焦虑");
        profile.put("social", "在同事眼里靠谱");
        profile.put("scene", "personal");
        profile.put("p0", List.of("核心动作", "结果保存", "失败提示"));
        profile.put("p1", List.of("撤销", "历史记录"));
        profile.put("p2", List.of("分类", "统计", "多人协作"));
        profile.put("hypothesis",
                "被这个问题困扰的人愿意用一个更短路径的工具，因为它比现有方案少几步、更不容易出错。");
        profile.put("validation", "2 周内 10 个试用者中至少 6 人连续 7 天使用。");
        profile.put("constraints", "打开到完成 ≤3 步；免登录；离线可用。");
        profile.put("redlines", "不丢数据；失败必须提示。");
        profile.put("acceptance", "核心动作 ≤3 步完成；连续 7 天使用；无数据丢失。");
        profile.put("elevator", "很多人都在为“" + (raw.isEmpty() ? "这件麻烦事" : raw)
                + "”反复花时间。这个工具用最少的步骤把它做完。");
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("project", raw.isEmpty() ? "一个还没想清楚的小工具" : raw);
        context.put("domain", "用户在这个场景下反复手动处理");
        context.put("task", "先做一个能跑通核心流程的最小版本");
        profile.put("context", context);
        profile.put("framework", "few-shot");
        return profile;
    }
}
