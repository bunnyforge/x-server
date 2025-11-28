package com.minecraft.k8s.domain.model;

import com.minecraft.k8s.domain.valueobject.K8sConfig;
import com.minecraft.k8s.domain.valueobject.MinecraftConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 充血模型 - Minecraft 服务器聚合根
 */
@Data
@Schema(description = "Minecraft 服务器")
public class MinecraftServer {
    @Schema(description = "命名空间", example = "minecraft31003")
    private String namespace;

    @Schema(description = "服务器名称", example = "my-minecraft-server")
    private String name;

    @Schema(description = "NodePort 端口", example = "31003")
    private int nodePort;

    @Schema(description = "集群 ID", example = "1")
    private Long clusterId;

    @Schema(description = "集群名称", example = "production-cluster")
    private String clusterName;

    @Schema(description = "服务器状态", example = "RUNNING")
    private String status;

    @Schema(description = "Kubernetes 资源配置")
    private K8sConfig k8sConfig;

    @Schema(description = "Minecraft 服务器配置")
    private MinecraftConfig minecraftConfig;

    // 业务方法
    public void validate() {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("Namespace is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (nodePort < 30000 || nodePort > 32767) {
            throw new IllegalArgumentException("NodePort must be between 30000 and 32767");
        }
        if (k8sConfig == null) {
            throw new IllegalArgumentException("K8s config is required");
        }
        if (minecraftConfig == null) {
            throw new IllegalArgumentException("Minecraft config is required");
        }
        if (minecraftConfig.getMaxPlayers() < 1 || minecraftConfig.getMaxPlayers() > 10000) {
            throw new IllegalArgumentException("Max players must be between 1 and 10000");
        }
    }

    public String getFullName() {
        return namespace + "/" + name;
    }
}
