package ru.practicum.stats.Controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stats.Service.HitService;
import ru.practicum.stats.Service.StatsService;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@Slf4j
public class StatsController {
    private final HitService hitService;
    private final StatsService statsService;

    public StatsController(HitService hitService, StatsService statsService) {
        this.hitService = hitService;
        this.statsService = statsService;
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@RequestBody EndpointHitDto endpointHitDto) {
        log.info("Save :{} ", endpointHitDto);
        hitService.saveHit(endpointHitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("Get stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return statsService.getStats(start, end, uris, unique);
    }
}
