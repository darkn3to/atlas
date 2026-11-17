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
@Table(name = "documentation_section")
public class DocumentationSection {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "heading", nullable = false)
    private String heading;

    @Column(name = "section_url")
    private String sectionUrl;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}