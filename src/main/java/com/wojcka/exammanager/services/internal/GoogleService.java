package com.wojcka.exammanager.services.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.classroom.model.Course;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class GoogleService {
    @Value("${authorization.path}")
    private String path;

    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    public List<Course> getCourses() throws IOException {
        File file = ResourceUtils.getFile("classpath:" + path);
        InputStream in = new FileInputStream(file);

        GoogleCredential credential = GoogleCredential.fromStream(in)
                .createScoped(ClassroomScopes.all());

        Classroom classroom = new Classroom.Builder(credential.getTransport(), credential.getJsonFactory(), credential)
                .setApplicationName("GoogleClassroomAPI") // Set your application name
                .build();

//        classroom.courses().create(new Course().setName("test").setOwnerId("")).execute();

        return classroom.courses().list().setCourseStates(Collections.singletonList("ACTIVE")).execute().getCourses();
    }
}
