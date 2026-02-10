package ru.practicum.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.mainservice.model.entity.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existByEmail(String email);

    List<User> findAllByIdIn(List<Long> ids);
}
