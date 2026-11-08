package com.ott.userauth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum RoleType {
    USER("user"),
    SUPER_ADMIN("super_admin"),
    CONTENT_MANAGER("content_manager"),
    SUPPORT_AGENT("support_agent"),
    FINANCE_ADMIN("finance_admin");

    private final String type;

    private RoleType(String type) {
        this.type = type;
    }

    @JsonCreator
    public static RoleType getRoleTypeFromType(String value) {
        for (RoleType roleType : RoleType.values()) {
            if (roleType.getType().equalsIgnoreCase(value)) {
                return roleType;
            }
        }
        return null;
    }
}