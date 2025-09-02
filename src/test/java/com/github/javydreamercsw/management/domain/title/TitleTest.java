package com.github.javydreamercsw.management.domain.title;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.wrestler.TitleTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for Title entity. Tests the ATW RPG championship system functionality. */
@DisplayName("Title Tests")
class TitleTest {

  private Title title;
  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setUp() {
    title = new Title();
    title.setName("Test Championship");
    title.setTier(TitleTier.WORLD);
    title.setDescription("Test title for unit tests");

    wrestler1 = createWrestler("Wrestler 1", 120000L);
    wrestler2 = createWrestler("Wrestler 2", 150000L);
  }

  @Test
  @DisplayName("Should initialize with default values")
  void shouldInitializeWithDefaultValues() {
    Title newTitle = new Title();

    assertThat(newTitle.getIsActive()).isTrue();
    assertThat(newTitle.getIsVacant()).isTrue();
    assertThat(newTitle.getCurrentChampion()).isNull();
    assertThat(newTitle.getTitleHistory()).isEmpty();
  }

  @Test
  @DisplayName("Should award title to new champion")
  void shouldAwardTitleToNewChampion() {
    // Initially vacant
    assertThat(title.getIsVacant()).isTrue();
    assertThat(title.getCurrentChampion()).isNull();

    Instant beforeAward = Instant.now();
    title.awardTitle(wrestler1);
    Instant afterAward = Instant.now();

    // Title should now be held
    assertThat(title.getIsVacant()).isFalse();
    assertThat(title.getCurrentChampion()).isEqualTo(wrestler1);
    assertThat(title.getTitleWonDate()).isBetween(beforeAward, afterAward);

    // Should create title reign
    assertThat(title.getTitleHistory()).hasSize(1);
    TitleReign reign = title.getTitleHistory().get(0);
    assertThat(reign.getChampion()).isEqualTo(wrestler1);
    assertThat(reign.getTitle()).isEqualTo(title);
    assertThat(reign.isCurrentReign()).isTrue();
  }

  @Test
  @DisplayName("Should transfer title between champions")
  void shouldTransferTitleBetweenChampions() {
    // Award to first champion
    title.awardTitle(wrestler1);
    TitleReign firstReign = title.getCurrentReign().orElseThrow();

    // Award to second champion
    title.awardTitle(wrestler2);

    // First reign should be ended
    assertThat(firstReign.isCurrentReign()).isFalse();
    assertThat(firstReign.getEndDate()).isNotNull();

    // Second reign should be current
    assertThat(title.getCurrentChampion()).isEqualTo(wrestler2);
    assertThat(title.getTitleHistory()).hasSize(2);

    TitleReign currentReign = title.getCurrentReign().orElseThrow();
    assertThat(currentReign.getChampion()).isEqualTo(wrestler2);
    assertThat(currentReign.isCurrentReign()).isTrue();
  }

  @Test
  @DisplayName("Should vacate title properly")
  void shouldVacateTitleProperly() {
    // Award title first
    title.awardTitle(wrestler1);
    TitleReign reign = title.getCurrentReign().orElseThrow();

    // Vacate title
    title.vacateTitle();

    // Title should be vacant
    assertThat(title.getIsVacant()).isTrue();
    assertThat(title.getCurrentChampion()).isNull();
    assertThat(title.getTitleWonDate()).isNull();

    // Reign should be ended
    assertThat(reign.isCurrentReign()).isFalse();
    assertThat(reign.getEndDate()).isNotNull();
  }

