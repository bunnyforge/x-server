package com.minecraft.k8s.config;

import com.minecraft.k8s.infrastructure.security.AuthInterceptor;
import com.minecraft.k8s.infrastructure.security.LauncherAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private LauncherAuthInterceptor launcherAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/launcher/**");

        registry.addInterceptor(launcherAuthInterceptor)
                .addPathPatterns("/api/launcher/**");
    }
}
