package org.kyh.auth.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestApiController {

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "principal", authentication.getPrincipal(), // userId(String)
                "authorities", authentication.getAuthorities()  // ROLE_USER
        );
    }

    @GetMapping("/api/test")
    public String test() {
        return "success";
    }
}