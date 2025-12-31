package org.project.serviceImpl;

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

        log.info("Saving token for userId={}", user.getId());

        /**
         * Create a new UserToken entity and
         * Associate the token with the authenticated user's ID
         * "N" indicates the token is currently valid.
         */
        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setToken(token);
        userToken.setRevoked("N");
        userToken.setExpiryTime(expiryTime);

        userTokenRepository.save(userToken);
        log.info("Token saved successfully for userId={}", user.getId());
    }

    /**
     * Checks whether a token is valid (exists, not revoked, and not expired).
     *
     * @param token JWT token
     * @return true if token is valid, false otherwise
     */
    @Override
    public boolean isTokenValid(String token) {
        log.info("Validating token");
        System.out.println("Token :: "+userTokenRepository.findByTokenAndRevoked(token, "N"));

        return userTokenRepository
                .findByTokenAndRevoked(token, "N")
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
        log.info("Checking token expiry");
        return userTokenRepository
                .findByToken(token)
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
        log.info("Revoking token");
        userTokenRepository.findByTokenAndRevoked(token, "N")
                .ifPresent(userToken -> {
                    userToken.setRevoked("Y");
                    userTokenRepository.save(userToken);
                    log.info("Token revoked successfully");
                });
    }

    //Revokes all active tokens associated with a specific user.
    @Override
    public void revokeAllTokensForUser(Long userId) {
        log.info("Revoking all tokens for userId={}", userId);
        userTokenRepository.revokeAllTokensByUserId(userId);
        log.info("All tokens revoked for userId={}", userId);
    }

    /**
     * Logs out a user from all active sessions except the current one.
     * @param userTokenRequest contains userId and the current active token
     */
    @Override
    public ApiResponse<?> revokeAllTokensByUserId(UserTokenRequest userTokenRequest) {

        log.info("Revoking all sessions except current session for userId={}",
                userTokenRequest.getUserId());
        ApiResponse<?> response;
        try {
            List<UserToken> userTokenList = userTokenRepository.findByUserId(userTokenRequest.getUserId());
            if (!userTokenList.isEmpty()) {
                log.info("Total tokens found={}", userTokenList.size());
                // Identify the current session token so it should not be revoked
                Optional<UserToken> currentToken=  userTokenList.stream().filter(userToken -> userToken.getToken().equals(userTokenRequest.getCurrentToken())).findFirst();
                // Remove the current token from the list
                currentToken.ifPresent(userTokenList::remove);
                // Revoke all remaining tokens (logout from other sessions)
                userTokenList.forEach(userToken ->userToken.setRevoked("Y"));
                userTokenRepository.saveAll(userTokenList);

                log.info("All other sessions revoked successfully");
                response = new ApiResponse<>(SUCCESS, ALL_SESSION_LOGOUT_SUCCESS, HttpStatus.OK.value(), null);
            }
            else {
                log.info("No tokens found for userId={}", userTokenRequest.getUserId());
                response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
            }

        }catch (Exception ex){
            log.error("Exception occurred while revoking sessions for userId={}",
                    userTokenRequest.getUserId(), ex);
            // Handle any unexpected errors during token revocation
            response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED_500, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;
    }
}


