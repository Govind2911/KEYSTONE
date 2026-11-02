package com.keystone.repository;

import com.keystone.domain.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    @EntityGraph(attributePaths = {"uploadedBy"})
    List<Attachment> findByWorkOrderIdOrderByUploadedAtDesc(Long workOrderId);
}
