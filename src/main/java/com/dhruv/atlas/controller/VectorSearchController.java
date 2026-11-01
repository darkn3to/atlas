package com.dhruv.atlas.controller;

import com.dhruv.atlas.dto.SearchApiResponse;
import com.dhruv.atlas.search.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class VectorSearchController {
    private final SearchService searchService;

    public VectorSearchController(SearchService searchService) {
        this.searchService=searchService;
    }

    @GetMapping("/vector")
    public ResponseEntity<SearchApiResponse> search(@RequestParam String query) {
        SearchApiResponse response = searchService.vectorSearch(query);
        return ResponseEntity.ok(response);
    }
}
