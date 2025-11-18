package org.project.controller;

import org.project.model.User;
import org.project.request.RegisterRequest;
import org.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        try {
            User createdUser = userService.register(request);

            if (createdUser == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/login")
    public String login() {
        return "Login successful!";
    }

    @GetMapping("/logout")
    public String logout() { return "Logout successful!"; }

}
