package com.wojcka.exammanager.components.email;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    CompletableFuture<Boolean> sendEmail(String to, String subject, String body);
}
