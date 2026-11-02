package com.keystone.service.storage;

import com.keystone.domain.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    @Value("${keystone.storage.dir}")
    private String storageDir;

    @Override public String provider() { return "LOCAL"; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Stored store(Long workOrderId, MultipartFile file, String contentType) throws IOException {
        String key = workOrderId + "/" + UUID.randomUUID();      // never derived from the user's file name
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new Stored(key, null, null, null);
    }

    @Override
    public InputStream open(Attachment a) throws IOException {
        return Files.newInputStream(resolve(a.getStorageKey()));
    }

    @Override
    public void delete(Attachment a) {
        try {
            Files.deleteIfExists(resolve(a.getStorageKey()));
        } catch (IOException e) {
            log.warn("Could not delete local file {}: {}", a.getStorageKey(), e.getMessage());
        }
    }

    private Path resolve(String key) throws IOException {
        Path root = Paths.get(storageDir).toAbsolutePath().normalize();
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw new IOException("Invalid storage path");     // path-traversal guard
        return p;
    }
}
