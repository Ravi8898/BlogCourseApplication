package org.project.serviceImpl;

import org.project.dto.requestDto.UserTokenRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.model.User;
import org.project.model.UserToken;
import org.project.repository.UserTokenRepository;
import org.project.service.UserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.project.constants.MessageConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class UserTokenServiceImpl implements UserTokenService {

    private static final Logger log = LoggerFactory.getLogger(UserTokenServiceImpl.class);

    @Autowired
    private UserTokenRepository userTokenRepository;

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

        log.info("Token successfully saved for userId: {}", user.getId());
    }

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

    @Override
    public void revokeAllTokensForUser(Long userId) {

        log.info("Revoking all tokens for userId: {}", userId);

        //revoke all tokens of given user from database
        userTokenRepository.revokeAllTokensByUserId(userId);

        log.info("All tokens revoked for userId: {}", userId);
    }

    @Override
    public ApiResponse<?> revokeAllTokensByUserId(UserTokenRequest userTokenRequest) {

        log.info("Revoke all tokens request received: {}", userTokenRequest);

        ApiResponse<?> response;
        try {
            // Fetch all tokens for user in a List which has all fields of UserToken Entity
            List<UserToken> userTokenList =
                    userTokenRepository.findByUserId(userTokenRequest.getUserId());

            log.info("Fetched tokens for userId {} : {}", userTokenRequest.getUserId(), userTokenList);

            //if List is not empty
            if (!userTokenList.isEmpty()) {

                // Find current token to exclude from revocation
                Optional<UserToken> currentToken =
                        userTokenList.stream()
                                .filter(userToken -> userToken.getToken().equals(userTokenRequest.getCurrentToken()))
                                .findFirst();

                log.info("Current active token identified: {}", currentToken);

                //removing currentToken from List
                currentToken.ifPresent(userTokenList::remove);

                // Revoke remaining tokens
                userTokenList.forEach(userToken -> userToken.setRevoked("Y"));

                log.info("Revoking tokens: {}", userTokenList);

                // Save all updated UserToken entities so token revocation is applied in the database
                userTokenRepository.saveAll(userTokenList);

                log.info("All sessions revoked successfully for userId: {}", userTokenRequest.getUserId());

                response = new ApiResponse<>(SUCCESS, ALL_SESSION_LOGOUT_SUCCESS, HttpStatus.OK.value(), null);
            }
            else {
                log.info("No tokens found for userId: {}", userTokenRequest.getUserId());
                response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
            }

        }catch (Exception ex){
            // Exception during revoke-all
            log.error("Exception occurred while revoking all tokens for request: {}", userTokenRequest, ex);
            response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED_500, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;
    }
}
