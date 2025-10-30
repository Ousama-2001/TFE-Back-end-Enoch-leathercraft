// src/main/java/com/enoch/leathercraft/web/PageController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Sélection Hiver");
        model.addAttribute("products", productService.getAll()); // liste pour un carrousel, etc.
        return "index"; // templates/index.ftl
    }

    @GetMapping("/produits")
    public String produits(Model model) {
        model.addAttribute("products", productService.getAll());
        return "produits/list"; // templates/produits/list.ftl
    }
}
