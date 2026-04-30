package com.yourorg.eventdashboard.admin;

import com.yourorg.eventdashboard.shared.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expiryHours;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${jwt.expiry-hours:8}") long expiryHours) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expiryHours = expiryHours;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDto dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.username(), dto.password()));
        String token = jwtService.generateToken(dto.username());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "expiresIn", expiryHours * 3600L));
    }
}
