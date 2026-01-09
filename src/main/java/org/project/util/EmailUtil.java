package org.project.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * EmailUtil implementation responsible for sending emails.
 * *
 * This util uses JavaMailSender to send transactional emails.
 * Currently supports sending password reset emails.
 */
@Component
@Transactional
public class EmailUtil{

    private final JavaMailSender mailSender;

    public EmailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private static final Logger log =
            LoggerFactory.getLogger(EmailUtil.class);
    /**
     * Sends password reset email to the user.
     *
     * @param toEmail   recipient email address
     * @param resetLink password reset link containing secure token
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {

        log.info("Preparing password reset email");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset your password");
        message.setText(
                "Click the link below to reset your password:\n\n" +
                        resetLink +
                        "\n\nThis link will expire in 15 minutes."
        );

        mailSender.send(message);

        log.info("Password reset email sent successfully");
    }
}
