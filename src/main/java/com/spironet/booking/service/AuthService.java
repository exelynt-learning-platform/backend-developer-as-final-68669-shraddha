package com.spironet.booking.service;

import com.spironet.booking.dto.auth.LoginRequest;
import com.spironet.booking.dto.auth.LoginResponse;
import com.spironet.booking.dto.auth.RegisterRequest;
import com.spironet.booking.entity.Role;
import com.spironet.booking.entity.User;
import com.spironet.booking.exception.ConflictException;
import com.spironet.booking.repository.UserRepository;
import com.spironet.booking.security.JwtService;
import com.spironet.booking.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        String role = principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .username(principal.getUsername())
                .role(role)
                .expiresInMs(jwtService.getExpirationMs())
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);
    }
}
