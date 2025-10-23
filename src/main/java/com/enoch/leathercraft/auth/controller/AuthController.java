package com.enoch.leathercraft.auth.controller;

import com.enoch.leathercraft.auth.dto.AuthRequest;
import com.enoch.leathercraft.auth.dto.AuthResponse;
import com.enoch.leathercraft.auth.dto.RegisterRequest;
import com.enoch.leathercraft.auth.service.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service){ this.service = service; }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req){
        return service.login(req);
    }
}
