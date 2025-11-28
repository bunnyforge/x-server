package com.minecraft.k8s.service;

import org.springframework.stereotype.Component;

/**
 * 资源计算器 - 根据最大限制计算初始资源
 */
@Component
public class ResourceCalculator {
    
    /**
     * 计算内存请求值（初始值为限制的 25%）
     */
    public String calculateMemoryRequest(String memoryLimit) {
        return calculatePercentage(memoryLimit, 0.25);
    }
    
    /**
     * 计算 CPU 请求值（初始值为限制的 25%）
     */
    public String calculateCpuRequest(String cpuLimit) {
        return calculatePercentage(cpuLimit, 0.25);
    }
    
    /**
     * 计算 JVM 最大内存（为内存限制的 80%）
     * JVM 使用 M 或 G 单位，不使用 Mi/Gi
     */
    public String calculateMaxMemory(String memoryLimit) {
        if (memoryLimit == null || memoryLimit.isEmpty()) {
            return "1G";
        }
        
        // 解析数值和单位
        String unit = memoryLimit.replaceAll("[0-9.]", "");
        String numStr = memoryLimit.replaceAll("[^0-9.]", "");
        
        try {
            double value = Double.parseDouble(numStr);
            double result = value * 0.8;
            
            // 转换为 JVM 可识别的单位
            if (unit.equalsIgnoreCase("Gi") || unit.equalsIgnoreCase("G")) {
                // 转换为 M（更精确）
                int resultM = (int) Math.ceil(result * 1024);
                return resultM + "M";
            } else if (unit.equalsIgnoreCase("Mi") || unit.equalsIgnoreCase("M")) {
                // 已经是 M，直接取整
                return (int) Math.ceil(result) + "M";
            } else {
                return "1G";
            }
        } catch (NumberFormatException e) {
            return "1G";
        }
    }
    
    /**
     * 根据百分比计算资源值
     * 统一使用 Gi（内存/存储）和整数（CPU 核心）
     */
    private String calculatePercentage(String resource, double percentage) {
        if (resource == null || resource.isEmpty()) {
            return "1Gi";
        }
        
        // 解析数值和单位
        String unit = resource.replaceAll("[0-9.]", "");
        String numStr = resource.replaceAll("[^0-9.]", "");
        
        try {
            double value = Double.parseDouble(numStr);
            double result = value * percentage;
            
            // 内存/存储单位统一用 Gi，取整（至少 1Gi）
            if (unit.equalsIgnoreCase("Gi") || unit.equalsIgnoreCase("G") ||
                unit.equalsIgnoreCase("Mi") || unit.equalsIgnoreCase("M")) {
                int resultGi = (int) Math.max(1, Math.ceil(result));
                return resultGi + "Gi";
            } else {
                // CPU 核心数，取整（至少 1 核）
                int resultCpu = (int) Math.max(1, Math.ceil(result));
                return String.valueOf(resultCpu);
            }
        } catch (NumberFormatException e) {
            return "1Gi";
        }
    }
    
    /**
     * 获取默认配置
     */
    public ServerResourceConfig getDefaultConfig() {
        return new ServerResourceConfig(
            1,                    // replicas
            "longhorn",          // storageClassName
            "PAPER",             // serverType (改用更稳定的 PAPER)
            false,               // onlineMode
            "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"        // jvmOptions (优化的 G1GC 参数)
        );
    }
    
    public record ServerResourceConfig(
        Integer replicas,
        String storageClassName,
        String serverType,
        Boolean onlineMode,
        String jvmOptions
    ) {}
}
