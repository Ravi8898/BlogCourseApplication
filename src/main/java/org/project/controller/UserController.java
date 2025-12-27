package org.project.controller;

import java.util.List;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.UserResponse;
import org.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.project.constants.MessageConstants.SUCCESS;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("/getUserById")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@RequestParam("userId") Long userId) {
        ApiResponse<UserResponse> response = userService.getUserById(userId);

        if (response.getStatus().equalsIgnoreCase(SUCCESS)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

        ApiResponse<List<UserResponse>> response = userService.getAllUsers();

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }
    @DeleteMapping("/deleteUserById")
    public ResponseEntity<ApiResponse<Void>> deleteUserById(
            @RequestParam("userId") Long userId) {

        ApiResponse<Void> response = userService.deleteUserById(userId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


}
