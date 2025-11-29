package com.minecraft.k8s.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.minecraft.k8s.config.CacheConfig;
import com.minecraft.k8s.dto.launcher.ServerMetricsDto;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Kubernetes Metrics 服务
 * 获取 Pod 的 CPU 和内存使用率
 * 
 * 注意: 需要 Kubernetes 集群安装 metrics-server
 * K3s 默认已安装,可以通过 kubectl top pods 验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class K8sMetricsService {

    private final Executor cacheRefreshExecutor;

    /**
     * 存储每个 ApiClient 对应的缓存
     * key = ApiClient hashCode, value = AsyncLoadingCache
     */
    private final Map<Integer, AsyncLoadingCache<String, ServerMetricsDto>> cacheMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定 ApiClient 的缓存
     */
    private AsyncLoadingCache<String, ServerMetricsDto> getOrCreateCache(ApiClient client) {
        return cacheMap.computeIfAbsent(System.identityHashCode(client), k ->
                CacheConfig.<String, ServerMetricsDto>newCacheBuilder()
                        .buildAsync((key, executor) -> {
                            String[] parts = key.split(":", 2);
                            String namespace = parts[0];
                            String podName = parts[1];
                            return CompletableFuture.supplyAsync(
                                    () -> doGetServerMetrics(client, namespace, podName),
                                    cacheRefreshExecutor
                            );
                        })
        );
    }

    /**
     * 获取服务器指标(带异步刷新缓存)
     * 
     * @param client K8s API 客户端
     * @param namespace 命名空间
     * @param podName Pod 名称(StatefulSet 名称)
     * @return 服务器指标,如果获取失败返回 null
     */
    public ServerMetricsDto getServerMetrics(ApiClient client, String namespace, String podName) {
        try {
            String key = namespace + ":" + podName;
            return getOrCreateCache(client).get(key).join();
        } catch (Exception e) {
            log.error("Failed to get metrics from cache: {}/{}", namespace, podName, e);
            return null;
        }
    }

    /**
     * 实际执行获取服务器指标
     */
    private ServerMetricsDto doGetServerMetrics(ApiClient client, String namespace, String podName) {
        try {
            // 1. 获取 Pod Metrics
            Metrics metricsApi = new Metrics(client);
            PodMetricsList podMetricsList = metricsApi.getPodMetrics(namespace);
            
            // 2. 查找对应的 Pod (StatefulSet 的 Pod 名称格式: podName-0)
            PodMetrics podMetrics = podMetricsList.getItems().stream()
                    .filter(pm -> pm.getMetadata().getName().startsWith(podName + "-"))
                    .findFirst()
                    .orElse(null);
            
            if (podMetrics == null) {
                log.warn("Pod metrics not found for: {}/{}", namespace, podName);
                return null;
            }
            
            // 3. 获取 Pod 信息(用于获取资源限制)
            V1Pod pod = getPod(client, namespace, podMetrics.getMetadata().getName());
            if (pod == null || pod.getSpec() == null || pod.getSpec().getContainers().isEmpty()) {
                log.warn("Pod not found or has no containers: {}/{}", namespace, podName);
                return null;
            }
            
            // 4. 获取资源限制
            V1ResourceRequirements resources = pod.getSpec().getContainers().get(0).getResources();
            if (resources == null || resources.getLimits() == null) {
                log.warn("No resource limits defined for pod: {}/{}", namespace, podName);
                return null;
            }
            
            Map<String, Quantity> limits = resources.getLimits();
            Quantity cpuLimit = limits.get("cpu");
            Quantity memoryLimit = limits.get("memory");
            
            // 5. 获取当前使用量
            ContainerMetrics containerMetrics = podMetrics.getContainers().get(0);
            Map<String, Quantity> usage = containerMetrics.getUsage();
            Quantity cpuUsage = usage.get("cpu");
            Quantity memoryUsage = usage.get("memory");
            
            // 6. 计算使用率
            Double cpuPercent = calculateUsagePercent(cpuUsage, cpuLimit);
            Double memoryPercent = calculateUsagePercent(memoryUsage, memoryLimit);
            
            log.debug("Metrics for pod {}/{}: CPU={}%, Memory={}%", 
                    namespace, podName, cpuPercent, memoryPercent);
            
            return ServerMetricsDto.builder()
                    .cpuUsagePercent(cpuPercent)
                    .memoryUsagePercent(memoryPercent)
                    .build();
            
        }catch(

    Exception e)
    {
        log.error("Failed to get metrics for pod: {}/{}", namespace, podName, e);
        return null;
    }
    }

    /**
     * 获取 Pod 信息
     */
    private V1Pod getPod(ApiClient client, String namespace, String podName) {
        try {
            CoreV1Api api = new CoreV1Api(client);
            return api.readNamespacedPod(podName, namespace).execute();
        } catch (ApiException e) {
            log.error("Failed to get pod: {}/{}", namespace, podName, e);
            return null;
        }
    }
    
    /**
     * 计算使用率百分比
     * 
     * @param usage 当前使用量
     * @param limit 资源限制
     * @return 使用率百分比,如果计算失败返回 null
     */
    private Double calculateUsagePercent(Quantity usage, Quantity limit) {
        if (usage == null || limit == null) {
            return null;
        }
        
        try {
            // Quantity.getNumber() 返回 BigDecimal
            BigDecimal usageValue = usage.getNumber();
            BigDecimal limitValue = limit.getNumber();
            
            if (limitValue.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }
            
            // 计算百分比: (usage / limit) * 100
            BigDecimal percent = usageValue
                    .multiply(BigDecimal.valueOf(100))
                    .divide(limitValue, 2, RoundingMode.HALF_UP);
            
            return percent.doubleValue();
        } catch (Exception e) {
            log.error("Failed to calculate usage percent", e);
            return null;
        }
    }
}
