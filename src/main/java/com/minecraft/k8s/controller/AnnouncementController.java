package com.minecraft.k8s.controller;

import com.minecraft.k8s.dto.AnnouncementDto;
import com.minecraft.k8s.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "公告管理 API")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @Operation(summary = "创建公告", description = "创建新的公告信息")
    public ResponseEntity<AnnouncementDto> createAnnouncement(@Valid @RequestBody AnnouncementDto dto) {
        return ResponseEntity.ok(announcementService.createAnnouncement(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新公告", description = "更新指定 ID 的公告")
    public ResponseEntity<AnnouncementDto> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementDto dto) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告", description = "删除指定 ID 的公告")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情", description = "获取指定 ID 的公告详情")
    public ResponseEntity<AnnouncementDto> getAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncement(id));
    }

    @GetMapping
    @Operation(summary = "获取所有公告", description = "获取所有公告列表")
    public ResponseEntity<List<AnnouncementDto>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }
}
