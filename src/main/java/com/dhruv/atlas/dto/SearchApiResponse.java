package com.dhruv.atlas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SearchApiResponse {
    private String query;
    private List<SearchResult> results;
}
