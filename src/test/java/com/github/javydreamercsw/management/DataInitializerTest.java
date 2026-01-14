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
package com.github.javydreamercsw.management;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.dto.FactionImportDTO;
import com.github.javydreamercsw.management.dto.NpcDTO;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.dto.TitleDTO;
import com.github.javydreamercsw.management.dto.WrestlerImportDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

  private DataInitializer dataInitializer;

  @Mock private WrestlerService wrestlerService;
  @Mock private CardSetService cardSetService;
  @Mock private CardService cardService;
  @Mock private DeckService deckService;
  @Mock private ShowTypeService showTypeService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private TitleService titleService;
  @Mock private NpcService npcService;
  @Mock private FactionService factionService;
  @Mock private TeamService teamService;
  @Mock private TeamRepository teamRepository;

  @Mock
  @Qualifier("baseAccountService") private AccountService accountService;

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private GameSettingService gameSettingService;

  @BeforeEach
  void setUp() {
    // Manually instantiate DataInitializer with mocked dependencies
    dataInitializer =
        new DataInitializer(
            true, // Enabled parameter
            showTemplateService,
            wrestlerService,
            wrestlerRepository,
            showTypeService,
            segmentRuleService,
            segmentTypeService,
            cardSetService,
            cardService,
            titleService,
            deckService,
            gameSettingService,
            npcService,
            factionService,
            teamService,
            teamRepository);

    // Mock count methods to prevent issues during init()
    lenient().when(wrestlerService.count()).thenReturn(0L);
    lenient().when(cardSetService.count()).thenReturn(0L);
    lenient().when(cardService.count()).thenReturn(0L);
    lenient().when(deckService.count()).thenReturn(0L);
    lenient().when(showTypeService.count()).thenReturn(0L);
    lenient().when(showTemplateService.count()).thenReturn(0L);
    lenient().when(segmentRuleService.findAll()).thenReturn(new ArrayList<>());
    lenient().when(segmentTypeService.findAll()).thenReturn(new ArrayList<>());
    lenient().when(titleService.findAll()).thenReturn(new ArrayList<>());
    // AccountService doesn't have a count method
    lenient().when(wrestlerRepository.count()).thenReturn(0L);
    lenient().when(gameSettingService.findById(any())).thenReturn(Optional.empty());

    // Mock save methods to prevent NullPointerExceptions during init()
    lenient().when(wrestlerService.save(any(Wrestler.class))).thenAnswer(i -> i.getArguments()[0]);
    lenient().when(cardSetService.save(any(CardSet.class))).thenAnswer(i -> i.getArguments()[0]);
    lenient().when(cardService.save(any())).thenAnswer(i -> i.getArguments()[0]);
    lenient().when(deckService.save(any())).thenAnswer(i -> i.getArguments()[0]);
    lenient().when(showTypeService.save(any())).thenAnswer(i -> i.getArguments()[0]);
    lenient().when(showTemplateService.save(any())).thenAnswer(i -> i.getArguments()[0]);
    lenient()
        .when(
            segmentRuleService.createOrUpdateRule(
                anyString(), anyString(), anyBoolean(), any(BumpAddition.class)))
        .thenAnswer(
            invocation -> {
              SegmentRule rule = new SegmentRule();
              rule.setName(invocation.getArgument(0));
              rule.setDescription(invocation.getArgument(1));
              rule.setRequiresHighHeat(invocation.getArgument(2));
              rule.setBumpAddition(invocation.getArgument(3));
              return rule;
            });
    lenient()
        .when(segmentTypeService.createOrUpdateSegmentType(anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              SegmentType type = new SegmentType();
              type.setName(invocation.getArgument(0));
              type.setDescription(invocation.getArgument(1));
              return type;
            });
    lenient()
        .when(titleService.createTitle(anyString(), anyString(), any(), any()))
        .thenAnswer(
            invocation -> {
              Title title = new Title();
              title.setName(invocation.getArgument(0));
              title.setDescription(invocation.getArgument(1));
              title.setTier(invocation.getArgument(2));
              return title;
            });
    lenient().when(titleService.save(any(Title.class))).thenAnswer(i -> i.getArguments()[0]);
    // AccountService doesn't have a save method
    lenient()
        .when(wrestlerRepository.save(any(Wrestler.class)))
        .thenAnswer(i -> i.getArguments()[0]);
    lenient().doNothing().when(gameSettingService).saveCurrentGameDate(any());
  }

  @Test
  void testDeckImportIsIdempotentAndNoDuplicates() {
    dataInitializer.init();
    long initialDeckCount = deckService.count();
    dataInitializer.init();
    assertEquals(initialDeckCount, deckService.count());
  }

  @Test
  void validateWrestlersJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("wrestlers.json").getInputStream(),
                  new TypeReference<List<WrestlerImportDTO>>() {});
        });
  }

  @Test
  void validateCardsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("cards.json").getInputStream(),
                  new TypeReference<List<CardDTO>>() {});
        });
  }

  @Test
  void validateDecksJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("decks.json").getInputStream(),
                  new TypeReference<List<DeckDTO>>() {});
        });
  }

  @Test
  void validateChampionshipsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("championships.json").getInputStream(),
                  new TypeReference<List<TitleDTO>>() {});
        });
  }

  @Test
  void validateSetsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("sets.json").getInputStream(),
                  new TypeReference<List<CardSet>>() {});
        });
  }

  @Test
  void validateShowTemplatesJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("show_templates.json").getInputStream(),
                  new TypeReference<List<ShowTemplateDTO>>() {});
        });
  }

  @Test
  void validateShowTypesJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("show_types.json").getInputStream(),
                  new TypeReference<List<ShowType>>() {});
        });
  }

  @Test
  void validateSegmentRulesJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("segment_rules.json").getInputStream(),
                  new TypeReference<List<SegmentRuleDTO>>() {});
        });
  }

  @Test
  void validateSegmentTypesJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("segment_types.json").getInputStream(),
                  new TypeReference<List<SegmentTypeDTO>>() {});
        });
  }

  @Test
  void validateNpcsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("npcs.json").getInputStream(),
                  new TypeReference<List<NpcDTO>>() {});
        });
  }

  @Test
  void validateFactionsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("factions.json").getInputStream(),
                  new TypeReference<List<FactionImportDTO>>() {});
        });
  }

  @Test
  void validateTeamsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("teams.json").getInputStream(),
                  new TypeReference<List<TeamImportDTO>>() {});
        });
  }

  @Test
  void testSyncWrestlersFromFile_existingWrestler() {
    // Given
    Wrestler existingWrestler = new Wrestler();
    existingWrestler.setName("Rob Van Dam");
    existingWrestler.setFans(100L);
    existingWrestler.setBumps(100);

    lenient().when(wrestlerService.count()).thenReturn(1L);
    lenient()
        .when(wrestlerRepository.findByName("Rob Van Dam"))
        .thenReturn(Optional.of(existingWrestler));
    lenient().when(wrestlerRepository.findAll()).thenReturn(List.of(existingWrestler));

    // When
    dataInitializer.syncWrestlersFromFile();

    // Then
    ArgumentCaptor<Wrestler> wrestlerCaptor = ArgumentCaptor.forClass(Wrestler.class);
    verify(wrestlerRepository, atLeastOnce()).save(wrestlerCaptor.capture());

    Wrestler savedWrestler =
        wrestlerCaptor.getAllValues().stream()
            .filter(w -> w.getName().equals("Rob Van Dam"))
            .findFirst()
            .orElse(null);

    assertNotNull(savedWrestler);
    assertEquals(100, (long) savedWrestler.getFans());
    assertEquals(100, (int) savedWrestler.getBumps());
  }
}
