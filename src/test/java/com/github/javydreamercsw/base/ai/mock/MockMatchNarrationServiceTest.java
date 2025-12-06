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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.Move;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.MoveSet;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.RefereeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for MockMatchNarrationService. Tests the mock AI provider's ability to generate
 * realistic wrestling segment narrations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Mock Match Narration Service Tests")
class MockMatchNarrationServiceTest {

  private record MockSegmentDTO(
      String segmentId,
      String type,
      String description,
      String outcome,
      List<String> participants) {}

  private MockSegmentNarrationService mockService;
  private SegmentNarrationContext testContext;

  @BeforeEach
  void setUp() {
    mockService = new MockSegmentNarrationService();
    testContext = createTestMatchContext();
  }

  @Test
  @DisplayName("Should always be available")
  void shouldAlwaysBeAvailable() {
    assertThat(mockService.isAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should return correct provider name")
  void shouldReturnCorrectProviderName() {
    assertThat(mockService.getProviderName()).isEqualTo("Mock AI");
  }

  @Test
  @DisplayName("Should generate non-empty segment narration")
  @SneakyThrows
  void shouldGenerateNonEmptyMatchNarration() {
    String narration = mockService.narrateSegment(testContext);

    assertThat(narration).isNotNull().isNotEmpty();

    List<MockSegmentDTO> segments =
        new ObjectMapper()
            .readValue(
                narration,
                new ObjectMapper()
                    .getTypeFactory()
                    .constructCollectionType(List.class, MockSegmentDTO.class));
    assertThat(segments).isNotEmpty();
  }

  @Test
  @DisplayName("Should include wrestler names in narration")
  @SneakyThrows
  void shouldIncludeWrestlerNamesInNarration() {
    String narration = mockService.narrateSegment(testContext);

    List<MockSegmentDTO> segments =
        new ObjectMapper()
            .readValue(
                narration,
                new ObjectMapper()
                    .getTypeFactory()
                    .constructCollectionType(List.class, MockSegmentDTO.class));
    assertThat(segments)
        .anySatisfy(
            segment ->
                assertThat(segment.participants())
                    .containsAnyOf("Wrestler A", "Wrestler B", "Wrestler C", "Wrestler D"));
  }

  @Test
  @DisplayName("Should include venue information in narration")
  @SneakyThrows
  void shouldIncludeVenueInformationInNarration() {
    String narration = mockService.narrateSegment(testContext);

    List<MockSegmentDTO> segments =
        new ObjectMapper()
            .readValue(
                narration,
                new ObjectMapper()
                    .getTypeFactory()
                    .constructCollectionType(List.class, MockSegmentDTO.class));
    assertThat(segments)
        .anySatisfy(
            segment -> assertThat(segment.description()).containsIgnoringCase("WrestleMania"));
  }

  @Test
  @DisplayName("Should include segment type in narration")
  @SneakyThrows
  void shouldIncludeMatchTypeInNarration() {
    String narration = mockService.narrateSegment(testContext);
    List<MockSegmentDTO> segments =
        new ObjectMapper()
            .readValue(
                narration,
                new ObjectMapper()
                    .getTypeFactory()
                    .constructCollectionType(List.class, MockSegmentDTO.class));
    assertThat(segments).anySatisfy(segment -> assertThat(segment.type()).contains("Match"));
  }

  @Test
  @DisplayName("Should generate different narrations for different contexts")
  @SneakyThrows
  void shouldGenerateDifferentNarrationsForDifferentContexts() {
    SegmentNarrationContext context1 = createTestMatchContext();
    SegmentNarrationContext context2 = createAlternativeMatchContext();

    String narration1 = mockService.narrateSegment(context1);
    String narration2 = mockService.narrateSegment(context2);

    assertThat(narration1).isNotEqualTo(narration2);

    List<MockSegmentDTO> segments =
        new ObjectMapper()
            .readValue(
                narration2,
                new ObjectMapper()
                    .getTypeFactory()
                    .constructCollectionType(List.class, MockSegmentDTO.class));
    assertThat(segments)
        .anySatisfy(
            segment ->
                assertThat(segment.participants())
                    .containsAnyOf("Wrestler A", "Wrestler B", "Wrestler C", "Wrestler D"));
  }

  @Test
  @DisplayName("Should handle minimal context gracefully")
  @SneakyThrows
  void shouldHandleMinimalContextGracefully() {
    SegmentNarrationContext minimalContext = new SegmentNarrationContext();

    // Set only required fields
    SegmentTypeContext matchType = new SegmentTypeContext();
    matchType.setSegmentType("Singles Match");
    minimalContext.setSegmentType(matchType);

    WrestlerContext wrestler1 = new WrestlerContext();
    wrestler1.setName("Wrestler A");
    WrestlerContext wrestler2 = new WrestlerContext();
    wrestler2.setName("Wrestler B");
    minimalContext.setWrestlers(Arrays.asList(wrestler1, wrestler2));

    String narration = mockService.narrateSegment(minimalContext);

    assertThat(narration).isNotNull().isNotEmpty();

    List<MockSegmentDTO> segments =
        new ObjectMapper()
            .readValue(
                narration,
                new ObjectMapper()
                    .getTypeFactory()
                    .constructCollectionType(List.class, MockSegmentDTO.class));
    assertThat(segments)
        .anySatisfy(
            segment ->
                assertThat(segment.participants()).containsAnyOf("Wrestler A", "Wrestler B"));
  }

  @Test
  @DisplayName("Should simulate processing time")
  void shouldSimulateProcessingTime() {
    long startTime = System.currentTimeMillis();

    mockService.narrateSegment(testContext);

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Should take at least 1 second (simulated processing time)
    assertThat(duration).isGreaterThanOrEqualTo(1000);
  }

  @Test
  @DisplayName("Should generate structured narration with multiple sections")
  @SneakyThrows
  void shouldGenerateStructuredNarrationWithMultipleSections() {
    String narration = mockService.narrateSegment(testContext);

    List<MockSegmentDTO> segments =
        new ObjectMapper()
            .readValue(
                narration,
                new ObjectMapper()
                    .getTypeFactory()
                    .constructCollectionType(List.class, MockSegmentDTO.class));
    assertThat(segments).hasSizeGreaterThanOrEqualTo(1);
  }

  /** Creates a comprehensive test segment context. */
  private SegmentNarrationContext createTestMatchContext() {
    SegmentNarrationContext context = new SegmentNarrationContext();

    // Match Type
    SegmentTypeContext matchType = new SegmentTypeContext();
    matchType.setSegmentType("Singles Match");
    matchType.setStipulation("WWE Championship");
    matchType.setRules(List.of("Standard Rules"));
    matchType.setTimeLimit(30);
    context.setSegmentType(matchType);

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("WrestleMania");
    venue.setLocation("MetLife Stadium, New Jersey");
    venue.setType("Stadium");
    venue.setCapacity(82500);
    venue.setDescription("The Grandest Stage of Them All");
    venue.setAtmosphere("Electric WrestleMania atmosphere");
    venue.setSignificance("The biggest event in sports entertainment");
    context.setVenue(venue);

    // Wrestlers
    WrestlerContext austin = new WrestlerContext();
    austin.setName("Stone Cold Steve Austin");
    austin.setDescription("Texas Rattlesnake - Anti-hero with beer-drinking attitude");

    MoveSet austinMoves = new MoveSet();
    austinMoves.setFinishers(
        List.of(new Move("Stone Cold Stunner", "Jaw-dropping finishing move", "finisher")));
    austinMoves.setTrademarks(
        List.of(new Move("Lou Thesz Press", "Explosive takedown with punches", "trademark")));
    austin.setMoveSet(austinMoves);
    austin.setFeudsAndHeat(List.of("Austin vs McMahon"));

    WrestlerContext rock = new WrestlerContext();
    rock.setName("The Rock");
    rock.setDescription("The People's Champion - Charismatic superstar");

    MoveSet rockMoves = new MoveSet();
    rockMoves.setFinishers(
        Arrays.asList(
            new Move("Rock Bottom", "Devastating slam", "finisher"),
            new Move("People's Elbow", "Electrifying elbow drop", "finisher")));
    rock.setMoveSet(rockMoves);
    rock.setFeudsAndHeat(List.of("Corporate Champion"));

    context.setWrestlers(Arrays.asList(austin, rock));

    // Referee
    RefereeContext referee = new RefereeContext();
    referee.setName("Earl Hebner");
    referee.setDescription("Veteran referee");
    referee.setPersonality("Fair but can be influenced");
    context.setReferee(referee);

    // NPCs
    NPCContext announcer = new NPCContext();
    announcer.setName("Howard Finkel");
    announcer.setRole("Ring Announcer");
    announcer.setDescription("The Fink - Legendary announcer");
    announcer.setPersonality("Professional and enthusiastic");

    NPCContext commentator = new NPCContext();
    commentator.setName("Jim Ross");
    commentator.setRole("Play-by-Play Commentator");
    commentator.setDescription("Good ol' JR");
    commentator.setPersonality("Passionate and iconic");

    context.setNpcs(Arrays.asList(announcer, commentator));

    // Context
    context.setAudience("82,500 screaming fans");
    context.setDeterminedOutcome("Stone Cold Steve Austin wins via Stone Cold Stunner");
    context.setRecentSegmentNarrations(List.of("Previous epic encounter..."));

    return context;
  }

  /** Creates an alternative segment context for comparison testing. */
  private SegmentNarrationContext createAlternativeMatchContext() {
    SegmentNarrationContext context = new SegmentNarrationContext();

    // Match Type
    SegmentTypeContext matchType = new SegmentTypeContext();
    matchType.setSegmentType("Hell in a Cell");
    matchType.setStipulation("Brothers of Destruction");
    matchType.setRules(Arrays.asList("No Escape", "Hardcore Rules"));
    context.setSegmentType(matchType);

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("Badd Blood");
    venue.setLocation("St. Louis, Missouri");
    context.setVenue(venue);

    // Wrestlers
    WrestlerContext undertaker = new WrestlerContext();
    undertaker.setName("The Undertaker");
    undertaker.setDescription("The Deadman");
    MoveSet undertakerMoves = new MoveSet();
    undertakerMoves.setFinishers(List.of(new Move("Tombstone Piledriver", "Finisher", "finisher")));
    undertaker.setMoveSet(undertakerMoves);

    WrestlerContext kane = new WrestlerContext();
    kane.setName("Kane");
    kane.setDescription("The Big Red Machine");
    MoveSet kaneMoves = new MoveSet();
    kaneMoves.setFinishers(List.of(new Move("Chokeslam", "Finisher", "finisher")));
    kane.setMoveSet(kaneMoves);

    context.setWrestlers(Arrays.asList(undertaker, kane));
    context.setAudience("Shocked crowd");
    context.setDeterminedOutcome("Kane debuts and destroys The Undertaker");

    return context;
  }
}
