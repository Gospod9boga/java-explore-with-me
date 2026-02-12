package ru.practicum.stats.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stats.Service.HitService;
import ru.practicum.stats.Service.StatsService;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping
@Slf4j
public class StatsController {
    private final HitService hitService;
    private final StatsService statsService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsController(HitService hitService, StatsService statsService) {
        this.hitService = hitService;
        this.statsService = statsService;
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@RequestBody EndpointHitDto endpointHitDto) {
        log.info("Получен хит: {}", endpointHitDto);
        hitService.saveHit(endpointHitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("Запрос статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        String cleanStart = start.replace("%20", " ").replace("+", " ");
        String cleanEnd = end.replace("%20", " ").replace("+", " ");

        LocalDateTime startTime;
        LocalDateTime endTime;

        try {
            startTime = LocalDateTime.parse(cleanStart, FORMATTER);
            endTime = LocalDateTime.parse(cleanEnd, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss", e);
        }

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return statsService.getStats(startTime, endTime, uris, unique);
    }
}