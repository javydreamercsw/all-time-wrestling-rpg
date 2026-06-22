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
package com.github.javydreamercsw.management.service.ringside;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionRepository;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionTypeRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.List;
import java.util.Optional;
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
class RingsideActionDataServiceTest {

  @Mock private RingsideActionRepository ringsideActionRepository;
  @Mock private RingsideActionTypeRepository ringsideActionTypeRepository;
  @Mock private ExpansionService expansionService;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseSettingsService universeSettingsService;

  @InjectMocks private RingsideActionDataService service;

  private RingsideActionType actionType;
  private RingsideAction action;

  @BeforeEach
  void setUp() {
    actionType = new RingsideActionType();
    actionType.setName("Interference");
    actionType.setIncreasesAwareness(true);
    actionType.setCanCauseDq(true);
    actionType.setBaseRiskMultiplier(1.5);

    action = new RingsideAction();
    action.setName("Low Blow");
    action.setType(actionType);
    action.setDescription("A cheap shot below the belt");
    action.setImpact(5);
    action.setRisk(3);
    action.setAlignment(AlignmentType.HEEL);
    action.setExpansionCode("BASE_GAME");

    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of("BASE_GAME"));
    when(ringsideActionTypeRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    when(ringsideActionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  void findAllTypes_delegatesToRepository() {
    List<RingsideActionType> expected = List.of(actionType);
    when(ringsideActionTypeRepository.findAll()).thenReturn(expected);

    List<RingsideActionType> result = service.findAllTypes();

    assertEquals(expected, result);
    verify(ringsideActionTypeRepository).findAll();
  }

  @Test
  void countTypes_returnsValue() {
    when(ringsideActionTypeRepository.count()).thenReturn(7L);

    long result = service.countTypes();

    assertEquals(7L, result);
    verify(ringsideActionTypeRepository).count();
  }

  @Test
  void findAllActions_delegatesToRepository() {
    List<RingsideAction> expected = List.of(action);
    when(ringsideActionRepository.findAll()).thenReturn(expected);

    List<RingsideAction> result = service.findAllActions();

    assertEquals(expected, result);
    verify(ringsideActionRepository).findAll();
  }

  @Test
  void countActions_returnsValue() {
    when(ringsideActionRepository.count()).thenReturn(42L);

    long result = service.countActions();

    assertEquals(42L, result);
    verify(ringsideActionRepository).count();
  }

  @Test
  void createOrUpdateType_newType_createsAndSaves() {
    when(ringsideActionTypeRepository.findByName("NewType")).thenReturn(Optional.empty());

    RingsideActionType result = service.createOrUpdateType("NewType", false, true, 2.0);

    assertNotNull(result);
    assertEquals("NewType", result.getName());
    assertEquals(false, result.isIncreasesAwareness());
    assertEquals(true, result.isCanCauseDq());
    assertEquals(2.0, result.getBaseRiskMultiplier());
    verify(ringsideActionTypeRepository).save(any(RingsideActionType.class));
  }

  @Test
  void createOrUpdateType_existingType_updatesAndSaves() {
    when(ringsideActionTypeRepository.findByName("Interference"))
        .thenReturn(Optional.of(actionType));

    RingsideActionType result = service.createOrUpdateType("Interference", false, false, 0.5);

    assertNotNull(result);
    assertEquals("Interference", result.getName());
    assertEquals(false, result.isIncreasesAwareness());
    assertEquals(false, result.isCanCauseDq());
    assertEquals(0.5, result.getBaseRiskMultiplier());
    verify(ringsideActionTypeRepository).save(actionType);
  }

  @Test
  void createOrUpdateAction_typeNotFound_throwsIllegalArgumentException() {
    when(ringsideActionTypeRepository.findByName("UnknownType")).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.createOrUpdateAction(
                "Some Action", "UnknownType", "desc", 3, 2, AlignmentType.NEUTRAL));
  }

  @Test
  void createOrUpdateAction_newAction_createsAndSaves() {
    when(ringsideActionTypeRepository.findByName("Interference"))
        .thenReturn(Optional.of(actionType));
    when(ringsideActionRepository.findByName("New Action")).thenReturn(Optional.empty());

    RingsideAction result =
        service.createOrUpdateAction(
            "New Action", "Interference", "A new action", 4, 2, AlignmentType.FACE);

    assertNotNull(result);
    assertEquals("New Action", result.getName());
    assertEquals(actionType, result.getType());
    assertEquals("A new action", result.getDescription());
    assertEquals(4, result.getImpact());
    assertEquals(2, result.getRisk());
    assertEquals(AlignmentType.FACE, result.getAlignment());
    verify(ringsideActionRepository).save(any(RingsideAction.class));
  }

  @Test
  void createOrUpdateAction_existingAction_updatesAndSaves() {
    when(ringsideActionTypeRepository.findByName("Interference"))
        .thenReturn(Optional.of(actionType));
    when(ringsideActionRepository.findByName("Low Blow")).thenReturn(Optional.of(action));

    RingsideAction result =
        service.createOrUpdateAction(
            "Low Blow", "Interference", "Updated description", 6, 4, AlignmentType.HEEL);

    assertNotNull(result);
    assertEquals("Low Blow", result.getName());
    assertEquals("Updated description", result.getDescription());
    assertEquals(6, result.getImpact());
    assertEquals(4, result.getRisk());
    verify(ringsideActionRepository).save(action);
  }

  @Test
  void createOrUpdateAction_nullAlignment_defaultsToNeutral() {
    when(ringsideActionTypeRepository.findByName("Interference"))
        .thenReturn(Optional.of(actionType));
    when(ringsideActionRepository.findByName("Neutral Action")).thenReturn(Optional.empty());

    RingsideAction result =
        service.createOrUpdateAction("Neutral Action", "Interference", "desc", 2, 1, null);

    assertNotNull(result);
    assertEquals(AlignmentType.NEUTRAL, result.getAlignment());
  }
}
