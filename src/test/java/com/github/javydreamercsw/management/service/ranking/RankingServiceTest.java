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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
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

    champion = new Wrestler();
    champion.setId(1L);
    champion.setName("Champion");
    champion.setFans(1000L);

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

    contender2 = new Wrestler();
    contender2.setId(3L);
    contender2.setName("Contender 2");
    contender2.setFans(700L);

    List<Wrestler> contenders = new ArrayList<>();
    contenders.add(contender1);
    contenders.add(contender2);
    title.setContender(contenders);
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
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findByFansGreaterThanEqual(WrestlerTier.MAIN_EVENTER.getMinFans()))
        .thenReturn(
            new ArrayList<>(
                List.of(
                    champion,
                    contender1,
                    contender2))); // Use mutable list to avoid issues if other tests modify it

    List<RankedWrestlerDTO> contenders = rankingService.getRankedContenders(1L);

    assertEquals(2, contenders.size());
    assertEquals("Contender 2", contenders.get(0).getName());
    assertEquals(1, contenders.get(0).getRank());
    assertEquals("Contender 1", contenders.get(1).getName());
    assertEquals(2, contenders.get(1).getRank());
  }

  @Test
  void testGetRankedContendersWithHigherTierWrestler() {
    Wrestler icon = new Wrestler();
    icon.setId(4L);
    icon.setName("Icon");
    icon.setFans(WrestlerTier.ICON.getMinFans() + 1000); // Above Main Eventer
    title.getContender().add(icon);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
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
