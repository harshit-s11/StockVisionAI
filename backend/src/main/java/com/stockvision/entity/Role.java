package com.stockvision.entity;

public enum Role {
    ROLE_USER,
    ROLE_ADMIN;

    public static Role fromString(String role) {
        if (role == null) return ROLE_USER;
        String normalized = role.trim().toUpperCase();
        if (normalized.equals("USER") || normalized.equals("ROLE_USER")) {
            return ROLE_USER;
        }
        if (normalized.equals("ADMIN") || normalized.equals("ROLE_ADMIN")) {
            return ROLE_ADMIN;
        }
        return ROLE_USER;
    }
}