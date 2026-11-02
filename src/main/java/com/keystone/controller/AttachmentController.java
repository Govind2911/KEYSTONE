package com.keystone.controller;

import com.keystone.dto.CommonDtos.AttachmentDto;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Attachments")
public class AttachmentController {
    private final AttachmentService service;

    @PostMapping(value = "/work-orders/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a photo or document",
            description = "multipart field 'file'. Images, PDF or plain text, max 10 MB. Stored on Cloudinary when configured, otherwise on the server disk.")
    public AttachmentDto upload(@PathVariable Long id, @RequestPart("file") MultipartFile file,
                                @AuthenticationPrincipal AppUserPrincipal me) {
        return service.upload(id, file, me);
    }

    @GetMapping("/work-orders/{id}/attachments")
    @Operation(summary = "List a job's attachments", description = "Each entry has url (inline stream), downloadUrl and, for staff on Cloudinary files, publicUrl.")
    public List<AttachmentDto> list(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.list(id, me);
    }

    @GetMapping("/attachments/{id}/view")
    @Operation(summary = "Stream an attachment inline", description = "For previews (images, PDF). Same visibility rules as the parent work order.")
    public ResponseEntity<InputStreamResource> view(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(service.open(id, me), true);
    }

    @GetMapping("/attachments/{id}/download")
    @Operation(summary = "Download an attachment", description = "Streams the file with a Content-Disposition: attachment header.")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(service.open(id, me), false);
    }

    @DeleteMapping("/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an attachment", description = "Uploader or manager only; removes the file from Cloudinary / disk as well.")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        service.delete(id, me);
    }

    private ResponseEntity<InputStreamResource> respond(AttachmentService.Download d, boolean inline) {
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(d.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(new InputStreamResource(d.stream()));
    }
}
