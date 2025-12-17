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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.dto.TitleDTO;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Import(TestSecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataInitializerTest {

  @Autowired private DataInitializer dataInitializer;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private CardSetService cardSetService;
  @Autowired private CardService cardService;
  @Autowired private DeckService deckService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private SegmentRuleService segmentRuleService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private TitleService titleService;

  @BeforeEach
  void setUp() {
    dataInitializer.init();
  }

  @Test
  void testDataLoadedFromFile() {
    // This test will fail if the data initializer is disabled.
    assertFalse(wrestlerService.findAll().isEmpty());
    assertFalse(cardSetService.findAll().isEmpty());
    assertFalse(cardService.findAll().isEmpty());
    assertFalse(deckService.findAll().isEmpty());
    assertFalse(showTypeService.findAll().isEmpty());
    assertFalse(showTemplateService.findAll().isEmpty());
    assertFalse(segmentRuleService.findAll().isEmpty());
    assertFalse(segmentTypeService.findAll().isEmpty());
    assertFalse(titleService.findAll().isEmpty());
  }

  @Test
  void testDeckImportIsIdempotentAndNoDuplicates() {
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
                  new TypeReference<List<Wrestler>>() {});
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
}
