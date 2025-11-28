package com.minecraft.k8s.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 端口分配器 - 自动分配可用端口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortAllocator {

    @Value("${minecraft.port.start:31001}")
    private Integer startPort;

    @Value("${minecraft.port.end:32000}")
    private Integer endPort;

    public synchronized Integer allocatePort(ApiClient apiClient) {
        try {
            // 使用 GenericKubernetesApi 避免 CoreV1Api 方法签名版本差异问题
            GenericKubernetesApi<V1Namespace, V1NamespaceList> namespaceApi = new GenericKubernetesApi<>(
                    V1Namespace.class,
                    V1NamespaceList.class,
                    "",
                    "v1",
                    "namespaces",
                    apiClient);

            V1NamespaceList list = namespaceApi.list().getObject();

            Set<Integer> usedPorts = new HashSet<>();
            int maxPort = startPort - 1;

            if (list != null && list.getItems() != null) {
                for (V1Namespace ns : list.getItems()) {
                    String name = ns.getMetadata().getName();
                    if (name != null && name.startsWith("minecraft")) {
                        try {
                            int port = Integer.parseInt(name.substring("minecraft".length()));
                            if (port >= startPort && port <= endPort) {
                                usedPorts.add(port);
                                maxPort = Math.max(maxPort, port);
                            }
                        } catch (NumberFormatException e) {
                            // 忽略不符合命名规则的 Namespace
                        }
                    }
                }
            }

            // 尝试分配下一个端口
            int nextPort = maxPort + 1;

            // 如果超出范围，回绕查找空闲端口
            if (nextPort > endPort) {
                for (int port = startPort; port <= endPort; port++) {
                    if (!usedPorts.contains(port)) {
                        log.info("Allocated port from K8s (gap filling): {}", port);
                        return port;
                    }
                }
                throw new RuntimeException("No available ports in range " + startPort + "-" + endPort);
            }

            log.info("Allocated port from K8s: {}", nextPort);
            return nextPort;

        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate port from K8s", e);
        }
    }

    public String generateNamespace(Integer nodePort) {
        // 生成命名空间：minecraft{端口号}
        String namespace = "minecraft" + nodePort;
        log.info("Generated namespace: {}", namespace);
        return namespace;
    }
}
