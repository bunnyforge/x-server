package com.minecraft.k8s.repository;

import com.minecraft.k8s.domain.entity.ClusterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClusterRepository extends JpaRepository<ClusterEntity, Long> {
    Optional<ClusterEntity> findByName(String name);
}
