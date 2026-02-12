package ru.practicum.mainservice.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewRatingDto {
    @NotNull(message = "isPositive must not be null")
    private Boolean isPositive;
}
