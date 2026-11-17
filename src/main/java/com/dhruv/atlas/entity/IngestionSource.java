package com.dhruv.atlas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ingestion_source")
public class IngestionSource {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
