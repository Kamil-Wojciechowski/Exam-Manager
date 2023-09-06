package com.wojcka.exammanager.components;

import com.wojcka.exammanager.models.Role;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.models.UserGroup;
import com.wojcka.exammanager.models.Group;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.repositories.RoleRepository;
import com.wojcka.exammanager.repositories.UserGroupRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Initializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${default.email}")
    private String email;

    @Value("${default.password}")
    private String password;

    @Value("${default.firstname}")
    private String firstname;

    @Value("${default.lastname}")
    private String lastname;

    @Value("${default.initialize}")
    private Boolean initialize;

    @Override
    public void run(String...args) throws Exception {
        roleInitializer();
        adminInitializer();
    }

    public void roleInitializer() throws ClassNotFoundException {
        if(roleRepository.findAll().isEmpty()) {
            List<Role> roles = new ArrayList<>();

            roles.add(Role.builder()
                    .key("CLASS_CREATE")
                    .name("Tworzeie klas")
                    .description("Możliwość tworzenia klas.")
                    .build());

            roles.add(Role.builder()
                    .key("CLASS_READ")
                    .name("Czytanie klas")
                    .description("Możliwość wyświetlania list klas.")
                    .build());

            roles.add(Role.builder()
                    .key("CLASS_FULL")
                    .name("")
                    .description("Pełen dostęp do klas"));

            roleRepository.saveAll(roles);
        }

        if(!roleRepository.existsByKey("Admin")) {
            Role role = Role.builder()
                    .key("ADMIN")
                    .name("Admin")
                    .description("Pełny dostęp do aplikacji.")
                    .build();

            roleRepository.save(role);
        }
    }

    private void adminInitializer() throws ClassNotFoundException {
        if(initialize && userRepository.findByEmail(email).isEmpty()) {
            User user = userRepository.save(
                    User.builder()
                            .email(email)
                            .firstname(firstname)
                            .lastname(lastname)
                            .password(passwordEncoder.encode(password))
                            .enabled(true)
                            .locked(false)
                            .build()
            );
            if(groupRepository.findByName("Admin").isEmpty()) {
                Group group = groupRepository.findByName("Admin")
                        .orElseThrow(NullPointerException::new);

                userGroupRepository.save(
                        UserGroup
                                .builder()
                                .user(user)
                                .group(group)
                                .build()
                );
            }
        }


    }

}
