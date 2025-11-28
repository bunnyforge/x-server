package com.minecraft.k8s.domain.valueobject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Minecraft 服务器配置
 */
@Data
@Schema(description = "Minecraft 服务器配置")
public class MinecraftConfig {
    @Schema(description = "服务器类型", example = "FOLIA")
    private String serverType = "FOLIA";
    
    @Schema(description = "是否开启正版验证", example = "false")
    private Boolean onlineMode = false;
    
    @Schema(description = "最大玩家数", example = "20")
    private Integer maxPlayers = 20;
    
    @Schema(description = "JVM 最大内存", example = "1639M")
    private String maxMemory = "1639M";
    
    @Schema(description = "JVM 启动参数", example = "-XX:+UseZGC")
    private String jvmOptions = "-XX:+UseZGC";
    
    @Schema(description = "Minecraft 版本", example = "latest")
    private String version = "latest";
    
    @Schema(description = "游戏难度", example = "normal")
    private String difficulty = "normal";
    
    @Schema(description = "是否开启 PVP", example = "true")
    private Boolean pvp = true;
    
    @Schema(description = "视距", example = "10")
    private Integer viewDistance = 10;
    
    // Modrinth 模组列表（普通服务器 + Mods 模式）
    @Schema(description = "Modrinth 模组列表，每行一个模组名称", example = "viaversion\nviabackwards\ngriefprevention")
    private String modrinthProjects;
    
    // Modrinth 整合包配置（整合包模式）
    @Schema(description = "Modrinth 整合包 URL", example = "https://modrinth.com/modpack/cobblemon-fabric/version/1.6.1.4")
    private String modrinthModpack;
}
