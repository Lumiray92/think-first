package com.thinkfirst.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.coach.CoachCandidate;
import com.thinkfirst.coach.CoachCitation;
import com.thinkfirst.coach.CoachLlm;
import com.thinkfirst.coach.CoachSession;
import com.thinkfirst.coach.CoachTurn;
import com.thinkfirst.vault.ApiKeyException;
import com.thinkfirst.vault.ApiKeyService;
import com.thinkfirst.vault.KeyVaultUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用用户自己的 DeepSeek Key 增强候选与 PRD。
 * 只增强“文案型”步骤；灵魂三问、场景、框架等依赖候选下标的步骤保留模板，避免语义错位。
 */
@Component
public class DeepSeekCoachLlm implements CoachLlm {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekCoachLlm.class);

    private static final Set<Integer> SAFE_STAGES = Set.of(1, 2, 3, 4, 5, 9, 12);
    private static final Set<Integer> SAFE_GROUPS = Set.of(1, 2);

    private static final List<String> PRD_SECTIONS = List.of(
            "一句话描述", "目标用户", "要解决的问题", "用户任务（JTBD）",
            "核心功能（P0，必须有）", "不做的功能", "用户流程", "技术约束",
            "成功标准", "风险与对策", "最终自检", "给 AI 的 Prompt");

    private static final String QUESTION_SYSTEM = """
            你是 ThinkFirst 的需求教练，严格按 course07 的 12 步方法论提问。
            规则：
            1. 不替用户做决定，只给 3 个方向明显不同的候选。
            2. 每个候选尽量在40字以内，只表达一个选择。提问不超过30字，直说现在要用户决定什么。禁止使用 P0、Top3、JTBD、Go、No-Go、灵魂三问、闸门、上下文、最小实验等术语；改用必须做的功能、主要风险、用户想做的事、可以开始、先找人试用等日常说法。
            3. 禁止编造用户没说过的事实或数字。
            4. 必须保持候选的 id、顺序及决策角色，但内容必须根据本次用户想法、最近回答和已确认信息重新生成。模板只表示决策角色，不是用户事实。
            6. 最近回答优先于早期推测，禁止把参考案例的人物、疾病、功能带入用户项目。每个选项都要承接上一步，提供具体可选的下一步方向。
            5. 只输出 JSON，不要 Markdown 代码围栏，不要额外解释。
            """;

    private static final String PRD_SYSTEM = """
            你是资深产品经理。把输入 PRD 改写成更清晰、可直接交给 AI 编码的中文 Markdown。
            硬性要求：
            1. 保留且只保留原有的 12 个 ## 章节，标题逐字不变。
            2. 不新增事实、不删关键约束，不改变 P0/P1/P2 的划分。
            3. 保留风险、验收标准和给 AI 的 Prompt。
            4. 文档给没有学习过知识库的大模型阅读，必须自包含：说明每个评分维度、分数、依据；展开缩写，解释规则、状态及验收条件，不引用未提供的方法论。不得删除评分表或评分解释。直接输出 Markdown，不要代码围栏，不要额外说明。
            """;

    private final LlmProperties properties;
    private final ApiKeyService apiKeys;
    private final DeepSeekClient client;
    private final LlmUsageService usage;
    private final ObjectMapper mapper;

    public DeepSeekCoachLlm(LlmProperties properties, ApiKeyService apiKeys,
                            DeepSeekClient client, LlmUsageService usage, ObjectMapper mapper) {
        this.properties = properties;
        this.apiKeys = apiKeys;
        this.client = client;
        this.usage = usage;
        this.mapper = mapper;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private SlotGenerationService slotGeneration;
    @Override
    public void updateSlots(CoachSession session, java.util.Map<String,Object> applied, String answer) {
        slotGeneration.updateSlots(session, applied, answer);
    }

    @Override
    public void enhanceQuestion(CoachSession session, CoachTurn turn, List<Integer> stages) {
        if (!properties.isEnabled() || session == null || turn == null
                || !"question".equals(turn.getKind()) || !isSafe(turn)) {
            return;
        }
        String apiKey = apiKey(session, turn);
        if (apiKey == null) {
            return;
        }
        try {
            DeepSeekClient.Completion completion = client.complete(
                    apiKey, QUESTION_SYSTEM, questionPrompt(session, turn), 0.6,
                    properties.getQuestionMaxTokens());
            applyQuestion(turn, completion.content());
            turn.setLlm("deepseek");
            turn.setLlmNote(null);
            usage.record(session.getOwnerId(), "question", turn.getStage(), completion);
        } catch (LlmException e) {
            markTemplate(turn, e.getMessage());
            log.warn("候选生成回退模板: userId={}, stage={}, reason={}",
                    session.getOwnerId(), turn.getStage(), e.getMessage());
        } catch (RuntimeException e) {
            markTemplate(turn, "模型输出异常，已使用内置模板");
            log.warn("候选生成回退模板: userId={}, stage={}, error={}",
                    session.getOwnerId(), turn.getStage(), e.getClass().getSimpleName());
        }
    }

    @Override
    public void enhancePrd(CoachSession session, CoachTurn turn, String basePrd) {
        if (!properties.isEnabled() || session == null || turn == null
                || basePrd == null || basePrd.isBlank()) {
            return;
        }
        String apiKey = apiKey(session, turn);
        if (apiKey == null) {
            return;
        }
        try {
            DeepSeekClient.Completion completion = client.complete(
                    apiKey, PRD_SYSTEM, "PRD 初稿：\n\n" + basePrd, 0.3,
                    properties.getPrdMaxTokens());
            String markdown = stripFences(completion.content()).trim();
            if (!hasAllSections(markdown)) {
                markTemplate(turn, "模型输出的 PRD 缺少必要章节，已保留模板版本");
                return;
            }
            turn.setPrd(markdown.endsWith("\n") ? markdown : markdown + "\n");
            session.setFinalPrd(turn.getPrd());
            turn.setLlm("deepseek");
            turn.setLlmNote(null);
            usage.record(session.getOwnerId(), "prd", 12, completion);
        } catch (LlmException e) {
            markTemplate(turn, e.getMessage());
            log.warn("PRD 生成回退模板: userId={}, reason={}",
                    session.getOwnerId(), e.getMessage());
        } catch (RuntimeException e) {
            markTemplate(turn, "模型输出异常，已保留模板 PRD");
            log.warn("PRD 生成回退模板: userId={}, error={}",
                    session.getOwnerId(), e.getClass().getSimpleName());
        }
    }

    @Override
    public String polish(CoachSession session, String source, String instructions) {
        if (!properties.isEnabled()) throw new IllegalStateException("请先启用 AI 服务");
        String key = apiKeys.requirePlaintext(session.getOwnerId());
        DeepSeekClient.Completion result = client.complete(key,
            "你是产品需求与编程任务文档编辑。按照用户选择的目标格式改写完整原稿，直接输出 Markdown。公司详细版目标为3000至5000个中文字，重视内容完整而非凑字数。"
            + "文档必须自包含，保留需求、限制、评分及依据、风险和验收条件；不依赖知识库。"
            + "不得编造调研结论、技术现状、预算或承诺。新增推断标记为待确认。"
            + "公司详细版需充分展开背景、目标、角色、功能流程、异常、数据、非功能需求、边界、验收、风险和待确认事项。"
            + "Codex和Claude Code版本需写成可直接交给相应编程智能体的完整任务提示词，包含目标、仓库检查、实现范围、约束、步骤、测试及交付要求，不能假设智能体已读原稿。",
            "用户格式及修改要求：\n" + instructions + "\n完整原稿：\n" + source,
            0.3, properties.getPolishMaxTokens());
        String output = stripFences(result.content()).trim();
        if (output.length() < 100) throw new IllegalStateException("生成内容不完整，请重试");
        usage.record(session.getOwnerId(), "polish", 12, result);
        return output + "\n";
    }

    private boolean isSafe(CoachTurn turn) {
        if (turn.getGroup() != null) {
            return SAFE_GROUPS.contains(turn.getGroup());
        }
        return turn.getStage() != null && SAFE_STAGES.contains(turn.getStage());
    }

    private String apiKey(CoachSession session, CoachTurn turn) {
        try {
            return apiKeys.requirePlaintext(session.getOwnerId());
        } catch (ApiKeyException e) {
            markTemplate(turn, "尚未设置 API Key，当前使用内置模板");
            return null;
        } catch (KeyVaultUnavailableException e) {
            markTemplate(turn, e.getMessage());
            return null;
        }
    }

    private String questionPrompt(CoachSession session, CoachTurn turn) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("项目想法：").append(limit(session.getIdea(), 400)).append('\n');
        prompt.append("当前步骤：第 ").append(turn.getStage()).append(" 步 · ")
                .append(text(turn.getStageName())).append('\n');
        prompt.append("这一步的目标：").append(text(turn.getHint())).append('\n');
        prompt.append("原始问题：").append(text(turn.getQuestion())).append('\n');
        prompt.append("用户此前的回答（按时间顺序，最后一条是上一步）：").append(session.getHistory()).append("\n");
        prompt.append("已确认的信息：").append(slotsSummary(session)).append("\n\n");

        prompt.append("参考资料（course07）：\n");
        List<CoachCitation> citations = turn.getCitations();
        if (citations == null || citations.isEmpty()) {
            prompt.append("（无）\n");
        } else {
            int index = 1;
            for (CoachCitation citation : citations) {
                prompt.append('[').append(index++).append("] ")
                        .append(text(citation.title())).append("：")
                        .append(limit(citation.snippet(), 160)).append('\n');
            }
        }

        prompt.append("\n候选角色（必须保持 id 和顺序不变）：\n");
        for (CoachCandidate candidate : turn.getCandidates()) {
            prompt.append("- ").append(candidate.id()).append("：")
                    .append(candidate.text()).append("（").append(candidate.why()).append("）\n");
        }
        prompt.append("""
                只输出 JSON：
                {"question":"更具体的提问","hint":"一句提示","candidates":[{"id":"p","text":"...","why":"..."},...]}
                要求：候选数量、id、顺序与上面完全一致；具体、口语化、不编造事实。
                """);
        return prompt.toString();
    }

    private void applyQuestion(CoachTurn turn, String content) {
        JsonNode root = readJson(content);
        if (root == null) {
            throw new LlmException("模型输出不是有效 JSON");
        }
        String question = root.path("question").asText("").trim();
        if (question.isEmpty()) {
            throw new LlmException("模型输出缺少问题");
        }

        List<CoachCandidate> existing = turn.getCandidates();
        JsonNode array = root.path("candidates");
        if (array == null || !array.isArray() || array.size() != existing.size()) {
            throw new LlmException("模型输出的候选数量不一致");
        }

        List<CoachCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < existing.size(); i++) {
            JsonNode node = array.get(i);
            String id = node.path("id").asText("").trim();
            String candidateText = node.path("text").asText("").trim();
            if (!existing.get(i).id().equals(id) || candidateText.isEmpty()) {
                throw new LlmException("模型输出的候选 id 或文案不完整");
            }
            String why = node.path("why").asText("").trim();
            candidates.add(new CoachCandidate(id, candidateText,
                    why.isEmpty() ? existing.get(i).why() : why));
        }

        turn.setQuestion(question);
        String hint = root.path("hint").asText("").trim();
        if (!hint.isEmpty()) {
            turn.setHint(hint);
        }
        turn.setCandidates(candidates);
    }

    private JsonNode readJson(String content) {
        String json = stripFences(content);
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return mapper.readTree(json.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private String slotsSummary(CoachSession session) {
        try {
            String json = mapper.writeValueAsString(session.getSlots());
            return limit(json, 1400);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean hasAllSections(String markdown) {
        for (String section : PRD_SECTIONS) {
            if (!markdown.contains("## " + section)) {
                return false;
            }
        }
        int headings = 0;
        for (String line : markdown.split("\n")) {
            if (line.startsWith("## ")) {
                headings++;
            }
        }
        return headings == PRD_SECTIONS.size();
    }

    private static String stripFences(String content) {
        String text = content == null ? "" : content.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        if (firstLineEnd < 0) {
            return "";
        }
        text = text.substring(firstLineEnd + 1).trim();
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }
        return text;
    }

    private static void markTemplate(CoachTurn turn, String note) {
        turn.setLlm("template");
        turn.setLlmNote(note);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String limit(String value, int max) {
        String text = text(value);
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
