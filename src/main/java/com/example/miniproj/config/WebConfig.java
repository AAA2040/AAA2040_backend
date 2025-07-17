package com.example.miniproj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:3000")  // 프론트 도메인 허용, 와일드카드 지원
                .allowedMethods("*")    // 모든 HTTP 메서드 허용
                .allowedHeaders("*")    // 모든 헤더 허용
                .allowCredentials(true); // 쿠키 인증 허용 시 필요
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/data/audio/**")
                .addResourceLocations("file:data/audio/");
    }
}
