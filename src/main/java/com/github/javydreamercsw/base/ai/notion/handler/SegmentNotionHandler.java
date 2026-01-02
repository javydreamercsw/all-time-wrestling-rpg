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
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;

@Slf4j
public class SegmentNotionHandler {

  private final NotionHandler notionHandler;

  public SegmentNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /** Loads a segment from the Notion database by name. */
  public Optional<SegmentPage> loadSegment(@NonNull String segmentName) {
    log.debug("Loading segment: '{}'", segmentName);

    String matchDbId = notionHandler.getDatabaseId("Segments");
    if (matchDbId == null) {
      log.warn("Segments database not found in workspace");
      return Optional.empty();
    }

    return notionHandler
        .createNotionClient()
        .flatMap(
            client -> {
              try {
                return notionHandler.loadEntityFromDatabase(
                    client, matchDbId, segmentName, "Segment", this::mapPageToSegmentPage);
              } catch (Exception e) {
                log.error("Failed to load segment: {}", segmentName, e);
                return Optional.empty();
              }
            });
  }

  /**
   * Loads a segment from the Notion database by ID.
   *
   * @param segmentId The ID of the segment to load.
   * @return Optional containing the MatchPage object if found, empty otherwise.
   */
  public Optional<SegmentPage> loadSegmentById(@NonNull String segmentId) {
    log.debug("Loading segment with ID: '{}'", segmentId);
    return notionHandler.loadPage(segmentId).map(page -> mapPageToSegmentPage(page, ""));
  }

  /**
   * Loads all matches from the Notion Matches database.
   *
   * @return List of all MatchPage objects from the Matches database
   */
  public List<SegmentPage> loadAllSegments() {
    log.debug("Loading all matches from Matches database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String matchDbId = notionHandler.getDatabaseId("Segments");
    if (matchDbId == null) {
      log.warn("Segment database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, matchDbId, "Segment", this::mapPageToSegmentPage);
              } catch (Exception e) {
                log.error("Failed to load all matches", e);
                throw new RuntimeException(
                    "Failed to load matches from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<SegmentPage>());
  }

  /**
   * Gets all segment IDs from the Notion database.
   *
   * @return List of segment IDs.
   */
  public List<String> getSegmentIds() {
    log.debug("Loading all segment IDs from Segments database");

    String dbId = notionHandler.getDatabaseId("Segments");
    if (dbId == null) {
      log.warn("'Segments' database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .<List<String>>map(
            client -> {
              try {
                return notionHandler.loadAllEntityIdsFromDatabase(client, dbId, "Segments");
              } catch (Exception e) {
                log.error("Failed to load all segment IDs from database 'Segments'", e);
                return new ArrayList<>();
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a MatchPage object. */
  private SegmentPage mapPageToSegmentPage(@NonNull Page pageData, @NonNull String matchName) {
    SegmentPage matchPage =
        notionHandler.mapPageToGenericEntity(
            pageData, matchName, "Segment", SegmentPage::new, SegmentPage.NotionParent::new);

    // Extract and set specific MatchPage properties
    SegmentPage.NotionProperties properties = new SegmentPage.NotionProperties();
    Map<String, PageProperty> notionProperties = pageData.getProperties();

    notionHandler
        .createNotionClient()
        .ifPresent(
            client -> {
              properties.setParticipants(createProperty(notionProperties, "Participants", client));
              properties.setWinners(createProperty(notionProperties, "Winners", client));
              properties.setShows(createProperty(notionProperties, "Shows", client));
              properties.setSegment_Type(createProperty(notionProperties, "Segment Type", client));
              properties.setReferee_s(createProperty(notionProperties, "Referee(s)", client));
              properties.setRules(createProperty(notionProperties, "Rules", client));
              properties.setTitle_s(createProperty(notionProperties, "Title(s)", client));
              properties.setNotes(createProperty(notionProperties, "Notes", client));
              properties.setDate(createProperty(notionProperties, "Date", client));
            });

    matchPage.setProperties(properties);
    return matchPage;
  }

  // Helper method to create a NotionPage.Property from a PageProperty
  private NotionPage.Property createProperty(
      Map<String, PageProperty> notionProperties, String propertyName, NotionClient client) {
    PageProperty pageProperty = notionProperties.get(propertyName);
    if (pageProperty == null) {
      return null;
    }
    NotionPage.Property property = new NotionPage.Property();
    property.setId(pageProperty.getId());
    property.setType(pageProperty.getType() != null ? pageProperty.getType().getValue() : null);
    property.setTitle(pageProperty.getTitle());
    property.setRich_text(pageProperty.getRichText());
    property.setDate(pageProperty.getDate());
    property.setSelect(pageProperty.getSelect());
    // Convert Notion API's PageReference list to NotionPage.Relation list
    if (pageProperty.getRelation() != null) {
      List<NotionPage.Relation> relations =
          pageProperty.getRelation().stream()
              .map(
                  pageReference -> {
                    NotionPage.Relation newRelation = new NotionPage.Relation();
                    newRelation.setId(pageReference.getId());
                    return newRelation;
                  })
              .collect(Collectors.toList());
      property.setRelation(relations);
    }
    property.setPeople(pageProperty.getPeople());
    property.setNumber(pageProperty.getNumber());
    property.setCreated_time(pageProperty.getCreatedTime());
    property.setLast_edited_time(pageProperty.getLastEditedTime());
    property.setCreated_by(pageProperty.getCreatedBy());
    property.setLast_edited_by(pageProperty.getLastEditedBy());
    property.setUnique_id(pageProperty.getUniqueId());
    property.setFormula(pageProperty.getFormula());
    property.setHas_more(pageProperty.getHasMore());
    return property;
  }
}
