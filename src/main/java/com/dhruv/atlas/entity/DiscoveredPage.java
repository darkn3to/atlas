package com.dhruv.atlas.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", referencedColumnName = "id", nullable = false)
    private IngestionSource source;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "last_modified")
    private OffsetDateTime lastModified;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "last_indexed_at")
    private OffsetDateTime lastIndexedAt;
}
