package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.response.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    class EventRequestStatusUpdateResult {
        private List<ParticipationRequestDto> confirmedRequests;
        private List<ParticipationRequestDto> rejectedRequests;

        public EventRequestStatusUpdateResult() {}

        public EventRequestStatusUpdateResult(List<ParticipationRequestDto> confirmedRequests,
                                              List<ParticipationRequestDto> rejectedRequests) {
            this.confirmedRequests = confirmedRequests;
            this.rejectedRequests = rejectedRequests;
        }

        public List<ParticipationRequestDto> getConfirmedRequests() {
            return confirmedRequests;
        }

        public void setConfirmedRequests(List<ParticipationRequestDto> confirmedRequests) {
            this.confirmedRequests = confirmedRequests;
        }

        public List<ParticipationRequestDto> getRejectedRequests() {
            return rejectedRequests;
        }

        public void setRejectedRequests(List<ParticipationRequestDto> rejectedRequests) {
            this.rejectedRequests = rejectedRequests;
        }
    }

    EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, List<Long> requestIds, String status);
}