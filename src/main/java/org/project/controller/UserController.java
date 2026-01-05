package org.project.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import org.project.dto.requestDto.UpdateUserRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.UserResponse;
import org.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.project.constants.MessageConstants.SUCCESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;


    @Operation(summary = "Fetch user details by userId")
    @GetMapping("/getUserById")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @RequestParam("userId") Long userId) {

        log.info("Received request to get user by userId: {}", userId);

        ApiResponse<UserResponse> response = userService.getUserById(userId);

        log.info("Response status for getUserById userId {} : {}", userId, response.getStatus());

        if (response.getStatus().equalsIgnoreCase(SUCCESS)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        }

        log.info("Failed to fetch user for userId: {}", userId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }


    @Operation(summary = "Fetch all active users")
    @GetMapping("/getAllUsers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

        log.info("Received request to get all users");

        ApiResponse<List<UserResponse>> response = userService.getAllUsers();

        log.info("getAllUsers response status: {}", response.getStatus());

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }


    @Operation(summary = "Soft delete (deactivate) user by userId")
    @DeleteMapping("/deleteUserById")
    public ResponseEntity<ApiResponse<Void>> deleteUserById(
            @RequestParam("userId") Long userId) {

        log.info("Received request to delete user by userId: {}", userId);

        ApiResponse<Void> response = userService.deleteUserById(userId);

        log.info("Delete user response for userId {} : {}", userId, response.getStatus());

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


    @Operation(summary = "Update user details by userId")
    @PostMapping("/updateUserById")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserById(
            @RequestBody UpdateUserRequest updateUserRequest) {

        log.info("Received request to update user: {}", updateUserRequest);

        ApiResponse<UserResponse> response =
                userService.updateUserById(updateUserRequest);

        log.info("Update user response for userId {} : {}",
                updateUserRequest.getUserId(), response.getStatus());

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }
}
