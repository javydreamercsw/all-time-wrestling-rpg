package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import com.github.javydreamercsw.base.ai.openai.OpenAISegmentNarrationService;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ShowBookingServiceTest extends ManagementIntegrationTest {
  @Autowired ShowBookingService showBookingService;
  @Autowired SeasonService seasonService;
  @Autowired WrestlerService wrestlerService;
  @Autowired DeckRepository deckRepository;
  @Autowired DeckCardRepository deckCardRepository;
  @Autowired SegmentTypeRepository segmentTypeRepository;
  @MockitoBean private OpenAISegmentNarrationService openAIService;

  private Season testSeason;
  private ShowType weeklyShowType;

  @BeforeEach
  public void setUp() throws Exception {
    testSeason =
        seasonService.createOrUpdateSeason(
            "Test Season",
            java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
            true);

    // Create and save a weekly show type
    weeklyShowType =
        showTypeService.createOrUpdateShowType("Weekly Show", "A weekly wrestling show");

    // Create segment types
    segmentTypeService.createOrUpdateSegmentType("One on One", "1 vs 1 match");
    segmentTypeService.createOrUpdateSegmentType("Promo", "A promotional segment");

    for (int i = 0; i < 10; i++) {
      wrestlerService.createWrestler("Wrestler " + i, true, null);
    }
  }

  @Test
  @DisplayName("Should book regular show with specified segment count")
  void shouldBookRegularShowWithSpecifiedSegmentCount() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      String showName = "Monday Night Wrestling";
      String showDescription = "Weekly wrestling showcase";
      int segmentCount = 5;
      log.info("Booking show with {} segments", segmentCount);

      // When
      Optional<Show> result =
          showBookingService.bookShow(showName, showDescription, "Weekly Show", segmentCount);

      // Then
      assertThat(result).isPresent();
      Show show = result.get();
      assertThat(show.getName()).isEqualTo(showName);
      assertThat(show.getDescription()).isEqualTo(showDescription);
      assertThat(show.getType()).isEqualTo(weeklyShowType);

      // Check segments and promos were created (may be fewer than requested due to wrestler
      // availability)
      List<Segment> allSegments = showBookingService.getSegmentsForShow(show.getId());
      log.info("Booked {} segments", allSegments.size());
      allSegments.forEach(
          segment -> {
            log.info("Segment: {}", segment.getSegmentType().getName());
            segment
                .getWrestlers()
                .forEach(wrestler -> log.info("  Wrestler: {}", wrestler.getName()));
          });
      assertThat(allSegments)
          .hasSizeGreaterThanOrEqualTo(3); // At least 3 segments (matches + promos)

      // Verify all segments have valid participants
      for (Segment segment : allSegments) {
        assertThat(segment.getWrestlers()).isNotEmpty();
        if (!segment.getSegmentType().getName().equals("Promo")) {
          assertThat(segment.getWinners()).isNotEmpty();
        }
        assertThat(segment.getShow()).isEqualTo(show);
      }
    }
  }

  @Test
  @DisplayName("Should book PPV with enhanced segment quality")
  void shouldBookPPVWithEnhancedSegmentQuality() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      String ppvName = "Ultimate Showdown 2024";
      String ppvDescription = "The biggest wrestling event of the year";

      // When
      Optional<Show> result = showBookingService.bookPPV(ppvName, ppvDescription);

      // Then
      assertThat(result).isPresent();
      Show ppv = result.get();
      assertThat(ppv.getName()).isEqualTo(ppvName);
      assertThat(ppv.getDescription()).isEqualTo(ppvDescription);
      assertThat(ppv.getType().getName()).isEqualTo("PPV");

      // Check segments and promos were created (PPVs should have multiple segments, may be fewer
      // due
      // to wrestler
      // availability)
      List<Segment> allSegments = showBookingService.getSegmentsForShow(ppv.getId());
      assertThat(allSegments)
          .hasSizeGreaterThanOrEqualTo(4); // At least 4 segments for PPV (matches + promos)
    }
  }

  @Test
  @DisplayName("Should generate show statistics correctly")
  void shouldGenerateShowStatisticsCorrectly() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given - Book a show
      Optional<Show> showOpt =
          showBookingService.bookShow("Test Show", "Test Description", "Weekly Show", 4);
      assertThat(showOpt).isPresent();
      Show show = showOpt.get();

      // When
      ShowBookingService.ShowStatistics stats = showBookingService.getShowStatistics(show.getId());

      // Then
      assertThat(stats.totalSegments()).isGreaterThanOrEqualTo(3); // At least 3 segments
      assertThat(stats.totalPromos()).isGreaterThanOrEqualTo(1); // At least 1 promo
      assertThat(stats.totalWrestlers()).isGreaterThan(0);
      assertThat(stats.rivalrySegments()).isGreaterThanOrEqualTo(0);
      assertThat(stats.multiPersonSegments()).isGreaterThanOrEqualTo(0);
    }
  }

  @Test
  @DisplayName("Should handle insufficient wrestlers gracefully")
  void shouldHandleInsufficientWrestlersGracefully() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given - Remove most wrestlers, leaving only 2
      List<Wrestler> allWrestlers = wrestlerRepository.findAll();
      for (int i = 2; i < allWrestlers.size(); i++) {
        Wrestler wrestlerToDelete = allWrestlers.get(i);
        // Delete associated decks first
        List<com.github.javydreamercsw.management.domain.deck.Deck> decksToDelete =
            deckRepository.findByWrestler(wrestlerToDelete);
        for (com.github.javydreamercsw.management.domain.deck.Deck deck : decksToDelete) {
          deckCardRepository.deleteAll(deck.getCards());
        }
        deckRepository.deleteAll(decksToDelete);
        wrestlerRepository.delete(wrestlerToDelete);
      }

      // When
      Optional<Show> result =
          showBookingService.bookShow("Test Show", "Test Description", "Weekly Show", 5);

      // Then - Should still create show but with fewer segments
      assertThat(result).isPresent();
      Show show = result.get();

      List<Segment> allSegments = showBookingService.getSegmentsForShow(show.getId());
      // Should create at least some segments even with limited wrestlers
      assertThat(allSegments).hasSizeGreaterThanOrEqualTo(1);
    }
  }

  @Test
  @DisplayName("Should reject invalid segment count")
  void shouldRejectInvalidSegmentCount() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      String showName = "Invalid Show";
      String showDescription = "Show with invalid segment count";

      // When - Try to book show with too few segments
      Optional<Show> result1 =
          showBookingService.bookShow(showName, showDescription, "Weekly Show", 2);

      // Then
      assertThat(result1).isEmpty();

      // When - Try to book show with too many segments
      Optional<Show> result2 =
          showBookingService.bookShow(showName, showDescription, "Weekly Show", 11);

      // Then
      assertThat(result2).isEmpty();
    }
  }

  @Test
  @DisplayName("Should handle non-existent show type gracefully")
  void shouldHandleNonExistentShowTypeGracefully() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      String showName = "Test Show";
      String showDescription = "Test Description";

      // When
      Optional<Show> result =
          showBookingService.bookShow(showName, showDescription, "Non-Existent Show Type", 4);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Test
  @DisplayName("Should add show to active season")
  void shouldAddShowToActiveSeason() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      String showName = "Season Test Show";
      String showDescription = "Show to test season integration";

      // When
      Optional<Show> result =
          showBookingService.bookShow(showName, showDescription, "Weekly Show", 4);

      // Then
      assertThat(result).isPresent();
      Show show = result.get();

      // Verify show was added to the active season
      Assertions.assertNotNull(testSeason.getId());
      Season updatedSeason = seasonRepository.findById(testSeason.getId()).orElseThrow();
      assertThat(updatedSeason.getShows()).contains(show);
    }
  }

  @Test
  @DisplayName("Should create segments with proper results")
  void shouldCreateSegmentsWithProperResults() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      Optional<Show> showOpt =
          showBookingService.bookShow(
              "Segment Test Show", "Testing segment creation", "Weekly Show", 3);
      assertThat(showOpt).isPresent();
      Show show = showOpt.get();

      // When
      List<Segment> allSegments = showBookingService.getSegmentsForShow(show.getId());

      // Then
      assertThat(allSegments)
          .hasSizeGreaterThanOrEqualTo(3); // At least 3 segments (matches + promos)

      for (Segment segment : allSegments) {
        // Each segment should have basic required fields
        if (!segment.getSegmentType().getName().equals("Promo")) { // Only matches have winners
          assertThat(segment.getWinners()).isNotEmpty();
        }
        assertThat(segment.getSegmentDate()).isNotNull();
        assertThat(segment.getShow()).isEqualTo(show);
        assertThat(segment.getWrestlers()).isNotEmpty();
        assertThat(segment.getIsNpcGenerated()).isTrue();
      }
    }
  }
}
