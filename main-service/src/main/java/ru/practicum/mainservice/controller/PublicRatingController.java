package ru.practicum.mainservice.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.dto.rating.EventRatingDto;
import ru.practicum.mainservice.service.RatingService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PublicRatingController {

    private final RatingService ratingService;

    @GetMapping("/events/{eventId}/rating")
    public EventRatingDto getEventRating(@PathVariable Long eventId) {
        log.info("GET /events/{}/rating", eventId);
        return ratingService.getEventRating(eventId);
    }

    @GetMapping("/events/top")
    public List<EventRatingDto> getTopEvents(@RequestParam(defaultValue = "10") int size) {
        log.info("GET /events/top?size={}", size);
        return ratingService.getTopEvents(size);
    }
}