package com.minecraft.k8s.domain.valueobject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * K8s 资源配置
 */
@Data
@Schema(description = "Kubernetes 资源配置")
public class K8sConfig {
    @Schema(description = "内存限制", example = "2Gi")
    private String memoryLimit = "2Gi";
    
    @Schema(description = "内存请求", example = "512Mi")
    private String memoryRequest = "512Mi";
    
    @Schema(description = "CPU 限制", example = "2")
    private String cpuLimit = "2";
    
    @Schema(description = "CPU 请求", example = "0.5")
    private String cpuRequest = "0.5";
    
    @Schema(description = "存储大小", example = "2Gi")
    private String storageSize = "2Gi";
    
    @Schema(description = "存储类名", example = "longhorn")
    private String storageClassName = "longhorn";
    
    @Schema(description = "副本数", example = "1")
    private Integer replicas = 1;
}
