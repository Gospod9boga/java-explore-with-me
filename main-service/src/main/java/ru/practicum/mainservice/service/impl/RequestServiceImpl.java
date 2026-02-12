package ru.practicum.mainservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.dto.response.ParticipationRequestDto;
import ru.practicum.mainservice.exception.*;
import ru.practicum.mainservice.mapper.RequestMapper;
import ru.practicum.mainservice.model.entity.Event;
import ru.practicum.mainservice.model.entity.ParticipationRequest;
import ru.practicum.mainservice.model.entity.User;
import ru.practicum.mainservice.model.enums.EventState;
import ru.practicum.mainservice.model.enums.RequestStatus;
import ru.practicum.mainservice.repository.EventRepository;
import ru.practicum.mainservice.repository.RequestRepository;
import ru.practicum.mainservice.repository.UserRepository;
import ru.practicum.mainservice.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание заявки пользователем ID: {} на событие ID: {}", userId, eventId);

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с ID " + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new RequestValidationException("Инициатор события не может подать заявку на участие в своём событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new RequestValidationException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new RequestValidationException("Нельзя добавить повторный запрос");
        }

        RequestStatus status;
        if (event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        } else {
            Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new RequestValidationException("Достигнут лимит участников");
            }

            if (event.getRequestModeration()) {
                status = RequestStatus.PENDING;
            } else {
                status = RequestStatus.CONFIRMED;
            }
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Заявка создана с ID: {}, статус: {}", savedRequest.getId(), savedRequest.getStatus());

        if (status == RequestStatus.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
            log.info("Обновлен счетчик подтвержденных заявок для события ID: {}, новое значение: {}",
                    eventId, event.getConfirmedRequests());
        }

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение заявок пользователя ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Пользователь с ID " + userId + " не найден");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);

        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки ID: {} пользователем ID: {}", requestId, userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка с ID " + requestId + " не найдена"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new EventAccessDeniedException("Заявка не принадлежит пользователю");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        return requestMapper.toParticipationRequestDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение заявок на событие ID: {} пользователем ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с ID " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EventAccessDeniedException("Пользователь не является инициатором события");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);

        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, List<Long> requestIds, String status) {

        log.info("Обновление статусов заявок на событие ID: {} пользователем ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с ID " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EventAccessDeniedException("Пользователь не является инициатором события");
        }

        if (!event.getRequestModeration() && event.getParticipantLimit() == 0) {
            throw new RequestValidationException("Для этого события не требуется модерация заявок");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);

        if (requests.size() != requestIds.size()) {
            throw new RequestNotFoundException("Некоторые заявки не найдены");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new RequestValidationException("Заявка ID " + request.getId() + " не принадлежит событию");
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new RequestValidationException(
                        "Можно изменять только заявки в статусе PENDING. Заявка ID: " +
                                request.getId() + " имеет статус: " + request.getStatus()
                );
            }
        }

        RequestStatus newStatus;
        if ("CONFIRMED".equals(status)) {
            newStatus = RequestStatus.CONFIRMED;
        } else if ("REJECTED".equals(status)) {
            newStatus = RequestStatus.REJECTED;
        } else {
            throw new RequestValidationException(
                    "Некорректный статус: " + status + ". Допустимые значения: CONFIRMED, REJECTED"
            );
        }

        if (newStatus == RequestStatus.CONFIRMED && event.getParticipantLimit() > 0) {
            Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            int availableSlots = event.getParticipantLimit() - confirmedCount.intValue();

            if (availableSlots <= 0) {
                throw new RequestValidationException("Лимит участников уже достигнут");
            }

            if (requests.size() > availableSlots) {
                throw new RequestValidationException(
                        String.format(
                                "Можно подтвердить только %d заявок из %d (достигнут лимит участников)",
                                availableSlots, requests.size()
                        )
                );
            }
        }

        for (ParticipationRequest request : requests) {
            request.setStatus(newStatus);
        }
        List<ParticipationRequest> updatedRequests = requestRepository.saveAll(requests);

        if (newStatus == RequestStatus.CONFIRMED) {
            long confirmedCount = updatedRequests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.CONFIRMED)
                    .count();

            event.setConfirmedRequests(event.getConfirmedRequests() + confirmedCount);
            eventRepository.save(event);
            log.info("Обновлен счетчик подтвержденных заявок для события ID: {}, новое значение: {}",
                    eventId, event.getConfirmedRequests());
        }

        if (newStatus == RequestStatus.CONFIRMED && event.getParticipantLimit() > 0) {
            Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedCount >= event.getParticipantLimit()) {
                List<ParticipationRequest> pendingRequests = requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
                for (ParticipationRequest pendingRequest : pendingRequests) {
                    pendingRequest.setStatus(RequestStatus.REJECTED);
                }
                requestRepository.saveAll(pendingRequests);
            }
        }

        List<ParticipationRequestDto> confirmed = updatedRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.CONFIRMED)
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        List<ParticipationRequestDto> rejected = updatedRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.REJECTED)
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        if (newStatus == RequestStatus.CONFIRMED && event.getParticipantLimit() > 0) {
            Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedCount >= event.getParticipantLimit()) {
                List<ParticipationRequest> autoRejected = requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.REJECTED);
                List<ParticipationRequestDto> autoRejectedDtos = autoRejected.stream()
                        .map(requestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList());

                for (ParticipationRequestDto dto : autoRejectedDtos) {
                    boolean alreadyExists = rejected.stream()
                            .anyMatch(r -> r.getId().equals(dto.getId()));
                    if (!alreadyExists) {
                        rejected.add(dto);
                    }
                }
            }
        }

        log.info("Обновлены статусы {} заявок: подтверждено {}, отклонено {}",
                updatedRequests.size(), confirmed.size(), rejected.size());

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }
}