package com.thinkfirst.coach.rag;

import java.util.List;

/** 一次检索的完整返回：query、top-k 引用与关键词覆盖。 */
public record CoachRetrievalResult(
        String query,
        int step,
        int topK,
        List<CoachHit> hits,
        CoachCoverage coverage
) {
}
