package com.minecraft.k8s.dto.launcher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务器运行指标 DTO
 * 包含资源使用率和游戏状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "服务器运行指标")
public class ServerMetricsDto {

    @Schema(description = "CPU 使用率 (%)", example = "45.5")
    private Double cpuUsagePercent;

    @Schema(description = "内存使用率 (%)", example = "68.2")
    private Double memoryUsagePercent;

    @Schema(description = "当前在线玩家数", example = "5")
    private Integer onlinePlayers;

    @Schema(description = "最大玩家数", example = "20")
    private Integer maxPlayers;
}
