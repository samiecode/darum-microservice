package com.darum.auth;

import com.darum.auth.domain.entity.User;
import com.darum.auth.domain.enums.Role;
import com.darum.auth.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication(scanBasePackages = "com.darum")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        return args -> {
            User user1 = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("password"))
                    .name("Admin User")
                    .role(Role.ADMIN)
                    .build();
            User user2 = User.builder()
                    .email("employee@example.com")
                    .password(passwordEncoder.encode("password"))
                    .name("Employee User")
                    .role(Role.EMPLOYEE)
                    .build();



            try {
                userRepository.save(user1);
                userRepository.save(user2);
            } catch (Exception ignored) {
            }
        };
    }

}
