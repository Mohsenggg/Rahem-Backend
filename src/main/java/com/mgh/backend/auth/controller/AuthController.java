package com.mgh.backend.auth.controller;


import com.mgh.backend.auth.domain.dto.AuthRequestDto;
import com.mgh.backend.auth.domain.dto.AuthResponseDto;
import com.mgh.backend.auth.domain.dto.RegisterRequestDto;
import com.mgh.backend.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200") // should be removed and configured with the filter chain

public class AuthController {

    @Autowired
    AuthService authService;


    @PostMapping("/register")
    public ResponseEntity register(@RequestBody RegisterRequestDto request){

        return authService.register(request);

    }



    @PostMapping("/login")
    public ResponseEntity login(@RequestBody AuthRequestDto request){

        AuthResponseDto authenticationResponse = authService.login(request);

        return ResponseEntity.ok(authenticationResponse);

    }


}
