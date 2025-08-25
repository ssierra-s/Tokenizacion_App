package com.challenge.tokenizacion_app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"user", "card", "items"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;
    private String deliveryAddress;
    private int attempts;

    /** Total de la orden (suma de quantity * unitPrice). */
    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /* ----------------- Helpers y consistencia ----------------- */

    /** Recalcula el total en base a los items (unitPrice * quantity). */
    public void recalcTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (OrderItem it : items) {
                if (it == null) continue;
                BigDecimal unit = it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO;
                int qty = it.getQuantity();
                sum = sum.add(unit.multiply(BigDecimal.valueOf(qty)));
            }
        }
        this.total = sum;
    }

    /** Agrega un item vinculándolo a la orden y actualiza el total. */
    public void addItem(OrderItem item) {
        if (item == null) return;
        item.setOrder(this);
        this.items.add(item);
        recalcTotal();
    }

    @PrePersist
    @PreUpdate
    private void computeTotalOnPersist() {
        if (this.items == null) this.items = new ArrayList<>();
        recalcTotal();
    }
}
