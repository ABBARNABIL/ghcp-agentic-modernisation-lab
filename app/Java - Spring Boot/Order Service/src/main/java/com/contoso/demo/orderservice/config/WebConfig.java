package com.contoso.demo.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Uses WebMvcConfigurer (WebMvcConfigurerAdapter was removed after being
 * deprecated in Spring Framework 5; WebMvcConfigurer is a default-method
 * interface drop-in replacement).
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
