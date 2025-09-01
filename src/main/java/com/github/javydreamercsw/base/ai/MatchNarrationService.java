package com.github.javydreamercsw.base.ai;

import java.util.List;
import java.util.Map;

/**
 * AI service interface focused specifically on wrestling match narration. This interface is
 * designed to be provider-agnostic, supporting OpenAI, Gemini, Claude, or other providers.
 */
public interface MatchNarrationService {

  /**
   * Narrates a complete wrestling match with rich context and storytelling.
   *
   * @param matchContext Complete match context including all participants and details
   * @return AI-generated match narration
   */
  String narrateMatch(MatchNarrationContext matchContext);

  /**
   * Gets the name/type of this AI service provider.
   *
   * @return Provider name (e.g., "OpenAI", "Gemini", "Claude")
   */
  String getProviderName();

  /**
   * Checks if the AI service is available and properly configured.
   *
   * @return true if service is ready to use
   */
  boolean isAvailable();

  /** Context object containing all information needed for match narration. */
  class MatchNarrationContext {
    private List<WrestlerContext> wrestlers;
    private MatchTypeContext matchType;
    private RefereeContext referee;
    private List<NPCContext> npcs;
    private String determinedOutcome;
    private VenueContext venue;
    private String audience;
    private List<String> recentMatchNarrations;

    // Getters and setters
    public List<WrestlerContext> getWrestlers() {
      return wrestlers;
    }

    public void setWrestlers(List<WrestlerContext> wrestlers) {
      this.wrestlers = wrestlers;
    }

    public MatchTypeContext getMatchType() {
      return matchType;
    }

    public void setMatchType(MatchTypeContext matchType) {
      this.matchType = matchType;
    }

    public RefereeContext getReferee() {
      return referee;
    }

    public void setReferee(RefereeContext referee) {
      this.referee = referee;
    }

    public List<NPCContext> getNpcs() {
      return npcs;
    }

    public void setNpcs(List<NPCContext> npcs) {
      this.npcs = npcs;
    }

    public String getDeterminedOutcome() {
      return determinedOutcome;
    }

    public void setDeterminedOutcome(String determinedOutcome) {
      this.determinedOutcome = determinedOutcome;
    }

    public VenueContext getVenue() {
      return venue;
    }

    public void setVenue(VenueContext venue) {
      this.venue = venue;
    }

    public String getAudience() {
      return audience;
    }

    public void setAudience(String audience) {
      this.audience = audience;
    }

    public List<String> getRecentMatchNarrations() {
      return recentMatchNarrations;
    }

    public void setRecentMatchNarrations(List<String> recentMatchNarrations) {
      this.recentMatchNarrations = recentMatchNarrations;
    }
  }

  /** Context for individual wrestlers in the match. */
  class WrestlerContext {
    private String name;
    private String description;
    private MoveSet moveSet;
    private List<String> feudsAndHeat;
    private List<String> recentMatches;
    private Map<String, Object> attributes;

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public MoveSet getMoveSet() {
      return moveSet;
    }

    public void setMoveSet(MoveSet moveSet) {
      this.moveSet = moveSet;
    }

    public List<String> getFeudsAndHeat() {
      return feudsAndHeat;
    }

    public void setFeudsAndHeat(List<String> feudsAndHeat) {
      this.feudsAndHeat = feudsAndHeat;
    }

    public List<String> getRecentMatches() {
      return recentMatches;
    }

    public void setRecentMatches(List<String> recentMatches) {
      this.recentMatches = recentMatches;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
    }
  }

  /** Wrestler's move set including finishers and trademarks. */
  class MoveSet {
    private List<Move> finishers;
    private List<Move> trademarks;
    private List<Move> commonMoves;

    // Getters and setters
    public List<Move> getFinishers() {
      return finishers;
    }

    public void setFinishers(List<Move> finishers) {
      this.finishers = finishers;
    }

    public List<Move> getTrademarks() {
      return trademarks;
    }

    public void setTrademarks(List<Move> trademarks) {
      this.trademarks = trademarks;
    }

    public List<Move> getCommonMoves() {
      return commonMoves;
    }

    public void setCommonMoves(List<Move> commonMoves) {
      this.commonMoves = commonMoves;
    }
  }

  /** Individual wrestling move with description. */
  class Move {
    private String name;
    private String description;
    private String type; // finisher, trademark, common, submission, etc.

    public Move() {}

    public Move(String name, String description, String type) {
      this.name = name;
      this.description = description;
      this.type = type;
    }

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

  /** Match type and rules context. */
  class MatchTypeContext {
    private String matchType; // Singles, Tag Team, Triple Threat, etc.
    private List<String> rules; // No DQ, Falls Count Anywhere, etc.
    private String stipulation; // Championship, #1 Contender, etc.
    private int timeLimit; // in minutes, 0 for no time limit

    // Getters and setters
    public String getMatchType() {
      return matchType;
    }

    public void setMatchType(String matchType) {
      this.matchType = matchType;
    }

    public List<String> getRules() {
      return rules;
    }

    public void setRules(List<String> rules) {
      this.rules = rules;
    }

    public String getStipulation() {
      return stipulation;
    }

    public void setStipulation(String stipulation) {
      this.stipulation = stipulation;
    }

    public int getTimeLimit() {
      return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
      this.timeLimit = timeLimit;
    }
  }

  /** Referee context affecting match flow. */
  class RefereeContext {
    private String name;
    private String description;
    private String personality; // Strict, Lenient, Corrupt, etc.
    private Map<String, Object> attributes; // reaction_speed, bias, etc.

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getPersonality() {
      return personality;
    }

    public void setPersonality(String personality) {
      this.personality = personality;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
    }
  }

  /** Non-player characters involved in the match (commentators, announcers, etc.). */
  class NPCContext {
    private String name;
    private String role; // Commentator, Ring Announcer, Interviewer, Manager, etc.
    private String description;
    private String personality;
    private Map<String, Object> attributes;

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getPersonality() {
      return personality;
    }

    public void setPersonality(String personality) {
      this.personality = personality;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
    }
  }

  /** Venue context with rich details about the wrestling venue. */
  class VenueContext {
    private String name;
    private String description;
    private String location; // City, State/Country
    private String type; // Arena, Stadium, Outdoor, etc.
    private int capacity;
    private String atmosphere; // Intimate, Electric, Historic, etc.
    private String significance; // WrestleMania venue, ECW stronghold, etc.
    private List<String> notableMatches; // Historic matches held here
    private Map<String, Object> attributes; // acoustics, lighting, etc.

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getLocation() {
      return location;
    }

    public void setLocation(String location) {
      this.location = location;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public int getCapacity() {
      return capacity;
    }

    public void setCapacity(int capacity) {
      this.capacity = capacity;
    }

    public String getAtmosphere() {
      return atmosphere;
    }

    public void setAtmosphere(String atmosphere) {
      this.atmosphere = atmosphere;
    }

    public String getSignificance() {
      return significance;
    }

    public void setSignificance(String significance) {
      this.significance = significance;
    }

    public List<String> getNotableMatches() {
      return notableMatches;
    }

    public void setNotableMatches(List<String> notableMatches) {
      this.notableMatches = notableMatches;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
    }
  }
}
