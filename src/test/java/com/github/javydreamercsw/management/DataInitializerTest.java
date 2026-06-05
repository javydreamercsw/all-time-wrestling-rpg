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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.ArenaImportDTO;
import com.github.javydreamercsw.management.dto.CampaignAbilityCardDTO;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.dto.FactionImportDTO;
import com.github.javydreamercsw.management.dto.LocationImportDTO;
import com.github.javydreamercsw.management.dto.NpcDTO;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.dto.StatusCardDTO;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.dto.TitleDTO;
import com.github.javydreamercsw.management.dto.WrestlerImportDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.commentator.CommentaryService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerTest {

  private DataInitializer dataInitializer;

  @Mock private CardSetRepository cardSetRepository;
  @Mock private DeckService deckService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private UniverseRepository universeRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private ShowTypeService showTypeService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private CardSetService cardSetService;
  @Mock private CardService cardService;
  @Mock private TitleService titleService;
  @Mock private GameSettingService gameSettingService;

  @Mock
  private com.github.javydreamercsw.management.domain.GameSettingRepository gameSettingRepository;

  @Mock private NpcService npcService;
  @Mock private FactionService factionService;
  @Mock private TeamService teamService;
  @Mock private TeamRepository teamRepository;
  @Mock private TierRecalculationService tierRecalculationService;
  @Mock private CampaignAbilityCardService campaignAbilityCardService;
  @Mock private CommentaryService commentaryService;
  @Mock private CampaignUpgradeService campaignUpgradeService;
  @Mock private StatusCardService statusCardService;
  @Mock private Environment env;
  @Mock private AchievementRepository achievementRepository;
  @Mock private RingsideActionDataService ringsideActionDataService;
  @Mock private ResourcePatternResolver resourcePatternResolver;
  @Mock private LocationRepository locationRepository;
  @Mock private ArenaRepository arenaRepository;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private OutcomeMatrixService outcomeMatrixService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  public void setUp() throws IOException {
    // Manually instantiate DataInitializer with mocked dependencies
    dataInitializer =
        new DataInitializer(
            true, // Enabled parameter
            false, // skipIfNotEmpty
            showTemplateService,
            wrestlerRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            showTypeService,
            segmentRuleService,
            segmentTypeService,
            cardSetService,
            cardSetRepository,
            cardService,
            titleService,
            deckService,
            gameSettingService,
            gameSettingRepository,
            npcService,
            factionService,
            teamService,
            teamRepository,
            tierRecalculationService,
            campaignAbilityCardService,
            commentaryService,
            campaignUpgradeService,
            statusCardService,
            env,
            achievementRepository,
            ringsideActionDataService,
            resourcePatternResolver,
            locationRepository,
            arenaRepository,
            relationshipService,
            objectMapper,
            outcomeMatrixService);

    // Common setup for many tests: ensure resources are found
    Resource mockResource = mock(Resource.class);
    when(resourcePatternResolver.getResources(anyString()))
        .thenReturn(new Resource[] {mockResource});
    when(mockResource.getInputStream())
        .thenReturn(new java.io.ByteArrayInputStream("[]".getBytes()));

    // Mock segmentTypeService to return a non-null SegmentType to avoid NPE in
    // loadSegmentTypesFromFile
    com.github.javydreamercsw.management.domain.show.segment.type.SegmentType mockSegmentType =
        mock(com.github.javydreamercsw.management.domain.show.segment.type.SegmentType.class);
    when(mockSegmentType.getName()).thenReturn("Mock Type");
    when(segmentTypeService.createOrUpdateSegmentType(anyString(), anyString()))
        .thenReturn(mockSegmentType);

    // Mock universeRepository to return a non-null Universe to avoid IllegalStateException in
    // syncWrestlersFromFile
    Universe mockUniverse = mock(Universe.class);
    when(mockUniverse.getId()).thenReturn(1L);
    when(universeRepository.findAll()).thenReturn(List.of(mockUniverse));

    com.github.javydreamercsw.management.domain.title.Title mockTitle =
        mock(com.github.javydreamercsw.management.domain.title.Title.class);
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenReturn(mockTitle);
  }

  @Test
  void testInitialize_Disabled() {
    DataInitializer disabledInitializer =
        new DataInitializer(
            false,
            false,
            showTemplateService,
            wrestlerRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            showTypeService,
            segmentRuleService,
            segmentTypeService,
            cardSetService,
            cardSetRepository,
            cardService,
            titleService,
            deckService,
            gameSettingService,
            gameSettingRepository,
            npcService,
            factionService,
            teamService,
            teamRepository,
            tierRecalculationService,
            campaignAbilityCardService,
            commentaryService,
            campaignUpgradeService,
            statusCardService,
            env,
            achievementRepository,
            ringsideActionDataService,
            resourcePatternResolver,
            locationRepository,
            arenaRepository,
            relationshipService,
            objectMapper,
            outcomeMatrixService);

    disabledInitializer.init();
    verify(gameSettingService, never()).findById(anyString());
  }

  @Test
  void testSyncWrestlersFromFile_NewWrestler() {
    WrestlerImportDTO dto = new WrestlerImportDTO();
    dto.setName("New Wrestler");
    dto.setFans(1000L);

    when(wrestlerRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(wrestlerRepository.saveAll(any())).thenAnswer(i -> i.getArguments()[0]);

    // Mock Universe and State for new wrestler
    Universe mockUniverse = mock(Universe.class);
    when(universeRepository.findAll()).thenReturn(List.of(mockUniverse));
    when(wrestlerService.getOrCreateState(any(), eq(1L))).thenReturn(new WrestlerState());

    // We test the entry point
    dataInitializer.syncWrestlersFromFile();
    // Since it's protected and we are in same package, it's accessible
  }

  @Test
  void validateCardsJson() {
    assertDoesNotThrow(
        () -> {
          org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver =
              new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
          org.springframework.core.io.Resource[] resources =
              resolver.getResources("classpath*:cards/*.json");
          for (org.springframework.core.io.Resource resource : resources) {
            new ObjectMapper()
                .readValue(resource.getInputStream(), new TypeReference<List<CardDTO>>() {});
          }
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
  void validateCampaignAbilityCardsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("campaign_ability_cards.json").getInputStream(),
                  new TypeReference<List<CampaignAbilityCardDTO>>() {});
        });
  }

  @Test
  void validateStatusCardsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("status_cards.json").getInputStream(),
                  new TypeReference<List<StatusCardDTO>>() {});
        });
  }

  @Test
  void testSyncWrestlersFromFile_existingWrestler() throws IOException {
    // Given
    Wrestler existingWrestler = new Wrestler();
    existingWrestler.setName("Rob Van Dam");

    Universe mockUniverse = mock(Universe.class);
    when(universeRepository.findAll()).thenReturn(List.of(mockUniverse));

    lenient().when(wrestlerRepository.count()).thenReturn(1L);
    lenient()
        .when(wrestlerRepository.findByName("Rob Van Dam"))
        .thenReturn(Optional.of(existingWrestler));
    lenient().when(wrestlerRepository.findAll()).thenReturn(List.of(existingWrestler));

    // Mock wrestler state to avoid NPE in syncWrestlersFromFile
    WrestlerState mockState = new WrestlerState();
    mockState.setFans(0L);
    mockState.setBumps(0);
    lenient().when(wrestlerService.getOrCreateState(any(), any())).thenReturn(mockState);

    Resource wrestlersResource = new ClassPathResource("wrestlers.json");
    when(resourcePatternResolver.getResources("classpath*:wrestlers*.json"))
        .thenReturn(new Resource[] {wrestlersResource});

    // When
    dataInitializer.syncWrestlersFromFile();

    // Then
    verify(wrestlerRepository, atLeastOnce()).saveAll(any());
  }

  @Test
  void syncAiSettingsFromEnvironment_doesNotOverwriteExistingDbValueByDefault() {
    // Simulate an environment variable being set
    when(env.getProperty("AI_OPENAI_ENABLED")).thenReturn("true");

    // But the DB already has a value (e.g., user explicitly disabled it).
    // Code now pre-loads via gameSettingRepository.findAll() — seed it there.
    GameSetting existingSetting = new GameSetting();
    existingSetting.setId("AI_OPENAI_ENABLED");
    existingSetting.setValue("false");
    when(gameSettingRepository.findAll()).thenReturn(List.of(existingSetting));

    dataInitializer.init();

    // Should NOT overwrite existing DB value unless forceOverride is enabled
    verify(gameSettingService, never()).save("AI_OPENAI_ENABLED", "true");
  }

  @Test
  void validateRelationshipsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("relationships.json").getInputStream(),
                  new TypeReference<
                      List<com.github.javydreamercsw.management.dto.RelationshipImportDTO>>() {});
        });
  }

  @Test
  void testSyncRelationshipsFromFile() {
    // Given
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    w1.setName("Johnny All Time");

    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    w2.setName("Taya Valkyrie");

    // Code now pre-loads all wrestlers via findAll; findByName is no longer called
    when(wrestlerRepository.findAll()).thenReturn(List.of(w1, w2));

    // syncDecksFromFile also uses findAll — guard against NPE when createDeck returns null
    com.github.javydreamercsw.management.domain.deck.Deck mockDeck =
        mock(com.github.javydreamercsw.management.domain.deck.Deck.class);
    when(mockDeck.getCards()).thenReturn(new java.util.HashSet<>());
    lenient().when(deckService.findByWrestlerWithCards(any())).thenReturn(List.of());
    lenient().when(deckService.createDeck(any())).thenReturn(mockDeck);

    // When
    dataInitializer.init();

    // Then
    verify(relationshipService, atLeastOnce())
        .createOrUpdateRelationship(eq(1L), eq(2L), any(), anyInt(), anyBoolean(), anyString());
  }

  @Test
  void validateLocationsJson() throws IOException {
    ClassPathResource resource = new ClassPathResource("locations.json");
    try (var is = resource.getInputStream()) {
      var locations = objectMapper.readValue(is, new TypeReference<List<LocationImportDTO>>() {});
      assertNotNull(locations);
      assertFalse(locations.isEmpty());
    }
  }

  @Test
  void validateArenasJson() throws IOException {
    ClassPathResource resource = new ClassPathResource("arenas.json");
    try (var is = resource.getInputStream()) {
      var arenas = objectMapper.readValue(is, new TypeReference<List<ArenaImportDTO>>() {});
      assertNotNull(arenas);
      assertFalse(arenas.isEmpty());
    }
  }

  @Test
  void validateArenaLocationsExistInLocationsJson() throws IOException {
    Set<String> locationNames;
    try (var is = new ClassPathResource("locations.json").getInputStream()) {
      locationNames =
          objectMapper.readValue(is, new TypeReference<List<LocationImportDTO>>() {}).stream()
              .map(LocationImportDTO::getName)
              .collect(Collectors.toSet());
    }

    try (var is = new ClassPathResource("arenas.json").getInputStream()) {
      List<ArenaImportDTO> arenas = objectMapper.readValue(is, new TypeReference<>() {});
      var missingLocationRefs =
          arenas.stream()
              .map(ArenaImportDTO::getLocation)
              .filter(location -> !locationNames.contains(location))
              .distinct()
              .toList();

      assertEquals(
          List.of(),
          missingLocationRefs,
          """
          Every arena location must exist in locations.json to avoid skipped arenas during seed\
           sync.\
          """);
    }
  }

  @Test
  void testSyncNpcsFromFile() {
    // Given
    Npc npc = new Npc();
    npc.setName("Mock NPC");
    when(npcService.findByName(anyString())).thenReturn(null);

    // When
    dataInitializer.syncNpcsFromFile();

    // Then
    verify(npcService, atLeastOnce()).saveAll(any());
  }
}
