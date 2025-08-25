package com.challenge.tokenizacion_app.service;

import com.challenge.tokenizacion_app.dto.ProductDTO;
import com.challenge.tokenizacion_app.model.entity.Product;
import com.challenge.tokenizacion_app.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    @Value("${business.min-stock-visible:1}")
    private int minStockVisible;
    private final ProductRepository productRepository;


    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = Product.builder()
                .name(productDTO.getName())
                .price(productDTO.getPrice())
                .stock(productDTO.getStock())
                .build();

        productRepository.save(product);

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }

    public List<ProductDTO> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .filter(p -> p.getStock() > 0) // parámetro configurable
                .map(p -> ProductDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .price(p.getPrice())
                        .stock(p.getStock())
                        .build())
                .toList();
    }

    public List<ProductDTO> listAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(p -> p.getStock() != null && p.getStock() >= minStockVisible)
                .map(p -> ProductDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .price(p.getPrice())
                        .stock(p.getStock())
                        .build())
                .toList();
    }
}

