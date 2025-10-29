package com.darum.auth.service;

import com.darum.auth.domain.dto.AuthRequest;
import com.darum.auth.domain.dto.AuthenticationResponse;
import com.darum.auth.domain.dto.RegisterUserRequest;
import com.darum.auth.domain.entity.User;
import com.darum.auth.domain.enums.Role;
import com.darum.auth.domain.repository.UserRepository;
import com.darum.auth.jwt.JwtService;
import com.darum.shareddomain.dto.ResponseBody;
import com.darum.auth.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public ResponseEntity<ResponseBody<AuthenticationResponse>> authenticate(AuthRequest request) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        var principal = (SecurityUser) authenticate.getPrincipal();

        String jwtToken = jwtService.generateToken(principal);

        AuthenticationResponse authResponse = new AuthenticationResponse(jwtToken, jwtService.getExpiration());


        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization")
                .body(ResponseBody.success("Login successfully", authResponse));

    }

    public ResponseEntity<ResponseBody<AuthenticationResponse>> register(RegisterUserRequest request) {
        User user = User.builder().name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.EMPLOYEE)
                .build();

        userRepository.save(user);

        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        var principal = (SecurityUser) authenticate.getPrincipal();

        String jwtToken = jwtService.generateToken(principal);

        AuthenticationResponse authResponse = new AuthenticationResponse(jwtToken, jwtService.getExpiration());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body( ResponseBody.success("User registered successfully", authResponse));
    }
}
