package com.minecraft.k8s.controller;

import com.minecraft.k8s.domain.entity.ClusterEntity;
import com.minecraft.k8s.domain.model.MinecraftServer;
import com.minecraft.k8s.dto.AnnouncementDto;
import com.minecraft.k8s.dto.launcher.LauncherClusterDto;
import com.minecraft.k8s.dto.launcher.LauncherServerDto;
import com.minecraft.k8s.dto.launcher.ServerMetricsDto;
import com.minecraft.k8s.service.AnnouncementService;
import com.minecraft.k8s.service.ClusterService;
import com.minecraft.k8s.service.K8sMetricsService;
import com.minecraft.k8s.service.MinecraftQueryService;
import com.minecraft.k8s.service.MinecraftServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/launcher")
@RequiredArgsConstructor
@Tag(name = "Launcher", description = "Minecraft 启动器支持 API")
public class LauncherController {

        private final ClusterService clusterService;
        private final MinecraftServerService serverService;
        private final K8sMetricsService metricsService;
        private final MinecraftQueryService queryService;
        private final AnnouncementService announcementService;
        private final com.minecraft.k8s.config.K8sClientFactory k8sClientFactory;

        @GetMapping("/data")
        @Operation(summary = "获取启动器数据", description = "获取所有区服及其下的游戏服务器列表")
        public ResponseEntity<List<LauncherClusterDto>> getLauncherData() {
                // 1. 获取所有集群
                List<ClusterEntity> clusters = clusterService.getAllClusters();

                // 2. 获取所有服务器
                List<MinecraftServer> servers = serverService.listServers();

                // 3. 按集群ID分组服务器
                Map<Long, List<MinecraftServer>> serversByCluster = servers.stream()
                                .collect(Collectors.groupingBy(MinecraftServer::getClusterId));

                // 4. 组装结果 - 转换为启动器专用 DTO
                List<LauncherClusterDto> result = clusters.stream()
                                .map(cluster -> LauncherClusterDto.builder()
                                                .id(cluster.getId())
                                                .name(cluster.getName())
                                                .domain(cluster.getDomain())
                                                .servers(convertToLauncherServerDtos(
                                                                cluster,
                                                                serversByCluster.getOrDefault(cluster.getId(),
                                                                                Collections.emptyList())))
                                                .build())
                                .collect(Collectors.toList());

                return ResponseEntity.ok(result);
        }

        @GetMapping("/announcements")
        @Operation(summary = "获取启动器公告", description = "获取所有启用的公告信息")
        public ResponseEntity<List<AnnouncementDto>> getAnnouncements() {
                return ResponseEntity.ok(announcementService.getActiveAnnouncements());
        }

        /**
         * 将 MinecraftServer 转换为 LauncherServerDto
         * 只提取启动器需要的信息,并使用虚拟线程并发获取实时指标
         */
        private List<LauncherServerDto> convertToLauncherServerDtos(ClusterEntity cluster,
                        List<MinecraftServer> servers) {
                // 使用虚拟线程执行器并发获取所有服务器的指标
                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        List<CompletableFuture<LauncherServerDto>> futures = servers.stream()
                                        .map(server -> CompletableFuture.supplyAsync(
                                                        () -> buildLauncherServerDto(cluster, server), executor))
                                        .toList();

                        // 等待所有任务完成
                        return futures.stream()
                                        .map(CompletableFuture::join)
                                        .collect(Collectors.toList());
                }
        }

        /**
         * 构建单个服务器的 LauncherServerDto
         */
        private LauncherServerDto buildLauncherServerDto(ClusterEntity cluster, MinecraftServer server) {
                var config = server.getMinecraftConfig();

                LauncherServerDto.LauncherServerDtoBuilder builder = LauncherServerDto.builder()
                                .name(server.getName())
                                .nodePort(server.getNodePort())
                                .status(server.getStatus());

                if (config != null) {
                        // 服务器类型和最大玩家数始终显示
                        builder.serverType(config.getServerType())
                                        .maxPlayers(config.getMaxPlayers());

                        // 正版验证始终显示
                        builder.onlineMode(config.getOnlineMode());

                        // 版本始终显示
                        builder.version(config.getVersion());

                        // 模组列表
                        if (config.getModrinthProjects() != null && !config.getModrinthProjects().isBlank()) {
                                builder.modrinthProjects(config.getModrinthProjects());
                        }

                        // 整合包
                        if (config.getModrinthModpack() != null && !config.getModrinthModpack().isBlank()) {
                                builder.modrinthModpack(config.getModrinthModpack());
                        }
                }

                LauncherServerDto dto = builder.build();

                // 获取服务器指标
                dto.setMetrics(getServerMetrics(cluster, server));

                return dto;
        }

        /**
         * 获取服务器运行指标
         * 使用虚拟线程并发获取 K8s 指标和玩家数
         */
        private ServerMetricsDto getServerMetrics(ClusterEntity cluster, MinecraftServer server) {
                try {
                        // 创建 K8s 客户端
                        io.kubernetes.client.openapi.ApiClient client = k8sClientFactory
                                        .createClient(cluster.getKubeconfig());

                        // 使用虚拟线程并发获取 K8s 指标和玩家数
                        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                                // 并发任务 1: 获取 K8s 指标
                                CompletableFuture<ServerMetricsDto> metricsFuture = CompletableFuture.supplyAsync(
                                                () -> metricsService.getServerMetrics(client, server.getNamespace(),
                                                                server.getName()),
                                                executor);

                                // 并发任务 2: 获取在线玩家数
                                CompletableFuture<Integer> playerCountFuture = CompletableFuture.supplyAsync(() -> {
                                        if (cluster.getDomain() != null && !cluster.getDomain().isEmpty()) {
                                                return queryService.getOnlinePlayerCount(cluster.getDomain(),
                                                                server.getNodePort());
                                        }
                                        return null;
                                }, executor);

                                // 等待两个任务完成
                                ServerMetricsDto metrics = metricsFuture.join();
                                Integer playerCount = playerCountFuture.join();

                                if (metrics == null) {
                                        metrics = ServerMetricsDto.builder().build();
                                }

                                metrics.setOnlinePlayers(playerCount);

                                // 设置最大玩家数
                                if (server.getMinecraftConfig() != null) {
                                        metrics.setMaxPlayers(server.getMinecraftConfig().getMaxPlayers());
                                }

                                return metrics;
                        }
                } catch (Exception e) {
                        // 如果获取指标失败,返回 null 而不是抛出异常
                        return null;
                }
        }
}
