package com.thinkfirst.coach.rag;

import java.util.List;

/** must_cover 关键词覆盖情况，用于检索质量评测。 */
public record CoachCoverage(
        List<String> matched,
        List<String> missing,
        double rate
) {
}
