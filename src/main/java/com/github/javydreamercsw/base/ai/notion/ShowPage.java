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
package com.github.javydreamercsw.base.ai.notion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@EqualsAndHashCode(callSuper = false)
public class ShowPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Show-specific properties only (common properties inherited from base)
    private Property Description;
    private Property Date;
    private Property Venue;
    private Property ShowType;
    private Property Matches;
  }

  /**
   * Retrieves all matches associated with this show from the Notion database.
   *
   * @return List of MatchPage objects representing all matches in this show
   */
  public List<SegmentPage> getMatches() {
    Logger log = LoggerFactory.getLogger(ShowPage.class);
    log.debug("Loading matches for show: {}", this.getId());

    List<SegmentPage> matches = new ArrayList<>();

    try (NotionClient client = NotionHandler.getInstance().get().createNotionClient().get()) {
      try {
        Page pageData = client.retrievePage(this.getId(), Collections.emptyList());

        Map<String, PageProperty> properties = pageData.getProperties();

        // Look for a "Segments" relation property
        PageProperty matchesProperty = properties.get("Segments");
        if (matchesProperty != null
            && matchesProperty.getRelation() != null
            && !matchesProperty.getRelation().isEmpty()) {
          log.debug(
              "Found {} segments in show's relation property",
              matchesProperty.getRelation().size());

          // Process each related segment
          for (Object relationObj : matchesProperty.getRelation()) {
            try {
              // Use reflection to get the segment ID
              String matchId =
                  (String) relationObj.getClass().getMethod("getId").invoke(relationObj);
              log.debug("Processing segment ID: {}", matchId);

              // Retrieve the segment page to get its details
              Page matchPageData = client.retrievePage(matchId, Collections.emptyList());

              // Create and populate MatchPage object
              SegmentPage matchPage = mapPageToMatchPage(matchPageData);
              matches.add(matchPage);

              log.debug("Added segment: {} (ID: {})", getMatchName(matchPageData), matchId);

            } catch (Exception e) {
              log.error("Failed to process segment relation: {}", e.getMessage());
            }
          }
        } else {
          log.debug("No matches found in show's Matches property");
        }

      } catch (Exception e) {
        log.error("Error loading matches for show: {}", e.getMessage());
      }
    } catch (Exception e) {
      log.error("Error creating Notion client for segment loading: {}", e.getMessage());
    }

    log.info("Loaded {} matches for show: {}", matches.size(), this.getId());
    return matches;
  }

  /** Helper method to map a Notion Page to a MatchPage object. */
  private SegmentPage mapPageToMatchPage(@NonNull Page pageData) {
    SegmentPage matchPage = new SegmentPage();

    // Set basic page information
    matchPage.setObject("page");
    matchPage.setId(pageData.getId());
    matchPage.setCreated_time(pageData.getCreatedTime());
    matchPage.setLast_edited_time(pageData.getLastEditedTime());
    matchPage.setArchived(pageData.getArchived());
    matchPage.setIn_trash(false);
    matchPage.setUrl(pageData.getUrl());
    matchPage.setPublic_url(pageData.getPublicUrl());

    // Set parent information
    SegmentPage.NotionParent parent = new SegmentPage.NotionParent();
    parent.setType("database_id");
    parent.setDatabase_id(pageData.getParent().getDatabaseId());
    matchPage.setParent(parent);

    return matchPage;
  }

  /** Helper method to extract segment name from page data. */
  private String getMatchName(@NonNull Page segmentPageData) {
    try {
      PageProperty nameProperty = segmentPageData.getProperties().get("Name");
      if (nameProperty != null
          && nameProperty.getTitle() != null
          && !nameProperty.getTitle().isEmpty()) {
        return nameProperty.getTitle().get(0).getPlainText();
      }
    } catch (Exception e) {
      Logger log = LoggerFactory.getLogger(ShowPage.class);
      log.warn("Failed to extract segment name: {}", e.getMessage());
    }
    return "Unknown Match";
  }
}
