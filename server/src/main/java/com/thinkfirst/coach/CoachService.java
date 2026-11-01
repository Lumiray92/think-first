package com.thinkfirst.coach;

import com.thinkfirst.coach.dto.CoachActionRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 12 步状态机。快速模式把交互合并成 5 组，但第 7–11 步的槽位照常生成；
 * 第 6、8 步闸门在两种模式下都生效。
 */
@Service
public class CoachService {

    private static final Map<String, Integer> ROLE_INDEX = Map.ofEntries(
            Map.entry("p", 0), Map.entry("a", 1), Map.entry("b", 2),
            Map.entry("green", 0), Map.entry("yellow", 1), Map.entry("red", 2),
            Map.entry("go", 0), Map.entry("no-go", 2),
            Map.entry("data", 0), Map.entry("auto", 1),
            Map.entry("personal", 2), Map.entry("family", 3),
            Map.entry("scaff", 0), Map.entry("rgc", 1), Map.entry("story", 2), Map.entry("universal", 3),
            Map.entry("few-shot", 0), Map.entry("zero-shot", 1), Map.entry("cot", 2),
            Map.entry("both", 0), Map.entry("self", 1), Map.entry("none", 2),
            Map.entry("ok", 0), Map.entry("edit", 1), Map.entry("back", 2));

    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private ConversationStore conversationStore;

    private final Map<String, CoachSession> sessions = new ConcurrentHashMap<>();

    private final CoachQuestionFactory questions;
    private final CoachApply apply;
    private final CoachSummary summary;
    private final CoachGateEvaluator gateEvaluator;
    private final CoachPrdBuilder prdBuilder;
    private final CoachLlm llm;
    private final List<CoachNextPhase> nextPhases;

    public CoachService(CoachQuestionFactory questions, CoachApply apply,
                        CoachSummary summary, CoachGateEvaluator gateEvaluator,
                        CoachPrdBuilder prdBuilder, CoachLlm llm,
                        List<CoachNextPhase> nextPhases) {
        this.questions = questions;
        this.apply = apply;
        this.summary = summary;
        this.gateEvaluator = gateEvaluator;
        this.prdBuilder = prdBuilder;
        this.llm = llm;
        this.nextPhases = nextPhases == null ? List.of() : List.copyOf(nextPhases);
    }

    public CoachSessionView start(Long ownerId, String idea, String mode) {
        CoachSession session = new CoachSession(UUID.randomUUID().toString(), ownerId, idea, mode);
        session.getMessages().add(CoachValues.entryMap("role", "你", "text", idea, "at", System.currentTimeMillis()));
        sessions.put(session.getId(), session);
        CoachTurn turn = "quick".equals(session.getMode())
                ? askGroup(session, 1)
                : askStage(session, 1);
        return checkpoint(session, turn);
    }

    public CoachSessionView act(String sessionId, Long ownerId, CoachActionRequest action) {
        CoachSession session = find(sessionId, ownerId);
        synchronized (session) {
            String type = action == null || action.type() == null ? "" : action.type();
            String selectedText = "";
            if (action != null && action.index() != null && session.getPending() != null) {
                var choices = session.getPending().getCandidates();
                int index = action.index();
                if (choices != null && index >= 0 && index < choices.size()) selectedText = choices.get(index).text();
            }
            CoachTurn turn = switch (type) {
                case "revise" -> revise(session, action.text());
                case "polish" -> polish(session, action.text());
                case "regenerate" -> regenerate(session);
                case "back" -> backTo(session, action.stage());
                case "confirm" -> confirm(session);
                default -> answer(session, action);
            };
            String reply = action == null ? "" : CoachValues.text(action.text());
            if (reply.isBlank() && !selectedText.isBlank()) reply = selectedText;
            if (reply.isBlank() && action != null) reply = "confirm".equals(type) ? "确认并继续" : "操作：" + type;
            session.getMessages().add(CoachValues.entryMap("role", "你", "text", reply, "at", System.currentTimeMillis()));
            return checkpoint(session, turn);
        }
    }

