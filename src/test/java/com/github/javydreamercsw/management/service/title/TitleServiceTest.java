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
package com.github.javydreamercsw.management.service.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Unit tests for TitleService. Tests the ATW RPG championship management functionality. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TitleService Tests")
class TitleServiceTest {
  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private TierBoundaryService tierBoundaryService;

  private TitleService titleService;

  private final Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

  @BeforeEach
  void setUp() {
    titleService =
        new TitleService(tierBoundaryService, titleRepository, wrestlerRepository, clock);
    Mockito.lenient().doNothing().when(eventPublisher).publishEvent(any());
    when(tierBoundaryService.findByTierAndGender(any(WrestlerTier.class), any(Gender.class)))
        .thenAnswer(
            invocation -> {
              WrestlerTier tier = invocation.getArgument(0);
              com.github.javydreamercsw.management.domain.wrestler.TierBoundary boundary =
                  new com.github.javydreamercsw.management.domain.wrestler.TierBoundary();
              boundary.setTier(tier);
              boundary.setMinFans(tier.getMinFans());
              boundary.setMaxFans(tier.getMaxFans());
              boundary.setChallengeCost(tier.getChallengeCost());
              boundary.setContenderEntryFee(tier.getContenderEntryFee());
              return Optional.of(boundary);
            });
    when(titleRepository.save(any(Title.class)))
        .thenAnswer(
            invocation -> {
              Title title = invocation.getArgument(0);
              if (title.getId() == null) {
                title.setId(1L);
              }
              return title;
            });
  }

  @Test
  @DisplayName("Should create new title")
  void shouldCreateNewTitle() {
    // Given
    Title newTitle = new Title();
    newTitle.setName("World Championship");
    newTitle.setDescription("Top title");
    newTitle.setTier(WrestlerTier.MAIN_EVENTER);
    newTitle.setCreationDate(Instant.now(clock));

    // When
    Title result =
        titleService.createTitle(newTitle.getName(), newTitle.getDescription(), newTitle.getTier());

    // Then
    assertThat(result.getName()).isEqualTo(newTitle.getName());
    assertThat(result.getDescription()).isEqualTo(newTitle.getDescription());
    assertThat(result.getTier()).isEqualTo(newTitle.getTier());
    assertThat(result.getIsActive()).isTrue();
    assertThat(result.isVacant()).isTrue();
    verify(titleRepository).save(any(Title.class));
  }

  @Test
  @DisplayName("Should get title by ID")
  void shouldGetTitleById() {
    // Given
    Title title = createTitle(1L, "Test Title", WrestlerTier.MAIN_EVENTER);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // When
    Optional<Title> result = titleService.getTitleById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(title);
  }

  @Test
  void testGetTitleById_found() {
    Title title = createTitle(1L, "Test Title", WrestlerTier.ROOKIE);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    Optional<Title> result = titleService.getTitleById(1L);
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(1L);
  }

  @Test
  void testGetTitleById_notFound() {
    when(titleRepository.findById(2L)).thenReturn(Optional.empty());
    Optional<Title> result = titleService.getTitleById(2L);
    assertThat(result).isEmpty();
  }

  @Test
  void testFindByName_found() {
    Title title = createTitle(1L, "Championship", WrestlerTier.ICON);
    when(titleRepository.findByName("Championship")).thenReturn(Optional.of(title));
    Optional<Title> result = titleService.findByName("Championship");
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Championship");
  }

  @Test
  void testFindByName_notFound() {
    when(titleRepository.findByName("Missing")).thenReturn(Optional.empty());
    Optional<Title> result = titleService.findByName("Missing");
    assertThat(result).isEmpty();
  }

