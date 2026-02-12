package ru.practicum.mainservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя не должно быть пустым")
    @Size(min = 2, max = 250, message = "Имя должно быть от 2 до 250 символов")
    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @NotBlank
    @Email(message = "email должен быть валидным")
    @Size(min = 6, max = 254, message = "email должен быть от 6 до 254 символов")
    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

}
