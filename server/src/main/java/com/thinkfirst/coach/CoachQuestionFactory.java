package com.thinkfirst.coach;

import com.thinkfirst.coach.rag.CoachHit;
import com.thinkfirst.coach.rag.CoachRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 出题、生成 3 个候选，并附上 course07 的检索引用。 */
@Component
public class CoachQuestionFactory {

    private static final Logger log = LoggerFactory.getLogger(CoachQuestionFactory.class);
    private static final int SNIPPET_LENGTH = 120;

    private final CoachProfileLibrary profiles;
    private final CoachRetriever retriever;
    private final CoachLlm llm;

    public CoachQuestionFactory(CoachProfileLibrary profiles, CoachRetriever retriever,
                                CoachLlm llm) {
        this.profiles = profiles;
        this.retriever = retriever;
        this.llm = llm;
    }

    public CoachTurn forStage(CoachSession session, int stageNo) {
        CoachSchema.Stage stage = CoachSchema.stage(stageNo);
        Map<String, Object> profile = profiles.pick(session.getIdea());
        CoachTurn turn = CoachTurn.builder()
                .kind("question")
                .stage(stageNo)
                .stageName(stage.name())
                .agent(stage.agent())
                .title(stage.name())
                .question(stage.driver())
                .hint(stage.goal())
                .candidates(stageCandidates(profile, stageNo))
                .citations(citationsFor(List.of(stageNo)))
                .build();
        if (llm != null) {
            llm.enhanceQuestion(session, turn, List.of(stageNo));
        }
        return turn;
    }

    public CoachTurn forGroup(CoachSession session, int groupId) {
        CoachSchema.Group group = CoachSchema.group(groupId);
        Map<String, Object> profile = profiles.pick(session.getIdea());
        CoachTurn turn = CoachTurn.builder()
                .kind("question")
                .group(group.id())
                .stage(group.stages().get(0))
                .stageName(group.title())
                .agent(CoachSchema.stage(group.stages().get(0)).agent())
                .title(group.title())
                .question(group.driver())
                .hint(group.hint())
                .candidates(groupCandidates(profile, group))
                .citations(citationsFor(group.stages()))
                .build();
        if (llm != null) {
            llm.enhanceQuestion(session, turn, group.stages());
        }
        return turn;
    }

