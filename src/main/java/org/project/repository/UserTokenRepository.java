package org.project.repository;

import org.project.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository
        extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenAndRevoked(String token, String revoked);
    Optional<UserToken> findByToken(String token);

    @Modifying
    @Query("""
        UPDATE UserToken ut
        SET ut.revoked = 'Y'
        WHERE ut.userId = :userId
        """)
    void revokeAllTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
        DELETE FROM UserToken ut
        WHERE ut.expiryTime < :now
        """)
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    List<UserToken> findByUserId(Long userId);
}