  @Test
  @DisplayName("Should calculate current reign days")
  void shouldCalculateCurrentReignDays() {
    // Vacant title
    assertThat(title.getCurrentReignDays()).isEqualTo(0);

    // Award title
    title.awardTitle(wrestler1);

    // Should be 0 days (same day)
    assertThat(title.getCurrentReignDays()).isEqualTo(0);

    // Simulate title won yesterday
    title.setTitleWonDate(Instant.now().minusSeconds(24 * 60 * 60));
    assertThat(title.getCurrentReignDays()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should count total reigns")
  void shouldCountTotalReigns() {
    assertThat(title.getTotalReigns()).isEqualTo(0);

    title.awardTitle(wrestler1);
    assertThat(title.getTotalReigns()).isEqualTo(1);

    title.awardTitle(wrestler2);
    assertThat(title.getTotalReigns()).isEqualTo(2);

    title.vacateTitle();
    assertThat(title.getTotalReigns()).isEqualTo(2); // Vacating doesn't add reign
  }

  @Test
  @DisplayName("Should check wrestler eligibility")
  void shouldCheckWrestlerEligibility() {
    title.setTier(TitleTier.WORLD); // Requires 100k fans

    Wrestler eligibleWrestler = createWrestler("Eligible", 120000L);
    Wrestler ineligibleWrestler = createWrestler("Ineligible", 50000L);

    assertThat(title.isWrestlerEligible(eligibleWrestler)).isTrue();
    assertThat(title.isWrestlerEligible(ineligibleWrestler)).isFalse();
  }

  @Test
  @DisplayName("Should get challenge costs from tier")
  void shouldGetChallengeCostsFromTier() {
    title.setTier(TitleTier.EXTREME);
    assertThat(title.getChallengeCost()).isEqualTo(15000L);
    assertThat(title.getContenderEntryFee()).isEqualTo(15000L);

    title.setTier(TitleTier.WORLD);
    assertThat(title.getChallengeCost()).isEqualTo(15000L);
    assertThat(title.getContenderEntryFee()).isEqualTo(15000L);
  }

  @Test
  @DisplayName("Should create display names")
  void shouldCreateDisplayNames() {
    // Vacant title
    title.setName("World Championship");
    assertThat(title.getDisplayName()).isEqualTo("World Championship (Vacant)");

    // Title with champion
    title.awardTitle(wrestler1);
    assertThat(title.getDisplayName()).isEqualTo("World Championship (Champion: Wrestler 1)");
  }

  @Test
  @DisplayName("Should show status emojis")
  void shouldShowStatusEmojis() {
    // Active vacant title
    title.setIsActive(true);
    title.setIsVacant(true);
    assertThat(title.getStatusEmoji()).isEqualTo("👑❓");

    // Active title with champion
    title.awardTitle(wrestler1);
    assertThat(title.getStatusEmoji()).isEqualTo("👑");

    // Inactive title
    title.setIsActive(false);
    assertThat(title.getStatusEmoji()).isEqualTo("🚫");
  }

  @Test
  @DisplayName("Should handle multiple title changes")
  void shouldHandleMultipleTitleChanges() {
    // Award to wrestler1
    title.awardTitle(wrestler1);
    assertThat(title.getTotalReigns()).isEqualTo(1);

    // Award to wrestler2
    title.awardTitle(wrestler2);
    assertThat(title.getTotalReigns()).isEqualTo(2);

    // Award back to wrestler1 (second reign)
    title.awardTitle(wrestler1);
    assertThat(title.getTotalReigns()).isEqualTo(3);

    // Verify current champion
    assertThat(title.getCurrentChampion()).isEqualTo(wrestler1);

    // Verify only one current reign
    long currentReigns =
        title.getTitleHistory().stream().filter(TitleReign::isCurrentReign).count();
    assertThat(currentReigns).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle vacating already vacant title")
  void shouldHandleVacatingAlreadyVacantTitle() {
    // Title is vacant by default
    assertThat(title.getIsVacant()).isTrue();

    // Vacating vacant title should not cause issues
    title.vacateTitle();

    assertThat(title.getIsVacant()).isTrue();
    assertThat(title.getCurrentChampion()).isNull();
    assertThat(title.getTitleHistory()).isEmpty();
  }

  private Wrestler createWrestler(String name, Long fans) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(System.nanoTime()); // Unique ID for testing
    wrestler.setName(name);
    wrestler.setFans(fans);
    wrestler.setStartingHealth(15);
    wrestler.setIsPlayer(true);
    wrestler.updateTier();
    return wrestler;
  }
}
