package com.challenge.tokenizacion_app.controller;

import com.challenge.tokenizacion_app.model.entity.LogEvent;
import com.challenge.tokenizacion_app.repository.LogEventRepository;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogEventController {

    private final LogEventRepository logEventRepository;

    @GetMapping
    public List<LogEvent> getAllLogs() {
        return logEventRepository.findAll();
    }
}

