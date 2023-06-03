package com.urisproject.facesearch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 3/6/2023
 * @Description:
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "user_role")
public class UserRole extends AuditBase implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn
    private User user;

    @ManyToOne
    @JoinColumn
    private Role role;

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
    }
}
