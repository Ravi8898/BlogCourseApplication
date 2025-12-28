package org.project.controller;

import java.util.List;

import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.UserResponse;
import org.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.project.constants.MessageConstants.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.DELETE}
)
public class UserController {

    // Logger instance for logging controller activities
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * getUserById API to fetch a single user by userId
     * URL: /api/user/getUserById?userId=1
     */
    @GetMapping("/getUserById")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @RequestParam("userId") Long userId) {

        log.info("Received request to fetch user with ID: {}", userId);
        //calls getUserById method from UserService which returns ApiResponse type
        ApiResponse<UserResponse> response = userService.getUserById(userId);

        //if response is success, user fetch is successful
        if (response.getStatus().equalsIgnoreCase(SUCCESS)) {
            log.info("User fetched successfully for userId: {}", userId);
            // Return HTTP 200 (OK) along with the ApiResponse in the response body
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
        }

        //else by default Return HTTP 500 (Internal Server Error) along with failure response details
        log.error("Failed to fetch user for userId: {}", userId);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * API to fetch all active users
     * URL: /api/user/getAllUsers
     */
    @GetMapping("/getAllUsers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

        log.info("Received request to fetch all users");

        ApiResponse<List<UserResponse>> response = userService.getAllUsers();

        log.info("Fetch all users completed with statusCode: {}" ,response.getStatus());
        //always returns status code and response received from Service implementation class
        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

    /**
     * API to soft delete the user by userId
     * soft delete means it deletes the user by marking isActive = 'N' rather than removing from database(hard delete)
     * URL: /api/user/deleteUserById?userId=1
     */
    @DeleteMapping("/deleteUserById")
    public ResponseEntity<ApiResponse<Void>> deleteUserById(
            @RequestParam("userId") Long userId) {

        log.info("Received request to delete user with ID: {}", userId);
        // Calls UserService to soft delete a user and returns ApiResponse<Void>
        ApiResponse<Void> response = userService.deleteUserById(userId);

        //this if-else is only for logging
        if (response.getStatus().equalsIgnoreCase(SUCCESS)) {
            log.info("User deleted successfully (soft delete) for userId: {}", userId);
        } else {
            log.error("Failed to delete user for userId: {}", userId);
        }
        //always returns status code and response received from Service implementation class
        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }
}
