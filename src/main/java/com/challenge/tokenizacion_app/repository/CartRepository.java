package com.challenge.tokenizacion_app.repository;

import com.challenge.tokenizacion_app.model.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    @Query("""
        select distinct c
        from Cart c
        left join fetch c.items i
        left join fetch i.product p
        where c.user.id = :userId
    """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
}
