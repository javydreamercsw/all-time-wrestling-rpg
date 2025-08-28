package com.github.javydreamercsw.management.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for the All Time Wrestling RPG Management System. Provides
 * comprehensive API documentation with proper categorization, security schemes, and detailed
 * metadata.
 */
@Configuration
public class OpenApiConfig {

  @Value("${server.port:8080}")
  private String serverPort;

  @Value("${spring.application.name:All Time Wrestling RPG}")
  private String applicationName;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(apiInfo())
        .servers(serverList())
        .tags(apiTags())
        .externalDocs(externalDocumentation())
        .components(securityComponents())
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  private Info apiInfo() {
    return new Info()
        .title("All Time Wrestling RPG - Management API")
        .version("1.0.0")
        .description(
            """
            ## All Time Wrestling RPG Management System API

            A comprehensive REST API for managing wrestling promotions, wrestlers, matches, and storylines
            in the All Time Wrestling RPG system. This API provides endpoints for:

            ### Core Features
            - **Wrestler Management**: Create, update, and manage wrestler profiles, stats, and careers
            - **Show Management**: Schedule and manage wrestling shows, events, and pay-per-views
            - **Match System**: Book matches, track results, and generate AI-powered match narrations
            - **Season Management**: Organize shows and storylines into seasons
            - **Title Management**: Track championship titles, reigns, and lineages

            ### Advanced Features
            - **AI Match Narration**: Generate detailed match stories using multiple AI providers
            - **Injury System**: Track wrestler injuries, recovery times, and health status
            - **Drama Events**: Manage storylines, feuds, and dramatic moments
            - **Rivalry System**: Track wrestler relationships and heat levels
            - **Faction Management**: Organize wrestlers into teams and stables

            ### Integration Features
            - **Notion Sync**: Synchronize data with Notion databases
            - **Calendar Integration**: View shows and events in calendar format
            - **Statistics**: Comprehensive stats tracking and reporting

            ### API Standards
            - RESTful design principles
            - JSON request/response format
            - Comprehensive error handling
            - Pagination support for large datasets
            - Filtering and sorting capabilities

            ### Getting Started
            1. Explore the available endpoints using the interactive documentation below
            2. Check the authentication requirements for protected endpoints
            3. Review the data models and example requests/responses
            4. Use the "Try it out" feature to test API calls directly

            For more information, visit our [GitHub repository](https://github.com/javydreamercsw/all-time-wrestling-rpg).
            """)
        .contact(
            new Contact()
                .name("ATW RPG Development Team")
                .url("https://github.com/javydreamercsw/all-time-wrestling-rpg")
                .email("support@atwrpg.com"))
        .license(
            new License()
                .name("MIT License")
                .url("https://github.com/javydreamercsw/all-time-wrestling-rpg/blob/main/LICENSE"));
  }

  private List<Server> serverList() {
    return List.of(
        new Server().url("http://localhost:" + serverPort).description("Local Development Server"),
        new Server().url("https://api.atwrpg.com").description("Production Server"),
        new Server().url("https://staging-api.atwrpg.com").description("Staging Server"));
  }

  private List<Tag> apiTags() {
    return List.of(
        new Tag()
            .name("Wrestler Management")
            .description("Operations for managing wrestlers, their profiles, stats, and careers"),
        new Tag()
            .name("Show Management")
            .description("Operations for scheduling and managing wrestling shows and events"),
        new Tag()
            .name("Match System")
            .description("Match booking, results tracking, and AI-powered narration generation"),
        new Tag()
            .name("Season Management")
            .description("Season creation, management, and organization of shows"),
        new Tag()
            .name("Title Management")
            .description("Championship title tracking, reigns, and lineage management"),
        new Tag()
            .name("Show Templates")
            .description("Template management for standardizing show formats and structures"),
        new Tag()
            .name("Injury Management")
            .description("ATW RPG Injury tracking and healing operations"),
        new Tag()
            .name("Drama Events")
            .description("Drama event management for storylines and narrative"),
        new Tag()
            .name("Rivalry System")
            .description("Wrestler relationship and heat level management"),
        new Tag().name("Factions").description("Faction management operations"),
        new Tag()
            .name("Notion Sync")
            .description("Notion database synchronization and integration"),
        new Tag()
            .name("AI Services")
            .description("AI-powered features including match narration and story generation"),
        new Tag().name("Statistics").description("Performance metrics, analytics, and reporting"),
        new Tag()
            .name("System")
            .description("System health, configuration, and administrative operations"));
  }

  private ExternalDocumentation externalDocumentation() {
    return new ExternalDocumentation()
        .description("ATW RPG Documentation")
        .url("https://github.com/javydreamercsw/all-time-wrestling-rpg/wiki");
  }

  private Components securityComponents() {
    return new Components()
        .addSecuritySchemes(
            "bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token authentication"))
        .addSecuritySchemes(
            "apiKey",
            new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API Key authentication for external integrations"));
  }
}
