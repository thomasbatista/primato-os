package com.primatoos.backend.service;

import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.FileStorageException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Shared validation for the photo uploads accepted by Daily Reports, Projects and Work Orders —
 * all three take the same image formats under the same size cap.
 */
@Component
public class PhotoUploadSupport {

    private static final long MAX_PHOTO_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_PHOTO_EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    public String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Arquivo de foto é obrigatório");
        }

        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new BusinessRuleException("O arquivo excede o tamanho máximo permitido de 10MB");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String expectedContentType = ALLOWED_PHOTO_EXTENSIONS.get(extension);

        if (expectedContentType == null) {
            throw new BusinessRuleException("Tipo de arquivo não permitido. Envie uma imagem jpg, jpeg, png ou webp");
        }

        if (!expectedContentType.equalsIgnoreCase(file.getContentType())) {
            throw new BusinessRuleException("O tipo do arquivo não corresponde à extensão informada");
        }

        return extension;
    }

    public byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new FileStorageException("Erro ao ler o arquivo enviado", ex);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
