package com.github.javydreamercsw.management.service.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for TitleService. Tests the ATW RPG championship management functionality. */
@ExtendWith(MockitoExtension.class)
@DisplayName("TitleService Tests")
class TitleServiceTest {

  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private Clock clock;

  @InjectMocks private TitleService titleService;

  private final Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");

  @BeforeEach
  void setUp() {
    // No general setup needed, stubbing moved to specific tests.
  }

  @Test
  @DisplayName("Should create new title")
  void shouldCreateNewTitle() {
    // Given
    when(clock.instant()).thenReturn(fixedInstant);
    when(titleRepository.saveAndFlush(any(Title.class)))
        .thenAnswer(
            invocation -> {
              Title title = invocation.getArgument(0);
              title.setId(1L);
              return title;
            });

    // When
    Title result =
        titleService.createTitle("World Championship", "Top title", WrestlerTier.MAIN_EVENTER);

    // Then
    assertThat(result.getName()).isEqualTo("World Championship");
    assertThat(result.getDescription()).isEqualTo("Top title");
    assertThat(result.getTier()).isEqualTo(WrestlerTier.MAIN_EVENTER);
    assertThat(result.getIsActive()).isTrue();
    assertThat(result.getIsVacant()).isTrue();
    verify(titleRepository).saveAndFlush(any(Title.class));
  }

  @Test
  @DisplayName("Should get title by ID")
  void shouldGetTitleById() {
    // Given
    Title title = createTitle("Test Title", WrestlerTier.MAIN_EVENTER);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // When
    Optional<Title> result = titleService.getTitleById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(title);
  }

