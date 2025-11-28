package com.minecraft.k8s.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI minecraftServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Minecraft Server Management API")
                        .description("用于管理 Kubernetes 上的 Minecraft 服务器")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BunnyForge")
                                .url("https://github.com/bunnyforge")));
    }
}
