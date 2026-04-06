package io.github.jukyeong.kvstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // Allow requests from GitHub Pages and local development
                        .allowedOrigins(
                                "https://jukyeong-git.github.io",
                                "http://localhost:8080"
                        )
                        .allowedMethods("GET", "POST")
                        .allowedHeaders("*");
            }
        };
    }
}
