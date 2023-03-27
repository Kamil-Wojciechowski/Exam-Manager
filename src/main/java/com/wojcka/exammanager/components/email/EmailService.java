package com.wojcka.exammanager.components.email;

public interface EmailService {
    Boolean sendEmail(String to, String subject, String body);
}
