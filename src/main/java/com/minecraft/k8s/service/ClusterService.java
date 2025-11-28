package com.minecraft.k8s.service;

import com.minecraft.k8s.domain.entity.ClusterEntity;
import com.minecraft.k8s.repository.ClusterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;

    public List<ClusterEntity> getAllClusters() {
        return clusterRepository.findAll();
    }

    public Optional<ClusterEntity> getClusterById(Long id) {
        return clusterRepository.findById(id);
    }

    @Transactional
    public ClusterEntity createCluster(ClusterEntity cluster) {
        if (clusterRepository.findByName(cluster.getName()).isPresent()) {
            throw new IllegalArgumentException("Cluster with name " + cluster.getName() + " already exists");
        }
        return clusterRepository.save(cluster);
    }

    @Transactional
    public ClusterEntity updateCluster(Long id, ClusterEntity clusterDetails) {
        ClusterEntity cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found with id: " + id));

        cluster.setName(clusterDetails.getName());
        cluster.setKubeconfig(clusterDetails.getKubeconfig());
        cluster.setDomain(clusterDetails.getDomain());

        return clusterRepository.save(cluster);
    }

    @Transactional
    public void deleteCluster(Long id) {
        ClusterEntity cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found with id: " + id));
        clusterRepository.delete(cluster);
    }
}
