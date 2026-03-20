package com.interop.controller;

import com.interop.model.SearchModels;
import com.interop.service.SearchIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchIndexService searchIndex;

    public SearchController(SearchIndexService searchIndex) {
        this.searchIndex = searchIndex;
    }

    @PostMapping("/search")
    public ResponseEntity<SearchModels.SearchResponse> search(
            @RequestBody SearchModels.SearchRequest request) {

        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("POST /search query='{}' top_k={} index_size={}", request.query(), request.topK(), searchIndex.size());

        List<SearchModels.SearchResult> results = searchIndex.search(request.query(), request.topK());

        return ResponseEntity.ok(new SearchModels.SearchResponse(request.query(), request.topK(), results));
    }
}
