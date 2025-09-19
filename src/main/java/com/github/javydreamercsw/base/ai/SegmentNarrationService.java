package com.github.javydreamercsw.base.ai;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * AI service interface focused specifically on wrestling segment narration. This interface is
 * designed to be provider-agnostic, supporting OpenAI, Gemini, Claude, or other providers.
 */
public interface SegmentNarrationService {

  /**
   * Narrates a complete wrestling segment with rich context and storytelling.
   *
   * @param segmentContext Complete segment context including all participants and details
   * @return AI-generated segment narration
   */
  String narrateSegment(SegmentNarrationContext segmentContext);

  /**
   * Summarizes a segment narration.
   *
   * @param narration The narration to summarize.
   * @return A summary of the narration.
   */
  String summarizeNarration(String narration);

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

  /**
   * Generates text based on a given prompt. This is a general-purpose text generation method.
   *
   * @param prompt The prompt for text generation.
   * @return AI-generated text.
   */
  String generateText(String prompt);

  /** Context object containing all information needed for segment narration. */
  @Setter
  @Getter
  class SegmentNarrationContext {
    // Getters and setters
    private List<WrestlerContext> wrestlers;
    private SegmentTypeContext segmentType;
    private RefereeContext referee;
    private List<NPCContext> npcs;
    private String determinedOutcome;
    private VenueContext venue;
    private String audience;
    private List<String> recentSegmentNarrations;
  }

  /** Context for individual wrestlers in the segment. */
  @Setter
  @Getter
  class WrestlerContext {
    // Getters and setters
    private String name;
    private String description;
    private String team;
    private MoveSet moveSet;
    private List<String> feudsAndHeat;
    private List<String> recentSegments;
    private Map<String, Object> attributes;
  }

  /** Wrestler's move set including finishers and trademarks. */
  @Setter
  @Getter
  class MoveSet {
    // Getters and setters
    private List<Move> finishers;
    private List<Move> trademarks;
    private List<Move> commonMoves;
  }

  /** Individual wrestling move with description. */
  @Setter
  @Getter
  class Move {
    // Getters and setters
    private String name;
    private String description;
    private String type; // finisher, trademark, common, submission, etc.

    public Move() {}

    public Move(String name, String description, String type) {
      this.name = name;
      this.description = description;
      this.type = type;
    }
  }

  /** Match type and rules context. */
  @Setter
  @Getter
  class SegmentTypeContext {
    // Getters and setters
    private String segmentType; // Singles, Tag Team, Triple Threat, etc.
    private List<String> rules; // No DQ, Falls Count Anywhere, etc.
    private String stipulation; // Championship, #1 Contender, etc.
    private int timeLimit; // in minutes, 0 for no time limit
  }

  /** Referee context affecting segment flow. */
  @Setter
  @Getter
  class RefereeContext {
    // Getters and setters
    private String name;
    private String description;
    private String personality; // Strict, Lenient, Corrupt, etc.
    private Map<String, Object> attributes; // reaction_speed, bias, etc.
  }

  /** Non-player characters involved in the segment (commentators, announcers, etc.). */
  @Setter
  @Getter
  class NPCContext {
    // Getters and setters
    private String name;
    private String role; // Commentator, Ring Announcer, Interviewer, Manager, etc.
    private String description;
    private String personality;
    private Map<String, Object> attributes;
  }

  /** Venue context with rich details about the wrestling venue. */
  @Setter
  @Getter
  class VenueContext {
    // Getters and setters
    private String name;
    private String description;
    private String location; // City, State/Country
    private String type; // Arena, Stadium, Outdoor, etc.
    private int capacity;
    private String atmosphere; // Intimate, Electric, Historic, etc.
    private String significance; // WrestleMania venue, ECW stronghold, etc.
    private List<String> notableSegments; // Historic segments held here
    private Map<String, Object> attributes; // acoustics, lighting, etc.
  }
}
