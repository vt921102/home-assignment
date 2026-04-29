package com.toanlv.flashsale.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        // Explicitly declare OpenAPI 3.0.3 — prevents Swagger UI
        // "does not specify a valid version field" error.
        // springdoc 2.8.x defaults to 3.1.0 which some Swagger UI
        // builds do not handle gracefully.
        .openapi("3.0.3")
        .info(
            new Info()
                .title("Flash Sale & Authentication API")
                .version("1.0.0")
                .description(
                    """
                                Backend API for user authentication and flash sale.

                                **Authentication:** Obtain a JWT via \
                                POST /api/v1/auth/login, \
                                then click "Authorize" and enter: Bearer {token}

                                **Idempotency:** POST /api/v1/flash-sale/purchase \
                                requires header X-Idempotency-Key (UUID v4) \
                                to prevent duplicate orders on client retry.
                                """)
                .contact(new Contact().name("Engineering").email("eng@example.com"))
                .license(new License().name("Proprietary")))
        .servers(
            List.of(
                new Server().url("http://localhost:8080").description("Local"),
                new Server().url("https://api.example.com").description("Production")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token from " + "POST /api/v1/auth/login")));
  }
}
