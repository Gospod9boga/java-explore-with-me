package ru.practicum.mainservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.dto.response.EventFullDto;
import ru.practicum.mainservice.dto.response.EventShortDto;
import ru.practicum.mainservice.service.EventService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public List<EventShortDto> getPublicEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size,
            HttpServletRequest request) {

        log.info("GET /events - публичный поиск событий");

        LocalDateTime start = null;
        LocalDateTime end = null;

        if (rangeStart != null) {
            start = LocalDateTime.parse(rangeStart, FORMATTER);
        }
        if (rangeEnd != null) {
            end = LocalDateTime.parse(rangeEnd, FORMATTER);
        }

        return eventService.getPublicEvents(text, categories, paid, start, end,
                onlyAvailable, sort, from, size, request);
    }

    @GetMapping("/{id}")
    public EventFullDto getPublicEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /events/{} - получение публичного события", id);
        return eventService.getPublicEventById(id, request);
    }
}