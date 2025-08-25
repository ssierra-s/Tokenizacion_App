package com.challenge.tokenizacion_app.service;

import com.challenge.tokenizacion_app.dto.CardDTO;
import com.challenge.tokenizacion_app.model.entity.Card;
import com.challenge.tokenizacion_app.model.entity.User;
import com.challenge.tokenizacion_app.repository.CardRepository;
import com.challenge.tokenizacion_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final LogEventService logEventService;

    /**
     * Probabilidad de rechazo configurable (0..1).
     * Se puede setear por env var TOKEN_REJECTION_PROB o en application.yml.
     */
    private double rejectionProbability;

    @Value("${tokenization.rejection-probability:0.20}")
    public void setRejectionProbability(double p) {
        // Clamp: evitamos valores inválidos
        if (Double.isNaN(p)) p = 0.20;
        this.rejectionProbability = Math.max(0.0, Math.min(1.0, p));
    }

    @Transactional
    public CardDTO tokenizeCard(Long userId, CardDTO cardDTO) {
        // 1) Validaciones básicas (no exponemos datos sensibles en logs)
        if (userId == null) {
            throw new IllegalArgumentException("userId es requerido");
        }
        validateCardInput(cardDTO);

        // 2) Probabilidad de rechazo (configurable)
        if (ThreadLocalRandom.current().nextDouble() < rejectionProbability) {
            logEventService.log(
                    "CARD_TOKENIZATION_REJECTED",
                    "Tokenización rechazada para user " + userId,
                    userId
            );
            throw new RuntimeException("Tokenización rechazada por probabilidad configurada");
        }

        // 3) Usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 4) Token único
        String token = UUID.randomUUID().toString();

        // 5) Enmascarado (solo guardamos el enmascarado, nunca el número en claro)
        String maskedNumber = maskCardNumber(cardDTO.getNumber());

        // 6) Persistencia
        //    IMPORTANTE: gracias al @Converter AES-GCM en la entidad,
        //    cvv y expiryDate se cifran/descifran automáticamente.
        Card card = Card.builder()
                .token(token)
                .maskedNumber(maskedNumber)
                .cvv(cardDTO.getCvv())                 // <- cifrado por JPA Converter
                .expiryDate(normalizeExpiry(cardDTO))  // <- cifrado por JPA Converter
                .user(user)
                .build();

        cardRepository.save(card);

        // 7) Log transaccional (sin datos sensibles)
        logEventService.log(
                "CARD_TOKENIZED",
                "Token creado exitosamente para user " + userId,
                userId
        );

        // 8) Respuesta (nunca regreses datos sensibles)
        return CardDTO.builder()
                .id(card.getId())
                .token(card.getToken())
                .maskedNumber(card.getMaskedNumber())
                .build();
    }

    public List<CardDTO> getCardsByUser(Long userId) {
        return cardRepository.findByUserId(userId).stream()
                .map(card -> CardDTO.builder()
                        .id(card.getId())
                        .token(card.getToken())
                        .maskedNumber(card.getMaskedNumber())
                        .build())
                .toList();
    }

    // -------------------- Helpers --------------------

    private void validateCardInput(CardDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Datos de tarjeta requeridos");

        // Número (mín 12-13, máx 19) – aquí solo validación básica de longitud y dígitos
        String number = safe(dto.getNumber());
        if (number.length() < 12 || number.length() > 19 || !number.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Número de tarjeta inválido");
        }

        // CVV (3 o 4 dígitos)
        String cvv = safe(dto.getCvv());
        if (!(cvv.length() == 3 || cvv.length() == 4) || !cvv.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("CVV inválido");
        }

        // Expiración aceptando "MM/YY" o "YYYY-MM"
        // Normalizamos luego a "YYYY-MM"
        String expiry = safe(dto.getExpiryDate());
        if (!isValidExpiry(expiry)) {
            throw new IllegalArgumentException("Fecha de expiración inválida");
        }
    }

    private String maskCardNumber(String number) {
        String n = safe(number);
        if (n.length() < 4) return "****";
        return "**** **** **** " + n.substring(n.length() - 4);
    }

    /**
     * Normaliza la expiración a "YYYY-MM" para almacenarla (y luego cifrarla).
     * Acepta entrada "MM/YY" (p.ej. 09/28) o "YYYY-MM" (p.ej. 2028-09).
     */
    private String normalizeExpiry(CardDTO dto) {
        String raw = safe(dto.getExpiryDate());
        // "YYYY-MM" directo válido
        if (raw.matches("^\\d{4}-\\d{2}$")) {
            // Validamos que sea un YearMonth real
            try {
                YearMonth.parse(raw);
                return raw;
            } catch (DateTimeParseException ignored) {
                throw new IllegalArgumentException("Fecha de expiración inválida");
            }
        }
        // "MM/YY" -> "YYYY-MM"
        if (raw.matches("^\\d{2}/\\d{2}$")) {
            String[] parts = raw.split("/");
            int mm = Integer.parseInt(parts[0]);
            int yy = Integer.parseInt(parts[1]); // 00..99
            if (mm < 1 || mm > 12) throw new IllegalArgumentException("Mes de expiración inválido");
            // Convención: 2000-2099 para YY
            int yyyy = 2000 + yy;
            String normalized = String.format("%04d-%02d", yyyy, mm);
            try {
                YearMonth.parse(normalized);
                return normalized;
            } catch (DateTimeParseException ignored) {
                throw new IllegalArgumentException("Fecha de expiración inválida");
            }
        }
        throw new IllegalArgumentException("Formato de expiración no soportado (use MM/YY o YYYY-MM)");
    }

    private boolean isValidExpiry(String raw) {
        if (raw == null) return false;
        if (raw.matches("^\\d{4}-\\d{2}$")) {
            try {
                YearMonth.parse(raw);
                return true;
            } catch (DateTimeParseException ignored) {
                return false;
            }
        }
        if (raw.matches("^\\d{2}/\\d{2}$")) {
            String[] p = raw.split("/");
            try {
                int mm = Integer.parseInt(p[0]);
                int yy = Integer.parseInt(p[1]);
                if (mm < 1 || mm > 12) return false;
                int yyyy = 2000 + yy;
                YearMonth.parse(String.format("%04d-%02d", yyyy, mm));
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