    private List<CoachCandidate> stageCandidates(Map<String, Object> profile, int no) {
        return switch (no) {
            case 1 -> List.of(
                    candidate("p", CoachValues.str(profile, "problem"), "来自真实的一周烦恼记录"),
                    candidate("a", "每天 " + CoachValues.str(profile, "frequency")
                                    + "，每次 " + CoachValues.str(profile, "duration")
                                    + "，最痛的是" + CoachValues.str(profile, "pain") + "。",
                            "把频率和痛苦说清楚"),
                    candidate("b", "现在用" + CoachValues.str(profile, "alternatives")
                                    + "，但" + CoachValues.str(profile, "alternativePain") + "。",
                            "强调现有方案为什么不行"));
            case 2 -> {
                String user = CoachValues.str(profile, "user");
                yield List.of(
                        candidate("p", "当" + user + "在" + whenClause(profile) + "时，想要"
                                        + CoachValues.str(profile, "task") + "，以便"
                                        + CoachValues.str(profile, "outcome") + "。",
                                "符合 JTBD 的 4+2 句式"),
                        candidate("a", user + "现在靠" + CoachValues.str(profile, "alternatives")
                                        + "，问题是" + CoachValues.str(profile, "alternativePain") + "。",
                                "从现有替代方案切入"),
                        candidate("b", "他想完成的是" + CoachValues.str(profile, "task") + "。",
                                "只保留任务，不猜方案"));
            }
            case 3 -> List.of(
                    candidate("p", CoachApply.RISKS.get(0) + "；" + CoachApply.RISKS.get(1)
                                    + "；" + CoachApply.RISKS.get(2),
                            "按需求/习惯/场景分层"),
                    candidate("a", String.join("；", CoachApply.RISKS.subList(0, 5)),
                            "先铺满，再选 Top3"),
                    candidate("b", "需求没验证、用户群判断错、替代方案太强",
                            "从需求层反向检查"));
            case 4 -> {
                List<String> p0 = CoachValues.strList(profile, "p0");
                List<String> p1 = CoachValues.strList(profile, "p1");
                List<String> p2 = CoachValues.strList(profile, "p2");
                yield List.of(
                        candidate("p", "只做 " + String.join("、", p0) + "，其余全部进不做清单。",
                                "P0 不超过 3～5 个"),
                        candidate("a", "先做 " + first(p0) + "，验证核心假设后再加 " + first(p1) + "。",
                                "一次只验证一个假设"),
                        candidate("b", String.join("、", p0) + "，再加 " + first(p2) + "，一次做完整。",
                                "完整方案，但风险更高"));
            }
            case 5 -> List.of(
                    candidate("p", CoachValues.str(profile, "user") + "，在"
                                    + CoachValues.str(profile, "situation") + "，最怕"
                                    + CoachValues.str(profile, "pain") + "。",
                            "具体到一个人和一个场景"),
                    candidate("a", CoachValues.str(profile, "user") + "；表面是"
                                    + CoachValues.str(profile, "problem") + "，深层是"
                                    + CoachValues.str(profile, "pain") + "。",
                            "区分表面痛点和深层动机"),
                    candidate("b", "先不限定用户，谁都能用。", "范围更大，但更难验证"));
            case 6 -> List.of(
                    candidate("green", "我知道给谁用、解决什么问题，也了解他们为什么愿意用。",
                            "可以直接开工"),
                    candidate("yellow", "用户和痛点清楚，为什么选我还说不清。（两绿一黄）",
                            "建议先做最小实验"),
                    candidate("red", "用户是谁还说不清，先别写代码。（有红灯）",
                            "回到 JTBD 补课"));
            case 7 -> CoachSchema.SCENES.stream()
                    .map(scene -> candidate(scene.key(), scene.title() + "：" + scene.desc(),
                            scene.constraint()))
                    .toList();
            case 8 -> List.of(
                    candidate("go", "准备好了：知道给谁用、解决什么问题，主要风险有应对办法，"
                                    + "第一版只做最需要的功能。",
                            "可以进入写 PRD"),
                    candidate("yellow", "还不太确定有没有人需要，想先找人试用，再决定要不要继续。",
                            "允许继续，但要带风险"),
                    candidate("no-go", "还没想清楚要做哪些功能、可能遇到什么问题，先补充这些。",
                            "闸门拦截，回到第 4 步"));
            case 9 -> {
                Map<String, Object> context = CoachValues.map(profile, "context");
                yield List.of(
                        candidate("p", "项目：" + CoachValues.str(context, "project")
                                        + "；领域：" + CoachValues.str(context, "domain")
                                        + "；任务：" + CoachValues.str(context, "task") + "。",
                                "三层上下文齐全"),
                        candidate("a", "项目：" + CoachValues.str(context, "project")
                                        + "；任务：" + CoachValues.str(context, "task") + "。",
                                "先给项目层和任务层"),
                        candidate("b", "不写上下文，直接把想法丢给 AI。",
                                "省事，但 AI 容易自由发挥"));
            }
            case 10 -> List.of(
                    candidate("scaff", "S.C.A.F.F.：完整说明背景、任务、使用者、交付格式和约束，适合开发项目。", "复杂开发任务"),
                    candidate("rgc", "R.G.C.：说明角色、目标和约束，适合简单任务。", "简短任务"),
                    candidate("story", "故事化 Prompt：讲清用户处境、问题和期望。", "产品需求沟通"),
                    candidate("universal", "万能模板：逐项整理背景、资料、任务与验收要求。", "通用任务"));
            case 11 -> List.of(
                    candidate("both", "Few-shot + Self-Critique：先给示例，再让 AI 自查一遍。",
                            "格式和正确性都要"),
                    candidate("self", "只加 Self-Critique：生成后自查修正。",
                            "核心代码/公开文档"),
                    candidate("none", "先不加，直接跑第一版。", "先看效果，再决定"));
            default -> List.of(
                    candidate("ok", "准确，可以导出。", "PRD 字段齐全"),
                    candidate("edit", "大方向对，细节我再改。", "进入编辑模式"),
                    candidate("back", "不对，我要回到对应步骤重想。", "回炉"));
        };
    }

