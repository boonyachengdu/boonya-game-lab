package com.metaforge.auth.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = {"users", "createdAt"})
@EqualsAndHashCode(exclude = {"users", "createdAt"})
@NoArgsConstructor
@Entity
@Table(name = "roles")
@EntityListeners(AuditingEntityListener.class)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    @Transient
    private Integer userCount = 0;

    public Role(String name) {
        this.name = name;
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Integer getUserCount() {
        return users.size();
    }
}