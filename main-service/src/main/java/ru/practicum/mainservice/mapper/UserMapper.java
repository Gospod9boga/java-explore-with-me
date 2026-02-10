package ru.practicum.mainservice.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainservice.dto.request.NewUserRequest;
import ru.practicum.mainservice.dto.response.UserDto;
import ru.practicum.mainservice.dto.response.UserShortDto;
import ru.practicum.mainservice.model.entity.User;

@Component
public class UserMapper {

    public UserDto toUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

    public User toUser(NewUserRequest newUserRequest) {
        User user = new User();
        user.setEmail(newUserRequest.getEmail());
        user.setName(newUserRequest.getName());
        return user;
    }


    public UserShortDto toUserShortDto(User user) {
        UserShortDto dto = new UserShortDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        return dto;
    }

    public User updateUserFromRequest(NewUserRequest newUserRequest, User user) {

        if (newUserRequest.getName() != null) {
            user.setName(newUserRequest.getName());
        }

        if (newUserRequest.getEmail() != null) {
            user.setEmail(newUserRequest.getEmail());

        }
        return user;
    }
}
