package com.challenge.tokenizacion_app.dto;
import com.challenge.tokenizacion_app.model.entity.Cart;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor @Getter @Setter
public class CartDTO {
    private Long id;
    private Long userId;
    private List<CartItemDTO> items;

    public static CartDTO fromEntity(Cart cart) {
        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(cart.getItems().stream()
                        .map(CartItemDTO::fromEntity)
                        .toList())
                .build();
    }
}

