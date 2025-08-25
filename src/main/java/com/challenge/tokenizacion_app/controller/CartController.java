package com.challenge.tokenizacion_app.controller;

import com.challenge.tokenizacion_app.dto.CartDTO;
import com.challenge.tokenizacion_app.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // Agregar producto al carrito
    @PostMapping("/add")
    public ResponseEntity<CartDTO> addToCart(@RequestParam Long userId,
                                             @RequestParam Long productId,
                                             @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.addToCart(userId, productId, quantity));
    }

    // Ver carrito de un usuario
    @GetMapping("/{userId}")
    public ResponseEntity<CartDTO> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }
}
