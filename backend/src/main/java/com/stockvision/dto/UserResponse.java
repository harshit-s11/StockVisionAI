package com.stockvision.dto;

import com.stockvision.entity.UserEntity;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String role;

    public UserResponse() {}

    public UserResponse(UUID id, String username, String email, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public static UserResponse fromEntity(UserEntity entity) {
        String roleStr = entity.getRole() != null ? entity.getRole().name().replace("ROLE_", "") : "USER";
        return new UserResponse(
            entity.getId(),
            entity.getUsername(),
            entity.getEmail(),
            roleStr
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}