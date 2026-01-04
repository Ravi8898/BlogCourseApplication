package org.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.project.dto.requestDto.RegisterRequest;
import org.project.dto.requestDto.UserTokenRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.model.UserToken;
import org.project.service.UserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.project.constants.MessageConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@RestController
@RequestMapping("/api/userToken")
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class UserTokenController {

    private static final Logger log = LoggerFactory.getLogger(UserTokenController.class);

    @Autowired
    private UserTokenService userTokenService;


    @Operation(summary = "Revoke all active tokens for a user except the current session token")
    @PostMapping("/revokeAllTokensByUserId")
    public ResponseEntity<ApiResponse<?>> revokeAllTokensByUserId(
            HttpServletRequest httpServletRequest) {


        ApiResponse<?> response =
                userTokenService.revokeAllTokensByUserId(httpServletRequest);

        log.info("Revoke all tokens response for userId {} : {}",
                httpServletRequest, response.getStatus());

        if (response.getStatus().equalsIgnoreCase(SUCCESS)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        }

        log.error("Failed to revoke all tokens for userId: {}", httpServletRequest);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
