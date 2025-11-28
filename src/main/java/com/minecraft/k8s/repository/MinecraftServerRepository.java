package com.minecraft.k8s.repository;

import com.minecraft.k8s.domain.entity.MinecraftServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MinecraftServerRepository extends JpaRepository<MinecraftServerEntity, Long> {
    
    Optional<MinecraftServerEntity> findByName(String name);
    
    Optional<MinecraftServerEntity> findByNamespace(String namespace);
    
    Optional<MinecraftServerEntity> findByNamespaceAndName(String namespace, String name);
    
    boolean existsByNodePort(Integer nodePort);
    
    @Query("SELECT MAX(e.nodePort) FROM MinecraftServerEntity e")
    Optional<Integer> findMaxNodePort();
}
