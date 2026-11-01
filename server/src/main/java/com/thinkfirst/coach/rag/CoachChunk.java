package com.thinkfirst.coach.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 语料块，字段与 chunks.jsonl 对齐。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoachChunk(
        String id,
        String source,
        String title,
        String chapter,
        @JsonProperty("doc_type") String docType,
        @JsonProperty("step_no") int stepNo,
        boolean reserved,
        String heading,
        @JsonProperty("heading_path") List<String> headingPath,
        @JsonProperty("char_count") int charCount,
        String text
) {

    public CoachChunk {
        headingPath = headingPath == null ? List.of() : List.copyOf(headingPath);
    }
}
