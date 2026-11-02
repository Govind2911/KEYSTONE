package com.keystone.service.storage;

import com.keystone.domain.Attachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** Where attachment bytes live. Each stored file remembers its provider, so switching providers never orphans old files. */
public interface FileStorage {
    record Stored(String key, String publicUrl, String cloudPublicId, String resourceType) {}

    String provider();

    boolean isAvailable();

    Stored store(Long workOrderId, MultipartFile file, String contentType) throws IOException;

    /** Streams the stored bytes. The caller must close the stream. */
    InputStream open(Attachment attachment) throws IOException;

    void delete(Attachment attachment);
}
