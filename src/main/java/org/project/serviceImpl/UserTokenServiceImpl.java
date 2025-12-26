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

@Service
@Transactional
public class UserTokenServiceImpl implements UserTokenService {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Override
    public void saveToken(User user, String token, LocalDateTime expiryTime) {

        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setToken(token);
        userToken.setRevoked("N");
        userToken.setExpiryTime(expiryTime);

        userTokenRepository.save(userToken);
    }

    @Override
    public boolean isTokenValid(String token) {
        System.out.println("Token :: "+userTokenRepository.findByTokenAndRevoked(token, "N"));

        return userTokenRepository
                .findByTokenAndRevoked(token, "N")
                .map(userToken ->
                        userToken.getExpiryTime()
                                .isBefore(LocalDateTime.now()))
                .orElse(true);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return userTokenRepository
                .findByToken(token)
                .map(userToken ->
                        userToken.getExpiryTime()
                                .isBefore(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    public void revokeToken(String token) {
        userTokenRepository.findByTokenAndRevoked(token, "N")
                .ifPresent(userToken -> {
                    userToken.setRevoked("Y");
                    userTokenRepository.save(userToken);
                });
    }

    @Override
    public void revokeAllTokensForUser(Long userId) {
        userTokenRepository.revokeAllTokensByUserId(userId);
    }
    @Override
    public ApiResponse<?> revokeAllTokensByUserId(UserTokenRequest userTokenRequest) {
        ApiResponse<?> response;
        try {
            List<UserToken> userTokenList = userTokenRepository.findByUserId(userTokenRequest.getUserId());
            if (!userTokenList.isEmpty()) {
                Optional<UserToken> currentToken=  userTokenList.stream().filter(userToken -> userToken.getToken().equals(userTokenRequest.getCurrentToken())).findFirst();
                currentToken.ifPresent(userTokenList::remove);
                userTokenList.forEach(userToken ->userToken.setRevoked("Y"));
                userTokenRepository.saveAll(userTokenList);
                response = new ApiResponse<>(SUCCESS, ALL_SESSION_LOGOUT_SUCCESS, HttpStatus.OK.value(), null);
            }
            else {
                response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
            }

        }catch (Exception ex){
            response = new ApiResponse<>(FAILED, ALL_SESSION_LOGOUT_FAILED_500, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
        return response;
    }
}


