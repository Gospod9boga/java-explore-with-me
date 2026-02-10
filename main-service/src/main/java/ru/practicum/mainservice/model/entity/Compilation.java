package ru.practicum.mainservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    @Builder.Default
    private Set<Event> events = new HashSet<>();

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = 50, message = "Title must be between 1 and 50 characters")
    @Column(name = "title", nullable = false, length = 50)
    private String title;
}