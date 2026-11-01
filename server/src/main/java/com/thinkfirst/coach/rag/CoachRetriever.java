package com.thinkfirst.coach.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地 hybrid 检索：中文字符二元组 BM25 + 向量 cosine，
 * 两路各自排名后用 RRF 融合。语料只有 1005 块，全量放内存即可。
 */
@Component
public class CoachRetriever {

    private static final Logger log = LoggerFactory.getLogger(CoachRetriever.class);

    private static final int RRF_K = 60;
    private static final int MAX_TOP_K = 8;
    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;

    private static final Pattern ASCII_TOKEN = Pattern.compile("[a-z0-9_.+]+");
    private static final Pattern NON_CHINESE = Pattern.compile("[^\\u4e00-\\u9fa5]+");
    private static final Pattern DOTTED_ACRONYM = Pattern.compile("\\b(?:[a-z]\\.){2,}[a-z]?\\.?");
    private static final Pattern NORMALIZE = Pattern.compile("[\\s，。；、：“”‘’（）()【】\\[\\]{}<>《》!?,.;:'\"·…—\\-_/]");

    private final CoachCorpusLoader loader;
    private volatile Index index;

    public CoachRetriever(CoachCorpusLoader loader) {
        this.loader = loader;
    }

    @PostConstruct
    void init() {
        rebuild(loader.getCorpus());
    }

    public void rebuild(CoachCorpus corpus) {
        if (corpus == null || corpus.isEmpty()) {
            this.index = null;
            log.warn("ThinkFirst coach 语料未就绪，检索接口将返回 503");
            return;
        }
        this.index = Index.build(corpus);
        log.info("ThinkFirst coach 检索索引已构建: {} 块，{} 维",
                corpus.chunks().size(), corpus.dimension());
    }

    public CoachRetrievalResult retrieve(int step, Integer requestedTopK) {
        Index current = index;
        if (current == null || current.corpus.isEmpty()) {
            throw new CoachUnavailableException("教练语料尚未加载");
        }
        CoachQuery query = current.corpus.queryFor(step);
        if (query == null) {
            throw new IllegalArgumentException("不支持的步骤: " + step);
        }
        float[] queryVector = current.corpus.queryVectorFor(step);
        if (queryVector == null) {
            throw new CoachUnavailableException("第 " + step + " 步缺少固定 query 向量");
        }
        int defaultTopK = query.topK() > 0 ? query.topK() : 6;
        int topK = requestedTopK == null ? defaultTopK : requestedTopK;
        topK = Math.max(1, Math.min(topK, MAX_TOP_K));
        return retrieve(query, queryVector, topK, current);
    }

    private CoachRetrievalResult retrieve(CoachQuery query, float[] queryVector, int topK, Index current) {
        List<Integer> candidates = candidatesFor(current, query.step());
        Set<String> queryTokens = new HashSet<>(tokenize(query.query()));

        double[] vectorScores = new double[current.chunks.size()];
        for (int docIndex : candidates) {
            vectorScores[docIndex] = cosine(queryVector, current.vectors[docIndex], current.vectorNorms[docIndex]);
        }
        Integer[] vectorOrder = orderByScore(candidates, vectorScores);

        double[] lexicalScores = new double[current.chunks.size()];
        for (int docIndex : candidates) {
            lexicalScores[docIndex] = bm25(queryTokens, current, docIndex);
        }
        Integer[] lexicalOrder = orderByScore(candidates, lexicalScores);

        Map<Integer, Fused> fused = new LinkedHashMap<>();
        for (int rank = 0; rank < vectorOrder.length; rank++) {
            int docIndex = vectorOrder[rank];
            fused.put(docIndex, new Fused(
                    docIndex, vectorScores[docIndex], 0d, rank + 1, null, 1d / (RRF_K + rank + 1)));
        }
        for (int rank = 0; rank < lexicalOrder.length; rank++) {
            int docIndex = lexicalOrder[rank];
            Fused entry = fused.get(docIndex);
            entry.lexicalScore = lexicalScores[docIndex];
            entry.lexicalRank = rank + 1;
            entry.score += 1d / (RRF_K + rank + 1);
        }

        List<Fused> ranked = new ArrayList<>(fused.values());
        ranked.sort(Comparator.comparingDouble((Fused entry) -> entry.score).reversed());
        List<Fused> selected = new ArrayList<>(ranked.subList(0, Math.min(topK, ranked.size())));

        // 需要模板的步骤至少保留一条模板，避免解释性正文把可直接套用的模板挤出 top-k
        if (query.preferDocTypes().contains("template")
                && selected.stream().noneMatch(entry -> isTemplate(current, entry.docIndex))) {
            ranked.stream()
                    .filter(entry -> isTemplate(current, entry.docIndex))
                    .findFirst()
                    .ifPresent(template -> {
                        if (!selected.isEmpty()) {
                            selected.set(selected.size() - 1, template);
                        }
                    });
        }

        List<CoachHit> hits = selected.stream().map(entry -> toHit(current, entry)).toList();
        return new CoachRetrievalResult(
                query.query(), query.step(), topK, hits, coverage(hits, query.mustCover()));
    }

