package com.challenge.tokenizacion_app.service;

import com.challenge.tokenizacion_app.dto.CartDTO;
import com.challenge.tokenizacion_app.dto.CartItemDTO;
import com.challenge.tokenizacion_app.dto.OrderDTO;
import com.challenge.tokenizacion_app.events.OrderFinalizedEvent;
import com.challenge.tokenizacion_app.model.entity.*;
import com.challenge.tokenizacion_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final ProductRepository productRepository;
    private final LogEventService logEventService;
    private final CartService cartService;
    private final ApplicationEventPublisher events;

    @Value("${payment.rejection-probability:0.3}")
    private double rejectionProb;

    @Value("${payment.max-attempts:3}")
    private int maxAttempts;

    /**
     * Crea la orden tomando los ítems del carrito del usuario.
     */
    @Transactional
    public OrderDTO createOrderFromCart(Long userId, String cardToken, String address) {
        CartDTO cart = cartService.getCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }

        // productId -> quantity
        Map<Long, Integer> productsMap = cart.getItems().stream()
                .collect(Collectors.toMap(
                        CartItemDTO::getProductId,
                        CartItemDTO::getQuantity,
                        Integer::sum
                ));

        OrderDTO dto = createOrder(userId, cardToken, address, productsMap);

        if ("APPROVED".equalsIgnoreCase(dto.getStatus())) {
            cartService.clearCart(userId);
        }
        return dto;
    }

    /**
     * Crea la orden a partir de un mapa productId -> quantity.
     * Verifica stock, intenta pago y SOLO descuenta stock si aprueba.
     */
    @Transactional
    public OrderDTO createOrder(Long userId, String cardToken, String address, Map<Long, Integer> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("No se enviaron productos.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Card card = cardRepository.findByToken(cardToken)
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada"));

        // La tarjeta debe pertenecer al usuario
        if (card.getUser() == null || !Objects.equals(card.getUser().getId(), userId)) {
            throw new RuntimeException("La tarjeta no pertenece al usuario.");
        }

        // 1) Traer productos y chequear stock
        Map<Long, Product> productById = new HashMap<>();
        for (Map.Entry<Long, Integer> e : products.entrySet()) {
            Long productId = e.getKey();
            Integer quantity = e.getValue();
            if (quantity == null || quantity <= 0) {
                throw new RuntimeException("Cantidad inválida para productId=" + productId);
            }
            Product p = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productId));
            if (p.getStock() < quantity) {
                throw new RuntimeException("Stock insuficiente para " + p.getName());
            }
            productById.put(productId, p);
        }

        // 2) Armar Order PENDING con snapshot de precios
        Order order = Order.builder()
                .user(user)
                .card(card)
                .status("PENDING")
                .deliveryAddress(address)
                .attempts(0)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : products.entrySet()) {
            Product p = productById.get(e.getKey());
            int qty = e.getValue();

            BigDecimal unitPrice = p.getPrice(); // BigDecimal en Product
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .build();

            order.getItems().add(item);
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }
        order.setTotal(total);

        // 3) Intentos de pago
        for (int i = 1; i <= maxAttempts; i++) {
            order.setAttempts(i);
            if (ThreadLocalRandom.current().nextDouble() >= rejectionProb) {
                order.setStatus("APPROVED");
                break;
            }
        }
        if (!"APPROVED".equals(order.getStatus())) {
            order.setStatus("REJECTED");
        }

        // 4) Si aprueba, recién ahí descuenta stock
        if ("APPROVED".equals(order.getStatus())) {
            for (OrderItem it : order.getItems()) {
                Product p = it.getProduct();
                int remaining = p.getStock() - it.getQuantity();
                if (remaining < 0) {
                    throw new RuntimeException("Stock insuficiente al confirmar: " + p.getName());
                }
                p.setStock(remaining);
                productRepository.save(p);
            }
        }

        // 5) Persistir y loguear
        orderRepository.save(order); // cascade guarda items
        logEventService.log("ORDER_" + order.getStatus(),
                "Orden creada para user " + userId + " total=" + total, userId);

        // 6) Publicar evento para envío de correo AFTER_COMMIT (asíncrono)
        events.publishEvent(new OrderFinalizedEvent(
                order.getId(),
                userId,
                user.getEmail(),
                order.getStatus(),
                order.getAttempts(),
                order.getTotal()
        ));

        return OrderDTO.builder()
                .id(order.getId())
                .status(order.getStatus())
                .attempts(order.getAttempts())
                .build();
    }
}
