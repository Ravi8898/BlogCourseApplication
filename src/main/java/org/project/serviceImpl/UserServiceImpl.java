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

        log.info("getUserById called with userId: {}", userId);

        ApiResponse<UserResponse> response;
        try{
            String isActive = "Y";

            // Fetch active user from repository
            Optional<User> userOptional = userRepository.findByIdAndIsActive(userId, isActive);

            log.info("User fetched from repository: {}", userOptional);

            if(userOptional.isPresent()){
                User user = userOptional.get();

                log.info("Active user found: {}", user);

                // Fetch address associated with user
                Address address= addressRepository.getAddressById(user.getAddressId());

                log.info("Address fetched for userId {} : {}", userId, address);

                // Map address entity to response DTO
                AddressResponse addressResponse = addressResponseMapper.map(address);

                // Prepare user response DTO
                UserResponse userResponse = new UserResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getDateOfBirth(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getRole(),
                        addressResponse
                );

                log.info("User found successfully with id: {}", userId);

                response = new ApiResponse<>(
                        SUCCESS, FETCH_USERS_SUCCESS, HttpStatus.OK.value(), userResponse
                );
            }else {
                // User not found or inactive
                log.info("No active user found for userId: {}", userId);
                response = new ApiResponse<>(FAILED, USER_NOT_FOUND, HttpStatus.NOT_FOUND.value(), null);
            }
        }catch (Exception ex){
            //unexpected exception
            log.error("Exception occurred while fetching user by userId: {}", userId, ex);
            response = new ApiResponse<>(FAILED, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;

    }

    /**
     * Fetch all active users.
     */
    @Override
    public ApiResponse<List<UserResponse>> getAllUsers() {

        log.info("getAllUsers called");

        ApiResponse<List<UserResponse>> response;

        try {
            // Fetch all active users
            List<User> users = userRepository.findByIsActive("Y");

            log.info("Users fetched from repository: {}", users);

            if (users.isEmpty()) {
                log.info("No active users found");
                return new ApiResponse<>(
                        FAILED,
                        NO_USERS_FOUND,
                        HttpStatus.NO_CONTENT.value(),
                        List.of()
                );
            }

            // Map each user entity to response DTO
            List<UserResponse> userResponses = users.stream()
                    .map(user -> {

                        // Fetch address for each user
                        AddressResponse addressResponse = null;

                        if (user.getAddressId() != null) {
                            Address address = addressRepository.getAddressById(user.getAddressId());
                            addressResponse = addressResponseMapper.map(address);
                        log.info("Fetching address for user during getAllUsers | userId={} | address={}", user.getId(), address);
                        }


                        return new UserResponse(
                                user.getId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getDateOfBirth(),
                                user.getEmail(),
                                user.getPhoneNumber(),
                                user.getRole(),
                                addressResponse
                        );
                    }).toList();

            log.info("Successfully mapped {} List of users to List of UserResponse", userResponses.size());

            response = new ApiResponse<>(
                    SUCCESS,
                    FETCH_USERS_SUCCESS,
                    HttpStatus.OK.value(),
                    userResponses
            );

        } catch (Exception ex) {
            log.error("Exception occurred while fetching all users", ex);
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

        log.info("deleteUserById called with userId: {}", userId);

        // Deactivate user in database
        int updated = userRepository.deactivateUserById(userId);

        log.info("Deactivate user result count: {}", updated);

        if (updated == 0) {
            log.info("User not found or already inactive for userId: {}", userId);
            return new ApiResponse<>(
                    FAILED,
                    DELETE_USER_FAILED,
                    HttpStatus.NOT_FOUND.value(),
                    null
            );
        }

        log.info("User successfully deactivated for userId: {}", userId);
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

        log.info("updateUserById called with request: {}", request);

        ApiResponse<UserResponse> response;
        try {
            String isActive = "Y";
            boolean shouldLogout = false;

            // Fetch active user for update
            Optional<User> userOptional = userRepository.findByIdAndIsActive(request.getUserId(), isActive);

            log.info("User fetched for update: {}", userOptional);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                log.info("Existing user before update: {}", user);

                // Update only provided user fields and trigger logout if sensitive details (email/phone) are changed
                if (request.getFirstName() != null) {
                    user.setFirstName(request.getFirstName());
                }

                if (request.getLastName() != null) {
                    user.setLastName(request.getLastName());
                }

                if (request.getDateOfBirth() !=null) {
                    user.setDateOfBirth((request.getDateOfBirth()));
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

                Address address = null;

                // Update or create address if provided
                if (request.getAddressRequest() != null) {

                    // CASE 1: Address already exists → UPDATE
                    if (user.getAddressId() != null) {

                        address = addressRepository.getAddressById(user.getAddressId());
                        log.info("Existing address fetched for update: {}", address);

                        addressRequestMapper.updateEntity(request.getAddressRequest(), address);
                        address = addressRepository.save(address);

                        log.info("Address updated successfully: {}", address);

                    }
                    // CASE 2: Address does NOT exist → CREATE
                    else {

                        log.info("No address found for user. Creating new address for userId={}", user.getId());

                        address = addressRequestMapper.map(request.getAddressRequest());
                        address = addressRepository.save(address);

                        user.setAddressId(address.getId());

                        log.info("New address created and linked to user. addressId={}", address.getId());
                    }
                }


                // Save updated user
                User updatedUser = userRepository.save(user);

                log.info("User updated and saved in database: {}", updatedUser);

                if (shouldLogout) {
                    // Revoke all tokens if sensitive data(email or phoneNumber) changed
                    userTokenRepository.revokeAllTokensByUserId(user.getId());
                    log.info("All tokens revoked for userId: {}", user.getId());
                }

                AddressResponse addressResponse = null;

                if (address != null) {
                    addressResponse = addressResponseMapper.map(address);
                }

                UserResponse userResponse = new UserResponse(
                        updatedUser.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getDateOfBirth(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getRole(),
                        addressResponse
                );

                response = new ApiResponse<>(
                        SUCCESS,shouldLogout ? LOGOUT_ON_PROFILE_UPDATE : PROFILE_UPDATE_SUCCESS, HttpStatus.OK.value(), userResponse
                );
            } else {
                log.info("User not found for update, userId: {}", request.getUserId());
                response = new ApiResponse<>(FAILED, PROFILE_UPDATE_FAILED, HttpStatus.NOT_FOUND.value(), null);
            }
        } catch (Exception ex) {
            // Exception during update
            log.error("Exception occurred while updating user, request: {}", request, ex);
            response = new ApiResponse<>(FAILED, PROFILE_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;

    }
}
