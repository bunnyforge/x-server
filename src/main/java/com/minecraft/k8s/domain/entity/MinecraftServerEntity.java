package com.minecraft.k8s.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.k8s.domain.valueobject.K8sConfig;
import com.minecraft.k8s.domain.valueobject.MinecraftConfig;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Minecraft 服务器实体（数据库）
 */
@Data
@Entity
@Table(name = "minecraft_server")
public class MinecraftServerEntity {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String namespace;

    @Column(nullable = false, unique = true)
    private Integer nodePort;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String k8sConfig;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String minecraftConfig;

    @Column(nullable = false)
    private String status = "CREATING";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // JSON 转换辅助方法
    public K8sConfig getK8sConfigObject() {
        try {
            return objectMapper.readValue(k8sConfig, K8sConfig.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse k8sConfig", e);
        }
    }

    public void setK8sConfigObject(K8sConfig config) {
        try {
            this.k8sConfig = objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize k8sConfig", e);
        }
    }

    public MinecraftConfig getMinecraftConfigObject() {
        try {
            return objectMapper.readValue(minecraftConfig, MinecraftConfig.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse minecraftConfig", e);
        }
    }

    public void setMinecraftConfigObject(MinecraftConfig config) {
        try {
            this.minecraftConfig = objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize minecraftConfig", e);
        }
    }
}
