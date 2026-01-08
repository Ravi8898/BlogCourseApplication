package org.project.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
