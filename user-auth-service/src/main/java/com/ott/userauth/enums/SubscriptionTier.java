package com.ott.userauth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum SubscriptionTier {
    FREE_USER("free_user"),
    BASIC("basic"),
    PREMIUM("premium");

    private final String type;

    private SubscriptionTier(String type) {
        this.type = type;
    }

    @JsonCreator
    public static SubscriptionTier getSubscriptionTierFromType(String value) {
        for (SubscriptionTier tier : SubscriptionTier.values()) {
            if (tier.getType().equalsIgnoreCase(value)) {
                return tier;
            }
        }
        return null;
    }
}