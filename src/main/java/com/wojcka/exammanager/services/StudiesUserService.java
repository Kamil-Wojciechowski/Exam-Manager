package com.wojcka.exammanager.services;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('TEACHER')")
public class StudiesUserService {
}
