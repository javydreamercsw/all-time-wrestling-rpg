package com.github.javydreamercsw.management.util;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Utility to retrieve actual show template data from the Show Templates database. */
@Slf4j
public class ShowTemplateRetriever {

  // Template IDs found from the debug output
  private static final List<String> TEMPLATE_IDS =
      Arrays.asList(
          "21190edc-c30f-800a-8ab6-ed6e9d055eae", // All Time Rumble
          "21190edc-c30f-8040-8307-da437cca05ab", // Chrono Clash
          "21190edc-c30f-8000-9057-e044e8ce8929", // Continuum
          "21190edc-c30f-80a4-ad0c-f975c6a13768" // Era of Champions
          );

  // Template names for reference
  private static final List<String> TEMPLATE_NAMES =
      Arrays.asList("All Time Rumble", "Chrono Clash", "Continuum", "Era of Champions");

  public static void main(String[] args) {
    log.info("Starting show template data retrieval from Show Templates database...");

    NotionHandler handler = NotionHandler.getInstance();

    // First, let's try to retrieve templates by name from the Show Templates database
    log.info("=== RETRIEVING TEMPLATES BY NAME ===");
    Map<String, ShowTemplatePage> templateData = handler.retrieveShowTemplateData(TEMPLATE_NAMES);

    if (!templateData.isEmpty()) {
      log.info("✅ Successfully retrieved {} templates by name", templateData.size());

      // Process each template and generate JSON structure
      for (Map.Entry<String, ShowTemplatePage> entry : templateData.entrySet()) {
        String templateName = entry.getKey();
        ShowTemplatePage templatePage = entry.getValue();

        log.info("=== PROCESSING TEMPLATE: {} ===", templateName);

        // Log template properties
        if (templatePage.getRawProperties() != null) {
          log.info("Template properties:");
          templatePage.getRawProperties().forEach((key, value) -> log.info("  {}: {}", key, value));
        }

        // Generate JSON structure for this template
        generateJsonForTemplate(templateName, templatePage);
      }
    } else {
      log.warn("❌ No templates retrieved by name. Templates might be stored differently.");
      log.info("The Show Templates database exists but templates might need to be accessed by ID.");
    }

    log.info("=== TEMPLATE RETRIEVAL COMPLETED ===");
  }

  private static void generateJsonForTemplate(String templateName, ShowTemplatePage templatePage) {
    log.info("=== JSON STRUCTURE FOR {} ===", templateName);

    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"name\": \"").append(templateName).append("\",\n");

    // Try to extract description
    String description = extractDescription(templatePage);
    if (description != null) {
      json.append("  \"description\": \"").append(description).append("\",\n");
    }

    // Try to extract show type
    String showType = extractShowType(templatePage);
    if (showType != null) {
      json.append("  \"showTypeName\": \"").append(showType).append("\",\n");
    }

    // Add content structure
    json.append("  \"content\": {\n");

    // Extract various properties
    String format = extractProperty(templatePage, "Format");
    if (format != null) {
      json.append("    \"format\": \"").append(format).append("\",\n");
    }

    String duration = extractProperty(templatePage, "Duration");
    if (duration != null) {
      json.append("    \"duration\": \"").append(duration).append("\",\n");
    }

    String matchCount = extractProperty(templatePage, "Match Count");
    if (matchCount != null) {
      json.append("    \"matchCount\": \"").append(matchCount).append("\",\n");
    }

    String mainEvent = extractProperty(templatePage, "Main Event");
    if (mainEvent != null) {
      json.append("    \"mainEvent\": \"").append(mainEvent).append("\",\n");
    }

    String venue = extractProperty(templatePage, "Venue");
    if (venue != null) {
      json.append("    \"venue\": \"").append(venue).append("\",\n");
    }

    // Add pyrotechnics (default based on show type)
    boolean pyrotechnics = "Premium Live Event (PLE)".equals(showType);
    json.append("    \"pyrotechnics\": ").append(pyrotechnics).append(",\n");

    String specialStaging = extractProperty(templatePage, "Special Staging");
    if (specialStaging != null) {
      json.append("    \"specialStaging\": \"").append(specialStaging).append("\",\n");
    }

    String commentary = extractProperty(templatePage, "Commentary");
    if (commentary != null) {
      json.append("    \"commentary\": \"").append(commentary).append("\"\n");
    }

    json.append("  }\n");
    json.append("}");

    log.info("Generated JSON:\n{}", json.toString());
    log.info("=== END JSON FOR {} ===", templateName);
  }

  private static String extractDescription(ShowTemplatePage templatePage) {
    return extractProperty(templatePage, "Description");
  }

  private static String extractShowType(ShowTemplatePage templatePage) {
    return extractProperty(templatePage, "Show Type");
  }

  private static String extractProperty(ShowTemplatePage templatePage, String propertyName) {
    if (templatePage.getRawProperties() != null) {
      Object value = templatePage.getRawProperties().get(propertyName);
      if (value != null) {
        return value.toString();
      }
    }
    return null;
  }
}
