package com.challenge.tokenizacion_app.repository;

import com.challenge.tokenizacion_app.model.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserId(Long userId);
    Optional<Card> findByToken(String token);
}

