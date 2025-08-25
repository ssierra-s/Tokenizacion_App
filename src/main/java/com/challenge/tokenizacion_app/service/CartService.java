package com.challenge.tokenizacion_app.service;

import com.challenge.tokenizacion_app.dto.CartDTO;
import com.challenge.tokenizacion_app.model.entity.Cart;
import com.challenge.tokenizacion_app.model.entity.CartItem;
import com.challenge.tokenizacion_app.model.entity.Product;
import com.challenge.tokenizacion_app.model.entity.User;
import com.challenge.tokenizacion_app.repository.CartRepository;
import com.challenge.tokenizacion_app.repository.ProductRepository;
import com.challenge.tokenizacion_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CartService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    @Transactional
    public CartDTO addToCart(Long userId, Long productId, Integer quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser > 0");
        }
        if (product.getStock() < quantity) {
            throw new RuntimeException("Stock insuficiente para el producto: " + product.getName());
        }

        // 🔁 trae el carrito con items y producto inicializados
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElse(Cart.builder().user(user).items(new ArrayList<>()).build());

        // Buscar si el producto ya está en el carrito
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = CartItem.builder()
                            .product(product)
                            .quantity(0)
                            .cart(cart)
                            .build();
                    cart.getItems().add(newItem);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + quantity);

        Cart saved = cartRepository.saveAndFlush(cart);
        return CartDTO.fromEntity(saved); // DTO se construye dentro de la tx, con colecciones cargadas
    }

    @Transactional(readOnly = true)
    public CartDTO getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new RuntimeException("Carrito vacío o no encontrado"));
        return CartDTO.fromEntity(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new RuntimeException("Carrito vacío o no encontrado"));
        cart.getItems().clear();       // requiere orphanRemoval = true en Cart.items
        cartRepository.save(cart);
    }
}
