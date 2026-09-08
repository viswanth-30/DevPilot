package com.devpilot.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    //@GetMapping("/api/hello")
    //public String hello() {
     //   return "Welcome to DevPilot";
    //}

    @GetMapping("/api/status")
    public String status(){
        return "DevPilot is running";
    }
}