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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.dto.*;
import com.github.javydreamercsw.management.sync.DataSyncContributor;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

  @Mock private DataSyncContributor contributor;

  private DataInitializer dataInitializer;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    dataInitializer = new DataInitializer(true, List.of(contributor));
  }

  @Test
  void testInitialize_Disabled() {
    DataInitializer disabled = new DataInitializer(false, List.of(contributor));
    disabled.init();
    verify(contributor, never()).sync();
  }

  @Test
  void init_delegatesToAllContributors() {
    dataInitializer.init();
    verify(contributor).sync();
  }

  // ── JSON validation tests ─────────────────────────────────────────────────

  @Test
  void validateCardsJson() {
    assertDoesNotThrow(
        () -> {
          PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
          Resource[] resources = resolver.getResources("classpath*:cards/*.json");
          for (Resource resource : resources) {
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
  void validateRelationshipsJson() {
    assertDoesNotThrow(
        () -> {
          new ObjectMapper()
              .readValue(
                  new ClassPathResource("relationships.json").getInputStream(),
                  new TypeReference<List<RelationshipImportDTO>>() {});
        });
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
  void validateWrestlersJson() {
    assertDoesNotThrow(
        () -> {
          PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
          Resource[] resources = resolver.getResources("classpath*:wrestlers*.json");
          for (Resource resource : resources) {
            new ObjectMapper()
                .readValue(
                    resource.getInputStream(), new TypeReference<List<WrestlerImportDTO>>() {});
          }
        });
  }
}
