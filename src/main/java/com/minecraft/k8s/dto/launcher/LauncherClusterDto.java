package com.minecraft.k8s.dto.launcher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 启动器专用的大区 DTO
 * 只包含启动器需要的信息,过滤掉敏感配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "启动器大区信息")
public class LauncherClusterDto {

    @Schema(description = "大区 ID", example = "1")
    private Long id;

    @Schema(description = "大区名称", example = "华东一区")
    private String name;

    @Schema(description = "大区域名", example = "east1.example.com")
    private String domain;

    @Schema(description = "服务器列表")
    private List<LauncherServerDto> servers;
}
