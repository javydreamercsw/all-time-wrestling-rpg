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
import com.github.javydreamercsw.base.ai.notion.TeamPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;

@Slf4j
public class TeamNotionHandler {

  private final NotionHandler notionHandler;

  public TeamNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /** Loads a team from the Notion database by name. */
  public Optional<TeamPage> loadTeam(@NonNull String teamName) {
    log.debug("Loading team: '{}'", teamName);

    String teamDbId = notionHandler.getDatabaseId("Teams");
    if (teamDbId == null) {
      log.warn("Teams database not found in workspace");
      return Optional.empty();
    }

    return notionHandler
        .createNotionClient()
        .flatMap(
            client -> {
              try {
                return notionHandler.loadEntityFromDatabase(
                    client, teamDbId, teamName, "Team", this::mapPageToTeamPage);
              } catch (Exception e) {
                log.error("Failed to load team: {}", teamName, e);
                return Optional.empty();
              }
            });
  }

  /**
   * Loads all teams from the Notion Teams database.
   *
   * @return List of all TeamPage objects from the Teams database
   */
  public List<TeamPage> loadAllTeams() {
    log.debug("Loading all teams from Teams database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String teamDbId = notionHandler.getDatabaseId("Teams");
    if (teamDbId == null) {
      log.warn("Teams database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, teamDbId, "Team", this::mapPageToTeamPage);
              } catch (Exception e) {
                log.error("Failed to load all teams", e);
                throw new RuntimeException(
                    "Failed to load teams from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a TeamPage object. */
  private TeamPage mapPageToTeamPage(@NonNull Page pageData, @NonNull String teamName) {
    TeamPage teamPage =
        notionHandler.mapPageToGenericEntity(
            pageData, teamName, "Team", TeamPage::new, TeamPage.NotionParent::new, true);

    Map<String, PageProperty> notionProperties = pageData.getProperties();
    TeamPage.NotionProperties props = mapPagePropertiesToNotionProperties(notionProperties);
    teamPage.setProperties(props);

    return teamPage;
  }

  /** Maps raw properties to TeamPage.NotionProperties. */
  private TeamPage.NotionProperties mapPagePropertiesToNotionProperties(
      Map<String, notion.api.v1.model.pages.PageProperty> notionProperties) {
    TeamPage.NotionProperties props = new TeamPage.NotionProperties();
    if (notionProperties == null) return props;
    if (notionProperties.containsKey("Member 1"))
      props.setMembers(toProperty(notionProperties.get("Member 1")));
    if (notionProperties.containsKey("Leader"))
      props.setLeader(toProperty(notionProperties.get("Leader")));
    if (notionProperties.containsKey("TeamType"))
      props.setTeamType(toProperty(notionProperties.get("TeamType")));
    if (notionProperties.containsKey("Status"))
      props.setStatus(toProperty(notionProperties.get("Status")));
    if (notionProperties.containsKey("FormedDate"))
      props.setFormedDate(toProperty(notionProperties.get("FormedDate")));
    if (notionProperties.containsKey("DisbandedDate"))
      props.setDisbandedDate(toProperty(notionProperties.get("DisbandedDate")));
    if (notionProperties.containsKey("Faction"))
      props.setFaction(toProperty(notionProperties.get("Faction")));
    return props;
  }

  /** Converts a Notion PageProperty to a TeamPage.Property. */
  private TeamPage.Property toProperty(notion.api.v1.model.pages.PageProperty pageProperty) {
    if (pageProperty == null) return null;
    TeamPage.Property property = new TeamPage.Property();
    property.setId(pageProperty.getId());
    property.setType(pageProperty.getType() != null ? pageProperty.getType().getValue() : null);
    property.setTitle(pageProperty.getTitle());
    property.setRich_text(pageProperty.getRichText());
    property.setDate(pageProperty.getDate());
    property.setSelect(pageProperty.getSelect());
    // Map relations
    if (pageProperty.getRelation() != null) {
      List<TeamPage.Relation> relations = new java.util.ArrayList<>();
      pageProperty
          .getRelation()
          .forEach(
              ref -> {
                TeamPage.Relation rel = new TeamPage.Relation();
                rel.setId(ref.getId());
                relations.add(rel);
              });
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