    public List<CoachSession> historyFor(Long ownerId) {
        return conversationStore == null ? List.of() : conversationStore.list(ownerId);
    }

    public CoachSessionView resumeView(String id, Long ownerId) {
        CoachSession session = find(id, ownerId);
        synchronized(session) { return view(session, session.getPending()); }
    }

    private CoachTurn revise(CoachSession session, String text) {
        if (text == null || text.isBlank() || text.length() > 5000) throw new IllegalArgumentException("请填写修改要求，最多5000字");
        if (session.getSlots().isEmpty()) throw new IllegalStateException("请先完成第一张需求卡片");
        if (llm == null) throw new IllegalStateException("请先连接 AI");
        // 先在副本中生成，失败时保留已有字段，不将半成品写入会话。
        Map<String,Object> updated = new LinkedHashMap<>(session.getSlots());
        llm.updateSlots(session, updated, "修改已有需求，未提及的内容保持原意：" + text);
        session.getSlots().putAll(updated);
        session.setFinalPrd(null);
        session.getHistory().add(history("stage", session.getStage(), new CoachAnswer(0, text)));
        CoachTurn pending = session.getPending();
        if ("confirm".equals(pending.getKind())) pending.setSummary(summary.build(session.getSlots(), session.getGroup(), updated));
        if ("question".equals(pending.getKind())) {
            return "quick".equals(session.getMode()) ? askGroup(session, session.getGroup()) : askStage(session, session.getStage());
        }
        decorate(session, pending);
        session.touch();
        return pending;
    }

    private CoachTurn polish(CoachSession session, String instructions) {
        if (!"done".equals(session.getStatus())) throw new IllegalStateException("请先完成需求梳理");
        if (instructions == null || instructions.isBlank() || instructions.length() > 6000)
            throw new IllegalArgumentException("请填写调整要求，最多6000字");
        if (llm == null) throw new IllegalStateException("请先连接 AI");
        String source = session.getFinalPrd() != null ? session.getFinalPrd() : prdBuilder.build(session.getSlots(), session.getIdea());
        String output = llm.polish(session, source, instructions);
        session.setFinalPrd(output);
        session.getPending().setPrd(output);
        session.getHistory().add(history("stage", 12, new CoachAnswer(0, "调整文档：" + instructions)));
        session.touch();
        return session.getPending();
    }

    public CoachPrdResult getPrd(String sessionId, Long ownerId) {
        CoachSession session = find(sessionId, ownerId);
        synchronized (session) {
            String markdown = session.getFinalPrd() != null
                    ? session.getFinalPrd()
                    : prdBuilder.build(session.getSlots(), session.getIdea());
            return new CoachPrdResult(session.getId(),
                    prdBuilder.title(session.getSlots(), session.getIdea()),
                    markdown);
        }
    }

    public CoachSession find(String sessionId, Long ownerId) {
        CoachSession session = find(sessionId);
        if (ownerId == null || !ownerId.equals(session.getOwnerId())) {
            // 不暴露会话是否存在
            throw new CoachSessionNotFoundException(sessionId);
        }
        return session;
    }

    /** 仅供同包测试与内部调用；外部请求必须带 ownerId。 */
    CoachSession find(String sessionId) {
        CoachSession session = sessions.get(sessionId);
        if (session == null && conversationStore != null) {
            session = conversationStore.load(sessionId);
            CoachSession existing = sessions.putIfAbsent(sessionId, session);
            if (existing != null) session = existing;
        }
        if (session == null) {
            throw new CoachSessionNotFoundException(sessionId);
        }
        return session;
    }

