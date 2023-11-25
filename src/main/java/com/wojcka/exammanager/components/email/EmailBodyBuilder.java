package com.wojcka.exammanager.components.email;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cryptacular.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@Service
@NoArgsConstructor
public class EmailBodyBuilder {
    private static String buildEmailContent(ClassPathResource resource, HashMap<String, String> items) {
        final String[] templateContent = new String[1];

        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            templateContent[0] = new String(bytes, StandardCharsets.UTF_8);

            items.forEach((key, item) -> {
                templateContent[0] = templateContent[0].replace(key, item);
            });

        } catch (IOException exception) {
            log.error("Error occured while reading templates for email!");
        }

        return templateContent[0];
    }

    public static String buildEmail(EmailBodyType emailBodyType, HashMap<String, String> data) {
        String body = "";
        ClassPathResource resource;

        switch (emailBodyType) {
            case ACTIVATION -> {
                resource = new ClassPathResource("templates/ActivationEmail.html");

                body = buildEmailContent(resource, data);

            }
            case RECOVERY ->{
                resource = new ClassPathResource("templates/RecoveryEmail.html");

                body = buildEmailContent(resource, data);
            }
        }

        if(body == "") {
            throw new RuntimeException("Failed to prepare email");
        }

        return body;

    }
}
