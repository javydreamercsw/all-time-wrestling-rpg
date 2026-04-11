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
package com.github.javydreamercsw.management.domain.drama;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Types of drama events that can occur in the ATW RPG system. Each type represents a different
 * category of storyline event that can affect wrestlers, rivalries, and the overall narrative.
 */
@Getter
public enum DramaEventType {
  /** Backstage altercations, locker room incidents, confrontations */
  BACKSTAGE_INCIDENT("Backstage Incident", "Physical or verbal confrontations behind the scenes"),

  /** Social media feuds, Twitter wars, Instagram drama */
  SOCIAL_MEDIA_DRAMA("Social Media Drama", "Online conflicts and public statements"),

  /** Training injuries, accident-related injuries, attack injuries */
  INJURY_INCIDENT("Injury Incident", "Events that cause or relate to wrestler injuries"),

  /** Fan meet-and-greets gone wrong, crowd incidents, fan reactions */
  FAN_INTERACTION("Fan Interaction", "Events involving wrestler-fan interactions"),

  /** Contract negotiations, salary disputes, booking complaints */
  CONTRACT_DISPUTE("Contract Dispute", "Business-related conflicts and negotiations"),

  /** Tag team breakups, stable betrayals, heel/face turns */
  BETRAYAL("Betrayal", "Trust broken between wrestlers or groups"),

  /** New tag teams formed, stables created, partnerships */
  ALLIANCE_FORMED("Alliance Formed", "New partnerships and alliances between wrestlers"),

  /** Wrestlers returning from injury, hiatus, or retirement */
  SURPRISE_RETURN("Surprise Return", "Unexpected comebacks and returns"),

  /** Retirement announcements, farewell tours, career endings */
  RETIREMENT_TEASE("Retirement Tease", "Career-ending threats and retirement hints"),

  /** Title challenges issued, championship opportunities */
  CHAMPIONSHIP_CHALLENGE("Championship Challenge", "Events related to title pursuits"),

  /** Personal life drama, family issues, relationship problems */
  PERSONAL_ISSUE("Personal Issue", "Private life matters affecting wrestling career"),

  /** Press conferences gone wrong, interview incidents, media scandals */
  MEDIA_CONTROVERSY("Media Controversy", "Public relations incidents and scandals"),

  /** Campaign specific: Rival encounter */
  CAMPAIGN_RIVAL("Campaign Rival", "A rival encounter in the solo campaign"),

  /** Campaign specific: Outsider encounter */
  CAMPAIGN_OUTSIDER("Campaign Outsider", "An outsider encounter in the solo campaign");

  private final String displayName;
  private final String description;

  DramaEventType(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  @JsonCreator
  public static DramaEventType fromString(String value) {
    if (value == null) {
      return null;
    }
    for (DramaEventType type : values()) {
      if (type.name().equalsIgnoreCase(value) || type.displayName.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown DramaEventType: " + value);
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  /** Check if this event type typically involves multiple wrestlers. */
  public boolean isMultiWrestlerType() {
    return switch (this) {
      case BACKSTAGE_INCIDENT,
          BETRAYAL,
          ALLIANCE_FORMED,
          CHAMPIONSHIP_CHALLENGE,
          CAMPAIGN_RIVAL,
          CAMPAIGN_OUTSIDER ->
          true;
      case SOCIAL_MEDIA_DRAMA,
          INJURY_INCIDENT,
          FAN_INTERACTION,
          CONTRACT_DISPUTE,
          SURPRISE_RETURN,
          RETIREMENT_TEASE,
          PERSONAL_ISSUE,
          MEDIA_CONTROVERSY ->
          false;
    };
  }

  /** Check if this event type typically creates heat between wrestlers. */
  public boolean createsHeat() {
    return switch (this) {
      case BACKSTAGE_INCIDENT,
          SOCIAL_MEDIA_DRAMA,
          BETRAYAL,
          CHAMPIONSHIP_CHALLENGE,
          MEDIA_CONTROVERSY,
          CAMPAIGN_RIVAL,
          CAMPAIGN_OUTSIDER ->
          true;
      case INJURY_INCIDENT,
          FAN_INTERACTION,
          CONTRACT_DISPUTE,
          ALLIANCE_FORMED,
          SURPRISE_RETURN,
          RETIREMENT_TEASE,
          PERSONAL_ISSUE ->
          false;
    };
  }

  /** Check if this event type typically affects fan count. */
  public boolean affectsFans() {
    return switch (this) {
      case FAN_INTERACTION,
          SURPRISE_RETURN,
          BETRAYAL,
          ALLIANCE_FORMED,
          MEDIA_CONTROVERSY,
          CAMPAIGN_OUTSIDER ->
          true;
      case BACKSTAGE_INCIDENT,
          SOCIAL_MEDIA_DRAMA,
          INJURY_INCIDENT,
          CONTRACT_DISPUTE,
          RETIREMENT_TEASE,
          CHAMPIONSHIP_CHALLENGE,
          PERSONAL_ISSUE,
          CAMPAIGN_RIVAL ->
          false;
    };
  }

  /** Check if this event type can cause injuries. */
  public boolean canCauseInjury() {
    return switch (this) {
      case BACKSTAGE_INCIDENT, INJURY_INCIDENT, CAMPAIGN_OUTSIDER -> true;
      case SOCIAL_MEDIA_DRAMA,
          FAN_INTERACTION,
          CONTRACT_DISPUTE,
          BETRAYAL,
          ALLIANCE_FORMED,
          SURPRISE_RETURN,
          RETIREMENT_TEASE,
          CHAMPIONSHIP_CHALLENGE,
          PERSONAL_ISSUE,
          MEDIA_CONTROVERSY,
          CAMPAIGN_RIVAL ->
          false;
    };
  }

  @Override
  public String toString() {
    return displayName;
  }
}
