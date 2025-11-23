package com.enoch.leathercraft.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // Utilisation de la variable d'environnement Spring pour le répertoire de base
    private final Path fileStorageLocation;

    // Initialisation du répertoire de stockage au démarrage du service
    public FileStorageService(@Value("${file.upload-dir:./uploads/products}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation); // Ceci devrait créer le dossier
            // LOG IMPORTANT : AJOUTEZ UN LOG POUR VOIR LE CHEMIN ABSOLU
            System.out.println("Chemin de stockage des fichiers: " + this.fileStorageLocation.toString());
        } catch (Exception ex) {
            // Erreur fatale si le répertoire ne peut être créé (permission, etc.)
            throw new RuntimeException("Impossible de créer le répertoire de stockage des fichiers. Vérifiez les permissions: " + this.fileStorageLocation, ex);
        }
    }

    /**
     * Stocke le fichier sur le système de fichiers avec un nom unique et retourne l'URL publique.
     * @param file Le fichier à stocker.
     * @return Le chemin public (ex: /uploads/products/UUID.jpg)
     */
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Génération d'un nom de fichier unique (UUID) pour éviter les collisions et les problèmes de sécurité.
        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (uniqueFileName.contains("..")) {
                throw new IOException("Le nom du fichier contient une séquence de chemin non valide.");
            }

            // Copie du fichier dans le répertoire de destination
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retourne l'URL publique que le frontend utilisera pour l'afficher
            return "/uploads/products/" + uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Impossible de stocker le fichier " + originalFileName, ex);
        }
    }
}