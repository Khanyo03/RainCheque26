package com.raincheque.service;

import com.raincheque.exception.FileSizeLimitExceededException;
import com.raincheque.exception.FileStorageException;
import com.raincheque.exception.InvalidFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ImageUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final Path uploadDir;

    public ImageUploadService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).resolve("events");
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    /**
     * Uploads an image file after validating content type and size.
     *
     * @param file the multipart file to upload
     * @return the relative URL path to the stored file (e.g., "/uploads/events/{uuid}.jpg")
     */
    public String uploadImage(MultipartFile file) {
        validateContentType(file);
        validateFileSize(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + "." + extension;

        Path targetPath = uploadDir.resolve(uniqueFilename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Image uploaded successfully: {}", uniqueFilename);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + uniqueFilename, e);
        }

        return "/uploads/events/" + uniqueFilename;
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    "Invalid file type: " + contentType + ". Allowed types: " + ALLOWED_CONTENT_TYPES);
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeLimitExceededException(
                    "File size " + file.getSize() + " bytes exceeds maximum allowed size of 5 MB");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
