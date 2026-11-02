package com.keystone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keystone.domain.NotificationType;
import com.keystone.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

public final class CommonDtos {
    private CommonDtos() {}

    /** Stable pagination envelope used by every list endpoint. */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    /** url = inline stream, downloadUrl = attachment download (both require the bearer token); publicUrl = direct Cloudinary link, staff only. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AttachmentDto(Long id, String fileName, String contentType, long sizeBytes, String uploadedBy,
                                Long uploadedById, Instant uploadedAt, String storage, String url, String downloadUrl,
                                String publicUrl) {}

    public record NotificationDto(Long id, NotificationType type, String message, Long workOrderId,
                                  boolean seen, Instant createdAt) {}

    public record UnreadCount(long unread) {}

    public record AuditEntry(Long id, Long workOrderId, String workOrderCode, String workOrderTitle,
                             WorkOrderStatus fromStatus, WorkOrderStatus toStatus, String changedBy,
                             Instant changedAt, String note) {}
}
