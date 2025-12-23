package org.project.controller;

import static org.project.constants.MessageConstants.*;

import org.project.dto.requestDto.LoginRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.dto.requestDto.RegisterRequest;
import org.project.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class LoginController {

    @Autowired
    private LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = loginService.register(request);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(FAILED, USERNAME_EXISTS, HttpStatus.CONFLICT.value(), null));
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponse<>(SUCCESS, REGISTRATION_SUCCESSFUL, HttpStatus.OK.value(), response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(FAILED, BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(FAILED, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null));
        }
    }

//    @PostMapping("/login")
//    public String login(@RequestBody LoginRequest request) {
//
//        Optional<User> user = userService.findByUsername(request.getUsername());
//        if (user.isEmpty() || !user.get().getPassword().equals(request.getPassword())) {
//            return "Invalid username or password.";
//        }
//        return "Login successful!";
//    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<RegisterResponse>> login(@RequestBody LoginRequest request) {

        ApiResponse<RegisterResponse> response = loginService.login(request);

        if (!response.getStatus().equalsIgnoreCase("SUCCESS")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/logout")
    public String logout() { return "Logout successful!"; }

//    @GetMapping("getAllUsers")
//    public ResponseEntity<ApiResponse<UserResponse>> getAllUsers(){
//
//    }
}
