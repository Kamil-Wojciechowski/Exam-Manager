package com.wojcka.exammanager.components.email;



import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    @Value("${spring.mail.from}")
    private String from;
    @Autowired
    private JavaMailSender mailSender;

    @Async
    public CompletableFuture<Boolean> sendEmail(String to, String subject, String body) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            message.setFrom(from);
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error(ex.getMessage());
            log.error("Error occured while sending email!");
        }


        return CompletableFuture.completedFuture(true);
    }
}
