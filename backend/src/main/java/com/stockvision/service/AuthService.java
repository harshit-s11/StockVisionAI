package com.stockvision.service;

import com.stockvision.dto.AuthResponse;
import com.stockvision.dto.LoginRequest;
import com.stockvision.dto.RegisterRequest;
import com.stockvision.dto.UserResponse;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.UserEntity;
import com.stockvision.exception.ResourceNotFoundException;
import com.stockvision.exception.UserAlreadyExistsException;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.JwtTokenProvider;
import com.stockvision.security.SecurityUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AuthService {

    private static final BigDecimal INITIAL_PORTFOLIO_CASH = new BigDecimal("100000.00");

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PortfolioRepository portfolioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UserAlreadyExistsException("Username '" + username + "' is already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("Email '" + email + "' is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        UserEntity user = new UserEntity(username, email, passwordHash, Role.ROLE_USER);
        UserEntity savedUser = userRepository.save(user);

        PortfolioEntity portfolio = new PortfolioEntity(savedUser, INITIAL_PORTFOLIO_CASH);
        PortfolioEntity savedPortfolio = portfolioRepository.save(portfolio);
        savedUser.setPortfolio(savedPortfolio);

        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        UserResponse userResponse = UserResponse.fromEntity(savedUser);

        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationTimeSeconds(), userResponse);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        UserEntity user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        UserResponse userResponse = UserResponse.fromEntity(user);

        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationTimeSeconds(), userResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId();

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserResponse.fromEntity(user);
    }
}