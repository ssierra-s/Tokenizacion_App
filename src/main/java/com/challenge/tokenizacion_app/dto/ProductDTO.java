package com.challenge.tokenizacion_app.dto;

import com.challenge.tokenizacion_app.model.entity.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDTO {

    private Long id;
    private String name;
    private BigDecimal price;  // <- antes Double, ahora BigDecimal
    private Integer stock;

    /* ---------- Helpers opcionales de mapeo ---------- */

    public static ProductDTO fromEntity(Product p) {
        if (p == null) return null;
        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .stock(p.getStock())
                .build();
    }

    public Product toEntity() {
        Product p = new Product();
        p.setId(this.id);        // opcional: normalmente no se envía en create
        p.setName(this.name);
        p.setPrice(this.price);
        p.setStock(this.stock);
        return p;
    }
}
