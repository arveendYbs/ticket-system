package com.ticketsystem.ticket_system.service;

import com.ticketsystem.ticket_system.model.Attachment;
import com.ticketsystem.ticket_system.model.Ticket;
import com.ticketsystem.ticket_system.model.User;
import com.ticketsystem.ticket_system.repository.AttachmentRepository;
import com.ticketsystem.ticket_system.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public Attachment saveAttachment(MultipartFile file, Long ticketId, User uploadedBy) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString() + fileExtension;

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Attachment attachment = new Attachment();
        attachment.setFileName(originalFileName);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFilePath(fileName);
        attachment.setTicket(ticket);
        attachment.setUploadedBy(uploadedBy);

        return attachmentRepository.save(attachment);
    }

    public List<Attachment> getAttachmentsByTicketId(Long ticketId) {
        return attachmentRepository.findByTicketId(ticketId);
    }

    public Optional<Attachment> getAttachmentById(Long id) {
        return attachmentRepository.findById(id);
    }

    public Path getAttachmentPath(String fileName) {
        return Paths.get(uploadDir).resolve(fileName);
    }

    @Transactional
    public void deleteAttachment(Long id) throws IOException {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + id));

        Path filePath = getAttachmentPath(attachment.getFilePath());
        Files.deleteIfExists(filePath);

        attachmentRepository.delete(attachment);
    }
}
