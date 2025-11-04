package com.metaforge.ai.openai.config;

// mode/OpenApiConfig.java
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI boonyaRAGOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Boonya RAG API")
                        .description("RAG (Retrieval-Augmented Generation) API for document-based question answering")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Boonya Chengdu")
                                .url("https://github.com/boonyachengdu/boonya-game-lab")));
    }
}
