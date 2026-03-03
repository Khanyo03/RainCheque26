package com.raincheque.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@raincheque.com}")
    private String fromEmail;
    
    public void sendEmailWithAttachment(String to, String subject, String text, byte[] attachment, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            
            if (attachment != null && attachmentName != null) {
                helper.addAttachment(attachmentName, () -> new java.io.ByteArrayInputStream(attachment));
            }
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    public void sendSimpleEmail(String to, String subject, String text) {
        sendEmailWithAttachment(to, subject, text, null, null);
    }
}