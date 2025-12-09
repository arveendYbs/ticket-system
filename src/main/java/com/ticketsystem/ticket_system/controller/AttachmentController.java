package com.ticketsystem.ticket_system.controller;


import com.ticketsystem.ticket_system.model.Attachment;
import com.ticketsystem.ticket_system.model.User;
import com.ticketsystem.ticket_system.service.AttachmentService;
import com.ticketsystem.ticket_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final UserService userService;

    @PostMapping("/upload/{ticketId}")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }

            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Attachment attachment = attachmentService.saveAttachment(file, ticketId, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not upload file: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<Attachment>> getAttachmentsByTicket(@PathVariable Long ticketId) {
        List<Attachment> attachments = attachmentService.getAttachmentsByTicketId(ticketId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Attachment> getAttachment(@PathVariable Long id) {
        return attachmentService.getAttachmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            Attachment attachment = attachmentService.getAttachmentById(id)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            Path filePath = attachmentService.getAttachmentPath(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = attachment.getFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id) {
        try {
            attachmentService.deleteAttachment(id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
