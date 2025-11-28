package com.minecraft.k8s.dto.launcher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启动器专用的服务器 DTO
 * 只包含启动器需要的信息,过滤掉内部配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "启动器服务器信息")
public class LauncherServerDto {

    @Schema(description = "服务器名称", example = "my-minecraft-server")
    private String name;

    @Schema(description = "NodePort 端口", example = "31003")
    private Integer nodePort;

    @Schema(description = "服务器状态", example = "RUNNING")
    private String status;

    @Schema(description = "服务器类型", example = "PAPER")
    private String serverType;

    @Schema(description = "最大玩家数", example = "20")
    private Integer maxPlayers;

    @Schema(description = "是否开启正版验证", example = "false")
    private Boolean onlineMode;

    @Schema(description = "Minecraft 版本", example = "1.21.1")
    private String version;

    @Schema(description = "Modrinth 模组列表")
    private String modrinthProjects;

    @Schema(description = "Modrinth 整合包 URL")
    private String modrinthModpack;

    @Schema(description = "服务器运行指标")
    private ServerMetricsDto metrics;
}
