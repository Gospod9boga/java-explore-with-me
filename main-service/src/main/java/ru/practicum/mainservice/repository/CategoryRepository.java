package ru.practicum.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.mainservice.model.entity.Category;

public interface CategoryRepository extends JpaRepository<Category,Long> {

    boolean existsByName(String name);
}
