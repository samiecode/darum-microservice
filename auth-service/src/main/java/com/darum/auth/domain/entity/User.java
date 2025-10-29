package com.darum.auth.domain.entity;

import com.darum.auth.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

import static jakarta.persistence.EnumType.STRING;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(STRING)
    private Role role;

    @Builder.Default
    private Boolean locked = false;

    @Builder.Default
    private Boolean enabled = true;

    @CreatedDate
    @Builder.Default
    private Instant createdAt = Instant.now();
}
