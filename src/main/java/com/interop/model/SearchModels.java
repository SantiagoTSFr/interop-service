package com.interop.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SearchModels {

    public record SearchRequest(
            String query,
            @JsonProperty("top_k") int topK
    ) {}

    public record SearchResult(
            @JsonProperty("case_id") long caseId,
            double score,
            String title,
            String status
    ) {}

    public record SearchResponse(
            String query,
            @JsonProperty("top_k") int topK,
            @JsonProperty("results") List<SearchResult> results
    ) {}
}
