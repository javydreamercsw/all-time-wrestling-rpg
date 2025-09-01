package com.github.javydreamercsw.base.ai;

import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * Abstract base class for match narration services. Contains common logic for building prompts from
 * match context that can be used by any AI provider.
 */
public abstract class AbstractMatchNarrationService implements MatchNarrationService {

  /**
   * Builds a comprehensive prompt for match narration based on the provided context. This method is
   * provider-agnostic and can be used by any AI service implementation.
   */
  protected String buildMatchNarrationPrompt(@NonNull MatchNarrationContext context) {
    // Check if this is a promo or a match
    boolean isPromo = isPromoType(context);

    if (isPromo) {
      return buildPromoNarrationPrompt(context);
    } else {
      return buildMatchNarrationPromptInternal(context);
    }
  }

  /** Determines if the context is for a promo rather than a match. */
  private boolean isPromoType(@NonNull MatchNarrationContext context) {
    if (context.getMatchType() == null || context.getMatchType().getMatchType() == null) {
      return false;
    }

    String matchType = context.getMatchType().getMatchType().toLowerCase();
    return matchType.contains("promo")
        || (context.getWrestlers() != null && context.getWrestlers().size() == 1);
  }

  /** Builds a prompt specifically for promo narration. */
  private String buildPromoNarrationPrompt(@NonNull MatchNarrationContext context) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("You are a professional wrestling commentator and storyteller. ");
    prompt.append(
        "Narrate a compelling wrestling promo segment with vivid detail, authentic character voice,"
            + " and dramatic storytelling. ");
    prompt.append(
        "Focus on the wrestler's personality, motivations, and storyline development.\n\n");

