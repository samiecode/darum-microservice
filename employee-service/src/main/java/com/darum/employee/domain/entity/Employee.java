package com.darum.employee.domain.entity;

import com.darum.employee.domain.enums.EmployeeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

import static jakarta.persistence.EnumType.STRING;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employees")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Employee {
  
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(name = "employee_seq", sequenceName = "employee_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();


    @LastModifiedDate
    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
