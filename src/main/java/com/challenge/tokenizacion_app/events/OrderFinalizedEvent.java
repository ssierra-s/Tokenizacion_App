package com.challenge.tokenizacion_app.events;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderFinalizedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String status,
        int attempts,
        BigDecimal total
) implements Serializable {}
