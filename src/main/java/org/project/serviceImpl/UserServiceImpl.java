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

import static org.project.constants.MessageConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

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

    /**
     * Fetch a user by ID if the user is active.
     */
    @Override
    public ApiResponse<UserResponse> getUserById(Long userId) {

        log.info("Fetching user by id: {}", userId);

        ApiResponse<UserResponse> response;
        try {
            String isActive = "Y";

            // Fetch active user by ID
            Optional<User> userOptional = userRepository.findByIdAndIsActive(userId, isActive);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Fetch and map address details
                Address address = addressRepository.getAddressById(user.getAddressId());
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

                log.info("User found successfully with id: {}", userId);

                response = new ApiResponse<>(
                        SUCCESS, FETCH_USERS_SUCCESS, HttpStatus.OK.value(), userResponse
                );
            } else {
                log.info("User not found with id: {}", userId);
                response = new ApiResponse<>(FAILED, USER_NOT_FOUND, HttpStatus.NOT_FOUND.value(), null);
            }
        } catch (Exception ex) {
            log.error("Error while fetching user by id: {}", userId, ex);
            response = new ApiResponse<>(FAILED, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;
    }

    /**
     * Fetch all active users.
     */
    @Override
    public ApiResponse<List<UserResponse>> getAllUsers() {

        log.info("Fetching all active users");

        ApiResponse<List<UserResponse>> response;

        try {
            // Fetch all active users
            List<User> users = userRepository.findByIsActive("Y");

            if (users.isEmpty()) {
                log.info("No active users found");
                return new ApiResponse<>(
                        FAILED,
                        NO_USERS_FOUND,
                        HttpStatus.NO_CONTENT.value(),
                        List.of()
                );
            }

            // Map users to response DTO
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

            log.info("Fetched {} users successfully", userResponses.size());

            response = new ApiResponse<>(
                    SUCCESS,
                    FETCH_USERS_SUCCESS,
                    HttpStatus.OK.value(),
                    userResponses
            );

        } catch (Exception ex) {
            log.error("Error while fetching all users", ex);
            response = new ApiResponse<>(
                    FAILED,
                    FETCH_USERS_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }
        return response;
    }

    /**
     * Soft delete a user by deactivating the record.
     */
    @Override
    public ApiResponse<Void> deleteUserById(Long userId) {

        log.info("Deleting user with id: {}", userId);

        int updated = userRepository.deactivateUserById(userId);

        if (updated == 0) {
            log.info("User not found for deletion with id: {}", userId);
            return new ApiResponse<>(
                    FAILED,
                    DELETE_USER_FAILED,
                    HttpStatus.NOT_FOUND.value(),
                    null
            );
        }

        log.info("User deleted successfully with id: {}", userId);

        return new ApiResponse<>(
                SUCCESS,
                DELETE_USER_SUCCESS,
                HttpStatus.OK.value(),
                null
        );
    }

    /**
     * Update user profile details except role.
     */
    public ApiResponse<UserResponse> updateUserById(UpdateUserRequest request) {

        log.info("Updating user profile for userId: {}", request.getUserId());

        ApiResponse<UserResponse> response;
        try {
            String isActive = "Y";
            boolean shouldLogout = false;

            // Fetch active user
            Optional<User> userOptional = userRepository.findByIdAndIsActive(request.getUserId(), isActive);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Fetch address linked to user
                Address address = addressRepository.getAddressById(user.getAddressId());

                // Update basic user details if provided
                if (request.getFirstName() != null) {
                    user.setFirstName(request.getFirstName());
                }

                if (request.getLastName() != null) {
                    user.setLastName(request.getLastName());
                }

                // Update phone number and trigger logout if changed
                if (request.getPhoneNumber() != null &&
                        !request.getPhoneNumber().equals(user.getPhoneNumber())) {
                    user.setPhoneNumber(request.getPhoneNumber());
                    shouldLogout = true;
                }

                // Update email and trigger logout if changed
                if (request.getEmail() != null &&
                        !request.getEmail().equals(user.getEmail())) {
                    user.setEmail(request.getEmail());
                    shouldLogout = true;
                }

                // Update address details if provided
                if (request.getAddressRequest() != null && user.getAddressId() != null) {
                    addressRequestMapper.updateEntity(request.getAddressRequest(), address);
                    addressRepository.save(address);
                }

                // Save updated user
                User updatedUser = userRepository.save(user);

                // Revoke all tokens if sensitive data changed
                if (shouldLogout) {
                    log.info("Revoking all tokens for userId due to profile update: {}", user.getId());
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

                log.info("User profile updated successfully for userId: {}", user.getId());

                response = new ApiResponse<>(
                        SUCCESS,
                        shouldLogout ? LOGOUT_ON_PROFILE_UPDATE : PROFILE_UPDATE_SUCCESS,
                        HttpStatus.OK.value(),
                        userResponse
                );
            } else {
                log.info("User not found for update with userId: {}", request.getUserId());
                response = new ApiResponse<>(FAILED, PROFILE_UPDATE_FAILED, HttpStatus.NOT_FOUND.value(), null);
            }
        } catch (Exception ex) {
            log.error("Error while updating user profile for userId: {}", request.getUserId(), ex);
            response = new ApiResponse<>(FAILED, PROFILE_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;
    }
}
