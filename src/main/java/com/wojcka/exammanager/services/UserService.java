package com.wojcka.exammanager.services;


import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public GenericResponsePageable getUsers(String role, String firstname, String lastname, String email, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page,size);

        role = "ROLE_" + role.toUpperCase();

        if(firstname != null) {
            firstname = '%' + firstname.toLowerCase() + '%';
        }
        if(lastname != null) {
            lastname = '%' + lastname.toLowerCase() + '%';
        }
        if(email != null) {
            email = '%' + email.toLowerCase() + '%';
        }

        Page result = userRepository.getByRoleAndParams(role, firstname, lastname, email, pageable);

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


    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT')" )
    public GenericResponse getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return GenericResponse.ok(user);
    }
}
