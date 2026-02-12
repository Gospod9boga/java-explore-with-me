package ru.practicum.mainservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.dto.rating.EventRatingDto;
import ru.practicum.mainservice.dto.rating.NewRatingDto;
import ru.practicum.mainservice.exception.NotFoundException;
import ru.practicum.mainservice.model.entity.Event;
import ru.practicum.mainservice.repository.EventRepository;
import ru.practicum.mainservice.service.RatingService;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingServiceImpl implements RatingService {

    private final EventRepository eventRepository;
    private final StatsClient statsClient;

    private static final String APP_NAME = "ewm-main-service";
    private static final LocalDateTime STATS_START = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final LocalDateTime STATS_END = LocalDateTime.of(2035, 1, 1, 0, 0);

    @Override
    @Transactional
    public EventRatingDto addRating(Long userId, Long eventId, NewRatingDto newRatingDto) {
        log.info("Добавление оценки: userId={}, eventId={}, isPositive={}",
                userId, eventId, newRatingDto.getIsPositive());
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        if (event.getState() != ru.practicum.mainservice.model.enums.EventState.PUBLISHED) {
            throw new IllegalStateException("Нельзя оценивать неопубликованное событие");
        }
        String uri = "/events/" + eventId + "/rating";
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(APP_NAME)
                .uri(uri)
                .ip(userId.toString())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.hit(hitDto);
        log.info("Оценка отправлена в статистику: userId={}, eventId={}, isPositive={}",
                userId, eventId, newRatingDto.getIsPositive());
        return getEventRating(eventId);
    }

    @Override
    @Transactional
    public void deleteRating(Long userId, Long eventId) {
        log.info("Удаление оценки: userId={}, eventId={}", userId, eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено");
        }
        log.info("Оценка удалена (симулировано): userId={}, eventId={}", userId, eventId);
    }

    @Override
    public EventRatingDto getEventRating(Long eventId) {
        log.info("Получение рейтинга события: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        String uri = "/events/" + eventId + "/rating";
        List<ViewStatsDto> stats = statsClient.getStats(
                STATS_START,
                STATS_END,
                Collections.singletonList(uri),
                true
        );
        Long totalRatings = 0L;
        if (!stats.isEmpty()) {
            totalRatings = stats.get(0).getHits();
        }
        return EventRatingDto.builder()
                .eventId(eventId)
                .eventTitle(event.getTitle())
                .likes(totalRatings)
                .dislikes(0L)
                .rating(totalRatings > 0 ? 1.0 : 0.0)
                .build();
    }

    @Override
    public List<EventRatingDto> getTopEvents(int size) {
        log.info("Получение топ-{} событий по рейтингу", size);

        List<Event> events = eventRepository.findAll().stream()
                .filter(event -> event.getState() == ru.practicum.mainservice.model.enums.EventState.PUBLISHED)
                .collect(Collectors.toList());
        List<EventRatingDto> ratings = events.stream()
                .map(event -> getEventRating(event.getId()))
                .sorted((r1, r2) -> r2.getLikes().compareTo(r1.getLikes()))
                .limit(size)
                .collect(Collectors.toList());
        return ratings;
    }
}