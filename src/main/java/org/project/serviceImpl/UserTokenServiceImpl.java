package org.project.serviceImpl;

import jakarta.servlet.http.HttpServletRequest;
import org.project.dto.requestDto.UserTokenRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.model.User;
import org.project.model.UserToken;
import org.project.repository.UserTokenRepository;
import org.project.service.UserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.project.constants.MessageConstants.*;
/**
 * Service implementation for managing user authentication tokens.
 * Handles token creation, validation, expiration, and revocation logic.
 */
@Service
@Transactional
public class UserTokenServiceImpl implements UserTokenService {

    private static final Logger log = LoggerFactory.getLogger(UserTokenServiceImpl.class);

    @Autowired
    private UserTokenRepository userTokenRepository;

    /**
     * Saves a new authentication token for a user.
     *
     * @param user        authenticated user
     * @param token       JWT token
     * @param expiryTime  token expiration time
     */
    @Override
    public void saveToken(User user, String token, LocalDateTime expiryTime) {

        log.info("Saving new token for userId: {}, expiryTime: {}", user.getId(), expiryTime);

        // Create new UserToken entity
        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setToken(token);
        userToken.setRevoked("N");
        userToken.setExpiryTime(expiryTime);

        log.info("UserToken entity created: {}", userToken);

        // Persist token
        userTokenRepository.save(userToken);
        log.info("Token saved successfully for userId={}", user.getId());

        log.info("Token successfully saved for userId: {}", user.getId());
    }

    /**
     * Checks whether a token is valid (exists, not revoked, and not expired).
     *
     * @param token JWT token
     * @return true if token is valid, false otherwise
     */
    @Override
    //returns true if token is INVALID!
    public boolean isTokenValid(String token) {

        log.info("Validating token: {}", token);

        System.out.println("Token :: "+userTokenRepository.findByTokenAndRevoked(token, "N"));

        // Fetch active (non-revoked) token from database
        Optional<UserToken> userTokenOptional =
                userTokenRepository.findByTokenAndRevoked(token, "N");

        log.info("Fetched token for validation: {}", userTokenOptional);

        // Return true if token is expired or not found i.e INVALID; false if token is still valid
        return userTokenOptional
                .map(userToken ->
                        userToken.getExpiryTime()
                                .isBefore(LocalDateTime.now()))
                .orElse(true);
    }

    /**
     * Checks whether the given token has expired.
     *
     * @param token JWT token
     * @return true if the token is expired, false otherwise
     */
    @Override
    public boolean isTokenExpired(String token) {

        log.info("Checking if token is expired: {}", token);

        // Fetch token from database
        Optional<UserToken> userTokenOptional =
                userTokenRepository.findByToken(token);

        log.info("Fetched token for expiry check: {}", userTokenOptional);
        // Returns true if the token is expired; false if it is still valid
        return userTokenOptional
                .map(userToken ->
                        userToken.getExpiryTime()
                                .isBefore(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * Revokes an active token by marking it as revoked.
     * @param token JWT token to be revoked
     */
    @Override
    public void revokeToken(String token) {

        log.info("Revoking single token: {}", token);

        // Find active token and revoke it by marking as revoked in the database
        userTokenRepository.findByTokenAndRevoked(token, "N")
                .ifPresent(userToken -> {

                    log.info("Active token found for revocation: {}", userToken);

                    userToken.setRevoked("Y");
                    userTokenRepository.save(userToken);

                    log.info("Token revoked successfully: {}", userToken);
                });
    }


    /**
     * revoke a user from all active sessions except the current one.
     * @param httpServletRequest contains userId and the current active token
     *
     */
    @Override
    public ApiResponse<?> revokeAllTokensExceptCurrentTokenByUserId(HttpServletRequest httpServletRequest) {

        Long userId = 0L;
        String authToken = "";
        ApiResponse<?> response;
        try {

            String authHeader = httpServletRequest.getHeader("Authorization");    // Extract Authorization header from the incoming request
            authToken = authHeader.substring(7);     // Extract JWT token by removing "Bearer " prefix

            // Fetch the current active token entry from the database
            // This token represents the current logged-in session/device
            Optional<UserToken> userTokenOptional =  userTokenRepository.findByToken(authToken);


            if (userTokenOptional.isPresent()) {
                userId = userTokenOptional.get().getUserId();
            }
            log.info("Received request to revoke all tokens for userId: {}", userId);


            log.info("Revoke all tokens request received: {}", userId);
            // Fetch all tokens for user in a List which has all fields of UserToken Entity
            List<UserToken> userTokenList =
                    userTokenRepository.findByUserId(userId);

            log.info("Fetched tokens for userId {} : {}", userId, userTokenList);

            //if List is not empty
            if (!userTokenList.isEmpty()) {

                // Find current token to exclude from revocation
                String finalAuthToken = authToken;
                Optional<UserToken> currentToken =
                        userTokenList.stream()
                                .filter(userToken -> userToken.getToken().equals(finalAuthToken))
                                .findFirst();

                log.info("Current active token identified: {}", currentToken);

                //removing currentToken from List
                currentToken.ifPresent(userTokenList::remove);

                // Revoke remaining tokens
                userTokenList.forEach(userToken -> userToken.setRevoked("Y"));

                log.info("Revoking tokens: {}", userTokenList);

                // Save all updated UserToken entities so token revocation is applied in the database
                userTokenRepository.saveAll(userTokenList);

                log.info("All sessions revoked successfully for userId: {}", userId);

                response = new ApiResponse<>(SUCCESS, ALL_SESSION_LOGOUT_SUCCESS, HttpStatus.OK.value(), null);
            }
            else {
                log.info("No tokens found for userId: {}", userId);
                response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
            }

        }catch (Exception ex){
            // Exception during revoke-all
            log.error("Exception occurred while revoking all tokens for userId: {} & userToken {}", userId,authToken, ex);
            response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED_500, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;
    }
}


