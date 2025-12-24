package org.project.mapper;

import org.project.dto.responseDto.RegisterResponse;
import org.project.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserToRegisterResponseMapper implements Mapper<User, RegisterResponse> {

    @Override
    public RegisterResponse map(User user) {
        if (user == null) {
            return null;
        }
        RegisterResponse response = new RegisterResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        return response;
    }
}
