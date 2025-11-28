package com.minecraft.k8s.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.Yaml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Kubernetes API 执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class K8sCommandExecutor {

    public void applyYaml(ApiClient apiClient, String yaml) {
        try {
            // 解析 YAML 中的多个资源
            List<Object> resources = Yaml.loadAll(yaml);

            CoreV1Api coreApi = new CoreV1Api(apiClient);
            AppsV1Api appsApi = new AppsV1Api(apiClient);

            for (Object resource : resources) {
                applyResource(resource, coreApi, appsApi);
            }

            log.info("YAML applied successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML", e);
        } catch (ApiException e) {
            throw new RuntimeException("Failed to apply YAML: " + e.getResponseBody(), e);
        }
    }

    private void applyResource(Object resource, CoreV1Api coreApi, AppsV1Api appsApi) throws ApiException {
        String kind = getKind(resource);

        switch (kind) {
            case "Namespace":
                applyNamespace(resource, coreApi);
                break;
            case "Service":
                applyService(resource, coreApi);
                break;
            case "StatefulSet":
                applyStatefulSet(resource, appsApi);
                break;
            case "PersistentVolumeClaim":
                applyPvc(resource, coreApi);
                break;
            default:
                log.warn("Unsupported resource kind: {}", kind);
        }
    }

    private void applyNamespace(Object resource, CoreV1Api api) throws ApiException {
        V1Namespace namespace = (V1Namespace) resource;

        String name = namespace.getMetadata().getName();

        try {
            // 尝试获取，如果存在则更新
            api.readNamespace(name).execute();
            api.replaceNamespace(name, namespace).execute();
            log.info("Namespace updated: {}", name);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                // 不存在则创建
                api.createNamespace(namespace).execute();
                log.info("Namespace created: {}", name);
            } else {
                throw e;
            }
        }
    }

    private void applyService(Object resource, CoreV1Api api) throws ApiException {
        V1Service service = (V1Service) resource;

        String namespace = service.getMetadata().getNamespace();
        String name = service.getMetadata().getName();

        try {
            api.readNamespacedService(name, namespace).execute();
            api.replaceNamespacedService(name, namespace, service).execute();
            log.info("Service updated: {}/{}", namespace, name);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                api.createNamespacedService(namespace, service).execute();
                log.info("Service created: {}/{}", namespace, name);
            } else {
                throw e;
            }
        }
    }

    private void applyStatefulSet(Object resource, AppsV1Api api) throws ApiException {
        V1StatefulSet newStatefulSet = (V1StatefulSet) resource;

        String namespace = newStatefulSet.getMetadata().getNamespace();
        String name = newStatefulSet.getMetadata().getName();

        try {
            V1StatefulSet existingStatefulSet = api.readNamespacedStatefulSet(name, namespace).execute();

            // Smart update: only update mutable fields
            if (existingStatefulSet.getSpec() != null && newStatefulSet.getSpec() != null) {
                existingStatefulSet.getSpec()
                        .replicas(newStatefulSet.getSpec().getReplicas())
                        .template(newStatefulSet.getSpec().getTemplate())
                        .updateStrategy(newStatefulSet.getSpec().getUpdateStrategy())
                        .minReadySeconds(newStatefulSet.getSpec().getMinReadySeconds())
                        .revisionHistoryLimit(newStatefulSet.getSpec().getRevisionHistoryLimit());
            }

            // Update metadata
            if (newStatefulSet.getMetadata().getAnnotations() != null) {
                existingStatefulSet.getMetadata().setAnnotations(newStatefulSet.getMetadata().getAnnotations());
            }
            if (newStatefulSet.getMetadata().getLabels() != null) {
                existingStatefulSet.getMetadata().setLabels(newStatefulSet.getMetadata().getLabels());
            }

            api.replaceNamespacedStatefulSet(name, namespace, existingStatefulSet).execute();
            log.info("StatefulSet updated: {}/{}", namespace, name);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                api.createNamespacedStatefulSet(namespace, newStatefulSet).execute();
                log.info("StatefulSet created: {}/{}", namespace, name);
            } else {
                throw e;
            }
        }
    }

    private void applyPvc(Object resource, CoreV1Api api) throws ApiException {
        V1PersistentVolumeClaim pvc = (V1PersistentVolumeClaim) resource;

        String namespace = pvc.getMetadata().getNamespace();
        String name = pvc.getMetadata().getName();

        try {
            api.readNamespacedPersistentVolumeClaim(name, namespace).execute();
            // PVC 不支持更新，跳过
            log.info("PVC already exists: {}/{}", namespace, name);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                api.createNamespacedPersistentVolumeClaim(namespace, pvc).execute();
                log.info("PVC created: {}/{}", namespace, name);
            } else {
                throw e;
            }
        }
    }

    public void deleteResources(ApiClient apiClient, String namespace, String name) {
        CoreV1Api coreApi = new CoreV1Api(apiClient);
        AppsV1Api appsApi = new AppsV1Api(apiClient);

        // 删除 StatefulSet
        try {
            appsApi.deleteNamespacedStatefulSet(name, namespace).execute();
            log.info("StatefulSet deleted: {}/{}", namespace, name);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.warn("Failed to delete StatefulSet: {}", e.getMessage());
            }
        }

        // 删除 Service
        try {
            coreApi.deleteNamespacedService(name, namespace).execute();
            log.info("Service deleted: {}/{}", namespace, name);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.warn("Failed to delete Service: {}", e.getMessage());
            }
        }

        // 删除 PVC (StatefulSet 的 volumeClaimTemplates 创建的 PVC)
        // PVC 命名规则: data-${statefulset-name}-${pod-index}
        String pvcName = "data-" + name + "-0";
        try {
            coreApi.deleteNamespacedPersistentVolumeClaim(pvcName, namespace).execute();
            log.info("PVC deleted: {}/{}", namespace, pvcName);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.warn("Failed to delete PVC: {}", e.getMessage());
            }
        }

        // 删除 Namespace
        try {
            coreApi.deleteNamespace(namespace).execute();
            log.info("Namespace deleted: {}", namespace);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.warn("Failed to delete Namespace: {}", e.getMessage());
            }
        }

        log.info("Resources deleted: {}/{}", namespace, name);
    }

    private String getKind(Object resource) {
        try {
            return (String) resource.getClass().getMethod("getKind").invoke(resource);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
