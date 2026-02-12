package ru.practicum.mainservice.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "RequestIds cannot be empty")
    private List<Long> requestIds;

    @NotNull(message = "Status cannot be null")
    private String status;
}