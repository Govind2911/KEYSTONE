package com.keystone.service;

import com.keystone.domain.Attachment;
import com.keystone.domain.Role;
import com.keystone.domain.WorkOrder;
import com.keystone.dto.CommonDtos.AttachmentDto;
import com.keystone.exception.ApiException;
import com.keystone.repository.AttachmentRepository;
import com.keystone.repository.UserRepository;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/** Photos / PDFs on a work order: upload, list, stream (inline view), download and delete. Whitelisted, non-executable types only. */
@Service
@RequiredArgsConstructor
public class AttachmentService {
    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf", "text/plain");

    private final AttachmentRepository attachments;
    private final UserRepository users;
    private final WorkOrderAccess access;
    private final List<FileStorage> storages;

    @Value("${keystone.storage.provider:auto}")
    private String configuredProvider;

    public record Download(InputStream stream, String fileName, String contentType) {}

    @Transactional
    public AttachmentDto upload(Long workOrderId, MultipartFile file, AppUserPrincipal me) {
        WorkOrder w = access.loadVisible(workOrderId, me);
        if (w.getStatus().isTerminal()) throw new ApiException.Conflict("Files cannot be added to a " + w.getStatus() + " work order");
        if (file == null || file.isEmpty()) throw new ApiException.BadRequest("Choose a file to upload");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED.contains(type)) throw new ApiException.BadRequest("Only images, PDF or plain text files are allowed");

        FileStorage storage = storageForUpload();
        FileStorage.Stored stored;
        try {
            stored = storage.store(w.getId(), file, type);
        } catch (IOException e) {
            log.error("Could not store attachment for {} via {}", w.getCode(), storage.provider(), e);
            throw new ApiException.Conflict("The file could not be saved. Please try again.");
        }
        Attachment a = new Attachment();
        a.setWorkOrder(w);
        a.setFileName(sanitize(file.getOriginalFilename()));
        a.setContentType(type);
        a.setSizeBytes(file.getSize());
        a.setStorageKey(stored.key());
        a.setStorageProvider(storage.provider());
        a.setPublicUrl(stored.publicUrl());
        a.setCloudPublicId(stored.cloudPublicId());
        a.setResourceType(stored.resourceType());
        a.setUploadedBy(users.getReferenceById(me.id()));
        try {
            attachments.save(a);
        } catch (RuntimeException e) {
            storage.delete(a);                                  // do not leave an orphaned file behind
            throw e;
        }
        log.info("Attachment {} ({} bytes, {}) added to {} by {}", a.getFileName(), file.getSize(), storage.provider(), w.getCode(), me.email());
        return toDto(a, me, me.name());
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> list(Long workOrderId, AppUserPrincipal me) {
        WorkOrder w = access.loadVisible(workOrderId, me);
        return attachments.findByWorkOrderIdOrderByUploadedAtDesc(w.getId()).stream()
                .map(a -> toDto(a, me, a.getUploadedBy().getName())).toList();
    }

    /** Opens the file for streaming. Same visibility rule as the parent work order, so another customer's file is a 404. */
    @Transactional(readOnly = true)
    public Download open(Long attachmentId, AppUserPrincipal me) {
        Attachment a = attachments.findById(attachmentId).orElseThrow(() -> new ApiException.NotFound("Attachment not found"));
        access.loadVisible(a.getWorkOrder().getId(), me);
        try {
            return new Download(storageFor(a).open(a), a.getFileName(), a.getContentType());
        } catch (IOException e) {
            log.warn("Attachment {} could not be opened: {}", attachmentId, e.getMessage());
            throw new ApiException.NotFound("The file is no longer available");
        }
    }

    @Transactional
    public void delete(Long attachmentId, AppUserPrincipal me) {
        Attachment a = attachments.findById(attachmentId).orElseThrow(() -> new ApiException.NotFound("Attachment not found"));
        WorkOrder w = access.loadVisible(a.getWorkOrder().getId(), me);
        boolean mine = a.getUploadedBy().getId().equals(me.id());
        if (!(me.role() == Role.MANAGER || mine)) throw new ApiException.Forbidden("Only the uploader or a manager can delete this file");
        if (w.getStatus().isTerminal()) throw new ApiException.Conflict("Files on a " + w.getStatus() + " work order cannot be changed");
        storageFor(a).delete(a);
        attachments.delete(a);
        log.info("Attachment {} deleted from {} by {}", a.getFileName(), w.getCode(), me.email());
    }

    // -------------------------------------------------------------- helpers

    private FileStorage byProvider(String provider) {
        return storages.stream().filter(s -> s.provider().equals(provider)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No storage provider " + provider));
    }

    private FileStorage storageFor(Attachment a) {
        return byProvider(a.getStorageProvider());
    }

    private FileStorage storageForUpload() {
        FileStorage cloud = byProvider("CLOUDINARY");
        String mode = configuredProvider == null ? "auto" : configuredProvider.trim().toLowerCase();
        if (mode.equals("local")) return byProvider("LOCAL");
        if (mode.equals("cloudinary")) {
            if (!cloud.isAvailable()) throw new ApiException.Conflict("Cloudinary storage is selected but not configured on the server");
            return cloud;
        }
        return cloud.isAvailable() ? cloud : byProvider("LOCAL");      // auto
    }

    private static String sanitize(String original) {
        String name = original == null ? "file" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (name.isEmpty()) name = "file";
        return name.length() > 150 ? name.substring(name.length() - 150) : name;
    }

    private AttachmentDto toDto(Attachment a, AppUserPrincipal viewer, String uploader) {
        boolean staff = viewer.role() != Role.CUSTOMER;
        String publicUrl = staff && "CLOUDINARY".equals(a.getStorageProvider()) ? a.getPublicUrl() : null;
        return new AttachmentDto(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(), uploader,
                a.getUploadedBy().getId(), a.getUploadedAt(), a.getStorageProvider(),
                "/api/attachments/" + a.getId() + "/view", "/api/attachments/" + a.getId() + "/download", publicUrl);
    }
}
