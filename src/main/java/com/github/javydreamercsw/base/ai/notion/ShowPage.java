package com.github.javydreamercsw.base.ai.notion;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShowPage extends NotionPage {
  private NotionProperties properties;

  @Data
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
  public List<MatchPage> getMatches() {
    Logger log = LoggerFactory.getLogger(ShowPage.class);
    log.debug("Loading matches for show: {}", this.getId());

    List<MatchPage> matches = new ArrayList<>();

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      // Get the original page data to access properties
      PrintStream originalOut = System.out;
      try {
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        Page pageData = client.retrievePage(this.getId(), Collections.emptyList());
        System.setOut(originalOut);

        Map<String, PageProperty> properties = pageData.getProperties();

        // Look for a "Matches" relation property
        PageProperty matchesProperty = properties.get("Matches");
        if (matchesProperty != null
            && matchesProperty.getRelation() != null
            && !matchesProperty.getRelation().isEmpty()) {
          log.debug(
              "Found {} matches in show's relation property", matchesProperty.getRelation().size());

          // Process each related match
          for (Object relationObj : matchesProperty.getRelation()) {
            try {
              // Use reflection to get the match ID
              String matchId =
                  (String) relationObj.getClass().getMethod("getId").invoke(relationObj);
              log.debug("Processing match ID: {}", matchId);

              // Retrieve the match page to get its details
              System.setOut(new PrintStream(new ByteArrayOutputStream()));
              Page matchPageData = client.retrievePage(matchId, Collections.emptyList());
              System.setOut(originalOut);

              // Create and populate MatchPage object
              MatchPage matchPage = mapPageToMatchPage(matchPageData);
              matches.add(matchPage);

              log.debug("Added match: {} (ID: {})", getMatchName(matchPageData), matchId);

            } catch (Exception e) {
              System.setOut(originalOut);
              log.error("Failed to process match relation: {}", e.getMessage());
            }
          }
        } else {
          log.debug("No matches found in show's Matches property");
        }

      } catch (Exception e) {
        System.setOut(originalOut);
        log.error("Error loading matches for show: {}", e.getMessage());
      } finally {
        System.setOut(originalOut);
      }
    } catch (Exception e) {
      log.error("Error creating Notion client for match loading: {}", e.getMessage());
    }

    log.info("Loaded {} matches for show: {}", matches.size(), this.getId());
    return matches;
  }

  /** Helper method to map a Notion Page to a MatchPage object. */
  private MatchPage mapPageToMatchPage(Page pageData) {
    MatchPage matchPage = new MatchPage();

    // Set basic page information
    matchPage.setObject("page");
    matchPage.setId(pageData.getId());
    matchPage.setCreated_time(pageData.getCreatedTime().toString());
    matchPage.setLast_edited_time(pageData.getLastEditedTime().toString());
    matchPage.setArchived(pageData.getArchived());
    matchPage.setIn_trash(false);
    matchPage.setUrl(pageData.getUrl());
    matchPage.setPublic_url(pageData.getPublicUrl());

    // Set parent information
    MatchPage.NotionParent parent = new MatchPage.NotionParent();
    parent.setType("database_id");
    parent.setDatabase_id(pageData.getParent().getDatabaseId());
    matchPage.setParent(parent);

    return matchPage;
  }

  /** Helper method to extract match name from page data. */
  private String getMatchName(Page matchPageData) {
    try {
      PageProperty nameProperty = matchPageData.getProperties().get("Name");
      if (nameProperty != null
          && nameProperty.getTitle() != null
          && !nameProperty.getTitle().isEmpty()) {
        return nameProperty.getTitle().get(0).getPlainText();
      }
    } catch (Exception e) {
      Logger log = LoggerFactory.getLogger(ShowPage.class);
      log.warn("Failed to extract match name: {}", e.getMessage());
    }
    return "Unknown Match";
  }
}
