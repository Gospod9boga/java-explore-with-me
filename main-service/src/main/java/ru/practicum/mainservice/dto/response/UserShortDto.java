package ru.practicum.mainservice.dto.response;

import jakarta.validation.constraints.Email;
import lombok.*;

@Data
@Email
@NoArgsConstructor
@AllArgsConstructor
public class UserShortDto {

    private Long id;

    private  String name;
}
