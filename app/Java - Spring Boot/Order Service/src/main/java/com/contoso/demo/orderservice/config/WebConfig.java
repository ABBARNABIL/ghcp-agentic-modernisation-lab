package com.contoso.demo.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Implements WebMvcConfigurer (replaces the removed WebMvcConfigurerAdapter
 * which was deleted in Spring Framework 6 / Spring Boot 3).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PATCH");
    }
}
