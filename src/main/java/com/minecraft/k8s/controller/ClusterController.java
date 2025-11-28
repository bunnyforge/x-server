package com.minecraft.k8s.controller;

import com.minecraft.k8s.domain.entity.ClusterEntity;
import com.minecraft.k8s.service.ClusterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;

    @GetMapping
    public List<ClusterEntity> getAllClusters() {
        return clusterService.getAllClusters();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClusterEntity> getClusterById(@PathVariable Long id) {
        return clusterService.getClusterById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ClusterEntity createCluster(@RequestBody ClusterEntity cluster) {
        return clusterService.createCluster(cluster);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClusterEntity> updateCluster(@PathVariable Long id,
            @RequestBody ClusterEntity clusterDetails) {
        try {
            return ResponseEntity.ok(clusterService.updateCluster(id, clusterDetails));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCluster(@PathVariable Long id) {
        try {
            clusterService.deleteCluster(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