    private List<Integer> candidatesFor(Index current, int step) {
        List<Integer> exact = new ArrayList<>();
        List<Integer> fallback = new ArrayList<>();
        for (int i = 0; i < current.chunks.size(); i++) {
            CoachChunk chunk = current.chunks.get(i);
            if (chunk.stepNo() > 12) {
                continue;
            }
            fallback.add(i);
            if ((chunk.stepNo() == step || chunk.stepNo() == 0) && !chunk.reserved()) {
                exact.add(i);
            }
        }
        return exact.size() >= 3 ? exact : fallback;
    }

    private Integer[] orderByScore(List<Integer> candidates, double[] scores) {
        Integer[] order = candidates.toArray(new Integer[0]);
        Arrays.sort(order, (left, right) -> {
            int compared = Double.compare(scores[right], scores[left]);
            return compared != 0 ? compared : Integer.compare(left, right);
        });
        return order;
    }

    private double bm25(Set<String> queryTokens, Index current, int docIndex) {
        Map<String, Integer> termFreq = current.termFreqs.get(docIndex);
        if (termFreq.isEmpty() || current.avgLength == 0) {
            return 0;
        }
        double score = 0;
        for (String term : queryTokens) {
            Integer tf = termFreq.get(term);
            if (tf == null || tf == 0) {
                continue;
            }
            int docFreq = current.documentFreq.getOrDefault(term, 0);
            double idf = Math.log(1 + (current.chunks.size() - docFreq + 0.5) / (docFreq + 0.5));
            double denominator = tf + BM25_K1 * (1 - BM25_B + BM25_B * current.lengths[docIndex] / current.avgLength);
            score += idf * (tf * (BM25_K1 + 1)) / denominator;
        }
        return score;
    }

    private CoachHit toHit(Index current, Fused entry) {
        CoachChunk chunk = current.chunks.get(entry.docIndex);
        return new CoachHit(
                chunk.id(),
                round(entry.score, 5),
                round(entry.vectorScore, 4),
                round(entry.lexicalScore, 3),
                entry.vectorRank,
                entry.lexicalRank,
                chunk.title(),
                chunk.heading(),
                chunk.headingPath(),
                chunk.source(),
                chunk.stepNo(),
                chunk.docType(),
                chunk.text());
    }

