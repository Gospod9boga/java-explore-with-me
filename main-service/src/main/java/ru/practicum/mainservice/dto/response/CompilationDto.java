package ru.practicum.mainservice.dto.response;

import lombok.Data;
import java.util.Set;

@Data
public class CompilationDto {

    private Long id;

    private Set<EventShortDto> events;

    private Boolean pinned;

    private String title;
}
