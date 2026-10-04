package com.atoma.marketplace.onboarding.service;

import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.config.OnboardingUploadProperties;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
import com.atoma.marketplace.onboarding.entity.OnboardingUploadedDocument;
import com.atoma.marketplace.onboarding.repository.OnboardingUploadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingDocumentUploadService {

    private final OnboardingUploadProperties uploadProperties;
    private final OnboardingUploadedDocumentRepository documentRepository;

    @Transactional
    public OnboardingApiDtos.UploadedDocumentResponse store(Merchant merchant, String kind, MultipartFile file) {
        validateKind(kind);
        if (file == null || file.isEmpty()) {
            throw validationField("file", "REQUIRED");
        }
        var contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !uploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw MarketplaceException.withDetails(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    ErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type",
                    Map.of("allowedContentTypes", uploadProperties.getAllowedContentTypes())
            );
        }
        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw MarketplaceException.withDetails(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    ErrorCodes.FILE_TOO_LARGE,
                    "File too large",
                    Map.of("maxFileSizeBytes", uploadProperties.getMaxFileSizeBytes())
            );
        }
        var storageKey = persistBytes(merchant.getId(), kind, file);
        var entity = documentRepository.save(OnboardingUploadedDocument.builder()
                .merchant(merchant)
                .kind(kind.trim().toUpperCase(Locale.ROOT))
                .fileName(safeFileName(file.getOriginalFilename()))
                .contentType(contentType)
                .fileSize(file.getSize())
                .storageKey(storageKey)
                .build());
        entity.setPreviewUrl("/api/v1/onboarding/documents/" + entity.getId() + "/preview");
        entity = documentRepository.save(entity);
        return OnboardingApiDtos.UploadedDocumentResponse.builder()
                .id(entity.getId())
                .kind(entity.getKind())
                .fileName(entity.getFileName())
                .uploadedAt(entity.getCreatedAt())
                .previewUrl(entity.getPreviewUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public void assertOwnedDocumentIds(Merchant merchant, java.util.List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        for (var id : documentIds) {
            if (!documentRepository.existsByIdAndMerchantId(id, merchant.getId())) {
                throw MarketplaceException.withDetails(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        ErrorCodes.VALIDATION_FAILED,
                        "Unknown document id",
                        Map.of("fields", Map.of("documentIds", "INVALID"))
                );
            }
        }
    }

    private void validateKind(String kind) {
        if (kind == null || kind.isBlank()) {
            throw validationField("kind", "REQUIRED");
        }
        var normalized = kind.trim().toUpperCase(Locale.ROOT);
        if (!uploadProperties.getAllowedKinds().contains(normalized)) {
            throw MarketplaceException.withDetails(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorCodes.VALIDATION_FAILED,
                    "Invalid document kind",
                    Map.of("fields", Map.of("kind", "INVALID"))
            );
        }
    }

    private static MarketplaceException validationField(String field, String code) {
        return MarketplaceException.withDetails(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCodes.VALIDATION_FAILED,
                "Validation failed",
                Map.of("fields", Map.of(field, code))
        );
    }

    private String persistBytes(UUID merchantId, String kind, MultipartFile file) {
        var root = Path.of(System.getProperty("java.io.tmpdir"), "atoma-uploads", merchantId.toString());
        try {
            Files.createDirectories(root);
            var key = kind.trim().toUpperCase(Locale.ROOT) + "-" + UUID.randomUUID();
            Files.write(root.resolve(key), file.getBytes());
            return key;
        } catch (IOException e) {
            throw MarketplaceException.of(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.INTERNAL_ERROR,
                    "Failed to store upload",
                    null,
                    null
            );
        }
    }

    private static String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "upload.bin";
        }
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String normalizeContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
