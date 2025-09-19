package com.github.javydreamercsw.base.ai.mock;

import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import java.util.List;
import java.util.Random;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of the SegmentNarrationService interface. Generates realistic wrestling
 * segment narrations without calling external AI services. Perfect for testing, development, and
 * when no AI providers are available.
 */
@Service
@Slf4j
public class MockSegmentNarrationService extends AbstractSegmentNarrationService {

  private final Random random = new Random();

  @Override
  protected String callAIProvider(@NonNull String prompt) {
    // Simulate AI processing time
    try {
      Thread.sleep(random.nextInt(2000) + 1000); // 1-3 seconds
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    log.info("Mock AI generating segment narration (simulated processing time)");
    return generateMockNarration(prompt);
  }

  @Override
  public String getProviderName() {
    return "Mock AI";
  }

  @Override
  public boolean isAvailable() {
    return true; // Always available for testing
  }

  @Override
  public String generateText(@NonNull String prompt) {
    return generateMockNarration(prompt);
  }

  /** Generates a realistic mock wrestling segment narration. */
  private String generateMockNarration(@NonNull String prompt) {
    // Extract wrestler names from the prompt for personalization
    List<String> wrestlerNames = extractWrestlerNames(prompt);
    String wrestler1 = !wrestlerNames.isEmpty() ? wrestlerNames.get(0) : "Wrestler A";
    String wrestler2 = wrestlerNames.size() > 1 ? wrestlerNames.get(1) : "Wrestler B";

    // Extract venue information
    String venue = extractVenue(prompt);
    String segmentType = extractSegmentType(prompt);

    StringBuilder narration = new StringBuilder();

    // Opening
    narration.append(generateOpening(wrestler1, wrestler2, venue, segmentType));
    narration.append("\n\n");

    // Early action
    narration.append(generateEarlyAction(wrestler1, wrestler2));
    narration.append("\n\n");

    // Mid-segment drama
    narration.append(generateMidSegmentDrama(wrestler1, wrestler2));
    narration.append("\n\n");

    // Climax and finish
    narration.append(generateClimaxAndFinish(wrestler1, wrestler2));

    return narration.toString();
  }

  private String generateOpening(
      String wrestler1, String wrestler2, String venue, String segmentType) {
    String[] openings = {
      "The atmosphere is electric here at %s! The crowd is on their feet as we prepare for this %s"
          + " between %s and %s!",
      "What a night we have in store! %s is packed to capacity for this incredible %s featuring %s"
          + " taking on %s!",
      "The energy is palpable here at %s! Two of wrestling's finest, %s and %s, are about to"
          + " collide in this %s!"
    };

    String opening = openings[random.nextInt(openings.length)];
    return String.format(opening, venue, segmentType, wrestler1, wrestler2);
  }

  private String generateEarlyAction(String wrestler1, String wrestler2) {
    String[] actions = {
      "The bell rings and both competitors circle each other cautiously. %s makes the first move"
          + " with a quick takedown attempt, but %s counters beautifully!",
      "%s starts aggressively, charging at %s with a series of strikes. The crowd roars as %s"
          + " fights back with incredible intensity!",
      "We're underway! %s and %s lock up in the center of the ring. %s gains the early advantage"
          + " with superior technique!"
    };

    String action = actions[random.nextInt(actions.length)];
    return String.format(action, wrestler1, wrestler2, wrestler2, wrestler1, wrestler2, wrestler1);
  }

  private String generateMidSegmentDrama(String wrestler1, String wrestler2) {
    String[] dramas = {
      "The momentum shifts as %s hits a devastating signature move! %s is down but not out - the"
          + " resilience is incredible! The crowd is split, half cheering for each competitor!",
      "What a sequence! %s attempts a high-risk maneuver but %s scouts it perfectly and counters!"
          + " This segment could go either way!",
      "The action spills outside the ring! %s and %s are giving everything they have! The referee"
          + " is struggling to maintain control as the intensity reaches fever pitch!"
    };

    String drama = dramas[random.nextInt(dramas.length)];
    return String.format(drama, wrestler1, wrestler2, wrestler2, wrestler1, wrestler1, wrestler2);
  }

  private String generateClimaxAndFinish(String wrestler1, String wrestler2) {
    // Randomly determine winner
    boolean wrestler1Wins = random.nextBoolean();
    String winner = wrestler1Wins ? wrestler1 : wrestler2;
    String loser = wrestler1Wins ? wrestler2 : wrestler1;

    String[] finishes = {
      "In an incredible turn of events, %s manages to hit their finishing move! The referee counts:"
          + " ONE! TWO! THREE! %s has done it! What an amazing victory here tonight!",
      "The crowd is on their feet! %s pulls off the upset of the century! %s fought valiantly but"
          + " %s was just too much tonight! What a segment!",
      "UNBELIEVABLE! %s with the victory! %s gave everything they had, but %s proved why they're"
          + " one of the best in the business! The crowd shows their appreciation for both"
          + " competitors!"
    };

    String finish = finishes[random.nextInt(finishes.length)];
    return String.format(finish, winner, winner, loser, winner, winner, loser, winner);
  }

  /** Extracts wrestler names from the prompt. */
  private List<String> extractWrestlerNames(String prompt) {
    // Simple extraction - look for wrestler names after "WRESTLERS:" section
    List<String> names = new java.util.ArrayList<>();

    if (prompt.contains("WRESTLERS:")) {
      String wrestlersSection = prompt.substring(prompt.indexOf("WRESTLERS:"));
      String[] lines = wrestlersSection.split("\n");

      for (String line : lines) {
        if (line.trim().startsWith("- ") && line.contains(":")) {
          String name = line.substring(line.indexOf("- ") + 2, line.indexOf(':')).trim();
          if (!name.isEmpty()) {
            names.add(name);
          }
        }
      }
    }

    return names;
  }

  /** Extracts venue information from the prompt. */
  private String extractVenue(String prompt) {
    if (prompt.contains("Venue: ")) {
      String venueSection = prompt.substring(prompt.indexOf("Venue: ") + 7);
      String venue = venueSection.split("\n")[0].trim();
      return venue.isEmpty() ? "the arena" : venue;
    }
    return "the arena";
  }

  /** Extracts segment type from the prompt. */
  private String extractSegmentType(String prompt) {
    if (prompt.contains("Segment Type: ")) {
      String segmentTypeSection = prompt.substring(prompt.indexOf("Segment Type: ") + 12);
      String segmentType = segmentTypeSection.split("\n")[0].trim();
      return segmentType.isEmpty() ? "wrestling segment" : segmentType;
    }
    return "wrestling segment";
  }
}
