package com.keystone.service;

import com.keystone.dto.CommonDtos.AuditEntry;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.repository.WorkOrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view over the append-only status history, across all work orders (newest first). */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final WorkOrderStatusHistoryRepository history;

    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> recent(Pageable pageable) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.of(history.findAllByOrderByChangedAtDescIdDesc(p), h -> new AuditEntry(h.getId(),
                h.getWorkOrder().getId(), h.getWorkOrder().getCode(), h.getWorkOrder().getTitle(),
                h.getFromStatus(), h.getToStatus(), h.getChangedBy().getName(), h.getChangedAt(), h.getNote()));
    }
}
