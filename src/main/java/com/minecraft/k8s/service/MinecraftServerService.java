package com.minecraft.k8s.service;

import com.minecraft.k8s.domain.entity.MinecraftServerEntity;
import com.minecraft.k8s.domain.model.K8sResource;
import com.minecraft.k8s.domain.model.MinecraftServer;
import com.minecraft.k8s.domain.valueobject.K8sConfig;
import com.minecraft.k8s.domain.valueobject.MinecraftConfig;
import com.minecraft.k8s.dto.CreateServerRequest;
import com.minecraft.k8s.dto.UpdateServerRequest;
import com.minecraft.k8s.mapper.MinecraftServerMapper;
import com.minecraft.k8s.repository.MinecraftServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 YAML 模板的服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinecraftServerService {

    private final K8sCommandExecutor k8sExecutor;
    private final MinecraftServerRepository repository;
    private final PortAllocator portAllocator;
    private final MinecraftServerMapper mapper;
    private final ResourceCalculator resourceCalculator;
    private final ClusterService clusterService;
    private final com.minecraft.k8s.config.K8sClientFactory k8sClientFactory;

    // @Transactional removed to prevent rollback on K8s failure
    public synchronized MinecraftServer createServer(CreateServerRequest request) {
        // 检查名称是否已存在
        if (repository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Server name already exists: " + request.getName());
        }

        // 获取集群信息
        com.minecraft.k8s.domain.entity.ClusterEntity cluster = clusterService.getClusterById(request.getClusterId())
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + request.getClusterId()));

        // 创建 K8s 客户端
        io.kubernetes.client.openapi.ApiClient client = k8sClientFactory.createClient(cluster.getKubeconfig());

        // 自动分配端口 (从 K8s 查询)
        Integer nodePort = portAllocator.allocatePort(client);
        // 根据端口号生成命名空间
        String namespace = portAllocator.generateNamespace(nodePort);

        // 构建 K8s 配置
        K8sConfig k8sConfig = buildK8sConfig(request.getK8sConfig());

        // 构建 Minecraft 配置
        MinecraftConfig minecraftConfig = buildMinecraftConfig(
                request.getMinecraftConfig(), k8sConfig.getMemoryLimit());

        // 创建实体
        MinecraftServerEntity entity = new MinecraftServerEntity();
        entity.setName(request.getName());
        entity.setNamespace(namespace);
        entity.setNodePort(nodePort);
        entity.setClusterId(cluster.getId());
        entity.setK8sConfigObject(k8sConfig);
        entity.setMinecraftConfigObject(minecraftConfig);
        entity.setStatus("CREATING");

        entity = repository.save(entity);

        // 转换 Entity -> Model
        MinecraftServer server = mapper.entityToModel(entity);
        server.validate();

        try {
            // 生成 YAML 并应用到 K8s
            String yaml = generateYaml(server);
            // io.kubernetes.client.openapi.ApiClient client =
            // k8sClientFactory.createClient(cluster.getKubeconfig()); // Already created
            // above
            k8sExecutor.applyYaml(client, yaml);

            // 更新状态
            entity.setStatus("RUNNING");
            repository.save(entity);

            log.info("Server created: {}", server.getFullName());
            return server;
        } catch (Exception e) {
            // 创建失败，更新状态
            entity.setStatus("ERROR");
            repository.save(entity);
            throw new RuntimeException("Failed to create server in K8s", e);
        }
    }

    // @Transactional removed to prevent rollback on K8s failure
    public MinecraftServer updateServer(String name, UpdateServerRequest request) {
        // 从数据库获取
        MinecraftServerEntity entity = repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + name));

        // 获取现有配置
        K8sConfig k8sConfig = entity.getK8sConfigObject();
        MinecraftConfig minecraftConfig = entity.getMinecraftConfigObject();

        // 更新 K8s 配置
        if (request.getK8sConfig() != null) {
            updateK8sConfig(k8sConfig, request.getK8sConfig());
        }

        // 更新 Minecraft 配置
        if (request.getMinecraftConfig() != null) {
            updateMinecraftConfig(minecraftConfig, request.getMinecraftConfig(), k8sConfig.getMemoryLimit());
        }

        // 保存更新后的配置
        entity.setK8sConfigObject(k8sConfig);
        entity.setMinecraftConfigObject(minecraftConfig);

        // 转换为领域模型并验证
        MinecraftServer server = mapper.entityToModel(entity);

        try {
            // 生成并应用新的 YAML
            String yaml = generateYaml(server);
            com.minecraft.k8s.domain.entity.ClusterEntity cluster = clusterService.getClusterById(entity.getClusterId())
                    .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + entity.getClusterId()));
            io.kubernetes.client.openapi.ApiClient client = k8sClientFactory.createClient(cluster.getKubeconfig());
            k8sExecutor.applyYaml(client, yaml);

            // 更新数据库
            entity.setStatus("RUNNING");
            repository.save(entity);

            log.info("Server updated: {}", server.getFullName());
            return server;
        } catch (Exception e) {
            entity.setStatus("ERROR");
            repository.save(entity);
            throw new RuntimeException("Failed to update server in K8s", e);
        }
    }

    @Transactional
    public void deleteServer(String name) {
        MinecraftServerEntity entity = repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + name));

        try {
            // 从 K8s 删除
            com.minecraft.k8s.domain.entity.ClusterEntity cluster = clusterService.getClusterById(entity.getClusterId())
                    .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + entity.getClusterId()));
            io.kubernetes.client.openapi.ApiClient client = k8sClientFactory.createClient(cluster.getKubeconfig());
            k8sExecutor.deleteResources(client, entity.getNamespace(), entity.getName());

            // 从数据库删除
            repository.delete(entity);

            log.info("Server deleted: {}", name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete server", e);
        }
    }

    public MinecraftServer getServer(String name) {
        MinecraftServerEntity entity = repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + name));
        return mapper.entityToModel(entity);
    }

    public MinecraftServer getServerByNamespace(String namespace) {
        MinecraftServerEntity entity = repository.findByNamespace(namespace)
                .orElseThrow(() -> new IllegalArgumentException("Server not found in namespace: " + namespace));
        return mapper.entityToModel(entity);
    }

    @Transactional
    public MinecraftServer updateServerByNamespace(String namespace, UpdateServerRequest request) {
        // 从数据库获取
        MinecraftServerEntity entity = repository.findByNamespace(namespace)
                .orElseThrow(() -> new IllegalArgumentException("Server not found in namespace: " + namespace));

        // 获取现有配置
        K8sConfig k8sConfig = entity.getK8sConfigObject();
        MinecraftConfig minecraftConfig = entity.getMinecraftConfigObject();

        // 更新 K8s 配置
        if (request.getK8sConfig() != null) {
            updateK8sConfig(k8sConfig, request.getK8sConfig());
        }

        // 更新 Minecraft 配置
        if (request.getMinecraftConfig() != null) {
            updateMinecraftConfig(minecraftConfig, request.getMinecraftConfig(), k8sConfig.getMemoryLimit());
        }

        // 保存更新后的配置
        entity.setK8sConfigObject(k8sConfig);
        entity.setMinecraftConfigObject(minecraftConfig);

        // 转换为领域模型并验证
        MinecraftServer server = mapper.entityToModel(entity);

        try {
            // 生成并应用新的 YAML
            String yaml = generateYaml(server);
            com.minecraft.k8s.domain.entity.ClusterEntity cluster = clusterService.getClusterById(entity.getClusterId())
                    .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + entity.getClusterId()));
            io.kubernetes.client.openapi.ApiClient client = k8sClientFactory.createClient(cluster.getKubeconfig());
            k8sExecutor.applyYaml(client, yaml);

            // 更新数据库
            entity.setStatus("RUNNING");
            repository.save(entity);

            log.info("Server updated in namespace {}: {}", namespace, server.getFullName());
            return server;
        } catch (Exception e) {
            entity.setStatus("ERROR");
            repository.save(entity);
            throw new RuntimeException("Failed to update server in K8s", e);
        }
    }

    @Transactional
    public void deleteServerByNamespace(String namespace) {
        MinecraftServerEntity entity = repository.findByNamespace(namespace)
                .orElseThrow(() -> new IllegalArgumentException("Server not found in namespace: " + namespace));

        try {
            // 从 K8s 删除
            com.minecraft.k8s.domain.entity.ClusterEntity cluster = clusterService.getClusterById(entity.getClusterId())
                    .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + entity.getClusterId()));
            io.kubernetes.client.openapi.ApiClient client = k8sClientFactory.createClient(cluster.getKubeconfig());
            k8sExecutor.deleteResources(client, entity.getNamespace(), entity.getName());

            // 从数据库删除
            repository.delete(entity);

            log.info("Server deleted from namespace: {}", namespace);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete server", e);
        }
    }

    public List<MinecraftServer> listServers() {
        return repository.findAll().stream()
                .map(mapper::entityToModel)
                .collect(Collectors.toList());
    }

    private String generateYaml(MinecraftServer server) {
        try {
            // 读取模板
            String template = loadTemplate();

            // 转换为 K8s 资源对象
            K8sResource resource = K8sResource.fromMinecraftServer(server);

            // 替换参数
            return replaceParams(template, resource);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate YAML", e);
        }
    }

    private String loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("k8s-template.yaml");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private String replaceParams(String template, K8sResource resource) {
        // 计算初始内存 (最大内存的 50%)
        String initMemory = calculateInitMemory(resource.getMaxMemory());

        String result = template
                .replace("${namespace}", resource.getNamespace())
                .replace("${name}", resource.getName())
                .replace("${replicas}", String.valueOf(resource.getReplicas()))
                .replace("${nodePort}", String.valueOf(resource.getNodePort()))
                .replace("${serverType}", resource.getServerType())
                .replace("${onlineMode}", String.valueOf(resource.getOnlineMode()).toUpperCase())
                .replace("${maxPlayers}", String.valueOf(resource.getMaxPlayers()))
                .replace("${initMemory}", initMemory)
                .replace("${maxMemory}", resource.getMaxMemory())
                .replace("${memoryLimit}", resource.getMemoryLimit())
                .replace("${memoryRequest}", resource.getMemoryRequest())
                .replace("${cpuLimit}", resource.getCpuLimit())
                .replace("${cpuRequest}", resource.getCpuRequest())
                .replace("${storageSize}", resource.getStorageSize())
                .replace("${storageClassName}", resource.getStorageClassName())
                .replace("${jvmOptions}", resource.getJvmOptions())
                .replace("${version}", resource.getVersion() != null ? resource.getVersion() : "latest");
        
        // Modrinth 模组列表（普通服务器 + Mods 模式）
        // 将换行符替换为逗号，确保 YAML 格式正确
        String modrinthProjects = "";
        if (resource.getModrinthProjects() != null && !resource.getModrinthProjects().isEmpty()) {
            modrinthProjects = resource.getModrinthProjects()
                    .replace("\r\n", ",")
                    .replace("\n", ",")
                    .replace("\r", ",")
                    .replaceAll(",+", ",")  // 多个逗号合并为一个
                    .replaceAll("^,|,$", "");  // 去除首尾逗号
        }
        result = result.replace("${modrinthProjects}", modrinthProjects);
        
        // Modrinth 整合包配置（整合包模式）
        String modpackPlatform = (resource.getModrinthModpack() != null && !resource.getModrinthModpack().isEmpty()) 
                ? "MODRINTH" : "";
        result = result.replace("${modpackPlatform}", modpackPlatform);
        result = result.replace("${modrinthModpack}", 
                resource.getModrinthModpack() != null ? resource.getModrinthModpack() : "");
        
        return result;
    }

    /**
     * 计算初始内存 (最大内存的 50%)
     * 例如: "4G" -> "2G", "2048M" -> "1024M"
     */
    private String calculateInitMemory(String maxMemory) {
        if (maxMemory == null || maxMemory.isEmpty()) {
            return "512M"; // 默认值
        }

        try {
            // 提取数字和单位
            String numStr = maxMemory.replaceAll("[^0-9]", "");
            String unit = maxMemory.replaceAll("[0-9]", "");

            if (numStr.isEmpty()) {
                return "512M";
            }

            int maxValue = Integer.parseInt(numStr);
            int initValue = maxValue / 2; // 50%

            return initValue + unit;
        } catch (Exception e) {
            log.warn("Failed to calculate init memory from: {}, using default", maxMemory);
            return "512M";
        }
    }

    private K8sConfig buildK8sConfig(CreateServerRequest.CreateK8sConfigDTO dto) {
        K8sConfig config = new K8sConfig();

        // 获取默认配置
        ResourceCalculator.ServerResourceConfig defaultConfig = resourceCalculator.getDefaultConfig();

        if (dto != null) {
            // 将数字转换为带单位的字符串
            config.setMemoryLimit(dto.getMemoryLimit() + "Gi");
            config.setCpuLimit(String.valueOf(dto.getCpuLimit()));
            config.setStorageSize(dto.getStorageSize() + "Gi");
        }

        // 设置系统默认值
        config.setStorageClassName(defaultConfig.storageClassName());
        config.setReplicas(defaultConfig.replicas());

        // 计算初始资源
        config.setMemoryRequest(resourceCalculator.calculateMemoryRequest(config.getMemoryLimit()));
        config.setCpuRequest(resourceCalculator.calculateCpuRequest(config.getCpuLimit()));

        return config;
    }

    private MinecraftConfig buildMinecraftConfig(
            CreateServerRequest.CreateMinecraftConfigDTO dto, String memoryLimit) {
        MinecraftConfig config = new MinecraftConfig();

        // 获取默认配置
        ResourceCalculator.ServerResourceConfig defaultConfig = resourceCalculator.getDefaultConfig();

        if (dto != null) {
            config.setServerType(dto.getServerType() != null ? dto.getServerType() : defaultConfig.serverType());
            config.setOnlineMode(dto.getOnlineMode() != null ? dto.getOnlineMode() : defaultConfig.onlineMode());
            config.setMaxPlayers(dto.getMaxPlayers());
            config.setJvmOptions(dto.getJvmOptions() != null ? dto.getJvmOptions() : defaultConfig.jvmOptions());
            config.setVersion(dto.getVersion());
            config.setDifficulty(dto.getDifficulty());
            config.setPvp(dto.getPvp());
            config.setViewDistance(dto.getViewDistance());
            
            // Modrinth 配置
            config.setModrinthProjects(dto.getModrinthProjects());
            config.setModrinthModpack(dto.getModrinthModpack());
            
            // 如果设置了 Modrinth 整合包，自动将 TYPE 设置为 MODRINTH
            if (dto.getModrinthModpack() != null && !dto.getModrinthModpack().isEmpty()) {
                config.setServerType("MODRINTH");
            }
        } else {
            // 如果没有传 minecraftConfig，使用默认值
            config.setServerType(defaultConfig.serverType());
            config.setOnlineMode(defaultConfig.onlineMode());
            config.setJvmOptions(defaultConfig.jvmOptions());
        }

        // 计算 JVM 最大内存
        config.setMaxMemory(resourceCalculator.calculateMaxMemory(memoryLimit));

        return config;
    }

    private void updateK8sConfig(K8sConfig config,
            UpdateServerRequest.UpdateK8sConfigDTO dto) {
        if (dto.getMemoryLimit() != null) {
            String memoryLimit = dto.getMemoryLimit() + "Gi";
            config.setMemoryLimit(memoryLimit);
            config.setMemoryRequest(resourceCalculator.calculateMemoryRequest(memoryLimit));
        }
        if (dto.getCpuLimit() != null) {
            String cpuLimit = String.valueOf(dto.getCpuLimit());
            config.setCpuLimit(cpuLimit);
            config.setCpuRequest(resourceCalculator.calculateCpuRequest(cpuLimit));
        }
        if (dto.getStorageSize() != null) {
            config.setStorageSize(dto.getStorageSize() + "Gi");
        }
        // replicas 和 storageClassName 不允许用户修改
    }

    private void updateMinecraftConfig(MinecraftConfig config,
            UpdateServerRequest.UpdateMinecraftConfigDTO dto,
            String memoryLimit) {
        if (dto.getMaxPlayers() != null) {
            config.setMaxPlayers(dto.getMaxPlayers());
        }
        if (dto.getServerType() != null) {
            config.setServerType(dto.getServerType());
        }
        if (dto.getOnlineMode() != null) {
            config.setOnlineMode(dto.getOnlineMode());
        }
        if (dto.getJvmOptions() != null) {
            config.setJvmOptions(dto.getJvmOptions());
        }
        if (dto.getVersion() != null) {
            config.setVersion(dto.getVersion());
        }
        if (dto.getDifficulty() != null) {
            config.setDifficulty(dto.getDifficulty());
        }
        if (dto.getPvp() != null) {
            config.setPvp(dto.getPvp());
        }
        if (dto.getViewDistance() != null) {
            config.setViewDistance(dto.getViewDistance());
        }
        
        // Modrinth 配置
        if (dto.getModrinthProjects() != null) {
            config.setModrinthProjects(dto.getModrinthProjects());
        }
        if (dto.getModrinthModpack() != null) {
            config.setModrinthModpack(dto.getModrinthModpack());
            // 如果设置了 Modrinth 整合包，自动将 TYPE 设置为 MODRINTH
            if (!dto.getModrinthModpack().isEmpty()) {
                config.setServerType("MODRINTH");
            }
        }

        // 如果内存限制变了，重新计算 JVM 内存
        config.setMaxMemory(resourceCalculator.calculateMaxMemory(memoryLimit));
    }
}
