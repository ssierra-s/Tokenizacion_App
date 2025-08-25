package com.challenge.tokenizacion_app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Precio como snapshot monetario (2 decimales). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /** Stock no negativo. */
    @Column(nullable = false)
    private Integer stock;

    /** Optimistic locking para evitar pisadas de stock en concurrencia. */
    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    private void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser >= 0.");
        }
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("El stock debe ser >= 0.");
        }
    }

    /** Helper opcional para ajustar stock con validación. */
    public void decreaseStock(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("La cantidad a descontar debe ser > 0.");
        int remaining = this.stock - qty;
        if (remaining < 0) throw new IllegalStateException("Stock insuficiente.");
        this.stock = remaining;
    }
}
