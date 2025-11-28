package com.minecraft.k8s.dto;

import com.minecraft.k8s.domain.model.MinecraftServer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterWithServersDto {
    private Long id;
    private String name;
    private String domain;
    private List<MinecraftServer> servers;
}
