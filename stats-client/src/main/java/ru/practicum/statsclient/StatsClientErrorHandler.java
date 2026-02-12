package ru.practicum.statsclient;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class StatsClientErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage());
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Incorrectly made request.",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }
}