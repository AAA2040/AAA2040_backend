package com.example.miniproj.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = "/{path:[^\\.]*}")
    public String redirect(@PathVariable String path) {
        // /api 로 시작하면 index.html로 포워딩하지 않음
        if (path.startsWith("api")) {
            return null; // Spring MVC에게 맡긴다 (예: @RestController 등)
        }
        return "forward:/index.html";
    }
}
