package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.DuplicateResourceException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.*;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.entity.UserType;
import com.rowingclub.app.repository.UserRepository;
import com.rowingclub.app.repository.UserTypeRepository;
import com.rowingclub.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public AuthResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new DuplicateResourceException("Bu email zaten kayıtlı: " + request.getEmail());
        }

        UserType userType = userTypeRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("UserType", "name", "USER"));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .userType(userType)
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse login(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request){
        String email;

        try {
            email = jwtService.extractEmail(request.getRefreshToken());
        } catch (Exception e) {
            throw new BusinessException("Geçersiz refresh token", HttpStatus.UNAUTHORIZED);
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BusinessException("Hesabınız pasif durumda, oturum yenilenemiyor", HttpStatus.FORBIDDEN);
        }

        if (!jwtService.isTokenValid(request.getRefreshToken(), email)) {
            throw new BusinessException("Geçersiz veya süresi dolmuş refresh token", HttpStatus.UNAUTHORIZED);
        }

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Bu email zaten kayıtlı: " + request.getEmail());
        }

        var userType = userTypeRepository.findByName(request.getUserTypeName())
                .orElseThrow(() -> new ResourceNotFoundException("UserType", "name", request.getUserTypeName()));

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .userType(userType)
                .build();
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }
}