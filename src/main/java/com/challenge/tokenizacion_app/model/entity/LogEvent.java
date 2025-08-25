package com.challenge.tokenizacion_app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LogEvent {

    @Id
    private String id; // UUID en lugar de Long

    private String eventType;
    private String message;
    private Long userId;
    private LocalDateTime timestamp;
}


