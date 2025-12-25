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
package com.github.javydreamercsw.management.service.ranking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TierBoundaryService tierBoundaryService;

  @InjectMocks private RankingService rankingService;

  private Title title;
  private Wrestler champion;
  private Wrestler contender1;
  private Wrestler contender2;

  @BeforeEach
  void setUp() {
    title = new Title();
    title.setId(1L);
    title.setName("World Heavyweight Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setGender(Gender.MALE);

    champion = new Wrestler();
    champion.setId(1L);
    champion.setName("Champion");
    champion.setFans(1000L);
    champion.setGender(Gender.MALE);
    champion.setTier(WrestlerTier.fromFanCount(champion.getFans()));

    TitleReign reign = new TitleReign();
    List<Wrestler> champions = new ArrayList<>();
    champions.add(champion);
    reign.setChampions(champions);
    reign.setStartDate(Instant.now());
    title.getTitleReigns().add(reign);

    contender1 = new Wrestler();
    contender1.setId(2L);
    contender1.setName("Contender 1");
    contender1.setFans(500L);
    contender1.setGender(Gender.MALE);
    contender1.setTier(WrestlerTier.fromFanCount(contender1.getFans()));

    contender2 = new Wrestler();
    contender2.setId(3L);
    contender2.setName("Contender 2");
    contender2.setFans(700L);
    contender2.setGender(Gender.MALE);
    contender2.setTier(WrestlerTier.fromFanCount(contender2.getFans()));

    List<Wrestler> contenders = new ArrayList<>();
    contenders.add(contender1);
    contenders.add(contender2);
    contenders.forEach(title::addChallenger);
  }

  @Test
  void testGetChampionships() {
    when(titleRepository.findAll()).thenReturn(List.of(title));

    List<ChampionshipDTO> championships = rankingService.getChampionships();

    assertEquals(1, championships.size());
    assertEquals("World Heavyweight Championship", championships.get(0).getName());
    assertEquals("world-heavyweight-championship.png", championships.get(0).getImageName());
  }

  @Test
  void testGetRankedContenders() {
    title.setTier(WrestlerTier.MIDCARDER);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    com.github.javydreamercsw.base.domain.wrestler.TierBoundary boundary =
        new com.github.javydreamercsw.base.domain.wrestler.TierBoundary();
    boundary.setTier(WrestlerTier.MIDCARDER);
    boundary.setMinFans(WrestlerTier.MIDCARDER.getMinFans());
    boundary.setMaxFans(WrestlerTier.MIDCARDER.getMaxFans());
    when(tierBoundaryService.findByTierAndGender(any(WrestlerTier.class), any(Gender.class)))
        .thenReturn(Optional.of(boundary));
    when(wrestlerRepository.findByFansBetween(
            WrestlerTier.MIDCARDER.getMinFans(), WrestlerTier.MIDCARDER.getMaxFans()))
        .thenReturn(new ArrayList<>(List.of(contender1, contender2)));

    List<RankedWrestlerDTO> contenders = rankingService.getRankedContenders(1L);

    assertEquals(2, contenders.size());
    assertEquals("Contender 2", contenders.get(0).getName());
    assertEquals(1, contenders.get(0).getRank());
    assertEquals("Contender 1", contenders.get(1).getName());
    assertEquals(2, contenders.get(1).getRank());
  }

  @Test
  void testGetRankedContendersWithWorldTitle() {
    Wrestler icon = new Wrestler();
    icon.setId(4L);
    icon.setName("Icon");
    icon.setGender(Gender.MALE);
    icon.setFans(WrestlerTier.ICON.getMinFans() + 1000); // Above Main Eventer
    icon.setTier(WrestlerTier.fromFanCount(icon.getFans()));
    title.addChallenger(icon);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    com.github.javydreamercsw.base.domain.wrestler.TierBoundary boundary =
        new com.github.javydreamercsw.base.domain.wrestler.TierBoundary();
    boundary.setTier(WrestlerTier.MAIN_EVENTER);
    boundary.setMinFans(WrestlerTier.MAIN_EVENTER.getMinFans());
    boundary.setMaxFans(WrestlerTier.MAIN_EVENTER.getMaxFans());
    when(tierBoundaryService.findByTierAndGender(any(WrestlerTier.class), any(Gender.class)))
        .thenReturn(Optional.of(boundary));
    when(wrestlerRepository.findByFansGreaterThanEqual(WrestlerTier.MAIN_EVENTER.getMinFans()))
        .thenReturn(new ArrayList<>(List.of(champion, contender1, contender2, icon)));

    List<RankedWrestlerDTO> contenders = rankingService.getRankedContenders(1L);

    assertEquals(3, contenders.size());
    assertEquals("Icon", contenders.get(0).getName());
    assertEquals(1, contenders.get(0).getRank());
    assertEquals("Contender 2", contenders.get(1).getName());
    assertEquals(2, contenders.get(1).getRank());
    assertEquals("Contender 1", contenders.get(2).getName());
    assertEquals(3, contenders.get(2).getRank());
  }

  @Test
  void testGetCurrentChampions() {
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    List<ChampionDTO> result = rankingService.getCurrentChampions(1L);

    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    assertEquals("Champion", result.get(0).getName());
  }
}
