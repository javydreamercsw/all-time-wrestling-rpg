package com.github.javydreamercsw.base.ai.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import java.util.ArrayList;
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
  private final ObjectMapper objectMapper = new ObjectMapper();

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
    try {
      // The prompt is not a simple JSON, but a text with a JSON schema in it.
      // We need to extract the expected number of matches and promos from the prompt.
      int matches = extractCount(prompt, "matches");
      int promos = extractCount(prompt, "promos");

      List<String> segmentTypes = extractSegmentTypes(prompt);
      List<String> participants = extractParticipants(prompt);
      String venue = extractVenue(prompt);

      List<Object> segments = new ArrayList<>();

      for (int i = 0; i < matches; i++) {
        segments.add(createMockSegment(segmentTypes, participants, "Match", venue));
      }
      for (int i = 0; i < promos; i++) {
        segments.add(createMockSegment(segmentTypes, participants, "Promo", venue));
      }

      return objectMapper.writeValueAsString(segments);
    } catch (Exception e) {
      log.error("Error generating mock narration.", e);
      return "[]";
    }
  }

  private int extractCount(String prompt, String type) {
    String searchString = "Generate a JSON array of exactly ";
    int startIndex = prompt.indexOf(searchString);
    if (startIndex != -1) {
      String substring = prompt.substring(startIndex + searchString.length());
      String[] parts = substring.split(" ");
      if (parts.length > 1) {
        if (type.equals("matches")) {
          return Integer.parseInt(parts[0]);
        } else if (type.equals("promos") && parts.length > 3) {
          return Integer.parseInt(parts[2]);
        }
      }
    }
    // Default to a random number between 1 and 3 if parsing fails
    return random.nextInt(3) + 1;
  }

  private List<String> extractSegmentTypes(String prompt) {
    String searchString = "Available Segment Types: ";
    int startIndex = prompt.indexOf(searchString);
    if (startIndex != -1) {
      String substring = prompt.substring(startIndex + searchString.length());
      int endIndex = substring.indexOf('\n');
      if (endIndex != -1) {
        String typesString = substring.substring(0, endIndex);
        return List.of(typesString.split(", "));
      }
    }
    return List.of("Match", "Promo");
  }

  private List<String> extractParticipants(String prompt) {
    List<String> participants = new ArrayList<>();
    String wrestlersBlockSearchString = "\"wrestlers\" : [";
    int wrestlersBlockStartIndex = prompt.indexOf(wrestlersBlockSearchString);
    if (wrestlersBlockStartIndex != -1) {
      int wrestlersBlockEndIndex = prompt.indexOf(']', wrestlersBlockStartIndex);
      if (wrestlersBlockEndIndex != -1) {
        String wrestlersBlock = prompt.substring(wrestlersBlockStartIndex, wrestlersBlockEndIndex);
        String searchString = "\"name\" : \"";
        int lastIndex = 0;
        while (lastIndex != -1) {
          lastIndex = wrestlersBlock.indexOf(searchString, lastIndex);
          if (lastIndex != -1) {
            int endIndex = wrestlersBlock.indexOf("\"", lastIndex + searchString.length());
            if (endIndex != -1) {
              participants.add(
                  wrestlersBlock.substring(lastIndex + searchString.length(), endIndex));
              lastIndex = endIndex;
            } else {
              break;
            }
          }
        }
      }
    }

    if (participants.isEmpty()) {
      return List.of("Wrestler A", "Wrestler B", "Wrestler C", "Wrestler D");
    }
    return participants;
  }

  private Object createMockSegment(
      List<String> segmentTypes, List<String> participants, String typeHint, String venue) {
    String type =
        segmentTypes.stream()
            .filter(t -> t.toLowerCase().contains(typeHint.toLowerCase()))
            .findAny()
            .orElse(segmentTypes.get(random.nextInt(segmentTypes.size())));

    List<String> segmentParticipants = new ArrayList<>();
    if (!participants.isEmpty()) {
      // Ensure at least one actual participant is included
      segmentParticipants.add(participants.get(random.nextInt(participants.size())));
      int additionalParticipants = random.nextInt(2); // 0 or 1 additional participant
      for (int i = 0; i < additionalParticipants; i++) {
        segmentParticipants.add(participants.get(random.nextInt(participants.size())));
      }
    } else {
      // Fallback if no participants are extracted
      segmentParticipants.add("Wrestler X");
    }

    return new MockSegmentDTO(
        java.util.UUID.randomUUID().toString(),
        type,
        "Mock description for " + type + " at " + venue,
        "Mock outcome for " + type,
        segmentParticipants);
  }

  private record MockSegmentDTO(
      String segmentId,
      String type,
      String description,
      String outcome,
      List<String> participants) {}

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

  /** Extracts wrestler names from the JSON node. */
  private List<String> extractWrestlerNames(JsonNode rootNode) {
    List<String> names = new ArrayList<>();
    if (rootNode.has("wrestlers")) {
      for (JsonNode wrestlerNode : rootNode.get("wrestlers")) {
        if (wrestlerNode.has("name")) {
          names.add(wrestlerNode.get("name").asText());
        }
      }
    }
    return names;
  }

  /** Extracts venue information from the JSON-like text in the prompt. */
  private String extractVenue(String prompt) {
    String searchString = "\"name\" : \"";
    int venueBlockStart = prompt.indexOf("\"venue\" :");
    if (venueBlockStart != -1) {
      int nameIndex = prompt.indexOf(searchString, venueBlockStart);
      if (nameIndex != -1) {
        int startIndex = nameIndex + searchString.length();
        int endIndex = prompt.indexOf("\"", startIndex);
        if (endIndex != -1) {
          return prompt.substring(startIndex, endIndex);
        }
      }
    }
    return "the arena";
  }

  /** Extracts segment type from the JSON node. */
  private String extractSegmentType(JsonNode rootNode) {
    if (rootNode.has("segmentType") && rootNode.get("segmentType").has("segmentType")) {
      return rootNode.get("segmentType").get("segmentType").asText("wrestling segment");
    }
    return "wrestling segment";
  }
}
