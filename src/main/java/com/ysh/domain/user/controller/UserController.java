package com.ysh.domain.user.controller;

import com.ysh.common.response.ApiResponse;
import com.ysh.domain.user.dto.LoginRequest;
import com.ysh.domain.user.dto.TokenResponse;
import com.ysh.domain.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(refreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("X-Refresh-Token") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
