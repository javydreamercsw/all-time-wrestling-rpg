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
package com.github.javydreamercsw.base.ai.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of the SegmentNarrationService interface. Generates realistic wrestling
 * segment narrations without calling external AI services. Perfect for testing, development, and
 * when no AI providers are available.
 */
@Service
@Profile({"test", "e2e"})
@Primary
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

    if (prompt.contains("Summarize the following segment narration")) {
      return "This is a mock summary.";
    }

    if (prompt.contains(
            "Generate a professional wrestling narrative segment appropriate for chapter")
        || prompt.contains("Generate a 'Post-Match' narrative segment")) {
      return generateMockCampaignEncounter(prompt);
    }

    log.info("Mock AI generating segment narration (simulated processing time)");
    if (prompt.contains("Generate a compelling wrestling narration")) {
      return generateMockTextNarration(prompt);
    }
    return generateMockNarration(prompt);
  }

  private String generateMockTextNarration(String prompt) {
    List<String> participants = extractParticipants(prompt);
    String wrestler1 = participants.size() > 0 ? participants.get(0) : "Wrestler A";
    String wrestler2 = participants.size() > 1 ? participants.get(1) : "Wrestler B";
    String venue = extractVenue(prompt);
    String type = prompt.contains("\"type\" : \"Promo\"") ? "Promo" : "Match";

    StringBuilder sb = new StringBuilder();
    sb.append(generateOpening(wrestler1, wrestler2, venue, type)).append("\n\n");
    if (type.equals("Match")) {
      sb.append(generateEarlyAction(wrestler1, wrestler2)).append("\n\n");
      sb.append(generateMidSegmentDrama(wrestler1, wrestler2)).append("\n\n");
      sb.append(generateClimaxAndFinish(wrestler1, wrestler2));
    } else {
      sb.append(wrestler1).append(" grabs the microphone and looks intensely at the crowd. ");
      sb.append("\"I've waited a long time for this moment,\" he declares. ");
      sb.append(wrestler2).append(" interrupts, walking down the ramp with a confident smirk. ");
      sb.append("The tension is thick as they stand face-to-face in the middle of the ring.");
    }
    return sb.toString();
  }

  private String generateMockCampaignEncounter(String prompt) {
    try {
      var choice1 =
          new com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO.Choice(
              "Accept the challenge like a hero.",
              "Accept Heroically",
              1,
              5,
              null,
              "One on One",
              null, // segmentRules
              "MATCH");
      var choice2 =
          new com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO.Choice(
              "Refuse the challenge and mock them.",
              "Refuse & Mock",
              -1,
              0,
              null,
              null,
              null, // segmentRules
              "BACKSTAGE");

      var response =
          new com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO(
              "Mock narrative: You are confronted by a local legend who wants to test your mettle.",
              List.of(choice1, choice2));

      return objectMapper.writeValueAsString(response);
    } catch (Exception e) {
      log.error("Error generating mock campaign encounter", e);
      return "{}";
    }
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
    if (prompt.contains(
            "Generate a professional wrestling narrative segment appropriate for chapter")
        || prompt.contains("Generate a 'Post-Match' narrative segment")) {
      return generateMockCampaignEncounter(prompt);
    }
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
    Pattern pattern = Pattern.compile("(\\d+)\\s+" + type);
    Matcher matcher = pattern.matcher(prompt);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
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

    // Try extracting from JSON "wrestlers" array block
    int wrestlersStart = prompt.indexOf("\"wrestlers\"");
    if (wrestlersStart != -1) {
      // Find the end of the wrestlers block (either next section or end of string)
      int wrestlersEnd = prompt.length();
      if (prompt.indexOf("\"venue\"", wrestlersStart) != -1)
        wrestlersEnd = Math.min(wrestlersEnd, prompt.indexOf("\"venue\"", wrestlersStart));
      if (prompt.indexOf("\"npcs\"", wrestlersStart) != -1)
        wrestlersEnd = Math.min(wrestlersEnd, prompt.indexOf("\"npcs\"", wrestlersStart));

      String wrestlersSection = prompt.substring(wrestlersStart, wrestlersEnd);

      // In the wrestlers section, we want names that are NOT inside a "moves" or "moveSet" block
      // But for a mock, let's just look for "name" : "..." that aren't preceded by "move" related
      // keys nearby
      Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
      Matcher matcher = namePattern.matcher(wrestlersSection);

      while (matcher.find()) {
        String name = matcher.group(1);
        // Basic heuristic: check if the name looks like a wrestler (not a move)
        // Moves often appear inside "moveSet" or "finisher" blocks
        int namePos = matcher.start();
        String contextBefore = wrestlersSection.substring(Math.max(0, namePos - 50), namePos);
        if (!contextBefore.contains("\"moves\"")
            && !contextBefore.contains("\"finishers\"")
            && !contextBefore.contains("\"trademarks\"")) {
          participants.add(name);
        }
      }
    }

    if (participants.isEmpty()) {
      // Fallback to "Full Roster:" if present (used in some other parts of the app)
      String rosterMarker = "Full Roster:";
      int rosterStart = prompt.indexOf(rosterMarker);
      if (rosterStart != -1) {
        int rosterEnd = prompt.length();
        // Look for likely next sections to stop parsing
        String[] nextSections = {
          "\nFactions:",
          "\nNext PLE",
          "\n**Other considerations:**",
          "\nAvailable Segment Types:",
          "\nIMPORTANT:"
        };
        for (String nextSection : nextSections) {
          int index = prompt.indexOf(nextSection, rosterStart);
          if (index != -1 && index < rosterEnd) {
            rosterEnd = index;
          }
        }

        String rosterSection = prompt.substring(rosterStart + rosterMarker.length(), rosterEnd);
        String[] lines = rosterSection.split("\\r?\\n");
        for (String line : lines) {
          if (line.trim().startsWith("- Name:")) {
            String name = line.substring(line.indexOf(':') + 1, line.indexOf(',')).trim();
            participants.add(name);
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
    String type;
    if (typeHint.equalsIgnoreCase("Match")) {
      type = "One on One";
    } else {
      type = "Promo";
    }

    List<String> segmentParticipants = new ArrayList<>();
    if (!participants.isEmpty()) {
      // Ensure unique participants
      List<String> availableParticipants = new ArrayList<>(participants);
      for (int i = 0; i < 3 && !availableParticipants.isEmpty(); i++) {
        int index = random.nextInt(availableParticipants.size());
        segmentParticipants.add(availableParticipants.remove(index));
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
