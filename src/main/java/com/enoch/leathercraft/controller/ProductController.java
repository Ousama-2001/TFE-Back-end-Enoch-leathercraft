package com.enoch.leathercraft.controller;// ProductController.java

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.dto.ProductUpdateRequest;
import com.enoch.leathercraft.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public Page<ProductResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        return service.search(q, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id){ return service.findById(id); }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest req){
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){ service.delete(id); }
}
