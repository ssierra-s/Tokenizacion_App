package com.challenge.tokenizacion_app.repository;

import com.challenge.tokenizacion_app.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        select distinct o
        from Order o
        left join fetch o.items i
        left join fetch i.product p
        where o.id = :id
    """)
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
