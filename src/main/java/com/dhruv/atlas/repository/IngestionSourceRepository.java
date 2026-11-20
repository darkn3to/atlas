package com.dhruv.atlas.repository;

import com.dhruv.atlas.entity.IngestionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngestionSourceRepository extends JpaRepository<IngestionSource, Long> {
    List<IngestionSource> findAllByOrderByIdAsc();
}
