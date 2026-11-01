package com.thinkfirst.coach.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 每一步的固定检索 query。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoachQuery(
        int step,
        String name,
        String query,
        @JsonProperty("top_k") int topK,
        @JsonProperty("must_cover") List<String> mustCover,
        @JsonProperty("prefer_doc_types") List<String> preferDocTypes
) {

    public CoachQuery {
        mustCover = mustCover == null ? List.of() : List.copyOf(mustCover);
        preferDocTypes = preferDocTypes == null ? List.of() : List.copyOf(preferDocTypes);
    }
}
