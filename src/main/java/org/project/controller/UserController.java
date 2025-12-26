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
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("/getUserById")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@RequestParam("id") Long id) {
        ApiResponse<UserResponse> response = userService.getUserById(id);

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

}
