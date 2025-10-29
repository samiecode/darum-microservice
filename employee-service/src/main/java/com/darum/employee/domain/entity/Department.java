package com.darum.employee.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "departments")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Department {

    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_seq")
    @SequenceGenerator(name = "department_seq", sequenceName = "department_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;


    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();

    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
