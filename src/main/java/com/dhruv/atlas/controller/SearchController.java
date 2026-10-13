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
class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService=searchService;
    }
    @GetMapping("/search")
    public ResponseEntity<SearchApiResponse> search(@RequestParam String query) {
        SearchApiResponse response = searchService.search(query);
        return ResponseEntity.ok(response);
    }
}
