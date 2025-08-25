package com.challenge.tokenizacion_app.controller;

import com.challenge.tokenizacion_app.dto.OrderDTO;
import com.challenge.tokenizacion_app.dto.OrderRequestDTO;
import com.challenge.tokenizacion_app.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestParam @NotNull Long userId,
                                                @RequestParam @NotBlank String address,
                                                @RequestBody(required = false) @Valid OrderRequestDTO request) {
        String cardToken = request != null ? request.getCardToken() : null;

        if (request == null || request.getProducts() == null || request.getProducts().isEmpty()) {
            if (cardToken == null || cardToken.isBlank()) {
                throw new IllegalArgumentException("cardToken es requerido");
            }
            return ResponseEntity.ok(orderService.createOrderFromCart(userId, cardToken, address));
        }
        return ResponseEntity.ok(orderService.createOrder(userId, cardToken, address, request.getProducts()));
    }
}
