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
                    Version-controlled Key-Value Store API
                    
                    - Automatic version management on every save
                    - Point-in-time value retrieval via UNIX timestamp
                    - Concurrency-safe with Redis distributed lock
                    - Java 21 Virtual Threads enabled
                    """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Ryan Jukyeong Kim")
                                .url("https://github.com/jukyeong-git/kv-store")));
    }
}
