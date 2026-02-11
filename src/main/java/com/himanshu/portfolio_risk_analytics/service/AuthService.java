package com.himanshu.portfolio_risk_analytics.service;

import com.himanshu.portfolio_risk_analytics.dto.AuthRequest;
import com.himanshu.portfolio_risk_analytics.dto.AuthResponse;
import com.himanshu.portfolio_risk_analytics.dto.UserDto;
import com.himanshu.portfolio_risk_analytics.entity.User;
import com.himanshu.portfolio_risk_analytics.repository.UserRepository;
import com.himanshu.portfolio_risk_analytics.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        logger.log(Level.INFO, "New user registered: " + savedUser.getEmail());

        return generateAuthResponse(savedUser);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        logger.log(Level.INFO, "User logged in: " + user.getEmail());
        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userDto)
                .expiresIn("24h")
                .build();
    }
}