package com.raincheque.service;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for unique filename generation.
 * Validates: Requirements 5.4
 */
// Feature: event-management, Property 10: Unique filename generation
class ImageUploadServiceFilenamePropertyTest {

    private Path tempDir;
    private ImageUploadService imageUploadService;

    @BeforeTry
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("image-upload-test");
        imageUploadService = new ImageUploadService(tempDir.toString());
    }

    @Property(tries = 100)
    // Feature: event-management, Property 10: Unique filename generation
    void allUploadedFilesHaveDistinctFilenames(
            @ForAll("uploadCount") int n) throws IOException {

        Set<String> generatedUrls = new HashSet<>();

        for (int i = 0; i < n; i++) {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "photo.jpg",
                    "image/jpeg",
                    new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0} // minimal JPEG header
            );

            String url = imageUploadService.uploadImage(file);
            generatedUrls.add(url);
        }

        assertThat(generatedUrls)
                .as("All %d uploads with identical original filename should produce distinct stored filenames", n)
                .hasSize(n);

        // Also verify the files actually exist on disk (no collisions in storage)
        Path eventsDir = tempDir.resolve("events");
        long fileCount = Files.list(eventsDir).count();
        assertThat(fileCount)
                .as("The number of files on disk should equal the number of uploads (no overwrites)")
                .isEqualTo(n);
    }

    // --- Generators ---

    @Provide
    Arbitrary<Integer> uploadCount() {
        // Generate counts between 2 and 20 to test collision resistance
        // The @Property(tries=100) ensures sufficient randomized coverage
        return Arbitraries.integers().between(2, 20);
    }
}
