package com.keystone.service.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.domain.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cloudinary storage over its plain REST API (signed uploads), so no extra SDK is needed.
 * Configure with CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME  (or the three separate variables).
 * Reads are streamed through the back end, so KEYSTONE's access rules still apply to every download.
 */
@Component
public class CloudinaryFileStorage implements FileStorage {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryFileStorage.class);

    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;

    public CloudinaryFileStorage(ObjectMapper mapper,
                                 @Value("${keystone.cloudinary.url:}") String url,
                                 @Value("${keystone.cloudinary.cloud-name:}") String cloud,
                                 @Value("${keystone.cloudinary.api-key:}") String key,
                                 @Value("${keystone.cloudinary.api-secret:}") String secret,
                                 @Value("${keystone.cloudinary.folder:keystone}") String folder) {
        String c = cloud, k = key, s = secret;
        if (url != null && !url.isBlank()) {
            try {
                URI u = URI.create(url.trim());
                String info = u.getUserInfo();
                if (info != null && info.contains(":")) {
                    k = info.substring(0, info.indexOf(':'));
                    s = info.substring(info.indexOf(':') + 1);
                }
                if (u.getHost() != null) c = u.getHost();
            } catch (IllegalArgumentException e) {
                log.warn("CLOUDINARY_URL is not a valid URL; Cloudinary storage disabled");
            }
        }
        this.mapper = mapper;
        this.cloudName = c;
        this.apiKey = k;
        this.apiSecret = s;
        this.folder = folder;
    }

    @Override public String provider() { return "CLOUDINARY"; }

    @Override
    public boolean isAvailable() {
        return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
    }

    @Override
    public Stored store(Long workOrderId, MultipartFile file, String contentType) throws IOException {
        requireConfigured();
        long ts = Instant.now().getEpochSecond();
        String folderPath = folder + "/wo-" + workOrderId;
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("api_key", apiKey);
        fields.put("timestamp", String.valueOf(ts));
        fields.put("folder", folderPath);
        fields.put("signature", sha1("folder=" + folderPath + "&timestamp=" + ts + apiSecret));   // params sorted A-Z + secret

        String boundary = "----keystone" + UUID.randomUUID();
        byte[] body = multipart(boundary, fields, file.getBytes(), "file", file.getOriginalFilename() == null ? "file" : file.getOriginalFilename().replace("\"", ""), contentType);
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        HttpResponse<String> res = send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            log.error("Cloudinary upload failed: HTTP {} {}", res.statusCode(), res.body());
            throw new IOException("Cloudinary upload failed (HTTP " + res.statusCode() + ")");
        }
        JsonNode json = mapper.readTree(res.body());
        String publicId = json.path("public_id").asText();
        String secureUrl = json.path("secure_url").asText();
        if (publicId.isBlank() || secureUrl.isBlank()) throw new IOException("Unexpected Cloudinary response");
        return new Stored(publicId, secureUrl, publicId, json.path("resource_type").asText("image"));
    }

    @Override
    public InputStream open(Attachment a) throws IOException {
        if (a.getPublicUrl() == null) throw new IOException("Attachment has no Cloudinary URL");
        HttpRequest req = HttpRequest.newBuilder(URI.create(a.getPublicUrl())).timeout(Duration.ofSeconds(60)).GET().build();
        HttpResponse<InputStream> res = send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) {
            res.body().close();
            throw new IOException("Cloudinary returned HTTP " + res.statusCode());
        }
        return res.body();
    }

    @Override
    public void delete(Attachment a) {
        if (!isAvailable() || a.getCloudPublicId() == null) return;
        try {
            long ts = Instant.now().getEpochSecond();
            String form = "public_id=" + enc(a.getCloudPublicId()) + "&timestamp=" + ts + "&api_key=" + enc(apiKey)
                    + "&signature=" + sha1("public_id=" + a.getCloudPublicId() + "&timestamp=" + ts + apiSecret);
            String type = a.getResourceType() == null ? "image" : a.getResourceType();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/" + type + "/destroy"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            HttpResponse<String> res = send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) log.warn("Cloudinary delete failed: HTTP {} {}", res.statusCode(), res.body());
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for {}: {}", a.getCloudPublicId(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------ helpers

    private <T> HttpResponse<T> send(HttpRequest req, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return http.send(req, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Cloudinary request interrupted", e);
        }
    }

    private void requireConfigured() throws IOException {
        if (!isAvailable()) throw new IOException("Cloudinary is not configured");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String sha1(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] multipart(String boundary, Map<String, String> fields, byte[] data, String name, String filename, String contentType) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n" + e.getValue() + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename
                + "\"\r\nContent-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
