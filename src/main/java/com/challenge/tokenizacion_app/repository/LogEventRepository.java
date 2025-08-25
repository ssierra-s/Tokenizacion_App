package com.challenge.tokenizacion_app.repository;

import com.challenge.tokenizacion_app.model.entity.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEventRepository extends JpaRepository<LogEvent, String> {
}

