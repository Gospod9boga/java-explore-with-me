package ru.practicum.mainservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.dto.rating.EventRatingDto;
import ru.practicum.mainservice.dto.rating.NewRatingDto;
import ru.practicum.mainservice.service.RatingService;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events/{eventId}/ratings")
public class PrivateRatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventRatingDto addRating(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @Valid @RequestBody NewRatingDto newRatingDto) {
        log.info("POST /users/{}/events/{}/ratings", userId, eventId);
        return ratingService.addRating(userId, eventId, newRatingDto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@PathVariable Long userId,
                             @PathVariable Long eventId) {
        log.info("DELETE /users/{}/events/{}/ratings", userId, eventId);
        ratingService.deleteRating(userId, eventId);
    }
}