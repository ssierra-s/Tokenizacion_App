package com.challenge.tokenizacion_app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenizeCardRequest {

    @NotNull
    private Long userId;

    @NotBlank @Pattern(regexp = "^\\d{13,19}$", message = "El número debe tener 13-19 dígitos")
    private String number;

    @NotBlank @Pattern(regexp = "^\\d{3,4}$", message = "CVV inválido")
    private String cvv;

    @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Formato yyyy-MM")
    private String expiryDate;
}
