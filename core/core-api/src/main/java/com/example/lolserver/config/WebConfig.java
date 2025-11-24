package com.example.lolserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOrigins("http://localhost:3000", "https://your-domain.com") // 허용할 Origin 목록
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP Method
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 자격 증명(쿠키, 인증 헤더) 허용
                .maxAge(3600); // Preflight 요청 결과 캐싱 시간 (초)
    }
}
