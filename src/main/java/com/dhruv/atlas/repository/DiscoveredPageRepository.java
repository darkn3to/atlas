package com.dhruv.atlas.repository;

import com.dhruv.atlas.entity.DiscoveredPage;
import com.dhruv.atlas.entity.IngestionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscoveredPageRepository extends JpaRepository<DiscoveredPage, Long> {
    public Optional<DiscoveredPage> findBySourceAndUrl(IngestionSource src, String url);
}
