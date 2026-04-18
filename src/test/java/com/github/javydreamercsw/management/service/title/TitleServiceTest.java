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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
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

/** Unit tests for TitleService. Tests the ATW RPG championship management functionality. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TitleService Tests")
class TitleServiceTest {
  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private UniverseRepository universeRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private TierBoundaryService tierBoundaryService;
  @Mock private DefaultImageService imageService;

  private TitleService titleService;

  private final Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
  private Universe defaultUniverse;

  @BeforeEach
  void setUp() {
    defaultUniverse = Universe.builder().id(1L).name("Default Universe").build();
    lenient().when(universeRepository.findById(1L)).thenReturn(Optional.of(defaultUniverse));

    titleService =
        new TitleService(
            tierBoundaryService,
            titleRepository,
            wrestlerRepository,
            wrestlerService,
            universeRepository,
            clock,
            imageService);

    Mockito.lenient().doNothing().when(eventPublisher).publishEvent(any());
    when(tierBoundaryService.findByTierAndGender(any(WrestlerTier.class), any(Gender.class)))
        .thenAnswer(
            invocation -> {
              WrestlerTier tier = invocation.getArgument(0);
              com.github.javydreamercsw.base.domain.wrestler.TierBoundary boundary =
                  new com.github.javydreamercsw.base.domain.wrestler.TierBoundary();
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
    newTitle.setChampionshipType(ChampionshipType.SINGLE);
    newTitle.setUniverse(defaultUniverse);

    // When
    Title result =
        titleService.createTitle(
            newTitle.getName(),
            newTitle.getDescription(),
            newTitle.getTier(),
            newTitle.getChampionshipType(),
            1L);

    // Then
    assertThat(result.getName()).isEqualTo(newTitle.getName());
    assertThat(result.getDescription()).isEqualTo(newTitle.getDescription());
    assertThat(result.getTier()).isEqualTo(newTitle.getTier());
    assertThat(result.getIsActive()).isTrue();
    assertThat(result.isVacant()).isTrue();
    verify(titleRepository).save(any(Title.class));
  }

  @Test
  @DisplayName("Should successfuly challenge for title")
  void shouldSuccessfullyChallengeForTitle() {
    // Given
    Title title = createTitle(1L, "World Championship", WrestlerTier.MAIN_EVENTER);
    title.setUniverse(defaultUniverse);
    Wrestler challenger = createWrestler(2L, "Challenger", 120000L);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(challenger)
            .fans(120000L)
            .tier(WrestlerTier.MAIN_EVENTER)
            .build();
    when(wrestlerService.getOrCreateState(eq(2L), anyLong())).thenReturn(state);
    when(wrestlerService.spendFans(eq(2L), anyLong(), anyLong())).thenReturn(true);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(challenger));

    // When
    TitleService.ChallengeResult result = titleService.challengeForTitle(2L, 1L, 1L);

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.message()).contains("Challenge successful");
    verify(wrestlerService).spendFans(eq(2L), anyLong(), anyLong());
    verify(titleRepository).save(title);
  }

  @Test
  void testAwardTitleTo() {
    // Given
    Title title = createTitle("Test Title", WrestlerTier.ROOKIE);
    Wrestler wrestler = createWrestler(10L, "Test Wrestler", 1000L);

    // When
    titleService.awardTitleTo(title, List.of(wrestler));

    // Then
    assertThat(title.getCurrentChampions()).contains(wrestler);
    verify(titleRepository, times(1)).save(any(Title.class));
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
    title.setUniverse(defaultUniverse);
    return title;
  }

  private Wrestler createWrestler(@NonNull Long id, @NonNull String name, @NonNull Long fans) {
    Wrestler wrestler = Wrestler.builder().name(name).startingHealth(15).isPlayer(true).build();
    wrestler.setId(id);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .fans(fans)
            .tier(WrestlerTier.fromFanCount(fans))
            .build();
    lenient().when(wrestlerService.getOrCreateState(eq(id), anyLong())).thenReturn(state);

    return wrestler;
  }
}
