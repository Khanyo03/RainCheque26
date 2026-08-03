package com.raincheque.dto;

/**
 * Response DTO for image upload operations.
 */
public record ImageUploadResponse(
        String url,
        String filename,
        String target
) {
}