  @Test
  void testGetAllTitles() {
    Title title = createTitle(1L, "Championship", WrestlerTier.ICON);
    Page<Title> page = new PageImpl<>(List.of(title));
    when(titleRepository.findAll(any(PageRequest.class))).thenReturn(page);
    Page<Title> result = titleService.getAllTitles(PageRequest.of(0, 10));
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void testGetActiveTitles() {
    Title title = createTitle(1L, "Active Title", WrestlerTier.MAIN_EVENTER);
    when(titleRepository.findByIsActiveTrue()).thenReturn(List.of(title));
    List<Title> result = titleService.getActiveTitles();
    assertThat(result).hasSize(1);
  }

  @Test
  void testGetVacantTitles() {
    Title title = createTitle(1L, "Vacant Title", WrestlerTier.ROOKIE);
    // Title is vacant by default since it has no champions
    when(titleRepository.findByIsActiveTrue()).thenReturn(List.of(title));
    List<Title> result = titleService.getVacantTitles();
    assertThat(result).hasSize(1);
  }

  @Test
  void testGetTitlesByTier() {
    Title title = new Title();
    WrestlerTier tier = WrestlerTier.MAIN_EVENTER;
    when(titleRepository.findByIsActiveTrueAndTier(tier)).thenReturn(List.of(title));
    List<Title> result = titleService.getTitlesByTier(tier);
    assertThat(result).hasSize(1);
  }

  @Test
  void testAwardTitleTo() {
    // Given
    Title title = createTitle("Test Title", WrestlerTier.ROOKIE);
    Wrestler wrestler = createWrestler("Test Wrestler", 1000L);

    // When
    titleService.awardTitleTo(title, List.of(wrestler));

    // Then
    assertThat(title.getCurrentChampions()).contains(wrestler);
    verify(titleRepository, times(1)).save(any(Title.class));
  }

  @Test
  void testVacateTitle_found() {
    Title title = createTitle("Test Title", WrestlerTier.ROOKIE);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    Optional<Title> result = titleService.vacateTitle(1L);
    assertThat(result).isPresent();
    verify(titleRepository, times(1)).save(any(Title.class));
  }

  @Test
  void testVacateTitle_notFound() {
    when(titleRepository.findById(2L)).thenReturn(Optional.empty());
    Optional<Title> result = titleService.vacateTitle(2L);
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should award title to eligible wrestler")
  void shouldAwardTitleToEligibleWrestler() {
    // Given
    Title title = createTitle(1L, "World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createWrestler("Champion", 120000L);

    // When
    titleService.awardTitleTo(title, List.of(wrestler));

    // Then
    assertThat(title.getCurrentChampions()).containsExactly(wrestler);
    assertThat(title.isVacant()).isFalse();
    verify(titleRepository).save(title);
  }

  @Test
  @DisplayName("Should not award title to ineligible wrestler")
  void shouldNotAwardTitleToIneligibleWrestler() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    title.vacateTitle(fixedInstant); // Explicitly ensure title is vacant
    Wrestler wrestler = createWrestler("Rookie", 50000L);
    wrestler.setId(2L);
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findById(title.getId())).thenReturn(Optional.of(title));

    // When
    Assertions.assertNotNull(wrestler.getId());
    Assertions.assertNotNull(title.getId());
    TitleService.ChallengeResult result =
        titleService.challengeForTitle(wrestler.getId(), title.getId());

    // Then
    assertThat(result.success()).isFalse();
    assertThat(title.isVacant()).isTrue();
  }

  @Test
  @DisplayName("Should vacate title")
  void shouldVacateTitle() {
    // Given
    Title title = createTitle(1L, "World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createWrestler("Champion", 120000L);
    title.awardTitleTo(List.of(wrestler), Instant.now(clock));
    when(titleRepository.findById(anyLong())).thenReturn(Optional.of(title));

    // When
    Optional<Title> result = titleService.vacateTitle(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(title.isVacant()).isTrue();
    assertThat(title.getCurrentChampions()).isEmpty();
    verify(titleRepository).save(title);
  }

  @Test
  @DisplayName("Should successfully challenge for title")
  void shouldSuccessfullyChallengeForTitle() {
    // Given
    Title title = createTitle(1L, "World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Challenger", 120000L);
    challenger.setId(2L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(challenger));

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(2L, 1L);

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.message()).contains("Challenge successful");
    assertThat(challenger.getFans()).isEqualTo(105000L); // 120k - 15k contender entry fee
    verify(wrestlerRepository).save(challenger);
    verify(titleRepository).save(title);
  }

  @Test
  @DisplayName("Should fail challenge when wrestler has insufficient fans for eligibility")
  void shouldFailChallengeWhenWrestlerHasInsufficientFansForEligibility() {
    // Given
    Title title = createTitle(1L, "Extreme Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Poor Challenger", 10000L);
    challenger.setId(2L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(challenger));

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(2L, 1L);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("not eligible");
  }

  @Test
  @DisplayName("Should fail challenge when wrestler is ineligible")
  void shouldFailChallengeWhenWrestlerIsIneligible() {
    // Given
    Title title = createTitle(1L, "World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Rookie", 50000L);
    challenger.setId(2L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(challenger));

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(2L, 1L);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("not eligible");
  }

  @Test
  @DisplayName("Should get eligible challengers")
  void shouldGetEligibleChallengers() {
    // Given
    Title title = createTitle(1L, "World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler eligible1 = createWrestler("Eligible 1", 120000L);
    Wrestler eligible2 =
        createWrestler(
            "Eligible 2", 140000L); // Changed from 150000L (ICON) to 140000L (MAIN_EVENTER)
    Wrestler ineligible = createWrestler("Ineligible", 50000L);
    eligible1.setId(2L);
    eligible2.setId(3L);
    ineligible.setId(4L);

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
    wrestler.setId(1L);
    Title title1 = createTitle(1L, "Title 1", WrestlerTier.MAIN_EVENTER);
    Title title2 = createTitle(2L, "Title 2", WrestlerTier.CONTENDER);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findTitlesHeldByWrestler(wrestler))
        .thenReturn(Arrays.asList(title1, title2));

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
    Title title = createTitle(1L, "Old Name", WrestlerTier.MAIN_EVENTER);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // When
    Optional<Title> result = titleService.updateTitle(1L, "New Name", "New description", false);

    // Then
    assertThat(result).isPresent();
    assertThat(title.getName()).isEqualTo("New Name");
    assertThat(title.getDescription()).isEqualTo("New description");
    assertThat(title.getIsActive()).isFalse();
    verify(titleRepository).save(title);
  }

  @Test
  void testDelete() {
    Title testTitle = createTitle(1L, "Old Name", WrestlerTier.MAIN_EVENTER);
    testTitle.setIsActive(false); // Ensure title is inactive
    testTitle.vacateTitle(Instant.now(clock)); // Ensure title is vacant
    when(titleRepository.findById(anyLong())).thenReturn(Optional.of(testTitle));
    doNothing().when(titleRepository).delete(any(Title.class));
    Assertions.assertNotNull(testTitle.getId());
    titleService.deleteTitle(testTitle.getId());
    verify(titleRepository, times(1)).delete(any(Title.class));
  }

  @Test
  @DisplayName("Should not delete active title")
  void shouldNotDeleteActiveTitle() {
    // Given
    Title title = createTitle(1L, "Test Title", WrestlerTier.MAIN_EVENTER);
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
    Title title = createTitle(1L, "World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler champion = createWrestler("Champion", 120000L);
    title.awardTitleTo(List.of(champion), Instant.now(clock));

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // When
    TitleService.TitleStats result = titleService.getTitleStats(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.titleName()).isEqualTo("World Championship");
    assertThat(result.totalReigns()).isEqualTo(1);
    assertThat(result.currentReignDays()).isZero();
    assertThat(result.currentChampionsCount()).isEqualTo(1);
  }

  private Title createTitle(@NonNull String name, @NonNull WrestlerTier tier) {
    return createTitle(1L, name, tier);
  }

  private Title createTitle(@NonNull Long id, @NonNull String name, @NonNull WrestlerTier tier) {
    Title title = new Title();
    title.setId(id);
    title.setName(name);
    title.setTier(tier);
    title.setIsActive(true);
    title.setCreationDate(fixedInstant);
    return title;
  }

  private Wrestler createWrestler(@NonNull String name, @NonNull Long fans) {
    Wrestler wrestler =
        Wrestler.builder()
            .name(name)
            .fans(fans)
            .startingHealth(15)
            .isPlayer(true)
            .tier(WrestlerTier.fromFanCount(fans))
            .build();
    wrestler.setId(1L); // Assign a default ID for testing purposes
    return wrestler;
  }
}
