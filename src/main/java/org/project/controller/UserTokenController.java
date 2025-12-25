package org.project.controller;

import org.project.dto.requestDto.RegisterRequest;
import org.project.dto.requestDto.UserTokenRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.service.UserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.project.constants.MessageConstants.*;

@RestController
@RequestMapping("/api/userToken")
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class UserTokenController {

    @Autowired
    private UserTokenService userTokenService;


    @PostMapping("/revokeAllTokensByUserId")
    public ResponseEntity<ApiResponse<?>> revokeAllTokensByUserId(@RequestBody UserTokenRequest userTokenRequest) {
        ApiResponse<?> response= UserTokenService.revokeAllTokensByUserId(userTokenRequest);
        if(response.getStatus().equalsIgnoreCase(SUCCESS)){
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
    }
