package org.project.repository;

import org.project.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

        Optional<PasswordResetToken> findByTokenAndUsed(String token, String used);
}
