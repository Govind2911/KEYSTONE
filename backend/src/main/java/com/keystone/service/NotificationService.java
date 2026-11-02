package com.keystone.service;

import com.keystone.domain.Notification;
import com.keystone.domain.NotificationType;
import com.keystone.domain.Role;
import com.keystone.domain.UserAccount;
import com.keystone.dto.CommonDtos.NotificationDto;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.exception.ApiException;
import com.keystone.repository.NotificationRepository;
import com.keystone.repository.UserRepository;
import com.keystone.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

/** In-app notifications. Runs inside the caller's transaction so a notification never outlives a rolled-back change. */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final EmailService emails;

    @Transactional
    public void notifyUser(Long userId, NotificationType type, String message, Long workOrderId) {
        Notification n = new Notification();
        n.setUser(users.getReferenceById(userId));
        n.setType(type);
        n.setMessage(message.length() > 300 ? message.substring(0, 297) + "..." : message);
        n.setWorkOrderId(workOrderId);
        notifications.save(n);
        String to = n.getUser().getEmail();
        String subject = "KEYSTONE: " + type.name().replace('_', ' ').toLowerCase();
        runAfterCommit(() -> emails.send(to, subject, n.getMessage()));
    }

    /** E-mail only goes out if the surrounding transaction actually commits. */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Transactional
    public void notifyRoles(Collection<Role> roles, NotificationType type, String message, Long workOrderId) {
        for (UserAccount u : users.findByRoleInAndActiveTrue(roles)) {
            notifyUser(u.getId(), type, message, workOrderId);
        }
    }

    @Transactional
    public void notifyCustomerUsers(Long customerId, NotificationType type, String message, Long workOrderId) {
        for (UserAccount u : users.findByCustomerIdAndActiveTrue(customerId)) {
            notifyUser(u.getId(), type, message, workOrderId);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> mine(AppUserPrincipal me, Pageable pageable) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.of(notifications.findByUserIdOrderByCreatedAtDesc(me.id(), p), this::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount(AppUserPrincipal me) {
        return notifications.countByUserIdAndSeenFalse(me.id());
    }

    @Transactional
    public void markSeen(Long id, AppUserPrincipal me) {
        Notification n = notifications.findById(id)
                .filter(x -> x.getUser().getId().equals(me.id()))
                .orElseThrow(() -> new ApiException.NotFound("Notification not found"));
        n.setSeen(true);
    }

    @Transactional
    public void markAllSeen(AppUserPrincipal me) {
        notifications.markAllSeen(me.id());
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getMessage(), n.getWorkOrderId(), n.isSeen(), n.getCreatedAt());
    }
}
