package com.challenge.tokenizacion_app.service;

import com.challenge.tokenizacion_app.model.entity.LogEvent;
import com.challenge.tokenizacion_app.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogEventService {

    private final LogEventRepository repository;

    public void log(String eventType, String message, Long userId) {
        LogEvent log = LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .eventType(eventType)
                .message(message)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        repository.save(log);
    }
}

