package org.project.serviceImpl;

import org.project.dto.requestDto.UpdateUserRequest;
import org.project.dto.responseDto.AddressResponse;
import org.project.dto.responseDto.UserResponse;
import org.project.mapper.AddressRequestMapper;
import org.project.mapper.AddressResponseMapper;
import org.project.model.Address;
import org.project.model.User;
import org.project.repository.AddressRepository;
import org.project.repository.UserRepository;
import org.project.repository.UserTokenRepository;
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

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AddressResponseMapper addressResponseMapper;

    @Autowired
    private AddressRequestMapper addressRequestMapper;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Override
    public ApiResponse<UserResponse> getUserById(Long userId) {

        ApiResponse<UserResponse> response;
        try{
            String isActive = "Y";
            Optional<User> userOptional = userRepository.findByIdAndIsActive(userId, isActive);;
            if(userOptional.isPresent()){
                User user = userOptional.get();

                Address address= addressRepository.getAddressById(user.getAddressId());
                AddressResponse addressResponse = addressResponseMapper.map(address);

                UserResponse userResponse = new UserResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getRole(),
                        addressResponse
                );
                response = new ApiResponse<>(
                        SUCCESS, FETCH_USERS_SUCCESS, HttpStatus.OK.value(), userResponse
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
                    .map(user -> {
                        Address address = addressRepository.getAddressById(user.getAddressId());
                        AddressResponse addressResponse = addressResponseMapper.map(address);

                        return new UserResponse(
                                user.getId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail(),
                                user.getPhoneNumber(),
                                user.getRole(),
                                addressResponse
                        );
                    }).toList();

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

    public ApiResponse<UserResponse> updateUserById(UpdateUserRequest request) {
        ApiResponse<UserResponse> response;
        try {
            String isActive = "Y";
            boolean shouldLogout = false;
            Optional<User> userOptional = userRepository.findByIdAndIsActive(request.getUserId(), isActive);
            ;
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                Address address = addressRepository.getAddressById(user.getAddressId());

                if (request.getFirstName() != null) {
                    user.setFirstName(request.getFirstName());
                }

                if (request.getLastName() != null) {
                    user.setLastName(request.getLastName());
                }

                if (request.getPhoneNumber() != null &&
                        !request.getPhoneNumber().equals(user.getPhoneNumber())) {
                    user.setPhoneNumber(request.getPhoneNumber());
                    shouldLogout = true;
                }

                if (request.getEmail() != null &&
                        !request.getEmail().equals(user.getEmail())) {
                    user.setEmail(request.getEmail());
                    shouldLogout = true;
                }

                if (request.getAddressRequest() != null && user.getAddressId() != null) {
                    addressRequestMapper.updateEntity(request.getAddressRequest(), address);

                    addressRepository.save(address);

                }

                User updatedUser = userRepository.save(user);
                if (shouldLogout) {
                    userTokenRepository.revokeAllTokensByUserId(user.getId());
                }
                AddressResponse addressResponse = addressResponseMapper.map(address);
                UserResponse userResponse = new UserResponse(
                        updatedUser.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getRole(),
                        addressResponse
                );
                response = new ApiResponse<>(
                        SUCCESS,shouldLogout ? LOGOUT_ON_PROFILE_UPDATE : PROFILE_UPDATE_SUCCESS, HttpStatus.OK.value(), userResponse
                );
            } else {
                response = new ApiResponse<>(FAILED, PROFILE_UPDATE_FAILED, HttpStatus.NOT_FOUND.value(), null);
            }
        } catch (Exception ex) {
            response = new ApiResponse<>(FAILED, PROFILE_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;

    }


}