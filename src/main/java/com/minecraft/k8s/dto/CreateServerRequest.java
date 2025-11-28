package com.minecraft.k8s.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "创建 Minecraft 服务器请求")
public class CreateServerRequest {
    @NotBlank(message = "Name is required")
    @Schema(description = "服务器名称（唯一标识）", example = "my-minecraft-server", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "Cluster ID is required")
    @Schema(description = "集群 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clusterId;

    @NotNull(message = "K8s config is required")
    @Schema(description = "Kubernetes 资源配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private CreateK8sConfigDTO k8sConfig;

    @NotNull(message = "Minecraft config is required")
    @Schema(description = "Minecraft 服务器配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private CreateMinecraftConfigDTO minecraftConfig;

    @Data
    @Schema(description = "Kubernetes 资源配置")
    public static class CreateK8sConfigDTO {
        @NotNull(message = "Memory limit is required")
        @Min(1)
        @Schema(description = "内存限制（单位：G）", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer memoryLimit = 2;

        @NotNull(message = "CPU limit is required")
        @Min(1)
        @Schema(description = "CPU 限制（单位：核心）", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer cpuLimit = 2;

        @NotNull(message = "Storage size is required")
        @Min(1)
        @Schema(description = "存储大小（单位：G）", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer storageSize = 2;
    }

    @Data
    @Schema(description = "Minecraft 服务器配置")
    public static class CreateMinecraftConfigDTO {
        @Schema(description = "服务器类型", example = "PAPER", allowableValues = { "VANILLA", "PAPER", "FOLIA", "PURPUR" })
        private String serverType = "PAPER";

        @Schema(description = "是否开启正版验证", example = "false")
        private Boolean onlineMode = false;

        @Min(1)
        @Max(10000)
        @Schema(description = "最大玩家数", example = "20", minimum = "1", maximum = "10000")
        private Integer maxPlayers = 20;

        @Schema(description = "JVM 启动参数", example = "-XX:+UseG1GC")
        private String jvmOptions = "-XX:+UseG1GC";

        @Schema(description = "Minecraft 版本", example = "latest")
        private String version = "latest";

        @Schema(description = "游戏难度", example = "normal", allowableValues = { "peaceful", "easy", "normal", "hard" })
        private String difficulty = "normal";

        @Schema(description = "是否开启 PVP", example = "true")
        private Boolean pvp = true;

        @Schema(description = "视距", example = "10")
        private Integer viewDistance = 10;
        
        // Modrinth 模组列表（普通服务器 + Mods 模式）
        @Schema(description = "Modrinth 模组列表，每行一个模组名称", example = "viaversion\nviabackwards\ngriefprevention")
        private String modrinthProjects;
        
        // Modrinth 整合包配置（整合包模式，如果设置，TYPE 将自动变为 MODRINTH）
        @Schema(description = "Modrinth 整合包 URL", example = "https://modrinth.com/modpack/cobblemon-fabric/version/1.6.1.4")
        private String modrinthModpack;
    }
}
