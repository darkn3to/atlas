package com.dhruv.atlas.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "discovered_page")
public class DiscoveredPage {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "last_indexed_at")
    private LocalDateTime lastIndexedAt;
}
