package com.mehdi.banking_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking API")
                        .description("REST API for banking operations — accounts, transfers, and transaction history.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("jwt-cookie"))
                .components(new Components()
                        .addSecuritySchemes("jwt-cookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("jwt")
                                .description("JWT token delivered as an HttpOnly cookie after login or register.")));
    }
}