    private CoachTurn answer(CoachSession session, CoachActionRequest action) {
        CoachTurn pending = session.getPending();
        List<CoachCandidate> candidates = pending == null || pending.getCandidates() == null
                ? List.of() : pending.getCandidates();
        int index = action == null || action.index() == null ? 0 : action.index();
        String actionText = action == null ? "" : CoachValues.text(action.text());
        boolean freeText = !actionText.isEmpty();
        if (pending != null && "confirm".equals(pending.getKind()) && freeText) {
            java.util.Map<String,Object> updated = new java.util.LinkedHashMap<>();
            for (int stage : CoachSchema.group(session.getGroup()).stages()) {
                updated.putAll(apply.applyStage(session.getSlots(), session.getIdea(), stage, new CoachAnswer(0, "")));
            }
            updated.replaceAll((key, value) -> session.getSlots().getOrDefault(key, value));
            if (llm == null) throw new IllegalStateException("编辑保存需要连接 AI");
            llm.updateSlots(session, updated, "用户直接编辑的最终内容，必须逐项保留：\n" + actionText);
            session.getSlots().putAll(updated);
            session.setFinalPrd(null);
            session.getHistory().add(history("group", session.getGroup(), new CoachAnswer(0, actionText)));
            return confirm(session);
        }

        String text = actionText;
        if (text.isEmpty() && index >= 0 && index < candidates.size()) {
            text = candidates.get(index).text();
        }

        // 候选可能被“换一批”轮转过，下标不能直接代表模板分支；
        // 用候选 id 还原它在模板里的固定角色，保证闸门和分支判断不错位。
        CoachCandidate selected = !freeText && index >= 0 && index < candidates.size()
                ? candidates.get(index) : null;
        int applyIndex = selected == null ? index : roleIndex(selected, index);

        CoachAnswer answer = new CoachAnswer(applyIndex, text);
        return "quick".equals(session.getMode())
                ? answerGroup(session, answer)
                : answerStage(session, answer);
    }

    private static int roleIndex(CoachCandidate candidate, int fallback) {
        if (candidate == null || candidate.id() == null) {
            return fallback;
        }
        return ROLE_INDEX.getOrDefault(candidate.id(), fallback);
    }

    private CoachTurn askStage(CoachSession session, int stageNo) {
        CoachTurn turn = questions.forStage(session, stageNo);
        session.setStage(stageNo);
        session.setGroup(CoachSchema.groupOfStage(stageNo).id());
        session.setGate(null);
        session.setStatus("asking");
        decorate(session, turn);
        session.setPending(turn);
        session.touch();
        return turn;
    }

    private CoachTurn askGroup(CoachSession session, int groupId) {
        CoachSchema.Group group = CoachSchema.group(groupId);
        CoachTurn turn = questions.forGroup(session, groupId);
        session.setGroup(group.id());
        session.setStage(group.stages().get(0));
        session.setGate(null);
        session.setStatus("asking");
        decorate(session, turn);
        session.setPending(turn);
        session.touch();
        return turn;
    }

    private CoachTurn answerStage(CoachSession session, CoachAnswer answer) {
        int stageNo = session.getStage();
        Map<String, Object> applied = apply.applyStage(
                session.getSlots(), session.getIdea(), stageNo, answer);
        if (llm != null) llm.updateSlots(session, applied, answer.text());
        session.getSlots().putAll(applied);
        session.getHistory().add(history("stage", stageNo, answer));

        CoachGate gate = gateEvaluator.evaluate(session.getSlots(), stageNo);
        if (gate != null) {
            session.setGate(gate);
            if (!"green".equals(gate.status()) && !"go".equals(gate.status())) {
                session.setStatus("blocked");
                CoachTurn turn = gateTurn(session, gate, stageNo, null);
                session.setPending(turn);
                session.touch();
                return turn;
            }
        }

        Integer next = CoachSchema.nextStageNo(stageNo);
        return next == null ? finish(session) : askStage(session, next);
    }

