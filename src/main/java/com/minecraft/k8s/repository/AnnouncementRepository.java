package com.minecraft.k8s.repository;

import com.minecraft.k8s.domain.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {
    List<AnnouncementEntity> findByActiveOrderByDisplayOrderAsc(Boolean active);
}
