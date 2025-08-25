package com.challenge.tokenizacion_app.repository;


import com.challenge.tokenizacion_app.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Buscar productos con stock mayor a un valor dado (configurable)
    List<Product> findByStockGreaterThan(int stock);

    // Buscar productos por nombre que contengan texto ignorando mayúsculas/minúsculas
    List<Product> findByNameContainingIgnoreCase(String name);
}


