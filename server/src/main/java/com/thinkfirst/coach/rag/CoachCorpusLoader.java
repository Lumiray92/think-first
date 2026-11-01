package com.thinkfirst.coach.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 启动时把 course07 语料、块向量、每步 query 与 query 向量全部读进内存。
 * 语料缺失只记日志，不阻断服务启动；检索接口再返回 503。
 */
@Component
public class CoachCorpusLoader implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(CoachCorpusLoader.class);

    private final CoachProperties properties;
    private final ObjectMapper mapper;
    private volatile CoachCorpus corpus;

    public CoachCorpusLoader(CoachProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.isEnabled()) {
            log.info("ThinkFirst coach 语料加载已关闭");
            return;
        }
        try {
            corpus = load(Path.of(properties.getDataDir()));
        } catch (Exception e) {
            log.error("ThinkFirst coach 语料加载失败: {}", properties.getDataDir(), e);
        }
    }

    public CoachCorpus getCorpus() {
        return corpus;
    }

    public CoachCorpus load(Path dir) throws IOException {
        List<CoachChunk> chunks = readChunks(dir.resolve("chunks.jsonl"));
        if (chunks.isEmpty()) {
            throw new IOException("语料为空: " + dir);
        }

        JsonNode embeddingIndex = mapper.readTree(dir.resolve("embeddings.index.json").toFile());
        int dimension = embeddingIndex.path("dimension").asInt();
        int count = embeddingIndex.path("count").asInt();
        List<String> ids = new ArrayList<>();
        embeddingIndex.path("ids").forEach(node -> ids.add(node.asText()));
        if (ids.size() != count) {
            throw new IOException("embeddings.index.json ids 数量与 count 不一致");
        }
        if (dimension <= 0 || count <= 0) {
            throw new IOException("embeddings.index.json 维度或数量非法");
        }

        float[][] rawVectors = readVectors(dir.resolve("embeddings.f32"), count, dimension);
        Map<String, Integer> vectorIndexById = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            vectorIndexById.put(ids.get(i), i);
        }

        float[][] vectors = new float[chunks.size()][];
        for (int i = 0; i < chunks.size(); i++) {
            CoachChunk chunk = chunks.get(i);
            Integer vectorIndex = vectorIndexById.get(chunk.id());
            if (vectorIndex == null) {
                throw new IOException("块缺少向量: " + chunk.id());
            }
            vectors[i] = rawVectors[vectorIndex];
        }

        Map<Integer, CoachQuery> queries = readQueries(dir.resolve("query-map.json"));
        Map<Integer, float[]> queryVectors = readQueryVectors(dir, dimension);

        log.info("ThinkFirst coach 语料已加载: {} 块，{} 维，{} 条 query", chunks.size(), dimension, queries.size());
        return new CoachCorpus(List.copyOf(chunks), vectors, queries, queryVectors, dimension);
    }

    private List<CoachChunk> readChunks(Path file) throws IOException {
        List<CoachChunk> chunks = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                chunks.add(mapper.readValue(line, CoachChunk.class));
            }
        }
        return chunks;
    }

    private Map<Integer, CoachQuery> readQueries(Path file) throws IOException {
        JsonNode root = mapper.readTree(file.toFile());
        Map<Integer, CoachQuery> queries = new TreeMap<>();
        for (JsonNode node : root.path("queries")) {
            CoachQuery query = mapper.treeToValue(node, CoachQuery.class);
            queries.put(query.step(), query);
        }
        if (queries.isEmpty()) {
            throw new IOException("query-map.json 没有 queries");
        }
        return queries;
    }

    private Map<Integer, float[]> readQueryVectors(Path dir, int dimension) throws IOException {
        JsonNode index = mapper.readTree(dir.resolve("query-vectors.index.json").toFile());
        int queryDimension = index.path("dimension").asInt();
        int count = index.path("count").asInt();
        if (queryDimension != dimension) {
            throw new IOException("query 向量维度与块向量不一致: " + queryDimension + " != " + dimension);
        }
        float[][] vectors = readVectors(dir.resolve("query-vectors.f32"), count, queryDimension);
        JsonNode steps = index.path("steps");
        if (steps.size() != count) {
            throw new IOException("query-vectors.index.json steps 数量与 count 不一致");
        }
        Map<Integer, float[]> byStep = new HashMap<>();
        for (int i = 0; i < count; i++) {
            byStep.put(steps.get(i).asInt(), vectors[i]);
        }
        return byStep;
    }

    /** float32 小端连续存储，按 count × dimension 读取。 */
    private float[][] readVectors(Path file, int count, int dimension) throws IOException {
        long expected = (long) count * dimension * Float.BYTES;
        long actual = Files.size(file);
        if (actual != expected) {
            throw new IOException("向量文件大小不符: " + file + " 期望 " + expected + " 实际 " + actual);
        }
        byte[] bytes = Files.readAllBytes(file);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[][] vectors = new float[count][dimension];
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < dimension; j++) {
                vectors[i][j] = buffer.getFloat();
            }
        }
        return vectors;
    }
}
