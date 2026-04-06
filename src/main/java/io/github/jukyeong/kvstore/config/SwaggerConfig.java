package io.github.jukyeong.kvstore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KV Store API")
                        .description("""
            ## Version-Controlled Key-Value Store API
            
            A production-grade key-value store with full version history \
            and point-in-time retrieval.
            
            ### Key Features
            - **Automatic versioning**: Every update creates a new version
            - **Point-in-time retrieval**: Query any key's value at a specific UNIX timestamp
            - **Concurrency-safe**: Redis distributed lock guarantees version integrity under 50+ concurrent requests
            - **High performance**: Java 21 Virtual Threads for non-blocking request handling
            
            ### Tech Stack
            - Java 21 + Spring Boot 3.5
            - PostgreSQL (Supabase) — persistent storage with ACID compliance
            - Redis (Upstash) — distributed locking for concurrency control
            - Deployed on Render (Singapore region)
            
            ### Notes
            - All timestamps are UNIX timestamps in UTC
            - Values accept any valid JSON (string, object, array, number)
            - Deployed on Render free tier — cold start may occur after 15 min inactivity
            """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Ryan Jukyeong Kim")
                                .url("https://github.com/jukyeong-git/kv-store")));
    }
}
