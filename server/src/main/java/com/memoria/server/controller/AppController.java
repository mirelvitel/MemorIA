package com.memoria.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {

    @GetMapping(value = "/test")
    public String getTest() {
        return "Hello World";
    }
}
