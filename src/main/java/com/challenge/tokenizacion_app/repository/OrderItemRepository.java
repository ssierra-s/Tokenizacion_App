package com.challenge.tokenizacion_app.repository;


import com.challenge.tokenizacion_app.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Listar ítems de una orden
    List<OrderItem> findByOrderId(Long orderId);
}

