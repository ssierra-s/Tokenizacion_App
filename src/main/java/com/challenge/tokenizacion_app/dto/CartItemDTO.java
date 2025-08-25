package com.challenge.tokenizacion_app.dto;

import com.challenge.tokenizacion_app.model.entity.CartItem;
import com.challenge.tokenizacion_app.model.entity.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemDTO {
    @NotNull
    private Long productId;

    private String productName;

    @NotNull @Min(1)
    private Integer quantity;

    public static CartItemDTO fromEntity(CartItem item) {
        if (item == null) return null;
        Product p = item.getProduct();
        return CartItemDTO.builder()
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .quantity(item.getQuantity())
                .build();
    }
}
