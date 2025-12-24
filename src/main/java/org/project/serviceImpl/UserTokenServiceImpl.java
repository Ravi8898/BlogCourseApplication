package org.project.serviceImpl;

import org.project.model.User;
import org.project.model.UserToken;
import org.project.repository.UserTokenRepository;
import org.project.service.UserTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class UserTokenServiceImpl implements UserTokenService {

    private final UserTokenRepository userTokenRepository;

    public UserTokenServiceImpl(UserTokenRepository userTokenRepository) {
        this.userTokenRepository = userTokenRepository;
    }

    @Override
    public void saveToken(User user, String token, LocalDateTime expiryTime) {

        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setToken(token);
        userToken.setRevoked(false);
        userToken.setExpiryTime(expiryTime);

        userTokenRepository.save(userToken);
    }

    @Override
    public boolean isTokenValid(String token) {
        return userTokenRepository
                .findByTokenAndRevokedFalse(token)
                .map(userToken ->
                        userToken.getExpiryTime()
                                .isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    public boolean isTokenExpired(String token) {

        return userTokenRepository
                .findByTokenAndRevokedFalse(token)
                .map(userToken ->
                        userToken.getExpiryTime()
                                .isBefore(LocalDateTime.now()))
                .orElse(true);
    }

    @Override
    public void revokeToken(String token) {

        userTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(userToken -> {
                    userToken.setRevoked(true);
                    userTokenRepository.save(userToken);
                });
    }

    @Override
    public void revokeAllTokensForUser(Long userId) {
        userTokenRepository.revokeAllTokensByUserId(userId);
    }
}


