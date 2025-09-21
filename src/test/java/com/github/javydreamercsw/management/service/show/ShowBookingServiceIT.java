package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(com.github.javydreamercsw.management.config.TestConfig.class)
class ShowBookingServiceIT {

  @Autowired ShowBookingService showBookingService;
  @Autowired SeasonService seasonService;
  @Autowired SeasonRepository seasonRepository;
  @Autowired ShowTypeRepository showTypeRepository;
  @Autowired SegmentTypeRepository matchTypeRepository;
  @Autowired SegmentTypeService matchTypeService;
  @Autowired SegmentRepository matchRepository;
  @Autowired WrestlerRepository wrestlerRepository;
  @Autowired DeckRepository deckRepository; // Autowire DeckRepository

  private Season testSeason;
  private ShowType weeklyShowType;
  private ShowType ppvShowType;
  private SegmentType singlesMatchType;

  @BeforeEach
  void setUp() {
    // Create test season
    testSeason = new Season();
    testSeason.setName("Test Season");
    testSeason.setDescription("Test season for show booking");

    testSeason.setShowsPerPpv(5);
    testSeason.setIsActive(true);
    testSeason = seasonRepository.save(testSeason);

    // Create show types
    weeklyShowType = new ShowType();
    weeklyShowType.setName("Weekly Show");
    weeklyShowType.setDescription("Regular weekly wrestling show");
    weeklyShowType = showTypeRepository.save(weeklyShowType);

    ppvShowType = new ShowType();
    ppvShowType.setName("PPV");
    ppvShowType.setDescription("Pay-Per-View special event");
    ppvShowType = showTypeRepository.save(ppvShowType);

    // Get the singles segment type for tests
    singlesMatchType = matchTypeRepository.findByName("One on One").orElseThrow();

    // Create test wrestlers
    for (int i = 1; i <= 8; i++) {
      Wrestler wrestler = createTestWrestler("Test Wrestler " + i);
      wrestlerRepository.save(wrestler);
    }
  }

  @Test
  @DisplayName("Should book regular show with specified segment count")
  void shouldBookRegularShowWithSpecifiedSegmentCount() {
    // Given
    String showName = "Monday Night Wrestling";
    String showDescription = "Weekly wrestling showcase";
    int segmentCount = 5;

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
    assertThat(allSegments)
        .hasSizeGreaterThanOrEqualTo(3); // At least 3 segments (matches + promos)

    // Verify all segments have valid participants
    for (Segment segment : allSegments) {
      System.out.println(segment);
      assertThat(segment.getWrestlers()).isNotEmpty();
      if (!segment.getSegmentType().getName().equals("Promo")) {
        assertThat(segment.getWinner()).isNotNull();
      }
      assertThat(segment.getShow()).isEqualTo(show);
    }
  }

  @Test
  @DisplayName("Should book PPV with enhanced segment quality")
  void shouldBookPPVWithEnhancedSegmentQuality() {
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

    // Check segments and promos were created (PPVs should have multiple segments, may be fewer due
    // to wrestler
    // availability)
    List<Segment> allSegments = showBookingService.getSegmentsForShow(ppv.getId());
    assertThat(allSegments)
        .hasSizeGreaterThanOrEqualTo(4); // At least 4 segments for PPV (matches + promos)
  }

  @Test
  @DisplayName("Should generate show statistics correctly")
  void shouldGenerateShowStatisticsCorrectly() {
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

  @Test
  @DisplayName("Should handle insufficient wrestlers gracefully")
  void shouldHandleInsufficientWrestlersGracefully() {
    // Given - Remove most wrestlers, leaving only 2
    List<Wrestler> allWrestlers = wrestlerRepository.findAll();
    for (int i = 2; i < allWrestlers.size(); i++) {
      Wrestler wrestlerToDelete = allWrestlers.get(i);
      // Delete associated decks first
      deckRepository.deleteByWrestler(wrestlerToDelete);
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

  @Test
  @DisplayName("Should reject invalid segment count")
  void shouldRejectInvalidSegmentCount() {
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

  @Test
  @DisplayName("Should handle non-existent show type gracefully")
  void shouldHandleNonExistentShowTypeGracefully() {
    // Given
    String showName = "Test Show";
    String showDescription = "Test Description";

    // When
    Optional<Show> result =
        showBookingService.bookShow(showName, showDescription, "Non-Existent Show Type", 4);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should add show to active season")
  void shouldAddShowToActiveSeason() {
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

  @Test
  @DisplayName("Should create segments with proper results")
  void shouldCreateSegmentsWithProperResults() {
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
        assertThat(segment.getWinner()).isNotNull();
      }
      assertThat(segment.getSegmentDate()).isNotNull();
      assertThat(segment.getShow()).isEqualTo(show);
      assertThat(segment.getWrestlers()).isNotEmpty();
      assertThat(segment.getIsNpcGenerated()).isTrue();
    }
  }

  private Wrestler createTestWrestler(String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    wrestler.setFans(10000L);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(0);
    wrestler.setStartingStamina(0);
    wrestler.setLowStamina(0);
    wrestler.setDeckSize(15);
    wrestler.setIsPlayer(false);
    return wrestler;
  }
}
