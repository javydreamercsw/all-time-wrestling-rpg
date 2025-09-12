package com.github.javydreamercsw.management.domain.title;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for TitleReign entity. Tests the ATW RPG title reign tracking functionality. */
@DisplayName("TitleReign Tests")
class TitleReignTest {

  private TitleReign titleReign;
  private Title title;
  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    title = new Title();
    title.setName("Test Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);

    wrestler = new Wrestler();
    wrestler.setName("Test Champion");
    wrestler.setFans(120000L);
    wrestler.setStartingHealth(15);

    titleReign = new TitleReign();
    titleReign.setTitle(title);
    titleReign.setChampion(wrestler);
    titleReign.setReignNumber(1);
    titleReign.setStartDate(Instant.now());
  }

  @Test
  @DisplayName("Should initialize as current reign")
  void shouldInitializeAsCurrentReign() {
    assertThat(titleReign.isCurrentReign()).isTrue();
    assertThat(titleReign.getEndDate()).isNull();
  }

  @Test
  @DisplayName("Should end reign properly")
  void shouldEndReignProperly() {
    assertThat(titleReign.isCurrentReign()).isTrue();

    Instant endTime = Instant.now();
    titleReign.endReign(endTime);

    assertThat(titleReign.isCurrentReign()).isFalse();
    assertThat(titleReign.getEndDate()).isEqualTo(endTime);
  }

  @Test
  @DisplayName("Should calculate reign length in days")
  void shouldCalculateReignLengthInDays() {
    // Same day reign
    assertThat(titleReign.getReignLengthDays()).isEqualTo(0);

    // Simulate reign started 5 days ago
    titleReign.setStartDate(Instant.now().minusSeconds(5 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDays()).isEqualTo(5);

    // End the reign 2 days later (7 days total)
    Instant endDate = titleReign.getStartDate().plusSeconds(7 * 24 * 60 * 60);
    titleReign.endReign(endDate);
    assertThat(titleReign.getReignLengthDays()).isEqualTo(7);
  }

  @Test
  @DisplayName("Should format reign length display correctly")
  void shouldFormatReignLengthDisplayCorrectly() {
    // Less than 1 day
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("Less than 1 day");

    // Exactly 1 day
    titleReign.setStartDate(Instant.now().minusSeconds(24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("1 day");

    // Multiple days (less than a week)
    titleReign.setStartDate(Instant.now().minusSeconds(3 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("3 days");

    // Exactly 1 week
    titleReign.setStartDate(Instant.now().minusSeconds(7 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("1 week");

    // Multiple weeks
    titleReign.setStartDate(Instant.now().minusSeconds(14 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("2 weeks");

    // Weeks and days
    titleReign.setStartDate(Instant.now().minusSeconds(10 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("1 week and 3 days");

    // Exactly 1 month (30 days)
    titleReign.setStartDate(Instant.now().minusSeconds(30 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("1 month");

    // Multiple months
    titleReign.setStartDate(Instant.now().minusSeconds(60 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("2 months");

    // Months and days
    titleReign.setStartDate(Instant.now().minusSeconds(35 * 24 * 60 * 60));
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("1 month and 5 days");
  }

  @Test
  @DisplayName("Should create display string for current reign")
  void shouldCreateDisplayStringForCurrentReign() {
    titleReign.setReignNumber(2);
    wrestler.setName("John Cena");

    String display = titleReign.getDisplayString();

    assertThat(display).startsWith("John Cena - Reign #2");
    assertThat(display).endsWith("(Current)");
  }

  @Test
  @DisplayName("Should create display string for ended reign")
  void shouldCreateDisplayStringForEndedReign() {
    titleReign.setReignNumber(1);
    wrestler.setName("The Rock");
    titleReign.setStartDate(Instant.now().minusSeconds(10 * 24 * 60 * 60)); // 10 days ago
    titleReign.endReign(Instant.now());

    String display = titleReign.getDisplayString();

    assertThat(display).startsWith("The Rock - Reign #1");
    assertThat(display).contains("1 week and 3 days");
    assertThat(display).doesNotContain("(Current)");
  }

  @Test
  @DisplayName("Should handle reign numbers correctly")
  void shouldHandleReignNumbersCorrectly() {
    titleReign.setReignNumber(3);
    assertThat(titleReign.getReignNumber()).isEqualTo(3);

    String display = titleReign.getDisplayString();
    assertThat(display).contains("Reign #3");
  }

  @Test
  @DisplayName("Should handle notes field")
  void shouldHandleNotesField() {
    String notes = "Won title in a steel cage segment at WrestleMania";
    titleReign.setNotes(notes);

    assertThat(titleReign.getNotes()).isEqualTo(notes);
  }

  @Test
  @DisplayName("Should maintain relationships correctly")
  void shouldMaintainRelationshipsCorrectly() {
    assertThat(titleReign.getTitle()).isEqualTo(title);
    assertThat(titleReign.getChampion()).isEqualTo(wrestler);
    assertThat(titleReign.getTitle().getName()).isEqualTo("Test Championship");
    assertThat(titleReign.getChampion().getName()).isEqualTo("Test Champion");
  }

  @Test
  @DisplayName("Should handle very short reigns")
  void shouldHandleVeryShortReigns() {
    // Reign that lasts only a few hours
    Instant start = Instant.now().minusSeconds(6 * 60 * 60); // 6 hours ago
    titleReign.setStartDate(start);
    titleReign.endReign(Instant.now());

    assertThat(titleReign.getReignLengthDays()).isEqualTo(0);
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("Less than 1 day");
  }

  @Test
  @DisplayName("Should handle very long reigns")
  void shouldHandleVeryLongReigns() {
    // Reign that lasts over a year
    titleReign.setStartDate(Instant.now().minusSeconds(400 * 24 * 60 * 60)); // 400 days ago

    assertThat(titleReign.getReignLengthDays()).isEqualTo(400);

    String display = titleReign.getReignLengthDisplay();
    assertThat(display).contains("month"); // Should show in months
  }

  @Test
  @DisplayName("Should handle edge cases in time calculations")
  void shouldHandleEdgeCasesInTimeCalculations() {
    // Reign that starts and ends at exactly the same time
    Instant sameTime = Instant.now();
    titleReign.setStartDate(sameTime);
    titleReign.endReign(sameTime);

    assertThat(titleReign.getReignLengthDays()).isEqualTo(0);
    assertThat(titleReign.getReignLengthDisplay()).isEqualTo("Less than 1 day");
  }
}
