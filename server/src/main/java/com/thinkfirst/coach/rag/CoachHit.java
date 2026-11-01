package com.thinkfirst.coach.rag;

import java.util.List;

/** 一条检索结果，字段与前端 citations 契约一致。 */
public record CoachHit(
        String id,
        double score,
        double vectorScore,
        double lexicalScore,
        int vectorRank,
        Integer lexicalRank,
        String title,
        String heading,
        List<String> headingPath,
        String source,
        int stepNo,
        String docType,
        String text
) {
}
