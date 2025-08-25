package com.challenge.tokenizacion_app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderRequestDTO {

    @NotBlank
    private String cardToken;

    @Size(min = 1, message = "Debe incluir al menos un producto si envias products")
    private Map<@NotNull Long, @NotNull @Min(1) Integer> products;
}
