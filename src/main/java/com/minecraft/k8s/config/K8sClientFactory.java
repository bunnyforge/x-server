package com.minecraft.k8s.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.Base64;

@Component
public class K8sClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(K8sClientFactory.class);

    public ApiClient createClient(String kubeconfigContent) {
        if (kubeconfigContent == null || kubeconfigContent.isBlank()) {
            throw new RuntimeException("Kubeconfig content is empty");
        }
        
        String content = kubeconfigContent.trim();
        
        // 尝试直接解析 YAML
        try {
            if (content.startsWith("apiVersion:") || content.contains("\napiVersion:")) {
                return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new StringReader(content))).build();
            }
        } catch (Exception e) {
            log.debug("Failed to parse as direct YAML: {}", e.getMessage());
        }
        
        // 尝试 Base64 解码
        try {
            String decoded = new String(Base64.getDecoder().decode(content));
            if (decoded.contains("apiVersion:")) {
                return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new StringReader(decoded))).build();
            }
        } catch (Exception e) {
            log.debug("Failed to parse as Base64 encoded YAML: {}", e.getMessage());
        }
        
        // 最后尝试直接解析（可能是其他格式）
        try {
            return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new StringReader(content))).build();
        } catch (Exception e) {
            log.error("Failed to parse kubeconfig: {}", e.getMessage());
            throw new RuntimeException(
                    "Failed to parse kubeconfig. Ensure it is valid YAML format starting with 'apiVersion:'", e);
        }
    }
}
