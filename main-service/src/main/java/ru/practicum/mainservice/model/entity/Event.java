package ru.practicum.mainservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.practicum.mainservice.model.enums.EventState;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Annotation cannot be blank")
    @Size(min = 20, max = 2000, message = "Annotation must be between 20 and 2000 characters")
    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    @NotNull(message = "Category cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "confirmed_requests")
    @Builder.Default
    private Long confirmedRequests = 0L;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 20, max = 7000, message = "Description must be between 20 and 7000 characters")
    @Column(name = "description", nullable = false, length = 7000)
    private String description;

    @NotNull(message = "Event date cannot be null")
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @NotNull(message = "Initiator cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "location_lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "location_lon"))
    })
    private Location location;

    @Column(name = "paid", nullable = false)
    @Builder.Default
    private Boolean paid = false;

    @Column(name = "participant_limit", nullable = false)
    @Builder.Default
    private Integer participantLimit = 0;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "request_moderation", nullable = false)
    @Builder.Default
    private Boolean requestModeration = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Builder.Default
    private EventState state = EventState.PENDING;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "views")
    @Builder.Default
    private Long views = 0L;

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Float lat;
        private Float lon;
    }
}
