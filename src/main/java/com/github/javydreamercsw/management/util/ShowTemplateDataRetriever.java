package com.github.javydreamercsw.management.util;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.request.databases.QueryDatabaseRequest;

/**
 * Utility class to retrieve show template data from Notion API and generate content for the
 * show_templates.json file.
 */
@Slf4j
public class ShowTemplateDataRetriever {

  private static final List<String> TEMPLATE_NAMES =
      Arrays.asList(
          "All Time Rumble",
          "Chrono Clash",
          "Continuum",
          "Era of Champions",
          "Infinity Burn",
          "Legends Converge",
          "Paradox Slam",
          "Quantum Quarrel",
          "Temporal Diversion",
          "Temporal Fallout",
          "Time Warp Takedown",
          "Timeless",
          "Timeline Turmoil");

  public static void main(String[] args) {
    log.info("Starting show template data retrieval from Show Templates database...");

    NotionHandler handler = NotionHandler.getInstance();

    // Get the Show Templates database ID
    String showTemplatesDbId = handler.getDatabaseId("Show Templates");
    if (showTemplatesDbId == null) {
      log.error("❌ Show Templates database not found in workspace");
      return;
    }

    log.info("✅ Found Show Templates database with ID: {}", showTemplatesDbId);

    // Query all templates from the Show Templates database directly
    log.info("=== RETRIEVING ALL TEMPLATES FROM SHOW TEMPLATES DATABASE ===");

    try {
      // Use the NotionHandler to query the Show Templates database directly
      var allTemplates = queryAllShowTemplates(showTemplatesDbId);

      if (!allTemplates.isEmpty()) {
        log.info(
            "✅ Successfully retrieved {} templates from Show Templates database",
            allTemplates.size());

        // Process each template and generate JSON structure
        for (ShowTemplatePage templatePage : allTemplates) {
          String templateName = extractTemplateName(templatePage);

          log.info("=== PROCESSING TEMPLATE: {} ===", templateName);

          // Log template properties
          if (templatePage.getRawProperties() != null) {
            log.info("Template properties:");
            templatePage
                .getRawProperties()
                .forEach((key, value) -> log.info("  {}: {}", key, value));
          }

          // Retrieve the actual page content using the Blocks API
          String pageContent = retrievePageContentWithBlocks(templatePage.getId());
          if (pageContent != null) {
            log.info("Page content for {}: {}", templateName, pageContent);
          }

          // Generate JSON structure for this template
          generateJsonForTemplate(templateName, templatePage, pageContent);

          log.info("---");
        }
      } else {
        log.warn("❌ No templates found in Show Templates database");
      }
    } catch (Exception e) {
      log.error("❌ Failed to retrieve templates from Show Templates database", e);
    }

    log.info("=== TEMPLATE RETRIEVAL COMPLETED ===");
  }

  /** Query all show templates from the Show Templates database. */
  private static List<ShowTemplatePage> queryAllShowTemplates(
      String databaseId) {
    List<ShowTemplatePage> templates = new ArrayList<>();

    try (NotionClient client = new NotionClient(EnvironmentVariableUtil.getNotionToken())) {
      // Query all pages from the Show Templates database
      QueryDatabaseRequest request = new QueryDatabaseRequest(databaseId);

      var response = client.queryDatabase(request);
      var results = response.getResults();

      log.info("Found {} pages in Show Templates database", results.size());

      // Convert each page to ShowTemplatePage
      for (var page : results) {
        try {
          // We need to create a method in NotionHandler to make this accessible
          // For now, let's create the ShowTemplatePage manually
          ShowTemplatePage templatePage = createShowTemplatePageFromNotionPage(page);
          templates.add(templatePage);
        } catch (Exception e) {
          log.warn("Failed to map page {} to ShowTemplatePage", page.getId(), e);
        }
      }

    } catch (Exception e) {
      log.error("Failed to query Show Templates database", e);
    }

    return templates;
  }

  /** Create a ShowTemplatePage from a Notion Page (simplified version). */
  private static ShowTemplatePage createShowTemplatePageFromNotionPage(
      notion.api.v1.model.pages.Page page) {
    ShowTemplatePage templatePage = new ShowTemplatePage();

    // Set basic page properties
    templatePage.setId(page.getId());
    templatePage.setUrl(page.getUrl());

    // Store raw properties for extraction
    java.util.Map<String, Object> rawProperties = new java.util.HashMap<>();
    if (page.getProperties() != null) {
      page.getProperties().forEach((key, value) -> rawProperties.put(key, value));
    }
    templatePage.setRawProperties(rawProperties);

    return templatePage;
  }

  /** Extract the template name from a ShowTemplatePage. */
  private static String extractTemplateName(ShowTemplatePage templatePage) {
    if (templatePage.getRawProperties() != null) {
      // The property is called "Template Name" in Notion, not "Name"
      Object nameProperty = templatePage.getRawProperties().get("Template Name");
      if (nameProperty != null) {
        // Extract the plain text from the title property
        if (nameProperty instanceof notion.api.v1.model.pages.PageProperty) {
          notion.api.v1.model.pages.PageProperty prop =
              (notion.api.v1.model.pages.PageProperty) nameProperty;
          if (prop.getTitle() != null && !prop.getTitle().isEmpty()) {
            return prop.getTitle().get(0).getPlainText();
          }
        }
        return nameProperty.toString();
      }
    }
    return "Unknown Template";
  }

  /** Retrieve page content using the Notion Blocks API. */
  private static String retrievePageContentWithBlocks(String pageId) {
    try {
      NotionBlocksRetriever blocksRetriever =
          new NotionBlocksRetriever(EnvironmentVariableUtil.getNotionToken());
      return blocksRetriever.retrievePageContent(pageId);
    } catch (Exception e) {
      log.error("Failed to retrieve page content for ID: {}", pageId, e);
      return null;
    }
  }

  /** Generate JSON structure for a template with page content. */
  private static void generateJsonForTemplate(
      String templateName, ShowTemplatePage templatePage, String pageContent) {
    log.info("=== JSON STRUCTURE FOR {} ===", templateName);

    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"name\": \"").append(templateName).append("\",\n");

    // Add the page content as description if available
    if (pageContent != null && !pageContent.trim().isEmpty()) {
      // Clean up the content for JSON
      String cleanContent = pageContent.replace("\"", "\\\"").replace("\n", "\\n");
      json.append("  \"description\": \"").append(cleanContent).append("\",\n");
    }

    // Try to extract description
    String description = extractProperty(templatePage, "Description");
    if (description != null) {
      json.append("  \"description\": \"").append(description).append("\",\n");
    }

    // Try to extract show type
    String showType = extractProperty(templatePage, "Show Type");
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

  /** Generate JSON structure for a template without page content (fallback method). */
  private static void generateJsonForTemplate(String templateName, ShowTemplatePage templatePage) {
    generateJsonForTemplate(templateName, templatePage, null);
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
