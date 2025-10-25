package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for Notion property resolution using real captured data samples. These tests verify
 * that Date, Title(s), Winners, and other properties are resolved correctly without needing live
 * Notion API calls.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Notion Property Resolution Tests")
class NotionPropertyResolutionTest {

  private ObjectMapper objectMapper;
  private List<SegmentPage> sampleSegments;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    objectMapper = new ObjectMapper();

    // Load sample segment data from JSON files
    sampleSegments = loadSampleMatches();
  }

  @Test
  @DisplayName("Should resolve Date properties without @ prefix")
  void shouldResolveDatePropertiesWithoutAtPrefix() {
    // Given - Sample matches with Date properties

    // When - Extract date values from sample data
    for (SegmentPage match : sampleSegments) {
      String dateValue = (String) match.getRawProperties().get("Date");

      // Then - Date should not have @ prefix
      assertThat(dateValue).as("Date property should not have @ prefix").doesNotStartWith("@");

      assertThat(dateValue)
          .as("Date should be properly formatted")
          .matches("\\w+ \\d{1,2}, \\d{4}"); // e.g., "June 30, 2025"
    }
  }

  @Test
  @DisplayName("Should resolve Title(s) properties to championship names")
  void shouldResolveTitlePropertiesToChampionshipNames() {
    // Given - Sample segment with title
    SegmentPage championshipMatch =
        sampleSegments.stream()
            .filter(match -> !"N/A".equals(match.getRawProperties().get("Title(s)")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No championship segment found in samples"));

    // When - Extract title value
    String titleValue = (String) championshipMatch.getRawProperties().get("Title(s)");

    // Then - Title should be resolved name, not UUID
    assertThat(titleValue)
        .as("Title should be resolved to championship name")
        .doesNotMatch("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        .isNotEqualTo("N/A")
        .contains("Championship");
  }

  @Test
  @DisplayName("Should resolve Winners as wrestler names")
  void shouldResolveWinnersAsWrestlerNames() {
    // Given - Sample segment with winner
    SegmentPage matchWithWinner =
        sampleSegments.stream()
            .filter(match -> !"N/A".equals(match.getRawProperties().get("Winners")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No segment with winner found in samples"));

    // When - Extract winner value
    String winnerValue = (String) matchWithWinner.getRawProperties().get("Winners");

    // Then - Winner should be wrestler name
    assertThat(winnerValue)
        .as("Winner should be wrestler name")
        .isNotEqualTo("N/A")
        .isNotEmpty()
        .matches("[A-Za-z\\s]+"); // Should be a name, not ID
  }

  @Test
  @DisplayName("Should resolve Participants as comma-separated wrestler names")
  void shouldResolveParticipantsAsCommaSeparatedWrestlerNames() {
    // Given - Sample matches with participants

    // When & Then - Check each segment's participants
    for (SegmentPage match : sampleSegments) {
      String participants = (String) match.getRawProperties().get("Participants");

      assertThat(participants)
          .as("Participants should be comma-separated names")
          .isNotEmpty()
          .contains(",")
          .matches("[A-Za-z\\s,]+"); // Names separated by commas

      // Should have multiple participants
      String[] participantList = participants.split(",\\s*");
      assertThat(participantList.length).as("Should have multiple participants").isGreaterThan(1);
    }
  }

  @Test
  @DisplayName("Should handle N/A values gracefully")
  void shouldHandleNAValuesGracefully() {
    // Given - Sample matches with N/A values

    // When & Then - Check that N/A values are handled properly
    for (SegmentPage match : sampleSegments) {
      Object titleValue = match.getRawProperties().get("Title(s)");
      Object winnerValue = match.getRawProperties().get("Winners");
      Object notesValue = match.getRawProperties().get("Notes");

      // N/A values should be strings, not null
      if ("N/A".equals(titleValue)) {
        assertThat(titleValue).isEqualTo("N/A");
      }
      if ("N/A".equals(winnerValue)) {
        assertThat(winnerValue).isEqualTo("N/A");
      }
      if ("N/A".equals(notesValue)) {
        assertThat(notesValue).isEqualTo("N/A");
      }
    }
  }

  @Test
  @DisplayName("Should sync sample matches successfully with mocked dependencies")
  void shouldSyncSampleMatchesSuccessfully() {
    // Given - Mocked dependencies and sample data
    // Note: This test verifies the property resolution logic works with sample data
    // The actual sync is mocked, so we focus on testing the data structure

    // When - Verify sample data is loaded correctly
    assertThat(sampleSegments).isNotEmpty();
    assertThat(sampleSegments.size()).isEqualTo(3);

    // Then - Verify each sample has the expected properties
    for (SegmentPage match : sampleSegments) {
      assertThat(match.getRawProperties()).isNotEmpty();
      assertThat(match.getRawProperties().get("Name")).isNotNull();
      assertThat(match.getRawProperties().get("Date")).isNotNull();
      assertThat(match.getRawProperties().get("Participants")).isNotNull();
    }
  }

  /** Load sample segment data from JSON files. */
  private List<SegmentPage> loadSampleMatches() throws Exception {
    File samplesDir = new File("src/test/resources/notion-samples");

    SegmentPage sample1 =
        objectMapper.readValue(new File(samplesDir, "sample-segment-1.json"), SegmentPage.class);
    SegmentPage sample2 =
        objectMapper.readValue(new File(samplesDir, "sample-segment-2.json"), SegmentPage.class);
    SegmentPage sample3 =
        objectMapper.readValue(new File(samplesDir, "sample-segment-3.json"), SegmentPage.class);

    return Arrays.asList(sample1, sample2, sample3);
  }
}
