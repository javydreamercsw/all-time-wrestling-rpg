/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.ai.notion.handler;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShowTemplateNotionHandler {

  private final NotionHandler notionHandler;

  public ShowTemplateNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /**
   * Loads a show template from the Notion database by name.
   *
   * @param templateName The name of the show template to load
   * @return Optional containing the ShowTemplatePage object if found, empty otherwise
   */
  public Optional<ShowTemplatePage> loadShowTemplate(@NonNull String templateName) {
    log.debug("Loading show template: '{}'", templateName);

    String templateDbId = notionHandler.getDatabaseId("Show Templates");
    if (templateDbId == null) {
      log.warn("Show Templates database not found in workspace");
      return Optional.empty();
    }

    return notionHandler
        .createNotionClient()
        .flatMap(
            client -> {
              try {
                return notionHandler.loadEntityFromDatabase(
                    client,
                    templateDbId,
                    templateName,
                    "ShowTemplate",
                    (pageData, entityName) ->
                        notionHandler.mapPageToGenericEntity(
                            pageData,
                            entityName,
                            "ShowTemplate",
                            ShowTemplatePage::new,
                            NotionPage.NotionParent::new));
              } catch (Exception e) {
                log.error("Failed to load show template: {}", templateName, e);
                return Optional.empty();
              }
            });
  }

  /**
   * Loads all show templates from the Notion Show Templates database for sync operations. This
   * method is optimized for bulk operations and extracts only essential properties.
   *
   * @return List of ShowTemplatePage objects with basic properties populated
   */
  public List<ShowTemplatePage> loadAllShowTemplates() {
    log.debug("Loading all show templates for sync operation");

    String templateDbId = notionHandler.getDatabaseId("Show Templates");
    if (templateDbId == null) {
      log.warn("Show Templates database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .<List<ShowTemplatePage>>map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client,
                    templateDbId,
                    "Show Template",
                    (pageData, entityName) ->
                        notionHandler.mapPageToGenericEntity(
                            pageData,
                            entityName,
                            "ShowTemplate",
                            ShowTemplatePage::new,
                            NotionPage.NotionParent::new));
              } catch (Exception e) {
                log.error("Failed to load all show templates for sync", e);
                return new ArrayList<>();
              }
            })
        .orElse(new ArrayList<>());
  }

  /**
   * Retrieves show template data from Notion for all specified template names.
   *
   * @param templateNames List of template names to retrieve
   * @return Map of template name to show template data
   */
  public Map<String, ShowTemplatePage> retrieveShowTemplateData(
      @NonNull List<String> templateNames) {
    log.debug("Retrieving show template data for {} templates", templateNames.size());
    Map<String, ShowTemplatePage> templateData = new HashMap<>();

    for (String templateName : templateNames) {
      log.debug("Loading template: {}", templateName);
      Optional<ShowTemplatePage> templatePageOpt = loadShowTemplate(templateName);

      if (templatePageOpt.isPresent()) {
        ShowTemplatePage templatePage = templatePageOpt.get();
        templateData.put(templateName, templatePage);
        log.debug("Successfully loaded template: {}", templateName);

        // Log the template data for inspection
        logShowTemplateData(templateName, templatePage);
      } else {
        log.warn("Template '{}' not found in Notion database", templateName);
      }
    }

    log.debug("Retrieved {} out of {} templates", templateData.size(), templateNames.size());
    return templateData;
  }

  /** Logs detailed information about a show template for inspection. */
  private void logShowTemplateData(
      @NonNull String templateName, @NonNull ShowTemplatePage templatePage) {
    log.debug("=== TEMPLATE DATA FOR {} ===", templateName);

    // Log raw properties for complete data inspection
    if (templatePage.getRawProperties() != null && !templatePage.getRawProperties().isEmpty()) {
      log.debug("Raw properties:");
      templatePage
          .getRawProperties()
          .forEach(
              (key, value) -> {
                log.debug("  {}: {}", key, value);
              });
    }

    log.debug("=== END TEMPLATE DATA FOR {} ===", templateName);
  }
}
