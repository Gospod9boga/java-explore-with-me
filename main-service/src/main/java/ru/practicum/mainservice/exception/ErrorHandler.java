package ru.practicum.mainservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(final RuntimeException e) {
        log.error("Conflict: {}", e.getMessage());
        return Map.of(
                "status", "CONFLICT",
                "reason", "Integrity constraint has been violated.",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @ExceptionHandler(CategoryNotEmptyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleCategoryNotEmpty(CategoryNotEmptyException e) {
        log.error("Category not empty: {}", e.getMessage());
        return Map.of(
                "status", "CONFLICT",
                "reason", "Category has related events.",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }
}