package com.challenge.tokenizacion_app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import com.challenge.tokenizacion_app.security.crypto.AesGcmStringCryptoConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"cvv", "expiryDate"}) // evita loggear datos sensibles
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token; // tokenizado

    @Column(nullable = false)
    private String maskedNumber; // **** **** **** 1234

    // ---- CAMPOS CIFRADOS ----
    @Column(nullable = false)
    @Convert(converter = AesGcmStringCryptoConverter.class)
    @JsonIgnore // no exponer en respuestas por accidente
    private String cvv;

    @Column(nullable = false)
    @Convert(converter = AesGcmStringCryptoConverter.class)
    @JsonIgnore // no exponer en respuestas por accidente
    private String expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}