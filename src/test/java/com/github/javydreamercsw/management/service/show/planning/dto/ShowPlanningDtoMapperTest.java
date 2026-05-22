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
package com.github.javydreamercsw.management.service.show.planning.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.PromoType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowPlanningDtoMapperTest {

  @InjectMocks private ShowPlanningDtoMapper mapper;

  @Mock private com.github.javydreamercsw.management.service.show.ShowService showService;
  @Mock private Segment segment;
  @Mock private Show show;
  @Mock private SegmentType segmentType;
  @Mock private Wrestler wrestler1;
  @Mock private Wrestler wrestler2;
  @Mock private SegmentParticipant participant1;
  @Mock private SegmentParticipant participant2;
  @Mock private SegmentRule segmentRule;

  @BeforeEach
  public void setUp() {
    when(segment.getShow()).thenReturn(show);
    when(show.getId()).thenReturn(1L);
    when(show.getName()).thenReturn("Test Show");
    when(segment.getSegmentDate()).thenReturn(Instant.now());
  }

  @Test
  void toDto_promoSegmentWithWinners_mapsCorrectly() {
    // Given
    when(wrestler1.getName()).thenReturn("Wrestler One");
    when(wrestler2.getName()).thenReturn("Wrestler Two");
    when(participant1.getWrestler()).thenReturn(wrestler1);
    when(participant2.getWrestler()).thenReturn(wrestler2);
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Promo");
    when(segment.getSegmentRules()).thenReturn(java.util.Set.of(segmentRule));
    when(segmentRule.getName()).thenReturn(PromoType.CONFRONTATION_PROMO.getDisplayName());
    when(segment.getParticipants()).thenReturn(java.util.Set.of(participant1, participant2));
    when(participant1.getIsWinner()).thenReturn(true);
    when(participant2.getIsWinner()).thenReturn(false);
    when(segment.getSummary()).thenReturn("Promo summary");
    when(show.getShowDate()).thenReturn(java.time.LocalDate.now());

    // When
    ShowPlanningSegmentDTO dto = mapper.toDto(segment);

    // Then
    assertEquals(PromoType.CONFRONTATION_PROMO.getDisplayName(), dto.getName());
    assertEquals(1, dto.getWinners().size());
    assertTrue(dto.getWinners().contains("Wrestler One"));
    assertEquals("Promo summary", dto.getSummary());
  }

  @Test
  void toDto_promoSegmentWithoutWinners_mapsCorrectly() {
    // Given
    when(wrestler1.getName()).thenReturn("Wrestler One");
    when(participant1.getWrestler()).thenReturn(wrestler1);
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Promo");
    when(segment.getSegmentRules()).thenReturn(java.util.Set.of(segmentRule));
    when(segmentRule.getName()).thenReturn(PromoType.SOLO_PROMO.getDisplayName());
    when(segment.getParticipants()).thenReturn(java.util.Set.of(participant1));
    when(participant1.getIsWinner()).thenReturn(false);
    when(segment.getSummary()).thenReturn("Solo promo summary");
    when(show.getShowDate()).thenReturn(java.time.LocalDate.now());

    // When
    ShowPlanningSegmentDTO dto = mapper.toDto(segment);

    // Then
    assertEquals(PromoType.SOLO_PROMO.getDisplayName(), dto.getName());
    assertTrue(dto.getWinners().isEmpty());
    assertEquals("Solo promo summary", dto.getSummary());
  }

  @Test
  void toDto_nonPromoSegmentWithWinners_mapsCorrectly() {
    // Given
    when(wrestler1.getName()).thenReturn("Wrestler One");
    when(wrestler2.getName()).thenReturn("Wrestler Two");
    when(participant1.getWrestler()).thenReturn(wrestler1);
    when(participant2.getWrestler()).thenReturn(wrestler2);
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Match");
    when(segment.getSegmentRulesAsString()).thenReturn("Standard Match Rules");
    when(segment.getParticipants()).thenReturn(java.util.Set.of(participant1, participant2));
    when(participant1.getIsWinner()).thenReturn(true); // Added this line
    when(participant2.getIsWinner()).thenReturn(false);
    when(segment.getSummary()).thenReturn("Match summary");
    when(show.getShowDate()).thenReturn(java.time.LocalDate.now());

    // When
    ShowPlanningSegmentDTO dto = mapper.toDto(segment);

    // Then
    assertEquals("Standard Match Rules", dto.getName());
    assertEquals(1, dto.getWinners().size());
    assertTrue(dto.getWinners().contains("Wrestler One"));
    assertEquals("Match summary", dto.getSummary());
  }

  @Test
  void toDto_nonPromoSegmentWithoutWinners_mapsCorrectly() {
    // Given
    when(wrestler1.getName()).thenReturn("Wrestler One");
    when(wrestler2.getName()).thenReturn("Wrestler Two");
    when(participant1.getWrestler()).thenReturn(wrestler1);
    when(participant2.getWrestler()).thenReturn(wrestler2);
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Match");
    when(segment.getSegmentRulesAsString()).thenReturn("Standard Match Rules");
    when(segment.getParticipants()).thenReturn(java.util.Set.of(participant1, participant2));
    when(participant1.getIsWinner()).thenReturn(false);
    when(participant2.getIsWinner()).thenReturn(false);
    when(segment.getSummary()).thenReturn("Match summary");
    when(show.getShowDate()).thenReturn(java.time.LocalDate.now());

    // When
    ShowPlanningSegmentDTO dto = mapper.toDto(segment);

    // Then
    assertEquals("Standard Match Rules", dto.getName());
    assertTrue(dto.getWinners().isEmpty());
    assertEquals("Match summary", dto.getSummary());
  }

  @Test
  void toRosterEntryDto_mapsNameAndGender() {
    // Given
    com.github.javydreamercsw.management.domain.wrestler.Wrestler wrestler =
        com.github.javydreamercsw.management.domain.wrestler.Wrestler.builder().build();
    wrestler.setName("John Doe");
    wrestler.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);

    // When
    ShowPlanningRosterEntryDTO dto = mapper.toRosterEntryDto(wrestler);

    // Then
    assertNotNull(dto);
    assertEquals("John Doe", dto.getName());
    assertEquals("MALE", dto.getGender());
  }

  @Test
  void toRosterEntryDto_withDefaultState_mapsTierAndFans() {
    // Given
    com.github.javydreamercsw.management.domain.wrestler.Wrestler wrestler =
        com.github.javydreamercsw.management.domain.wrestler.Wrestler.builder().build();
    wrestler.setName("Main Eventer");
    wrestler.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.FEMALE);
    WrestlerState state = WrestlerState.builder().tier(WrestlerTier.MAIN_EVENTER).build();
    state.setFans(5000L);
    wrestler.getWrestlerStates().add(state);

    // When
    ShowPlanningRosterEntryDTO dto = mapper.toRosterEntryDto(wrestler);

    // Then
    assertNotNull(dto);
    assertEquals("Main Eventer", dto.getName());
    assertEquals("FEMALE", dto.getGender());
    assertEquals(WrestlerTier.MAIN_EVENTER, dto.getTier());
    assertEquals(5000L, dto.getFans());
  }

  @Test
  void toRosterEntryDto_nullGender_defaultsMale() {
    // Given
    com.github.javydreamercsw.management.domain.wrestler.Wrestler wrestler =
        com.github.javydreamercsw.management.domain.wrestler.Wrestler.builder().build();
    wrestler.setName("Unknown");
    wrestler.setGender(null);

    // When
    ShowPlanningRosterEntryDTO dto = mapper.toRosterEntryDto(wrestler);

    // Then
    assertEquals("MALE", dto.getGender());
  }
}
