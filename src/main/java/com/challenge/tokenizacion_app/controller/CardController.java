package com.challenge.tokenizacion_app.controller;

import com.challenge.tokenizacion_app.dto.CardDTO;
import com.challenge.tokenizacion_app.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) { // ← Constructor explícito
        this.cardService = cardService;
    }

    @PostMapping("/tokenize")
    public ResponseEntity<CardDTO> tokenizeCard(@RequestParam Long userId,
                                                @Valid @RequestBody CardDTO cardDTO) {
        return ResponseEntity.ok(cardService.tokenizeCard(userId, cardDTO));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CardDTO>> getCardsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getCardsByUser(userId));
    }
}