    private CoachTurn answerGroup(CoachSession session, CoachAnswer answer) {
        int groupId = session.getGroup();
        Map<String, Object> applied = apply.applyGroup(
                session.getSlots(), session.getIdea(), groupId, answer);
        if (llm != null) llm.updateSlots(session, applied, answer.text());
        session.getSlots().putAll(applied);
        session.getHistory().add(history("group", groupId, answer));

        CoachSchema.Group group = CoachSchema.group(groupId);
        session.setStage(group.stages().get(group.stages().size() - 1));

        CoachGate gate = null;
        for (int stageNo : group.stages()) {
            CoachGate result = gateEvaluator.evaluate(session.getSlots(), stageNo);
            if (result != null && !"green".equals(result.status()) && !"go".equals(result.status())) {
                gate = result;
            }
        }

        if (gate != null) {
            session.setGate(gate);
            session.setStatus("blocked");
            CoachTurn turn = gateTurn(session, gate, session.getStage(), groupId);
            session.setPending(turn);
            session.touch();
            return turn;
        }

        List<CoachSummaryItem> items = summary.build(session.getSlots(), groupId, applied);
        session.setStatus("confirming");
        CoachTurn turn = CoachTurn.builder()
                .kind("confirm")
                .group(groupId)
                .stage(session.getStage())
                .stageName(group.title())
                .agent(CoachSchema.stage(group.stages().get(0)).agent())
                .title(group.title())
                .message("这些内容符合你的想法吗？")
                .summary(items)
                .candidates(List.of())
                .build();
        decorate(session, turn);
        session.setPending(turn);
        session.touch();
        return turn;
    }

    private CoachTurn gateTurn(CoachSession session, CoachGate gate, Integer stageNo, Integer groupId) {
        int no = stageNo == null ? session.getStage() : stageNo;
        CoachSchema.Stage stage = CoachSchema.stage(no);
        CoachTurn turn = CoachTurn.builder()
                .kind("gate")
                .stage(no)
                .group(groupId)
                .stageName(stage.name())
                .agent(stage.agent())
                .title(gate.label())
                .message(gate.message())
                .gate(gate)
                .issues(gate.issues())
                .returnTo(gate.returnTo())
                .canContinue(!gateEvaluator.isBlocking(gate))
                .candidates(List.of())
                .build();
        decorate(session, turn);
        return turn;
    }

    private CoachTurn finish(CoachSession session) {
        session.setStatus("done");
        session.setStage(CoachSchema.TOTAL_STAGES);
        session.setGroup(5);
        session.setGate(null);
        CoachTurn turn = CoachTurn.builder()
                .kind("done")
                .stage(CoachSchema.TOTAL_STAGES)
                .stageName("PRD 成稿")
                .agent("PRD 写手")
                .title("PRD 已生成")
                .message("12 个步骤全部完成。可以导出 Markdown，也可以回到任意步骤继续改。")
                .candidates(List.of())
                .build();
        decorate(session, turn);
        if (llm != null) {
            llm.enhancePrd(session, turn, turn.getPrd());
        }
        session.setPending(turn);
        session.touch();

        for (CoachNextPhase phase : nextPhases) {
            if (phase.supports(session)) {
                CoachTurn next = phase.start(session);
                if (next != null) {
                    session.setPending(next);
                    session.touch();
                    return next;
                }
            }
        }
        return turn;
    }

    private CoachTurn confirm(CoachSession session) {
        CoachTurn pending = session.getPending();
        if (pending == null) {
            throw new IllegalStateException("当前没有待确认的回合");
        }
        if ("done".equals(pending.getKind())) {
            return pending;
        }
        if ("gate".equals(pending.getKind())) {
            if (gateEvaluator.isBlocking(pending.getGate())) {
                return pending;
            }
            if ("quick".equals(session.getMode())) {
                Integer next = CoachSchema.nextGroupId(session.getGroup());
                return next == null ? finish(session) : askGroup(session, next);
            }
            Integer next = CoachSchema.nextStageNo(session.getStage());
            return next == null ? finish(session) : askStage(session, next);
        }
        if ("confirm".equals(pending.getKind()) && "quick".equals(session.getMode())) {
            Integer next = CoachSchema.nextGroupId(session.getGroup());
            return next == null ? finish(session) : askGroup(session, next);
        }
        return pending;
    }

