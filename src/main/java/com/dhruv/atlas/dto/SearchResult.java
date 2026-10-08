package com.dhruv.atlas.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SearchResult {
    private String id, title, content, source, url;
    private Double score;
}
