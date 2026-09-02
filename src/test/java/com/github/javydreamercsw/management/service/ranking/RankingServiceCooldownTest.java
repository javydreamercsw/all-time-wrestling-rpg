/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldown;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldownRepository;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RankingServiceCooldownTest {

  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private DefaultImageService imageService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private GameSettingService gameSettingService;
  @Mock private WrestlerTitleCooldownRepository cooldownRepository;

  private RankingService rankingService;

  private static final LocalDate GAME_DATE = LocalDate.of(2026, 9, 1);
  private static final int COOLDOWN_DAYS = 30;

  @BeforeEach
  void setUp() {
    rankingService =
        new RankingService(
            titleRepository,
            wrestlerRepository,
            factionRepository,
            teamRepository,
            imageService,
            wrestlerService,
            wrestlerStateRepository,
            gameSettingService,
            cooldownRepository);

    when(gameSettingService.getCurrentGameDate()).thenReturn(GAME_DATE);
    when(gameSettingService.getContenderFailedChallengeCooldownDays()).thenReturn(COOLDOWN_DAYS);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(anyLong(), anyLong()))
        .thenReturn(Optional.empty());
  }

  private Wrestler wrestler(final long id, final String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    w.setActive(true);
    return w;
  }

  private WrestlerState state(final long id, final long fans, final WrestlerTier tier) {
    WrestlerState s = new WrestlerState();
    s.setId(id);
    s.setFans(fans);
    s.setTier(tier);
    return s;
  }

  private Title singleTitle(final long id, final WrestlerTier tier) {
    Title title = new Title();
    title.setId(id);
    title.setName("World Title");
    title.setTier(tier);
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setIncludeInRankings(true);
    return title;
  }

  @Test
  void wrestlerNotOnCooldown_onCooldownFalse() {
    Wrestler w = wrestler(1L, "Top Star");
    Title title = singleTitle(10L, WrestlerTier.ICON);
    WrestlerState s = state(100L, 5000L, WrestlerTier.ICON);

    when(titleRepository.findById(10L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findAllByGenderAndActive(any(), anyBoolean())).thenReturn(List.of(w));
    when(wrestlerService.getOrCreateState(1L, 1L)).thenReturn(s);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(100L, 10L))
        .thenReturn(Optional.empty());

    List<?> result = rankingService.getRankedContenders(10L);

    assertThat(result).hasSize(1);
    RankedWrestlerDTO dto = (RankedWrestlerDTO) result.get(0);
    assertThat(dto.isOnCooldown()).isFalse();
    assertThat(dto.getCooldownExpiresDate()).isNull();
  }

  @Test
  void wrestlerWithActiveCooldown_onCooldownTrue() {
    Wrestler w = wrestler(1L, "Failed Challenger");
    Title title = singleTitle(10L, WrestlerTier.ICON);
    WrestlerState s = state(100L, 5000L, WrestlerTier.ICON);

    // Failed 10 days ago — cooldown of 30 days is still active
    LocalDate failedDate = GAME_DATE.minusDays(10);
    WrestlerTitleCooldown cooldown =
        WrestlerTitleCooldown.builder()
            .id(1L)
            .wrestlerState(s)
            .title(title)
            .failedChallengeDate(failedDate)
            .build();

    when(titleRepository.findById(10L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findAllByGenderAndActive(any(), anyBoolean())).thenReturn(List.of(w));
    when(wrestlerService.getOrCreateState(1L, 1L)).thenReturn(s);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(100L, 10L))
        .thenReturn(Optional.of(cooldown));

    List<?> result = rankingService.getRankedContenders(10L);

    assertThat(result).hasSize(1);
    RankedWrestlerDTO dto = (RankedWrestlerDTO) result.get(0);
    assertThat(dto.isOnCooldown()).isTrue();
    assertThat(dto.getCooldownExpiresDate()).isEqualTo(failedDate.plusDays(COOLDOWN_DAYS));
  }

  @Test
  void wrestlerWithExpiredCooldown_onCooldownFalse() {
    Wrestler w = wrestler(1L, "Recovered Challenger");
    Title title = singleTitle(10L, WrestlerTier.ICON);
    WrestlerState s = state(100L, 5000L, WrestlerTier.ICON);

    // Failed 40 days ago — cooldown of 30 days has expired
    LocalDate failedDate = GAME_DATE.minusDays(40);
    WrestlerTitleCooldown cooldown =
        WrestlerTitleCooldown.builder()
            .id(1L)
            .wrestlerState(s)
            .title(title)
            .failedChallengeDate(failedDate)
            .build();

    when(titleRepository.findById(10L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findAllByGenderAndActive(any(), anyBoolean())).thenReturn(List.of(w));
    when(wrestlerService.getOrCreateState(1L, 1L)).thenReturn(s);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(100L, 10L))
        .thenReturn(Optional.of(cooldown));

    List<?> result = rankingService.getRankedContenders(10L);

    assertThat(result).hasSize(1);
    RankedWrestlerDTO dto = (RankedWrestlerDTO) result.get(0);
    assertThat(dto.isOnCooldown()).isFalse();
    assertThat(dto.getCooldownExpiresDate()).isNull();
  }

  @Test
  void cooldownIsPerTitle_noEffectOnOtherTitle() {
    Wrestler w = wrestler(1L, "Multi-Title Contender");
    Title titleA = singleTitle(10L, WrestlerTier.ICON);
    Title titleB = singleTitle(20L, WrestlerTier.ICON);
    WrestlerState s = state(100L, 5000L, WrestlerTier.ICON);

    // Cooldown exists only for titleA
    WrestlerTitleCooldown cooldownA =
        WrestlerTitleCooldown.builder()
            .id(1L)
            .wrestlerState(s)
            .title(titleA)
            .failedChallengeDate(GAME_DATE.minusDays(5))
            .build();

    when(titleRepository.findById(20L)).thenReturn(Optional.of(titleB));
    when(wrestlerRepository.findAllByGenderAndActive(any(), anyBoolean())).thenReturn(List.of(w));
    when(wrestlerService.getOrCreateState(1L, 1L)).thenReturn(s);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(100L, 10L))
        .thenReturn(Optional.of(cooldownA));
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(100L, 20L))
        .thenReturn(Optional.empty());

    List<?> result = rankingService.getRankedContenders(20L);

    assertThat(result).hasSize(1);
    RankedWrestlerDTO dto = (RankedWrestlerDTO) result.get(0);
    assertThat(dto.isOnCooldown()).isFalse();
  }
}
