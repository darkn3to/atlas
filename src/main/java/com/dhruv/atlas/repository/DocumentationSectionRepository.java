package com.dhruv.atlas.repository;

import com.dhruv.atlas.entity.DocumentationSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentationSectionRepository extends JpaRepository<DocumentationSection, Long> {
}
