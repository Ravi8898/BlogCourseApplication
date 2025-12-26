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

import java.util.Optional;

import static org.project.constants.MessageConstants.*;
@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public ApiResponse<UserResponse> getUserById(Long id) {

        ApiResponse<UserResponse> response;
        try{
            Optional<User> userOpt = userRepository.findById(id);
            if(userOpt.isPresent()){
                User user = userOpt.get();
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
            response = new ApiResponse<>(FAILED, USER_NOT_FOUND, HttpStatus.NOT_FOUND.value(), null);
        }
        return response;

    }

}
