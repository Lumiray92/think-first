package com.thinkfirst.coach.rag;

import java.util.List;
import java.util.Map;

/** 内存中的完整语料：块、块向量、每步 query 与 query 向量。 */
public record CoachCorpus(
        List<CoachChunk> chunks,
        float[][] vectors,
        Map<Integer, CoachQuery> queries,
        Map<Integer, float[]> queryVectors,
        int dimension
) {

    public boolean isEmpty() {
        return chunks == null || chunks.isEmpty();
    }

    public CoachQuery queryFor(int step) {
        return queries == null ? null : queries.get(step);
    }

    public float[] queryVectorFor(int step) {
        return queryVectors == null ? null : queryVectors.get(step);
    }
}