    private List<CoachCandidate> groupCandidates(Map<String, Object> profile,
                                                 CoachSchema.Group group) {
        return switch (group.id()) {
            case 1 -> List.of(
                    candidate("p", "我想做一个" + CoachValues.str(profile, "name") + "："
                                    + CoachValues.str(profile, "user") + "在"
                                    + CoachValues.str(profile, "situation") + "，"
                                    + CoachValues.str(profile, "problem") + "。",
                            "把用户、场景、麻烦一次说清"),
                    candidate("a", CoachValues.str(profile, "problem") + "，现在用"
                                    + CoachValues.str(profile, "alternatives") + "，但"
                                    + CoachValues.str(profile, "alternativePain") + "。",
                            "从现有方案切入"),
                    candidate("b", "帮我做一个" + CoachValues.str(profile, "name") + "，先解决"
                                    + CoachValues.str(profile, "task") + "。",
                            "一句话版本，细节交给后面"));
            case 2 -> List.of(
                    candidate("p", "最可能死在" + CoachApply.RISKS.get(1) + "和"
                                    + CoachApply.RISKS.get(0) + "。",
                            "先抓两个最致命的"),
                    candidate("a", "功能太多做不完、没人用、做完只有自己用。",
                            "需求/习惯/场景三层各一条"),
                    candidate("b", "还没想过，先做起来再说。", "风险未评估，后面会被闸门拦"));
            case 3 -> stageCandidates(profile, 6);
            case 4 -> stageCandidates(profile, 7);
            default -> {
                List<String> p0 = CoachValues.strList(profile, "p0");
                yield List.of(
                        candidate("p", "先跑通「" + first(p0) + " → " + second(p0) + "」这条最短流程。",
                                "P0 只留两个，最快验证"),
                        candidate("a", "把 " + String.join("、", p0) + " 都做出来。",
                                "完整 P0，风险略高"),
                        candidate("b", "先做一个能点的静态原型，不接数据。",
                                "验证交互，不验证功能"));
            }
        };
    }

    private List<CoachCitation> citationsFor(List<Integer> stages) {
        List<CoachCitation> citations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int stage : stages) {
            try {
                for (CoachHit hit : retriever.retrieve(stage, 2).hits()) {
                    if (citations.size() >= 3) {
                        return citations;
                    }
                    if (!seen.add(hit.id())) {
                        continue;
                    }
                    citations.add(new CoachCitation(
                            hit.source(),
                            hit.stepNo(),
                            hit.heading() == null || hit.heading().isBlank() ? hit.title() : hit.heading(),
                            snippet(hit.text())));
                }
            } catch (RuntimeException e) {
                log.debug("第 {} 步检索引用不可用: {}", stage, e.getMessage());
            }
        }
        return citations;
    }

    private static String snippet(String text) {
        String flat = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return flat.length() <= SNIPPET_LENGTH ? flat : flat.substring(0, SNIPPET_LENGTH) + "…";
    }

    private static CoachCandidate candidate(String id, String text, String why) {
        return new CoachCandidate(id, text, why);
    }

    private static String whenClause(Map<String, Object> profile) {
        return CoachValues.str(profile, "situation").replaceAll("的时候$", "");
    }

    private static String first(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }

    private static String second(List<String> values) {
        return values.size() < 2 ? first(values) : values.get(1);
    }
}
