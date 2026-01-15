package org.kyh.auth.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/login/success")
    public String success() {
        return "OAUTH LOGIN SUCCESS";
    }
}