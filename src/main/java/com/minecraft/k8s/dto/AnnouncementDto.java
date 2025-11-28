package com.minecraft.k8s.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "公告信息")
public class AnnouncementDto {
    
    @Schema(description = "公告 ID")
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Schema(description = "公告标题", example = "服务器维护通知", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    
    @NotBlank(message = "Content is required")
    @Schema(description = "公告内容（支持 HTML）", example = "<p>服务器将于今晚 10 点进行维护</p>", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    
    @NotNull(message = "Active status is required")
    @Schema(description = "是否启用", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean active;
    
    @Schema(description = "显示顺序（数字越小越靠前）", example = "0")
    private Integer displayOrder;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
