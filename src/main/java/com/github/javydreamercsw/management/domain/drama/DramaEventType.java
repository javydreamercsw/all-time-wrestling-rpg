package com.github.javydreamercsw.management.domain.drama;

/**
 * Types of drama events that can occur in the ATW RPG system. Each type represents a different
 * category of storyline event that can affect wrestlers, rivalries, and the overall narrative.
 */
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
  MEDIA_CONTROVERSY("Media Controversy", "Public relations incidents and scandals");

  private final String displayName;
  private final String description;

  DramaEventType(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  /** Check if this event type typically involves multiple wrestlers. */
  public boolean isMultiWrestlerType() {
    return switch (this) {
      case BACKSTAGE_INCIDENT, BETRAYAL, ALLIANCE_FORMED, CHAMPIONSHIP_CHALLENGE -> true;
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
          MEDIA_CONTROVERSY ->
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
      case FAN_INTERACTION, SURPRISE_RETURN, BETRAYAL, ALLIANCE_FORMED, MEDIA_CONTROVERSY -> true;
      case BACKSTAGE_INCIDENT,
          SOCIAL_MEDIA_DRAMA,
          INJURY_INCIDENT,
          CONTRACT_DISPUTE,
          RETIREMENT_TEASE,
          CHAMPIONSHIP_CHALLENGE,
          PERSONAL_ISSUE ->
          false;
    };
  }

  /** Check if this event type can cause injuries. */
  public boolean canCauseInjury() {
    return switch (this) {
      case BACKSTAGE_INCIDENT, INJURY_INCIDENT -> true;
      case SOCIAL_MEDIA_DRAMA,
          FAN_INTERACTION,
          CONTRACT_DISPUTE,
          BETRAYAL,
          ALLIANCE_FORMED,
          SURPRISE_RETURN,
          RETIREMENT_TEASE,
          CHAMPIONSHIP_CHALLENGE,
          PERSONAL_ISSUE,
          MEDIA_CONTROVERSY ->
          false;
    };
  }

  @Override
  public String toString() {
    return displayName;
  }
}
