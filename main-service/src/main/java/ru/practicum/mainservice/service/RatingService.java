package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.rating.EventRatingDto;
import ru.practicum.mainservice.dto.rating.NewRatingDto;

import java.util.List;

public interface RatingService {
    EventRatingDto addRating(Long userId, Long eventId, NewRatingDto newRatingDto);

    void deleteRating(Long userId, Long eventId);

    EventRatingDto getEventRating(Long eventId);

    List<EventRatingDto> getTopEvents(int size);
}