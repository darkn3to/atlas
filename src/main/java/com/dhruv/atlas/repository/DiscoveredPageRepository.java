package com.dhruv.atlas.repository;

import com.dhruv.atlas.entity.DiscoveredPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscoveredPageRepository extends JpaRepository<DiscoveredPage, Long> {
}
