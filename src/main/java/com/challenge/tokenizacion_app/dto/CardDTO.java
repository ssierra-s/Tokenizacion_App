package com.challenge.tokenizacion_app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class CardDTO {
    private Long id;

    // Entrada: número de tarjeta (no persiste, no se devuelve)
    @NotBlank(message = "El número de tarjeta es requerido")
    @Pattern(regexp = "^[0-9]{12,19}$", message = "Número de tarjeta inválido")
    @JsonProperty(access = WRITE_ONLY)
    private String number;

    // Entrada: CVV (3 o 4 dígitos)
    @NotBlank(message = "El CVV es requerido")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV inválido")
    @JsonProperty(access = WRITE_ONLY)
    private String cvv;

    // Entrada: expiración "MM/YY" o "YYYY-MM" (validación fina se hace en el servicio)
    @NotBlank(message = "La fecha de expiración es requerida")
    @Pattern(
            regexp = "^(\\d{2}/\\d{2}|\\d{4}-(0[1-9]|1[0-2]))$",
            message = "Fecha de expiración inválida (use MM/YY o YYYY-MM)"
    )
    @JsonProperty(access = WRITE_ONLY)
    private String expiryDate;

    // Salida: estos no deben venir en el request
    @JsonProperty(access = READ_ONLY)
    private String token;

    @JsonProperty(access = READ_ONLY)
    private String maskedNumber;
}
