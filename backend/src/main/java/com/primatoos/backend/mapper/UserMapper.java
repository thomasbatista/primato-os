package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.common.UserSummaryResponse;
import com.primatoos.backend.dto.user.UserResponse;
import com.primatoos.backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(User user) {
        if (user == null) {
            return null;
        }

        return new UserSummaryResponse(user.getId(), user.getName(), user.getEmail());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
