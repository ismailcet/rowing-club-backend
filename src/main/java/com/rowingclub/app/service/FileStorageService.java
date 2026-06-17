package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".jpg", ".jpeg", ".png");

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    public String storeReceipt(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Dosya boş olamaz", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );

        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    "Geçersiz dosya türü. İzin verilenler: PDF, JPG, PNG",
                    HttpStatus.BAD_REQUEST
            );
        }

        String filename = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Dekont kaydedildi: {}", filename);
            return filename;
        } catch (IOException e) {
            throw new BusinessException("Dosya kaydedilemedi", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadReceipt(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new BusinessException("Dosya bulunamadı", HttpStatus.NOT_FOUND);
        } catch (MalformedURLException e) {
            throw new BusinessException("Dosya yüklenemedi", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}