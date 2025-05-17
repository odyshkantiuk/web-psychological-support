package com.psychsupport.webpsychologicalsupport.config;

import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DefaultAdminInitializer {

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin12345}")
    private String adminPassword;

    @Value("${admin.email:admin@psychsupport.com}")
    private String adminEmail;

    @Bean
    public CommandLineRunner initializeDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByRole(User.Role.ADMIN).isEmpty()) {
                User adminUser = new User();
                adminUser.setUsername(adminUsername);
                adminUser.setPassword(passwordEncoder.encode(adminPassword));
                adminUser.setEmail(adminEmail);
                adminUser.setFullName("System Administrator");
                adminUser.setRole(User.Role.ADMIN);
                adminUser.setCreatedAt(LocalDateTime.now());

                userRepository.save(adminUser);

                System.out.println("Default admin user created with username: " + adminUsername);
            }
        };
    }
}