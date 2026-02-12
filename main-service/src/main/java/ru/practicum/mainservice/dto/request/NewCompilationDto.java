package ru.practicum.mainservice.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Data
public class NewCompilationDto {

    private Set<Long> events = new HashSet<>();

    private Boolean pinned = false;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = 50, message = "Title must be between 1 and 50 characters")
    private String title;
}