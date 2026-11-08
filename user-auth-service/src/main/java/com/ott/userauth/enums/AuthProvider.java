package com.ott.userauth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum AuthProvider{
    LOCAL("local"),
    GOOGLE("google"),
    FACEBOOK("facebook"),
    APPLE("apple");

    private final String type;

    private AuthProvider(String type) {
        this.type = type;
    }

    @JsonCreator
    public static AuthProvider getAuthProviderFromType(String value) {
        for (AuthProvider provider : AuthProvider.values()) {
            if (provider.getType().equalsIgnoreCase(value)) {
                return provider;
            }
        }
        return null;
    }
}
