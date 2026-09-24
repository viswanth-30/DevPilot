package com.devpilot.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpilot.backend.service.ProjectService;

@RestController
public class HelloController {

    private final ProjectService projectService;

    public HelloController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/api/hello")
    public String hello() {
        return "Welcome to DevPilot";
    }

    @GetMapping("/api/status")
    public String status() {
        return projectService.getProjectMessage();
    }
}