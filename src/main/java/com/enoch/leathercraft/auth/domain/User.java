package com.enoch.leathercraft.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "users")
@Getter @Setter
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String firstName;
    private String lastName;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "username", unique = true, length = 50)
    private String username;   // nullable si tu veux, sinon ajoute nullable=false

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.CUSTOMER;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    @PrePersist void onCreate(){ createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void onUpdate(){ updatedAt = Instant.now(); }
}
