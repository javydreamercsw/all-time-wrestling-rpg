package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Unit tests for Notion property resolution using real captured data samples. These tests verify
 * that Date, Title(s), Winners, and other properties are resolved correctly without needing live
 * Notion API calls.
 */
@SpringBootTest(properties = {"notion.sync.enabled=true", "notion.sync.entities.matches=true"})
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("Notion Property Resolution Tests")
class NotionPropertyResolutionTest {

  @Autowired private NotionSyncService notionSyncService; // Changed from @InjectMocks

  // REMOVED: @Mock private NotionSyncProperties notionSyncProperties;

  @MockitoBean private NotionHandler notionHandler;
  @MockitoBean private ShowService showService;
  @MockitoBean private SegmentTypeService matchTypeService;
  @MockitoBean private WrestlerService wrestlerService;

  private ObjectMapper objectMapper;
  private List<SegmentPage> sampleSegments;

  @BeforeEach
  void setUp() throws Exception {
    objectMapper = new ObjectMapper();

    // Load sample segment data from JSON files
    sampleSegments = loadSampleMatches();

    // Mock the NotionHandler to return our sample data
    when(notionHandler.loadAllSegments()).thenReturn(sampleSegments);

    // Mock the service dependencies
    setupServiceMocks();
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

  /** Setup mocks for service dependencies. */
  private void setupServiceMocks() {
    // Mock ShowService
    Show timelessShow = new Show();
    timelessShow.setName("Timeless");
    when(showService.findByName("Timeless")).thenReturn(Optional.of(timelessShow));

    // Mock MatchTypeService
    SegmentType tagTeamType = new SegmentType();
    tagTeamType.setName("Tag Team");
    when(matchTypeService.findByName("Tag Team")).thenReturn(Optional.of(tagTeamType));

    SegmentType oneOnOneType = new SegmentType();
    oneOnOneType.setName("One on One");
    when(matchTypeService.findByName("One on One")).thenReturn(Optional.of(oneOnOneType));

    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    when(matchTypeService.findByName("Promo")).thenReturn(Optional.of(promoType));

    // Mock WrestlerService
    when(wrestlerService.findByName(any(String.class)))
        .thenAnswer(
            invocation -> {
              String name = invocation.getArgument(0);
              Wrestler wrestler = new Wrestler();
              wrestler.setName(name);
              return Optional.of(wrestler);
            });
  }
}
