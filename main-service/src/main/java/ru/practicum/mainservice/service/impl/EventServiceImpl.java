package ru.practicum.mainservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.dto.request.NewEventDto;
import ru.practicum.mainservice.dto.request.UpdateEventAdminRequest;
import ru.practicum.mainservice.dto.request.UpdateEventUserRequest;
import ru.practicum.mainservice.dto.response.EventFullDto;
import ru.practicum.mainservice.dto.response.EventShortDto;
import ru.practicum.mainservice.exception.*;
import ru.practicum.mainservice.mapper.EventMapper;
import ru.practicum.mainservice.model.entity.Category;
import ru.practicum.mainservice.model.entity.Event;
import ru.practicum.mainservice.model.entity.User;
import ru.practicum.mainservice.model.enums.EventState;
import ru.practicum.mainservice.repository.CategoryRepository;
import ru.practicum.mainservice.repository.EventRepository;
import ru.practicum.mainservice.repository.UserRepository;
import ru.practicum.mainservice.service.EventService;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    private static final String APP_NAME = "ewm-main-service";
    private static final LocalDateTime STATS_START = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final LocalDateTime STATS_END = LocalDateTime.of(2035, 1, 1, 0, 0);
    private static final String TEST_IP = "0.0.0.0";

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события пользователем ID: {}", userId);

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new CategoryNotFoundException("Категория с ID " + newEventDto.getCategory() + " не найдена"));

        validateEventDate(newEventDto.getEventDate(), 2, "Дата события должна быть не ранее чем через 2 часа от текущего момента");

        Event event = eventMapper.toEvent(newEventDto, initiator, category);
        Event savedEvent = eventRepository.save(event);
        log.info("Событие создано с ID: {}", savedEvent.getId());

        EventFullDto dto = eventMapper.toEventFullDto(savedEvent);
        dto.setViews(0L);

        return dto;
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.info("Получение событий пользователя ID: {}, from: {}, size: {}", userId, from, size);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Пользователь с ID " + userId + " не найден");
        }

        Pageable pageable = createPageable(from, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> viewsMap = getAllViews(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.info("Получение события ID: {} пользователя ID: {}", eventId, userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Пользователь с ID " + userId + " не найден");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с ID " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EventAccessDeniedException("Событие не принадлежит пользователю");
        }

        EventFullDto dto = eventMapper.toEventFullDto(event);
        Map<Long, Long> viewsMap = getAllViews(Collections.singletonList(eventId));
        dto.setViews(viewsMap.getOrDefault(eventId, 0L));

        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события ID: {} пользователем ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с ID " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EventAccessDeniedException("Событие не принадлежит пользователю");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new EventAccessDeniedException("Можно редактировать только события в состоянии PENDING или CANCELED");
        }

        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate(), 2, "Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new CategoryNotFoundException("Категория с ID " + request.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        eventMapper.updateEventFromUserRequest(request, event);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new EventValidationException("Некорректное значение stateAction: " + request.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие ID: {} обновлено пользователем ID: {}, статус: {}", eventId, userId, updatedEvent.getState());

        EventFullDto dto = eventMapper.toEventFullDto(updatedEvent);
        Map<Long, Long> viewsMap = getAllViews(Collections.singletonList(eventId));
        dto.setViews(viewsMap.getOrDefault(eventId, 0L));

        return dto;
    }

    @Override
    public List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           int from, int size) {
        log.info("Поиск событий админом: users={}, states={}, categories={}", users, states, categories);

        Pageable pageable = createPageable(from, size);
        Specification<Event> spec = buildAdminSpecification(users, states, categories, rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> viewsMap = getAllViews(eventIds);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события ID: {} администратором", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с ID " + eventId + " не найдено"));

        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new EventValidationException("Дата события не может быть в прошлом");
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new CategoryNotFoundException("Категория с ID " + request.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        eventMapper.updateEventFromAdminRequest(request, event);

        if (request.getStateAction() != null) {
            handleAdminStateAction(event, request.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие ID: {} обновлено администратором, статус: {}", eventId, updatedEvent.getState());

        EventFullDto dto = eventMapper.toEventFullDto(updatedEvent);
        Map<Long, Long> viewsMap = getAllViews(Collections.singletonList(eventId));
        dto.setViews(viewsMap.getOrDefault(eventId, 0L));

        return dto;
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               int from, int size, HttpServletRequest request) {

        log.info("Публичный поиск событий: text={}, categories={}, paid={}", text, categories, paid);

        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new EventValidationException("rangeEnd must be after rangeStart");
        }

        if (request != null) {
            saveHit(request);
        }

        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime end = rangeEnd;

        Specification<Event> spec = buildPublicSpecification(text, categories, paid, start, end, onlyAvailable);
        Pageable pageable = createPageable(from, size);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> viewsMap = getUniqueViews(eventIds);

        List<EventShortDto> dtos = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());

        if ("EVENT_DATE".equals(sort)) {
            dtos.sort(Comparator.comparing(EventShortDto::getEventDate));
        } else if ("VIEWS".equals(sort)) {
            dtos.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return dtos;
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               int from, int size) {

        return getPublicEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, null);
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId, HttpServletRequest request) {
        log.info("Получение публичного события ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с ID " + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new EventNotFoundException("Событие с ID " + eventId + " не найдено или не опубликовано");
        }
        
        if (request != null) {
            saveHit(request);
        } else {
            saveTestHit("/events/" + eventId);
        }

        EventFullDto dto = eventMapper.toEventFullDto(event);
        Map<Long, Long> viewsMap = getUniqueViews(Collections.singletonList(eventId));
        dto.setViews(viewsMap.getOrDefault(eventId, 0L));

        return dto;
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        return getPublicEventById(eventId, null);
    }

    private void validateEventDate(LocalDateTime eventDate, int hoursBefore, String errorMessage) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hoursBefore))) {
            throw new EventValidationException(errorMessage);
        }
    }

    private Pageable createPageable(int from, int size) {
        validatePaginationParams(from, size);
        int page = from / size;
        return PageRequest.of(page, size);
    }

    private void validatePaginationParams(int from, int size) {
        if (from < 0) {
            throw new IllegalArgumentException("Параметр 'from' должен быть больше или равен 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Параметр 'size' должен быть больше 0");
        }
    }

    private Specification<Event> buildAdminSpecification(List<Long> users, List<String> states,
                                                         List<Long> categories, LocalDateTime rangeStart,
                                                         LocalDateTime rangeEnd) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("initiator").get("id").in(users));
            }

            if (states != null && !states.isEmpty()) {
                List<EventState> eventStates = states.stream()
                        .map(state -> {
                            try {
                                return EventState.valueOf(state);
                            } catch (IllegalArgumentException e) {
                                throw new EventValidationException("Некорректное состояние события: " + state);
                            }
                        })
                        .collect(Collectors.toList());
                predicates.add(root.get("state").in(eventStates));
            }

            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            if (rangeStart != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }

            if (rangeEnd != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Event> buildPublicSpecification(String text, List<Long> categories, Boolean paid,
                                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                          Boolean onlyAvailable) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("state"), EventState.PUBLISHED));

            if (text != null && !text.isBlank()) {
                String searchText = "%" + text.toLowerCase() + "%";
                Predicate annotationPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("annotation")), searchText);
                Predicate descriptionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), searchText);
                predicates.add(criteriaBuilder.or(annotationPredicate, descriptionPredicate));
            }

            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            if (paid != null) {
                predicates.add(criteriaBuilder.equal(root.get("paid"), paid));
            }

            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));

            if (rangeEnd != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }

            if (Boolean.TRUE.equals(onlyAvailable)) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("participantLimit"), 0),
                        criteriaBuilder.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void handleAdminStateAction(Event event, String stateAction) {
        switch (stateAction) {
            case "PUBLISH_EVENT":
                if (event.getState() != EventState.PENDING) {
                    throw new EventAccessDeniedException("Нельзя публиковать событие в статусе: " + event.getState());
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new EventValidationException("Нельзя публиковать событие, которое начинается менее чем через час");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case "REJECT_EVENT":
                if (event.getState() == EventState.PUBLISHED) {
                    throw new EventAccessDeniedException("Нельзя отклонить уже опубликованное событие");
                }
                event.setState(EventState.CANCELED);
                break;
            default:
                throw new EventValidationException("Некорректное значение stateAction: " + stateAction);
        }
    }

    private void saveHit(HttpServletRequest request) {
        try {
            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app(APP_NAME)
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Отправка хита в статистику: app={}, uri={}, ip={}, timestamp={}",
                    hitDto.getApp(), hitDto.getUri(), hitDto.getIp(), hitDto.getTimestamp());

            statsClient.hit(hitDto);
            log.info("Хит успешно отправлен");
        } catch (Exception e) {
            log.error("Ошибка при отправке хита в статистику: {}", e.getMessage(), e);
        }
    }

    private void saveTestHit(String uri) {
        try {
            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app(APP_NAME)
                    .uri(uri)
                    .ip(TEST_IP)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Отправка тестового хита в статистику: app={}, uri={}, ip={}",
                    hitDto.getApp(), hitDto.getUri(), hitDto.getIp());

            statsClient.hit(hitDto);
            log.info("Тестовый хит успешно отправлен");
        } catch (Exception e) {
            log.error("Ошибка при отправке тестового хита: {}", e.getMessage(), e);
        }
    }

    private Map<Long, Long> getUniqueViews(List<Long> eventIds) {
        return getViews(eventIds, true);
    }

    private Map<Long, Long> getAllViews(List<Long> eventIds) {
        return getViews(eventIds, false);
    }

    private Map<Long, Long> getViews(List<Long> eventIds, boolean unique) {
        Map<Long, Long> viewsMap = new HashMap<>();

        if (eventIds.isEmpty()) {
            return viewsMap;
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            log.info("Получение статистики для URI: {}, unique={}", uris, unique);

            List<ViewStatsDto> stats = statsClient.getStats(
                    STATS_START,
                    STATS_END,
                    uris,
                    unique
            );

            log.info("Получено записей статистики: {}", stats.size());

            for (ViewStatsDto stat : stats) {
                String uri = stat.getUri();
                if (uri.startsWith("/events/")) {
                    try {
                        Long eventId = Long.parseLong(uri.substring("/events/".length()));
                        viewsMap.put(eventId, stat.getHits());
                        log.info("Событие {} имеет {} просмотров (unique={})", eventId, stat.getHits(), unique);
                    } catch (NumberFormatException e) {
                        log.warn("Некорректный URI в статистике: {}", uri);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage(), e);
        }

        return viewsMap;
    }
}