    private CoachCoverage coverage(List<CoachHit> hits, List<String> mustCover) {
        if (mustCover.isEmpty()) {
            return new CoachCoverage(List.of(), List.of(), 1d);
        }
        StringBuilder haystack = new StringBuilder();
        for (CoachHit hit : hits) {
            haystack.append(hit.title()).append(' ')
                    .append(String.join(" ", hit.headingPath())).append(' ')
                    .append(hit.text()).append('\n');
        }
        String normalizedHaystack = normalize(haystack.toString());
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String term : mustCover) {
            if (normalizedHaystack.contains(normalize(term))) {
                matched.add(term);
            } else {
                missing.add(term);
            }
        }
        double rate = (double) matched.size() / mustCover.size();
        return new CoachCoverage(List.copyOf(matched), List.copyOf(missing), round(rate, 4));
    }

    private boolean isTemplate(Index current, int docIndex) {
        return "template".equals(current.chunks.get(docIndex).docType());
    }

    static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        lower = DOTTED_ACRONYM.matcher(lower).replaceAll(match -> match.group().replace(".", ""));

        List<String> tokens = new ArrayList<>();
        Matcher ascii = ASCII_TOKEN.matcher(lower);
        while (ascii.find()) {
            tokens.add(ascii.group());
        }
        String chinese = NON_CHINESE.matcher(lower).replaceAll(" ");
        for (String segment : chinese.split("\\s+")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (segment.length() == 1) {
                tokens.add(segment);
            }
            for (int i = 0; i < segment.length() - 1; i++) {
                tokens.add(segment.substring(i, i + 2));
            }
        }
        return tokens;
    }

    private static double cosine(float[] left, float[] right, double rightNorm) {
        double dot = 0;
        double leftNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * rightNorm);
    }

    private static String normalize(String text) {
        return NORMALIZE.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    private static final class Fused {
        private final int docIndex;
        private final double vectorScore;
        private double lexicalScore;
        private final int vectorRank;
        private Integer lexicalRank;
        private double score;

        private Fused(int docIndex, double vectorScore, double lexicalScore,
                      int vectorRank, Integer lexicalRank, double score) {
            this.docIndex = docIndex;
            this.vectorScore = vectorScore;
            this.lexicalScore = lexicalScore;
            this.vectorRank = vectorRank;
            this.lexicalRank = lexicalRank;
            this.score = score;
        }
    }

    private static final class Index {
        private final CoachCorpus corpus;
        private final List<CoachChunk> chunks;
        private final float[][] vectors;
        private final double[] vectorNorms;
        private final List<Map<String, Integer>> termFreqs;
        private final int[] lengths;
        private final Map<String, Integer> documentFreq;
        private final double avgLength;

        private Index(CoachCorpus corpus, float[][] vectors, List<Map<String, Integer>> termFreqs,
                      int[] lengths, Map<String, Integer> documentFreq, double avgLength) {
            this.corpus = corpus;
            this.chunks = corpus.chunks();
            this.vectors = vectors;
            this.vectorNorms = new double[vectors.length];
            for (int i = 0; i < vectors.length; i++) {
                double norm = 0;
                for (float value : vectors[i]) {
                    norm += value * value;
                }
                vectorNorms[i] = Math.sqrt(norm);
            }
            this.termFreqs = termFreqs;
            this.lengths = lengths;
            this.documentFreq = documentFreq;
            this.avgLength = avgLength;
        }

        private static Index build(CoachCorpus corpus) {
            int size = corpus.chunks().size();
            List<Map<String, Integer>> termFreqs = new ArrayList<>(size);
            int[] lengths = new int[size];
            Map<String, Integer> documentFreq = new HashMap<>();
            long totalLength = 0;

            for (int i = 0; i < size; i++) {
                CoachChunk chunk = corpus.chunks().get(i);
                String haystack = chunk.title() + " " + String.join(" ", chunk.headingPath()) + " " + chunk.text();
                List<String> tokens = tokenize(haystack);
                Map<String, Integer> termFreq = new HashMap<>();
                for (String token : tokens) {
                    termFreq.merge(token, 1, Integer::sum);
                }
                termFreqs.add(termFreq);
                lengths[i] = tokens.size();
                totalLength += tokens.size();
                for (String term : termFreq.keySet()) {
                    documentFreq.merge(term, 1, Integer::sum);
                }
            }
            double avgLength = size == 0 ? 0 : (double) totalLength / size;
            return new Index(corpus, corpus.vectors(), termFreqs, lengths, documentFreq, avgLength);
        }
    }
}