  @Test
  @DisplayName("Should award title to eligible wrestler")
  void shouldAwardTitleToEligibleWrestler() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createWrestler("Champion", 120000L); // Eligible for World title

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.saveAndFlush(any(Title.class))).thenReturn(title);

    // When
    Optional<Title> result = titleService.awardTitle(1L, 1L);

    // Then
    assertThat(result).isPresent();
    assertThat(title.getCurrentChampion()).isEqualTo(wrestler);
    assertThat(title.getIsVacant()).isFalse();
    verify(titleRepository).saveAndFlush(title);
  }

  @Test
  @DisplayName("Should not award title to ineligible wrestler")
  void shouldNotAwardTitleToIneligibleWrestler() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createWrestler("Rookie", 50000L); // Not eligible for World title

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    // When
    Optional<Title> result = titleService.awardTitle(1L, 1L);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should vacate title")
  void shouldVacateTitle() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createWrestler("Champion", 120000L);
    title.awardTitle(wrestler);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(titleRepository.saveAndFlush(any(Title.class))).thenReturn(title);

    // When
    Optional<Title> result = titleService.vacateTitle(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(title.getIsVacant()).isTrue();
    assertThat(title.getCurrentChampion()).isNull();
    verify(titleRepository).saveAndFlush(title);
  }

  @Test
  @DisplayName("Should successfully challenge for title")
  void shouldSuccessfullyChallengeForTitle() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Challenger", 120000L); // Has enough fans

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(challenger));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(challenger);

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(1L, 1L);

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.message()).isEqualTo("Challenge accepted");
    assertThat(challenger.getFans()).isEqualTo(105000L); // 120k - 15k challenge cost
    verify(wrestlerRepository).saveAndFlush(challenger);
  }

  @Test
  @DisplayName("Should fail challenge when wrestler has insufficient fans for eligibility")
  void shouldFailChallengeWhenWrestlerHasInsufficientFansForEligibility() {
    // Given
    Title title =
        createTitle("Extreme Championship", WrestlerTier.MAIN_EVENTER); // Requires 25k fans
    Wrestler challenger = createWrestler("Poor Challenger", 10000L); // Only 10k fans, not eligible

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(challenger));

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(1L, 1L);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("needs 25,000 fans"); // Will fail eligibility check first
  }

  @Test
  @DisplayName("Should fail challenge when wrestler is ineligible")
  void shouldFailChallengeWhenWrestlerIsIneligible() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Rookie", 50000L); // Not eligible for World title

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(challenger));

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(1L, 1L);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("needs 100,000 fans");
  }

  @Test
  @DisplayName("Should get eligible challengers")
  void shouldGetEligibleChallengers() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler eligible1 = createWrestler("Eligible 1", 120000L);
    Wrestler eligible2 = createWrestler("Eligible 2", 150000L);
    Wrestler ineligible = createWrestler("Ineligible", 50000L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findAll()).thenReturn(Arrays.asList(eligible1, eligible2, ineligible));

    // When
    List<Wrestler> result = titleService.getEligibleChallengers(1L);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(Wrestler::getName)
        .containsExactlyInAnyOrder("Eligible 1", "Eligible 2");
  }

  @Test
  @DisplayName("Should get titles held by wrestler")
  void shouldGetTitlesHeldByWrestler() {
    // Given
    Wrestler wrestler = createWrestler("Champion", 120000L);
    Title title1 = createTitle("Title 1", WrestlerTier.MAIN_EVENTER);
    Title title2 = createTitle("Title 2", WrestlerTier.CONTENDER);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findByCurrentChampion(wrestler)).thenReturn(Arrays.asList(title1, title2));

    // When
    List<Title> result = titleService.getTitlesHeldBy(1L);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactly(title1, title2);
  }

  @Test
  @DisplayName("Should update title information")
  void shouldUpdateTitleInformation() {
    // Given
    Title title = createTitle("Old Name", WrestlerTier.MAIN_EVENTER);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(titleRepository.saveAndFlush(any(Title.class))).thenReturn(title);

    // When
    Optional<Title> result = titleService.updateTitle(1L, "New Name", "New description", false);

    // Then
    assertThat(result).isPresent();
    assertThat(title.getName()).isEqualTo("New Name");
    assertThat(title.getDescription()).isEqualTo("New description");
    assertThat(title.getIsActive()).isFalse();
    verify(titleRepository).saveAndFlush(title);
  }

  @Test
  @DisplayName("Should delete inactive vacant title")
  void shouldDeleteInactiveVacantTitle() {
    // Given
    Title title = createTitle("Test Title", WrestlerTier.MAIN_EVENTER);
    title.setIsActive(false);
    title.setIsVacant(true);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // When
    boolean result = titleService.deleteTitle(1L);

    // Then
    assertThat(result).isTrue();
    verify(titleRepository).delete(title);
  }

  @Test
  @DisplayName("Should not delete active title")
  void shouldNotDeleteActiveTitle() {
    // Given
    Title title = createTitle("Test Title", WrestlerTier.MAIN_EVENTER);
    title.setIsActive(true);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // When
    boolean result = titleService.deleteTitle(1L);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should get title statistics")
  void shouldGetTitleStatistics() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler champion = createWrestler("Champion", 120000L);
    title.awardTitle(champion);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // When
    TitleService.TitleStats result = titleService.getTitleStats(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("World Championship");
    assertThat(result.tier()).isEqualTo(WrestlerTier.MAIN_EVENTER);
    assertThat(result.isVacant()).isFalse();
    assertThat(result.currentChampion()).isEqualTo("Champion");
    assertThat(result.isActive()).isTrue();
  }

  private Title createTitle(String name, WrestlerTier tier) {
    Title title = new Title();
    title.setId(1L);
    title.setName(name);
    title.setTier(tier);
    title.setIsActive(true);
    title.setIsVacant(true);
    title.setCreationDate(fixedInstant);
    return title;
  }

  private Wrestler createWrestler(String name, Long fans) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName(name);
    wrestler.setFans(fans);
    wrestler.setStartingHealth(15);
    wrestler.setIsPlayer(true);
    wrestler.updateTier();
    return wrestler;
  }
}
