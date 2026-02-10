package ru.practicum.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainservice.model.entity.ParticipationRequest;
import ru.practicum.mainservice.model.enums.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT COUNT(r) > 0 FROM ParticipationRequest r " +
            "WHERE r.event.id = :eventId AND r.requester.id = :requesterId")
    boolean existsByEventIdAndRequesterId(@Param("eventId") Long eventId,
                                          @Param("requesterId") Long requesterId);
}
