package org.project.serviceImpl;

import org.project.dto.responseDto.UserResponse;
import org.project.model.User;
import org.project.repository.UserRepository;
import org.project.service.UserService;
import org.project.dto.responseDto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.project.constants.MessageConstants.*;
@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public ApiResponse<UserResponse> getUserById(Long userId) {

        ApiResponse<UserResponse> response;
        try{
            Optional<User> userOptional = userRepository.findByIdAndIsActive(userId, "Y");;
            if(userOptional.isPresent()){
                User user = userOptional.get();
                UserResponse userResponse = new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhoneNumber()
                );
                response = new ApiResponse<>(
                        SUCCESS, SUCCESS, HttpStatus.OK.value(), userResponse
                );
            }else {
                response = new ApiResponse<>(FAILED, USER_NOT_FOUND, HttpStatus.NOT_FOUND.value(), null);
            }
        }catch (Exception ex){
            response = new ApiResponse<>(FAILED, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;

    }
    @Override
    public ApiResponse<List<UserResponse>> getAllUsers() {

        ApiResponse<List<UserResponse>> response;

        try {
            List<User> users = userRepository.findByIsActive("Y");

            if (users.isEmpty()) {
                return new ApiResponse<>(
                        FAILED,
                        NO_USERS_FOUND,
                        HttpStatus.NO_CONTENT.value(),
                        List.of()
                );
            }

            List<UserResponse> userResponses = users.stream()
                    .map(user -> new UserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getPhoneNumber()
                    ))
                    .collect(Collectors.toList());

            response = new ApiResponse<>(
                    SUCCESS,
                    FETCH_USERS_SUCCESS,
                    HttpStatus.OK.value(),
                    userResponses
            );

        } catch (Exception ex) {
            response = new ApiResponse<>(
                    FAILED,
                    FETCH_USERS_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }
        return response;
    }
    @Override
    public ApiResponse<Void> deleteUserById(Long userId) {
        int updated = userRepository.deactivateUserById(userId);

        if (updated == 0) {
            return new ApiResponse<>(
                    FAILED,
                    DELETE_USER_FAILED,
                    HttpStatus.NOT_FOUND.value(),
                    null
            );
        }

        return new ApiResponse<>(
                SUCCESS,
                DELETE_USER_SUCCESS,
                HttpStatus.OK.value(),
                null
        );
    }


}