    private CoachTurn regenerate(CoachSession session) {
        CoachTurn pending = session.getPending();
        if (pending == null || !"question".equals(pending.getKind())
                || pending.getCandidates() == null || pending.getCandidates().isEmpty()) {
            return pending;
        }
        CoachTurn fresh = "quick".equals(session.getMode())
                ? questions.forGroup(session, session.getGroup())
                : questions.forStage(session, session.getStage());

        // 没有模型结果时保持原有“换一批”体验：轮转模板候选。
        if (!"deepseek".equals(fresh.getLlm())) {
            List<CoachCandidate> candidates = new ArrayList<>(fresh.getCandidates());
            candidates.add(candidates.remove(0));
            fresh.setCandidates(candidates);
        }
        fresh.setRegenerated(true);
        session.setPending(fresh);
        session.touch();
        return fresh;
    }

    private CoachTurn backTo(CoachSession session, Integer requestedStage) {
        if (requestedStage == null || requestedStage < 1 || requestedStage > CoachSchema.TOTAL_STAGES)
            throw new IllegalArgumentException("请选择有效的回退阶段");
        int stageNo = requestedStage;
        // 快速模式按整组回退，必须从组首清理，防止旧字段与新回答混用。
        if ("quick".equals(session.getMode())) {
            CoachSchema.Group target = CoachSchema.groupOfStage(stageNo);
            if (target.id() > session.getGroup()) throw new IllegalArgumentException("不能跳过前面的步骤");
            stageNo = target.stages().get(0);
        } else if (stageNo > session.getStage()) {
            throw new IllegalArgumentException("只能返回当前或之前的步骤");
        }
        clearStagesFrom(session, stageNo);
        session.setFinalPrd(null);
        session.setGate(null);
        session.setStatus("asking");
        session.setStage(stageNo);
        session.setGroup(CoachSchema.groupOfStage(stageNo).id());
        return "quick".equals(session.getMode())
                ? askGroup(session, session.getGroup())
                : askStage(session, stageNo);
    }

    private void clearStagesFrom(CoachSession session, int stageNo) {
        for (int no = stageNo; no <= CoachSchema.TOTAL_STAGES; no++) {
            for (String key : CoachSchema.slotsOf(no)) {
                session.getSlots().remove(key);
            }
        }
        session.touch();
    }

    private void decorate(CoachSession session, CoachTurn turn) {
        turn.setProgress(new CoachProgress(
                session.getStage(), CoachSchema.TOTAL_STAGES,
                CoachSchema.groupOfStage(session.getStage()).id(), CoachSchema.GROUPS.size()));
        turn.setPrd(prdBuilder.build(session.getSlots(), session.getIdea()));
    }

    private Map<String, Object> history(String kind, int id, CoachAnswer answer) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(kind, id);
        entry.put("answer", CoachValues.entryMap("index", answer.index(), "text", answer.text()));
        entry.put("at", System.currentTimeMillis());
        return entry;
    }

    private CoachSessionView checkpoint(CoachSession session, CoachTurn turn) {
        // 不重复记录纯读取；生成下一张卡片或文档变化时才追加。
        String content = turn.getQuestion() != null ? turn.getQuestion() : turn.getTitle();
        Map<String,Object> message = CoachValues.entryMap("role", "ThinkFirst", "text", content == null ? "" : content,
            "candidates", turn.getCandidates() == null ? List.of() : turn.getCandidates(),
            "summary", turn.getSummary() == null ? List.of() : turn.getSummary());
        if (session.getMessages().isEmpty() || !message.equals(session.getMessages().get(session.getMessages().size()-1))) session.getMessages().add(message);
        if (turn.getPrd() != null && (session.getVersions().isEmpty() || !turn.getPrd().equals(session.getVersions().get(session.getVersions().size()-1).get("markdown"))))
            session.getVersions().add(CoachValues.entryMap("at", System.currentTimeMillis(), "markdown", turn.getPrd()));
        if (conversationStore != null) conversationStore.save(session);
        return view(session, turn);
    }

    /** 查询只返回状态，避免历史列表或恢复页面触发数据库写入。 */
    private CoachSessionView view(CoachSession session, CoachTurn turn) {
        return new CoachSessionView(session.getId(), session.getMode(), session.getStatus(),
                turn, session.getSlots());
    }
}
