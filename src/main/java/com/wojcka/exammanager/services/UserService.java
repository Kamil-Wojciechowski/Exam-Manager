package com.wojcka.exammanager.services;


import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public GenericResponsePageable getUsers(String role, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page,size);

        role = "ROLE_" + role.toUpperCase();

        Page result = userRepository.getUsersByRole(role, pageable);

        return GenericResponsePageable.builder()
                .code(200)
                .status("OK")
                .data(result.get())
                .page(page)
                .size(size)
                .hasNext(result.hasNext())
                .pages(result.getTotalPages())
                .total(result.getTotalElements())
                .build();
    }
}
