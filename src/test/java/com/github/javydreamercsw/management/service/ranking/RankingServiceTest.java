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
    reign.setChampions(List.of(champion));
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

    title.setContender(List.of(contender1, contender2));
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
    when(wrestlerRepository.findByFansBetween(
            WrestlerTier.MAIN_EVENTER.getMinFans(),
            WrestlerTier.MAIN_EVENTER.getNextTier().getMinFans()))
        .thenReturn(List.of(champion, contender1, contender2));

    List<RankedWrestlerDTO> contenders = rankingService.getRankedContenders(1L);

    assertEquals(2, contenders.size());
    assertEquals("Contender 2", contenders.get(0).getName());
    assertEquals(1, contenders.get(0).getRank());
    assertEquals("Contender 1", contenders.get(1).getName());
    assertEquals(2, contenders.get(1).getRank());
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
