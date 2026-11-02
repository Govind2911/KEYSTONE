package com.keystone.controller;

import com.keystone.dto.CommonDtos.NotificationDto;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.CommonDtos.UnreadCount;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    @Operation(summary = "My notifications", description = "Newest first, paginated.")
    public PageResponse<NotificationDto> mine(Pageable pageable, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.mine(me, pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications")
    public UnreadCount unread(@AuthenticationPrincipal AppUserPrincipal me) {
        return new UnreadCount(service.unreadCount(me));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark one notification as read")
    public void read(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        service.markSeen(id, me);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all notifications as read")
    public void readAll(@AuthenticationPrincipal AppUserPrincipal me) {
        service.markAllSeen(me);
    }
}
