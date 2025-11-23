package com.enoch.leathercraft.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Récupérer le chemin absolu du dossier uploads
        Path uploadDir = Paths.get("./uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // 2. Afficher ce chemin dans la console pour être SÛR (Regardez ce log au démarrage !)
        System.out.println("----------------------------------------------------------");
        System.out.println("📂 CONFIGURATION DES IMAGES : ");
        System.out.println("   URL Mappée   : /uploads/**");
        System.out.println("   Dossier Phys : " + uploadPath);
        System.out.println("----------------------------------------------------------");

        // 3. Configurer le mappage
        // L'URL /uploads/** pointe vers le système de fichier
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}