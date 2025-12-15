package com.enoch.leathercraft.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageDir;

    public FileStorageService(
            @Value("${file.upload-dir:./uploads/products}") String uploadDir
    ) {
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer le dossier uploads", e);
        }
    }

    public String storeFile(MultipartFile file) {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext != null ? "." + ext : "");

        try {
            Path target = storageDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Erreur upload fichier", e);
        }
    }

    public List<String> storeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            urls.add(storeFile(f));
        }
        return urls;
    }

    // ✅ suppression physique du fichier (si url = /uploads/products/xxx.jpg)
    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) return;

        try {
            String filename = Paths.get(url).getFileName().toString(); // xxx.jpg
            Path target = storageDir.resolve(filename).normalize();

            // sécurité: empêche de sortir du dossier
            if (!target.startsWith(storageDir)) return;

            Files.deleteIfExists(target);
        } catch (Exception ignored) {
            // on ignore pour ne pas casser la suppression DB
        }
    }
}
