package com.atoma.marketplace.auth.service;

import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.JwtTokenProvider;
import com.atoma.marketplace.auth.security.MarketplaceUserDetails;
import com.atoma.marketplace.common.enums.UserRole;
import com.atoma.marketplace.common.enums.UserStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw MarketplaceException.conflict("Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw MarketplaceException.conflict("Phone already registered");
        }

        validateRoles(request.roles());

        var user = User.builder()
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .status(UserStatus.ACTIVE)
                .roles(request.roles())
                .build();

        userRepository.save(user);
        return buildAuthResponse(new MarketplaceUserDetails(user));
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> MarketplaceException.unauthorized("Invalid credentials"));
        return buildAuthResponse(new MarketplaceUserDetails(user));
    }

    private AuthDtos.AuthResponse buildAuthResponse(MarketplaceUserDetails userDetails) {
        return AuthDtos.AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(userDetails))
                .tokenType("Bearer")
                .userId(userDetails.getId())
                .email(userDetails.getEmail())
                .roles(userDetails.getAuthorities().stream()
                        .map(a -> UserRole.valueOf(a.getAuthority().replace("ROLE_", "")))
                        .collect(java.util.stream.Collectors.toSet()))
                .build();
    }

    private void validateRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw MarketplaceException.badRequest("At least one role is required");
        }
        if (roles.contains(UserRole.ADMIN)) {
            throw MarketplaceException.badRequest("Admin role cannot be self-assigned");
        }
    }
}
