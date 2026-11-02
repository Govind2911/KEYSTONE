package com.keystone.config;

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
    OpenAPI keystoneOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("KEYSTONE Field Service Management API")
                        .version("1.0")
                        .description("""
                                REST API for Meridian Facilities Management. Log in via POST /api/auth/login, then click
                                "Authorize" and paste the returned token. Every endpoint except login requires it.
                                Roles: DISPATCHER, TECHNICIAN, MANAGER, CUSTOMER. Errors share one JSON shape:
                                timestamp, status, error, message, path, errors[]. Illegal lifecycle transitions return 409."""))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
