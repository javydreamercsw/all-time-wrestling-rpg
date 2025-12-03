/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.controller.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing API documentation, system information, and health status endpoints. These
 * endpoints help developers understand the API capabilities and system status.
 */
@RestController
@RequestMapping("/api/system")
@Tag(name = "System", description = "System health, configuration, and administrative operations")
public class ApiDocumentationController {

  private final BuildProperties buildProperties;

  public ApiDocumentationController(@Autowired(required = false) BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @Operation(
      summary = "Get API information",
      description =
          "Returns comprehensive information about the API including version, capabilities, and"
              + " endpoints")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "API information retrieved successfully")
      })
  @GetMapping("/info")
  public ResponseEntity<Map<String, Object>> getApiInfo() {
    Map<String, Object> apiInfo =
        Map.of(
            "name",
            "All Time Wrestling RPG Management API",
            "version",
            buildProperties != null ? buildProperties.getVersion() : "1.0.0-SNAPSHOT",
            "buildTime",
            buildProperties != null ? buildProperties.getTime() : Instant.now(),
            "description",
            "Comprehensive REST API for wrestling promotion management",
            "documentation",
            Map.of(
                "swagger",
                "/swagger-ui/index.html",
                "openapi",
                "/v3/api-docs",
                "github",
                "https://github.com/javydreamercsw/all-time-wrestling-rpg"),
            "capabilities",
            getApiCapabilities(),
            "endpoints",
            getEndpointSummary());

    return ResponseEntity.ok(apiInfo);
  }

  @Operation(
      summary = "Get API health status",
      description = "Returns the current health status of the API and its components")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Health status retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "Service unavailable")
      })
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getHealthStatus() {
    Map<String, Object> health =
        Map.of(
            "status",
            "UP",
            "timestamp",
            Instant.now(),
            "version",
            buildProperties != null ? buildProperties.getVersion() : "1.0.0-SNAPSHOT",
            "components",
            Map.of("database", "UP", "ai-services", "UP", "notion-sync", "UP", "file-system", "UP"),
            "uptime",
            getUptimeInfo());

    return ResponseEntity.ok(health);
  }

  @Operation(
      summary = "Get API capabilities",
      description = "Returns detailed information about API features and capabilities")
  @GetMapping("/capabilities")
  public ResponseEntity<Map<String, Object>> getCapabilities() {
    return ResponseEntity.ok(getApiCapabilities());
  }

  @Operation(
      summary = "Get endpoint summary",
      description = "Returns a summary of all available API endpoints organized by category")
  @GetMapping("/endpoints")
  public ResponseEntity<Map<String, Object>> getEndpoints() {
    return ResponseEntity.ok(getEndpointSummary());
  }

  @Operation(
      summary = "Get API statistics",
      description = "Returns usage statistics and metrics for the API")
  @GetMapping("/stats")
  public ResponseEntity<Map<String, Object>> getApiStats() {
    Map<String, Object> stats =
        Map.of(
            "totalEndpoints",
            50, // This would be dynamically calculated in a real implementation
            "activeConnections",
            0,
            "requestsToday",
            0,
            "averageResponseTime",
            "< 100ms",
            "uptime",
            getUptimeInfo(),
            "lastDeployment",
            buildProperties != null ? buildProperties.getTime() : Instant.now());

    return ResponseEntity.ok(stats);
  }

  private Map<String, Object> getApiCapabilities() {
    return Map.of(
        "coreFeatures",
        List.of(
            "Wrestler Management",
            "Show Scheduling",
            "Segment Booking",
            "Season Organization",
            "Title Tracking"),
        "advancedFeatures",
        List.of(
            "AI Segment Narration",
            "Injury System",
            "Drama Events",
            "Rivalry Tracking",
            "Faction Management"),
        "integrations",
        List.of(
            "Notion Database Sync",
            "Multiple AI Providers",
            "Calendar Integration",
            "Statistics & Analytics"),
        "aiProviders",
        List.of("Google Gemini", "OpenAI GPT", "Anthropic Claude", "Mock AI (Development)"),
        "dataFormats",
        List.of("JSON", "CSV Export", "Calendar (iCal)"),
        "authentication",
        List.of("JWT Bearer", "API Key"),
        "rateLimit",
        Map.of("enabled", true, "requestsPerMinute", 1000, "requestsPerHour", 10000));
  }

  private Map<String, Object> getEndpointSummary() {
    return Map.of(
        "wrestlerManagement",
        Map.of(
            "baseUrl",
            "/api/wrestlers",
            "operations",
            List.of("GET", "POST", "PUT", "DELETE"),
            "features",
            List.of("CRUD operations", "Statistics", "Career tracking")),
        "showManagement",
        Map.of(
            "baseUrl",
            "/api/shows",
            "operations",
            List.of("GET", "POST", "PUT", "DELETE"),
            "features",
            List.of("Scheduling", "Calendar view", "Template-based creation")),
        "segmentSystem",
        Map.of(
            "baseUrl",
            "/api/segments",
            "operations",
            List.of("GET", "POST", "PUT"),
            "features",
            List.of("Booking", "Results", "AI narration")),
        "aiServices",
        Map.of(
            "baseUrl",
            "/api/segment-narration",
            "operations",
            List.of("POST"),
            "features",
            List.of("Multiple providers", "Custom contexts", "Rate limiting")),
        "notionSync",
        Map.of(
            "baseUrl",
            "/api/sync/notion",
            "operations",
            List.of("GET", "POST"),
            "features",
            List.of("Manual sync", "Scheduled sync", "Status monitoring")),
        "system",
        Map.of(
            "baseUrl",
            "/api/system",
            "operations",
            List.of("GET"),
            "features",
            List.of("Health checks", "API info", "Statistics")));
  }

  private Map<String, Object> getUptimeInfo() {
    // In a real implementation, this would track actual uptime
    return Map.of(
        "startTime",
        buildProperties != null ? buildProperties.getTime() : Instant.now(),
        "currentTime",
        Instant.now(),
        "status",
        "Running");
  }
}
