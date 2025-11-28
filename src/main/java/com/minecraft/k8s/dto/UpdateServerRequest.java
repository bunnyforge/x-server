package com.minecraft.k8s.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "更新 Minecraft 服务器请求")
public class UpdateServerRequest {
    @Schema(description = "Kubernetes 资源配置（可选更新）")
    private UpdateK8sConfigDTO k8sConfig;
    
    @Schema(description = "Minecraft 服务器配置（可选更新）")
    private UpdateMinecraftConfigDTO minecraftConfig;
    
    @Data
    @Schema(description = "Kubernetes 资源配置")
    public static class UpdateK8sConfigDTO {
        @Min(1)
        @Schema(description = "内存限制（单位：G）", example = "2")
        private Integer memoryLimit;
        
        @Min(1)
        @Schema(description = "CPU 限制（单位：核心）", example = "2")
        private Integer cpuLimit;
        
        @Min(1)
        @Schema(description = "存储大小（单位：G）", example = "2")
        private Integer storageSize;
    }
    
    @Data
    @Schema(description = "Minecraft 服务器配置")
    public static class UpdateMinecraftConfigDTO {
        @Min(1) @Max(10000)
        @Schema(description = "最大玩家数", example = "50", minimum = "1", maximum = "10000")
        private Integer maxPlayers;
        
        @Schema(description = "服务器类型", example = "PAPER", allowableValues = {"VANILLA", "PAPER", "FOLIA", "PURPUR"})
        private String serverType;
        
        @Schema(description = "是否开启正版验证", example = "true")
        private Boolean onlineMode;
        
        @Schema(description = "JVM 启动参数", example = "-XX:+UseG1GC")
        private String jvmOptions;
        
        @Schema(description = "Minecraft 版本", example = "1.20.4")
        private String version;
        
        @Schema(description = "游戏难度", example = "hard", allowableValues = {"peaceful", "easy", "normal", "hard"})
        private String difficulty;
        
        @Schema(description = "是否开启 PVP", example = "false")
        private Boolean pvp;
        
        @Schema(description = "视距", example = "12")
        private Integer viewDistance;
        
        // Modrinth 模组列表（普通服务器 + Mods 模式）
        @Schema(description = "Modrinth 模组列表，每行一个模组名称", example = "viaversion\nviabackwards\ngriefprevention")
        private String modrinthProjects;
        
        // Modrinth 整合包配置（整合包模式，如果设置，TYPE 将自动变为 MODRINTH）
        @Schema(description = "Modrinth 整合包 URL", example = "https://modrinth.com/modpack/cobblemon-fabric/version/1.6.1.4")
        private String modrinthModpack;
    }
}
