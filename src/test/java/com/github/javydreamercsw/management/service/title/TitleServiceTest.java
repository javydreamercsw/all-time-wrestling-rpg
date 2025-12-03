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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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
  @Mock private Clock clock;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private TitleService titleService;

  private final Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    Mockito.lenient().doNothing().when(eventPublisher).publishEvent(any());
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
    assertThat(result.isVacant()).isTrue();
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
  void testGetTitleById_found() {
    Title title = new Title();
    title.setId(1L);
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
    Title title = new Title();
    title.setName("Championship");
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
    Title title = new Title();
    Page<Title> page = new PageImpl<>(List.of(title));
    when(titleRepository.findAllBy(any())).thenReturn(page);
    Page<Title> result = titleService.getAllTitles(PageRequest.of(0, 10));
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void testGetActiveTitles() {
    Title title = new Title();
    when(titleRepository.findByIsActiveTrue()).thenReturn(List.of(title));
    List<Title> result = titleService.getActiveTitles();
    assertThat(result).hasSize(1);
  }

  @Test
  void testGetVacantTitles() {
    Title title = new Title();
    when(titleRepository.findVacantActiveTitles()).thenReturn(List.of(title));
    List<Title> result = titleService.getVacantTitles();
    assertThat(result).hasSize(1);
  }

  @Test
  void testGetTitlesByTier() {
    Title title = new Title();
    WrestlerTier tier = WrestlerTier.MAIN_EVENTER;
    when(titleRepository.findActiveTitlesByTier(tier)).thenReturn(List.of(title));
    List<Title> result = titleService.getTitlesByTier(tier);
    assertThat(result).hasSize(1);
  }

  @Test
  void testAwardTitleTo() {

    Title title = mock(Title.class);

    Wrestler wrestler = mock(Wrestler.class);

    doNothing().when(title).awardTitleTo(anyList(), any());

    when(titleRepository.saveAndFlush(title)).thenReturn(title);

    titleService.awardTitleTo(title, List.of(wrestler));

    verify(title, times(1)).awardTitleTo(anyList(), any());

    verify(titleRepository, times(1)).saveAndFlush(title);

    verify(eventPublisher, times(1)).publishEvent(any());
  }

  @Test
  void testVacateTitle_found() {
    Title title = mock(Title.class);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(titleRepository.saveAndFlush(title)).thenReturn(title);
    Optional<Title> result = titleService.vacateTitle(1L);
    assertThat(result).isPresent();
    verify(title, times(1)).vacateTitle();
    verify(titleRepository, times(1)).saveAndFlush(title);
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
    when(clock.instant()).thenReturn(fixedInstant);
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createWrestler("Champion", 120000L);

    titleService.awardTitleTo(title, List.of(wrestler));

    // Then
    assertThat(title.getCurrentChampions()).containsExactly(wrestler);
    assertThat(title.isVacant()).isFalse();
    verify(titleRepository).saveAndFlush(title);
    verify(eventPublisher, times(1)).publishEvent(any());
  }

  @Test
  @DisplayName("Should not award title to ineligible wrestler")
  void shouldNotAwardTitleToIneligibleWrestler() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    title.vacateTitle(); // Explicitly ensure title is vacant
    Wrestler wrestler = createWrestler("Rookie", 50000L);

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
    when(clock.instant()).thenReturn(fixedInstant);
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createWrestler("Champion", 120000L);
    title.awardTitleTo(java.util.List.of(wrestler), Instant.now(clock));

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(titleRepository.saveAndFlush(any(Title.class))).thenReturn(title);

    // When
    Optional<Title> result = titleService.vacateTitle(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(title.isVacant()).isTrue();
    assertThat(title.getCurrentChampions()).isEmpty();
    verify(titleRepository).saveAndFlush(title);
  }

  @Test
  @DisplayName("Should successfully challenge for title")
  void shouldSuccessfullyChallengeForTitle() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Challenger", 120000L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(challenger));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(challenger);

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(1L, 1L);

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.message()).isEqualTo("Challenge accepted");
    assertThat(challenger.getFans()).isEqualTo(20000L); // 120k - 100k challenge cost
    verify(wrestlerRepository).saveAndFlush(challenger);
  }

  @Test
  @DisplayName("Should fail challenge when wrestler has insufficient fans for eligibility")
  void shouldFailChallengeWhenWrestlerHasInsufficientFansForEligibility() {
    // Given
    Title title = createTitle("Extreme Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Poor Challenger", 10000L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(challenger));

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(1L, 1L);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("needs 100,000 fans");
  }

  @Test
  @DisplayName("Should fail challenge when wrestler is ineligible")
  void shouldFailChallengeWhenWrestlerIsIneligible() {
    // Given
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createWrestler("Rookie", 50000L);

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
  void testDelete() {
    Title testTitle = createTitle("Old Name", WrestlerTier.MAIN_EVENTER);
    testTitle.setIsActive(false); // Ensure title is inactive
    testTitle.vacateTitle(); // Ensure title is vacant
    when(titleRepository.findById(anyLong())).thenReturn(Optional.of(testTitle));
    doNothing().when(titleRepository).deleteById(anyLong());
    Assertions.assertNotNull(testTitle.getId());
    titleService.deleteTitle(testTitle.getId());
    verify(titleRepository, times(1)).deleteById(testTitle.getId());
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
    when(clock.instant()).thenReturn(fixedInstant);
    Title title = createTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler champion = createWrestler("Champion", 120000L);
    title.awardTitleTo(java.util.List.of(champion), Instant.now(clock));

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
    title.setCreationDate(fixedInstant);
    return title;
  }

  private Wrestler createWrestler(String name, Long fans) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(1L);
    wrestler.setName(name);
    wrestler.setFans(fans);
    wrestler.setStartingHealth(15);
    wrestler.setIsPlayer(true);
    wrestler.updateTier();
    return wrestler;
  }
}
