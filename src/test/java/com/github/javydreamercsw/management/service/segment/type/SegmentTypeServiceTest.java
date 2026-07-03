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
package com.github.javydreamercsw.management.service.segment.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentTypeServiceTest {

  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private ExpansionService expansionService;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseSettingsService universeSettingsService;

  @InjectMocks private SegmentTypeService segmentTypeService;

  private SegmentType segmentType;

  @BeforeEach
  void setUp() {
    segmentType = new SegmentType();
    segmentType.setName("Match");
    segmentType.setDescription("A standard wrestling match");
    segmentType.setExpansionCode("BASE_GAME");

    // Default: no active universe, all expansions enabled
    Mockito.when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    Mockito.when(expansionService.getEnabledExpansionCodes())
        .thenReturn(List.of("BASE_GAME", "CUSTOM"));
    Mockito.when(expansionService.buildPriorityMap())
        .thenReturn(Map.of("BASE_GAME", 10, "CUSTOM", 0));
  }

  // ==================== findByName ====================

  @Test
  void findByName_found_returnsSegmentType() {
    when(segmentTypeRepository.findByName("Match")).thenReturn(Optional.of(segmentType));

    Optional<SegmentType> result = segmentTypeService.findByName("Match");

    assertTrue(result.isPresent());
    assertSame(segmentType, result.get());
  }

  @Test
  void findByName_notFound_returnsEmpty() {
    when(segmentTypeRepository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<SegmentType> result = segmentTypeService.findByName("Unknown");

    assertTrue(result.isEmpty());
  }

  // ==================== findAll ====================

  @Test
  void findAll_returnsList() {
    SegmentType st2 = new SegmentType();
    st2.setName("Promo");
    st2.setExpansionCode("BASE_GAME");
    when(segmentTypeRepository.findAll()).thenReturn(List.of(segmentType, st2));

    List<SegmentType> result = segmentTypeService.findAll();

    assertEquals(2, result.size());
  }

  // ==================== count ====================

  @Test
  void count_returnsValue() {
    when(segmentTypeRepository.count()).thenReturn(5L);

    long count = segmentTypeService.count();

    assertEquals(5L, count);
  }

  // ==================== createSegmentType ====================

  @Test
  void createSegmentType_savesAndReturns() {
    when(segmentTypeRepository.save(segmentType)).thenReturn(segmentType);

    SegmentType result = segmentTypeService.createSegmentType(segmentType);

    assertSame(segmentType, result);
    verify(segmentTypeRepository).save(segmentType);
  }

  // ==================== createOrUpdateSegmentType ====================

  @Test
  void createOrUpdate_existingUnchangedDescription_returnsExistingWithoutSave() {
    when(segmentTypeRepository.findByName("Match")).thenReturn(Optional.of(segmentType));

    SegmentType result =
        segmentTypeService.createOrUpdateSegmentType("Match", "A standard wrestling match");

    assertSame(segmentType, result);
    verify(segmentTypeRepository, never()).save(any());
  }

  @Test
  void createOrUpdate_existingChangedDescription_updatesAndSaves() {
    when(segmentTypeRepository.findByName("Match")).thenReturn(Optional.of(segmentType));
    when(segmentTypeRepository.save(segmentType)).thenReturn(segmentType);

    SegmentType result = segmentTypeService.createOrUpdateSegmentType("Match", "Updated desc");

    assertEquals("Updated desc", segmentType.getDescription());
    assertSame(segmentType, result);
    verify(segmentTypeRepository).save(segmentType);
  }

  @Test
  void createOrUpdate_notFound_createsNewSegmentType() {
    when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.empty());
    when(segmentTypeRepository.save(any(SegmentType.class))).thenAnswer(inv -> inv.getArgument(0));

    SegmentType result = segmentTypeService.createOrUpdateSegmentType("Promo", "A mic segment");

    assertEquals("Promo", result.getName());
    assertEquals("A mic segment", result.getDescription());
    verify(segmentTypeRepository).save(any(SegmentType.class));
  }

  // ==================== deleteSegmentType ====================

  @Test
  void deleteSegmentType_found_deletesById() {
    when(segmentTypeRepository.existsById(1L)).thenReturn(true);

    segmentTypeService.deleteSegmentType(1L);

    verify(segmentTypeRepository).deleteById(1L);
  }

  @Test
  void deleteSegmentType_notFound_throwsIllegalArgumentException() {
    when(segmentTypeRepository.existsById(99L)).thenReturn(false);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> segmentTypeService.deleteSegmentType(99L));

    assertTrue(ex.getMessage().contains("99"));
    verify(segmentTypeRepository, never()).deleteById(any());
  }

  // ==================== priority deduplication ====================

  @Test
  void findAll_sameNameDifferentExpansions_returnsHigherPriorityVersion() {
    SegmentType baseTagTeam = new SegmentType();
    baseTagTeam.setName("Tag Team");
    baseTagTeam.setExpansionCode("BASE_GAME");
    baseTagTeam.setDescription("Base version");

    SegmentType v2TagTeam = new SegmentType();
    v2TagTeam.setName("Tag Team");
    v2TagTeam.setExpansionCode("TAG_V2");
    v2TagTeam.setDescription("Enhanced version");

    Mockito.when(expansionService.getEnabledExpansionCodes())
        .thenReturn(List.of("BASE_GAME", "TAG_V2"));
    Mockito.when(expansionService.buildPriorityMap())
        .thenReturn(Map.of("BASE_GAME", 10, "TAG_V2", 20));
    when(segmentTypeRepository.findAll()).thenReturn(List.of(baseTagTeam, v2TagTeam));

    List<SegmentType> result = segmentTypeService.findAll();

    assertEquals(1, result.size());
    assertEquals("Enhanced version", result.get(0).getDescription());
    assertEquals("TAG_V2", result.get(0).getExpansionCode());
  }

  @Test
  void findAll_customAndOfficialSameName_officialWins() {
    SegmentType customLadder = new SegmentType();
    customLadder.setName("Ladder Match");
    customLadder.setExpansionCode("CUSTOM");
    customLadder.setDescription("Custom variant");

    SegmentType officialLadder = new SegmentType();
    officialLadder.setName("Ladder Match");
    officialLadder.setExpansionCode("BASE_GAME");
    officialLadder.setDescription("Official");

    when(segmentTypeRepository.findAll()).thenReturn(List.of(customLadder, officialLadder));

    List<SegmentType> result = segmentTypeService.findAll();

    assertEquals(1, result.size());
    assertEquals("Official", result.get(0).getDescription());
  }

  @Test
  void findAll_resultIsSortedByName() {
    SegmentType promo = new SegmentType();
    promo.setName("Promo");
    promo.setExpansionCode("BASE_GAME");

    SegmentType match = new SegmentType();
    match.setName("Match");
    match.setExpansionCode("BASE_GAME");

    when(segmentTypeRepository.findAll()).thenReturn(List.of(promo, match));

    List<SegmentType> result = segmentTypeService.findAll();

    assertEquals(List.of("Match", "Promo"), result.stream().map(SegmentType::getName).toList());
  }
}
