package com.dhruv.atlas.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SearchResult {
    private String chunkId, sectionId, pageTitle, content, source, url, sectionUrl;
    private List<String> sectionPath;
    private Double score;
}
