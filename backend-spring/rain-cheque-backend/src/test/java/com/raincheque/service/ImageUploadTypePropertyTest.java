package com.raincheque.service;

import com.raincheque.exception.InvalidFileTypeException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based test for Image upload type validation.
 * Validates: Requirements 5.1, 5.2
 */
// Feature: event-management, Property 9: Image upload type validation
class ImageUploadTypePropertyTest {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private ImageUploadService imageUploadService;
    private Path tempDir;

    @BeforeProperty
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("image-upload-test");
        imageUploadService = new ImageUploadService(tempDir.toString());
    }

    @Property(tries = 100)
    // Feature: event-management, Property 9: Image upload type validation
    void invalidContentTypeAlwaysRejected(
            @ForAll("invalidContentTypes") String contentType) throws IOException {

        MultipartFile mockFile = createMockFile(contentType, 1024, "test.bin");

        assertThatThrownBy(() -> imageUploadService.uploadImage(mockFile))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 9: Image upload type validation
    void validContentTypeWithValidSizeSucceeds(
            @ForAll("allowedContentTypes") String contentType,
            @ForAll("validFileSizes") long fileSize) throws IOException {

        String extension = extensionForContentType(contentType);
        MultipartFile mockFile = createMockFile(contentType, fileSize, "image." + extension);

        String result = imageUploadService.uploadImage(mockFile);

        assertThat(result)
                .isNotNull()
                .startsWith("/uploads/events/")
                .endsWith("." + extension);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> invalidContentTypes() {
        // Generate content types that are NOT in the allowed set
        return Arbitraries.oneOf(
                // Common non-image MIME types
                Arbitraries.of(
                        "application/pdf",
                        "text/plain",
                        "text/html",
                        "application/json",
                        "application/xml",
                        "image/gif",
                        "image/bmp",
                        "image/tiff",
                        "image/svg+xml",
                        "video/mp4",
                        "audio/mpeg",
                        "application/octet-stream",
                        "application/zip",
                        "text/csv"
                ),
                // Random arbitrary strings that aren't valid image types
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(1)
                        .ofMaxLength(50)
                        .map(s -> "application/" + s)
                        .filter(ct -> !ALLOWED_CONTENT_TYPES.contains(ct))
        );
    }

    @Provide
    Arbitrary<String> allowedContentTypes() {
        return Arbitraries.of("image/jpeg", "image/png", "image/webp");
    }

    @Provide
    Arbitrary<Long> validFileSizes() {
        // Between 1 byte and 5 MB (inclusive)
        return Arbitraries.longs().between(1L, MAX_FILE_SIZE);
    }

    // --- Helpers ---

    private MultipartFile createMockFile(String contentType, long size, String filename) throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn(contentType);
        when(mockFile.getSize()).thenReturn(size);
        when(mockFile.getOriginalFilename()).thenReturn(filename);

        // Create a minimal byte array (we don't need actual image bytes for validation)
        byte[] content = new byte[(int) Math.min(size, 1024)];
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        return mockFile;
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }
}
