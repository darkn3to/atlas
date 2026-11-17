package com.dhruv.atlas.repository;

import com.dhruv.atlas.entity.IngestionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionSourceRepository extends JpaRepository<IngestionSource, Long> {
}
