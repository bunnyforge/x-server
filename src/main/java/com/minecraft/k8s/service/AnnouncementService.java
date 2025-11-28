package com.minecraft.k8s.service;

import com.minecraft.k8s.domain.entity.AnnouncementEntity;
import com.minecraft.k8s.dto.AnnouncementDto;
import com.minecraft.k8s.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository repository;

    @Transactional
    public AnnouncementDto createAnnouncement(AnnouncementDto dto) {
        AnnouncementEntity entity = AnnouncementEntity.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .active(Boolean.TRUE.equals(dto.getActive()) ? dto.getActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();

        entity = repository.save(entity);
        log.info("Announcement created: {}", entity.getId());
        return toDto(entity);
    }

    @Transactional
    public AnnouncementDto updateAnnouncement(Long id, AnnouncementDto dto) {
        AnnouncementEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + id));

        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setActive(dto.getActive());
        if (dto.getDisplayOrder() != null) {
            entity.setDisplayOrder(dto.getDisplayOrder());
        }

        entity = repository.save(entity);
        log.info("Announcement updated: {}", entity.getId());
        return toDto(entity);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Announcement not found: " + id);
        }
        repository.deleteById(id);
        log.info("Announcement deleted: {}", id);
    }

    public AnnouncementDto getAnnouncement(Long id) {
        AnnouncementEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + id));
        return toDto(entity);
    }

    public List<AnnouncementDto> getAllAnnouncements() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<AnnouncementDto> getActiveAnnouncements() {
        return repository.findByActiveOrderByDisplayOrderAsc(true).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private AnnouncementDto toDto(AnnouncementEntity entity) {
        AnnouncementDto dto = new AnnouncementDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setActive(entity.getActive());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