    return buildPromoPromptInternal(context, prompt);
  }

  /** Builds the internal match narration prompt (non-promo). */
  private String buildMatchNarrationPromptInternal(@NonNull MatchNarrationContext context) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("You are a professional wrestling play-by-play commentator and storyteller. ");
    prompt.append(
        "Narrate a complete wrestling match with vivid detail, compelling storytelling, and"
            + " authentic wrestling commentary. ");
    prompt.append("Include crowd reactions, move descriptions, and dramatic moments.\n\n");

    return buildMatchPromptInternal(context, prompt);
  }

  /** Builds the internal match prompt content. */
  private String buildMatchPromptInternal(
      @NonNull MatchNarrationContext context, StringBuilder prompt) {
    // Match Setup
    prompt.append("MATCH SETUP:\n");
    prompt.append("Match Type: ").append(context.getMatchType().getMatchType()).append("\n");
    prompt.append("Stipulation: ").append(context.getMatchType().getStipulation()).append("\n");
    if (context.getMatchType().getRules() != null && !context.getMatchType().getRules().isEmpty()) {
      prompt
          .append("Special Rules: ")
          .append(String.join(", ", context.getMatchType().getRules()))
          .append("\n");
    }
    if (context.getMatchType().getTimeLimit() > 0) {
      prompt
          .append("Time Limit: ")
          .append(context.getMatchType().getTimeLimit())
          .append(" minutes\n");
    }

    // Venue Details
    if (context.getVenue() != null) {
      prompt.append("Venue: ").append(context.getVenue().getName()).append("\n");
      prompt.append("Location: ").append(context.getVenue().getLocation()).append("\n");
      prompt.append("Type: ").append(context.getVenue().getType()).append("\n");
      prompt.append("Capacity: ").append(context.getVenue().getCapacity()).append("\n");
      prompt.append("Description: ").append(context.getVenue().getDescription()).append("\n");
      prompt.append("Atmosphere: ").append(context.getVenue().getAtmosphere()).append("\n");
      prompt.append("Significance: ").append(context.getVenue().getSignificance()).append("\n");
      if (context.getVenue().getNotableMatches() != null
          && !context.getVenue().getNotableMatches().isEmpty()) {
        prompt
            .append("Notable Matches: ")
            .append(String.join(", ", context.getVenue().getNotableMatches()))
            .append("\n");
      }
    }

    prompt.append("Audience: ").append(context.getAudience()).append("\n\n");

    // Wrestlers
    prompt.append("WRESTLERS:\n");
    for (WrestlerContext wrestler : context.getWrestlers()) {
      prompt.append("- ").append(wrestler.getName()).append(":\n");
      prompt.append("  Description: ").append(wrestler.getDescription()).append("\n");

      if (wrestler.getMoveSet() != null) {
        if (wrestler.getMoveSet().getFinishers() != null
            && !wrestler.getMoveSet().getFinishers().isEmpty()) {
          prompt.append("  Finishers: ");
          prompt.append(
              wrestler.getMoveSet().getFinishers().stream()
                  .map(move -> move.getName() + " (" + move.getDescription() + ")")
                  .collect(Collectors.joining(", ")));
          prompt.append("\n");
        }

        if (wrestler.getMoveSet().getTrademarks() != null
            && !wrestler.getMoveSet().getTrademarks().isEmpty()) {
          prompt.append("  Trademark Moves: ");
          prompt.append(
              wrestler.getMoveSet().getTrademarks().stream()
                  .map(move -> move.getName() + " (" + move.getDescription() + ")")
                  .collect(Collectors.joining(", ")));
          prompt.append("\n");
        }
      }

      if (wrestler.getFeudsAndHeat() != null && !wrestler.getFeudsAndHeat().isEmpty()) {
        prompt
            .append("  Current Feuds/Heat: ")
            .append(String.join(", ", wrestler.getFeudsAndHeat()))
            .append("\n");
      }

      if (wrestler.getRecentMatches() != null && !wrestler.getRecentMatches().isEmpty()) {
        prompt
            .append("  Recent Match History: ")
            .append(String.join("; ", wrestler.getRecentMatches()))
            .append("\n");
      }
      prompt.append("\n");
    }

    // Referee
    if (context.getReferee() != null) {
      prompt.append("REFEREE:\n");
      prompt.append("- ").append(context.getReferee().getName()).append(":\n");
      prompt.append("  Description: ").append(context.getReferee().getDescription()).append("\n");
      prompt.append("  Personality: ").append(context.getReferee().getPersonality()).append("\n\n");
    }

    // NPCs
    if (context.getNpcs() != null && !context.getNpcs().isEmpty()) {
      prompt.append("SUPPORTING CHARACTERS:\n");
      for (NPCContext npc : context.getNpcs()) {
        prompt.append("- ").append(npc.getName()).append(" (").append(npc.getRole()).append("):\n");
        prompt.append("  Description: ").append(npc.getDescription()).append("\n");
        prompt.append("  Personality: ").append(npc.getPersonality()).append("\n");
      }
      prompt.append("\n");
    }

    // Recent Match Context
    if (context.getRecentMatchNarrations() != null
        && !context.getRecentMatchNarrations().isEmpty()) {
      prompt.append("RECENT MATCH CONTEXT (for continuity and avoiding repetition):\n");
      for (int i = 0; i < Math.min(3, context.getRecentMatchNarrations().size()); i++) {
        prompt.append("Recent Match ").append(i + 1).append(": ");
        prompt.append(
            context
                .getRecentMatchNarrations()
                .get(i)
                .substring(0, Math.min(200, context.getRecentMatchNarrations().get(i).length())));
        prompt.append("...\n");
      }
      prompt.append("\n");
    }

    // Predetermined Outcome
    prompt.append("PREDETERMINED OUTCOME:\n");
    prompt.append(context.getDeterminedOutcome()).append("\n\n");

    // Instructions
    prompt.append("NARRATION INSTRUCTIONS:\n");
    prompt.append(
        "1. Create a compelling 3-act structure: Opening/Setup, Middle/Action, Climax/Finish\n");
    prompt.append("2. Use the wrestlers' signature moves and personalities authentically\n");
    prompt.append("3. Include realistic crowd reactions and atmosphere\n");
    prompt.append("4. Incorporate the referee's personality into key moments\n");
    prompt.append("5. Reference the feuds/heat to add emotional weight to the action\n");
    prompt.append("6. Build to the predetermined outcome naturally and dramatically\n");
    prompt.append("7. Include commentary from the NPCs when appropriate\n");
    prompt.append("8. Make it feel like a real wrestling match with proper pacing\n");
    prompt.append("9. Use vivid, descriptive language that captures the excitement\n");
    prompt.append("10. Create a detailed, comprehensive match narration of 1500-2500 words\n");
    prompt.append(
        "11. Include multiple match segments: opening, early action, mid-match drama, climax, and"
            + " finish\n");
    prompt.append("12. Don't rush - take time to build drama and tell the complete story\n\n");

    prompt.append("Begin the match narration now:");

    return prompt.toString();
  }

  /** Builds the internal promo prompt content. */
  private String buildPromoPromptInternal(
      @NonNull MatchNarrationContext context, StringBuilder prompt) {
    // Promo Setup
    prompt.append("PROMO SETUP:\n");
    prompt.append("Segment Type: ").append(context.getMatchType().getMatchType()).append("\n");
    if (context.getMatchType().getStipulation() != null) {
      prompt.append("Purpose: ").append(context.getMatchType().getStipulation()).append("\n");
    }
    if (context.getMatchType().getTimeLimit() > 0) {
      prompt
          .append("Time Limit: ")
          .append(context.getMatchType().getTimeLimit())
          .append(" minutes\n");
    }

    // Venue Details
    if (context.getVenue() != null) {
      prompt.append("Venue: ").append(context.getVenue().getName());
      if (context.getVenue().getLocation() != null) {
        prompt.append(" (").append(context.getVenue().getLocation()).append(")");
      }
      prompt.append("\n");

      if (context.getVenue().getDescription() != null) {
        prompt
            .append("Venue Description: ")
            .append(context.getVenue().getDescription())
            .append("\n");
      }
      if (context.getVenue().getAtmosphere() != null) {
        prompt.append("Atmosphere: ").append(context.getVenue().getAtmosphere()).append("\n");
      }
    }

    // Wrestler Details (usually just one for promos)
    if (context.getWrestlers() != null && !context.getWrestlers().isEmpty()) {
      prompt.append("\nWRESTLER DETAILS:\n");
      for (WrestlerContext wrestler : context.getWrestlers()) {
        prompt.append("Name: ").append(wrestler.getName()).append("\n");
        if (wrestler.getDescription() != null) {
          prompt.append("Character: ").append(wrestler.getDescription()).append("\n");
        }
        if (wrestler.getFeudsAndHeat() != null && !wrestler.getFeudsAndHeat().isEmpty()) {
          prompt
              .append("Current Storylines: ")
              .append(String.join(", ", wrestler.getFeudsAndHeat()))
              .append("\n");
        }
        if (wrestler.getRecentMatches() != null && !wrestler.getRecentMatches().isEmpty()) {
          prompt
              .append("Recent Activity: ")
              .append(String.join(", ", wrestler.getRecentMatches()))
              .append("\n");
        }
        prompt.append("\n");
      }
    }

    // Audience
    if (context.getAudience() != null) {
      prompt.append("Audience: ").append(context.getAudience()).append("\n\n");
    }

    // Predetermined Outcome/Message
    if (context.getDeterminedOutcome() != null) {
      prompt.append("PROMO OBJECTIVE:\n");
      prompt.append(context.getDeterminedOutcome()).append("\n\n");
    }

    // Instructions for Promo
    prompt.append("PROMO NARRATION INSTRUCTIONS:\n");
    prompt.append("1. Create an engaging promo segment with authentic character voice\n");
    prompt.append("2. Focus on the wrestler's personality, motivations, and current storylines\n");
    prompt.append("3. Include realistic crowd reactions and atmosphere\n");
    prompt.append("4. Build emotional connection between wrestler and audience\n");
    prompt.append("5. Reference current feuds and storylines naturally\n");
    prompt.append("6. Show character development and progression\n");
    prompt.append("7. Use wrestling terminology and authentic dialogue\n");
    prompt.append("8. Create dramatic moments and memorable quotes\n");
    prompt.append("9. Make it feel like a real wrestling promo with proper pacing\n");
    prompt.append("10. Create a detailed promo narration of 800-1200 words\n");
    prompt.append("11. Include setup, main message delivery, and crowd reaction\n");
    prompt.append("12. Focus on storytelling and character rather than physical action\n\n");

    prompt.append("Begin the promo narration now:");

    return prompt.toString();
  }

  /**
   * Abstract method that each provider must implement to call their specific AI service.
   *
   * @param prompt The formatted prompt for match narration
   * @return The AI-generated match narration
   */
  protected abstract String callAIProvider(@NonNull String prompt);

  @Override
  public String narrateMatch(@NonNull MatchNarrationContext matchContext) {
    if (!isAvailable()) {
      return getProviderName()
          + " AI service is not available. Please configure the required API key.";
    }

    String prompt = buildMatchNarrationPrompt(matchContext);
    return callAIProvider(prompt);
  }
}
