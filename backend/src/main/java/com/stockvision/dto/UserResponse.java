package com.stockvision.dto;

import com.stockvision.entity.UserEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private BigDecimal cashBalance;
    private Instant createdAt;

    public UserResponse() {}

    public UserResponse(UUID id, String username, String email, String role, BigDecimal cashBalance, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.cashBalance = cashBalance;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(UserEntity entity, BigDecimal cashBalance) {
        String roleStr = entity.getRole() != null ? entity.getRole().name().replace("ROLE_", "") : "USER";
        return new UserResponse(
            entity.getId(),
            entity.getUsername(),
            entity.getEmail(),
            roleStr,
            cashBalance,
            entity.getCreatedAt()
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

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}