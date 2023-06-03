package com.urisproject.facesearch.model;

import lombok.Getter;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 3/6/2023
 * @Description:
 */
@Getter
public enum RoleType {
    USER("USER", "User"),
    ADMIN("ADMIN", "Admin");
    private final String name;
    private final String description;

    RoleType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
