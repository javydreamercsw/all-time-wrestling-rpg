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

  private static final int COOLDOWN_DEFENSES = 2;

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

    when(gameSettingService.getContenderFailedChallengeCooldownDefenses())
        .thenReturn(COOLDOWN_DEFENSES);
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

  private Title singleTitle(final long id, final WrestlerTier tier, final long defenseCount) {
    Title title = new Title();
    title.setId(id);
    title.setName("World Title");
    title.setTier(tier);
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setIncludeInRankings(true);
    title.setDefenseCount(defenseCount);
    return title;
  }

  @Test
  void wrestlerNotOnCooldown_onCooldownFalse() {
    Wrestler w = wrestler(1L, "Top Star");
    Title title = singleTitle(10L, WrestlerTier.ICON, 5L);
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
    assertThat(dto.getDefensesUntilEligible()).isZero();
  }

  @Test
  void wrestlerWithActiveCooldown_onCooldownTrue() {
    Wrestler w = wrestler(1L, "Failed Challenger");
    // Title has been defended 3 times total; challenger failed at defense #2 → 1 defense since
    Title title = singleTitle(10L, WrestlerTier.ICON, 3L);
    WrestlerState s = state(100L, 5000L, WrestlerTier.ICON);

    WrestlerTitleCooldown cooldown =
        WrestlerTitleCooldown.builder()
            .id(1L)
            .wrestlerState(s)
            .title(title)
            .defenseCountAtChallenge(2L)
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
    assertThat(dto.getDefensesUntilEligible()).isEqualTo(1L); // needs 2, only 1 since challenge
  }

  @Test
  void wrestlerWithExpiredCooldown_onCooldownFalse() {
    Wrestler w = wrestler(1L, "Recovered Challenger");
    // Title defended 5 times total; challenger failed at #2 → 3 defenses since (>= 2 required)
    Title title = singleTitle(10L, WrestlerTier.ICON, 5L);
    WrestlerState s = state(100L, 5000L, WrestlerTier.ICON);

    WrestlerTitleCooldown cooldown =
        WrestlerTitleCooldown.builder()
            .id(1L)
            .wrestlerState(s)
            .title(title)
            .defenseCountAtChallenge(2L)
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
    assertThat(dto.getDefensesUntilEligible()).isZero();
  }

  @Test
  void cooldownIsPerTitle_noEffectOnOtherTitle() {
    Wrestler w = wrestler(1L, "Multi-Title Contender");
    Title titleA = singleTitle(10L, WrestlerTier.ICON, 3L);
    Title titleB = singleTitle(20L, WrestlerTier.ICON, 3L);
    WrestlerState s = state(100L, 5000L, WrestlerTier.ICON);

    // Cooldown exists only for titleA (failed at defense #2, title now at #3 → 1 more needed)
    WrestlerTitleCooldown cooldownA =
        WrestlerTitleCooldown.builder()
            .id(1L)
            .wrestlerState(s)
            .title(titleA)
            .defenseCountAtChallenge(2L)
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
