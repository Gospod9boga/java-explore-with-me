package ru.practicum.mainservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.mainservice.dto.LocationDto;
import ru.practicum.mainservice.model.entity.Event;

import java.time.LocalDateTime;

@Data
public class UpdateEventUserRequest {

    @Size(min = 20, max = 2000, message = "Annotation must be between 20 and 2000 characters")
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000, message = "Description must be between 20 and 7000 characters")
    private String description;

    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    private LocationDto location;
    private Event.Location oldLocation;

    private Boolean paid;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    private Integer participantLimit;

    private Boolean requestModeration;

    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    private String title;

    private String stateAction;
}