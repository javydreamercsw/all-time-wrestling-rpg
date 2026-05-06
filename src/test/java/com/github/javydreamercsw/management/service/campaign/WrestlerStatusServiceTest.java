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
package com.github.javydreamercsw.management.service.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatus;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusAction;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusHistory;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WrestlerStatusServiceTest {

  @Mock private WrestlerStatusRepository wrestlerStatusRepository;
  @Mock private WrestlerStatusHistoryRepository wrestlerStatusHistoryRepository;
  @Mock private StatusCardService statusCardService;
  @Mock private WrestlerService wrestlerService;
  @Mock private CampaignScriptService campaignScriptService;
  @Mock private GameSettingService gameSettingService;
  @Mock private ExpansionService expansionService;

  @InjectMocks private WrestlerStatusService wrestlerStatusService;

  private void mockEnabled(boolean enabled) {
    when(gameSettingService.isStatusCardsEnabled()).thenReturn(enabled);
    if (enabled) {
      when(expansionService.isExpansionEnabled(WrestlerStatusService.STATUS_CARDS_EXPANSION_CODE))
          .thenReturn(true);
    }
  }

  @Test
  void testIsStatusMechanicEnabled() {
    when(gameSettingService.isStatusCardsEnabled()).thenReturn(true);
    when(expansionService.isExpansionEnabled(WrestlerStatusService.STATUS_CARDS_EXPANSION_CODE))
        .thenReturn(true);
    assertTrue(wrestlerStatusService.isStatusMechanicEnabled());

    when(gameSettingService.isStatusCardsEnabled()).thenReturn(false);
    assertFalse(wrestlerStatusService.isStatusMechanicEnabled());

    when(gameSettingService.isStatusCardsEnabled()).thenReturn(true);
    when(expansionService.isExpansionEnabled(WrestlerStatusService.STATUS_CARDS_EXPANSION_CODE))
        .thenReturn(false);
    assertFalse(wrestlerStatusService.isStatusMechanicEnabled());
  }

  @Test
  void testAssignStatus_GainLevel1() {
    mockEnabled(true);
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    StatusCard card = new StatusCard();
    card.setId(10L);
    card.setKey("status_draw");

    when(wrestlerService.findById(1L)).thenReturn(Optional.of(wrestler));
    when(statusCardService.findByKey("status_draw")).thenReturn(card);
    when(wrestlerStatusRepository.findByWrestlerAndStatusCard(wrestler, card))
        .thenReturn(Optional.empty());

    wrestlerStatusService.assignStatus(1L, "status_draw");

    ArgumentCaptor<WrestlerStatus> statusCaptor = ArgumentCaptor.forClass(WrestlerStatus.class);
    verify(wrestlerStatusRepository).save(statusCaptor.capture());
    assertEquals(1, statusCaptor.getValue().getLevel());

    ArgumentCaptor<WrestlerStatusHistory> historyCaptor =
        ArgumentCaptor.forClass(WrestlerStatusHistory.class);
    verify(wrestlerStatusHistoryRepository).save(historyCaptor.capture());
    assertEquals(WrestlerStatusAction.GAIN, historyCaptor.getValue().getAction());
    assertEquals(1, historyCaptor.getValue().getNewLevel());
  }

  @Test
  void testAssignStatus_Disabled() {
    mockEnabled(false);
    wrestlerStatusService.assignStatus(1L, "status_draw");
    verify(wrestlerStatusRepository, never()).save(any());
  }

  @Test
  void testAssignStatus_FlipToLevel2() {
    mockEnabled(true);
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    StatusCard card = new StatusCard();
    card.setId(10L);
    card.setKey("status_draw");
    WrestlerStatus existingStatus =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(1).build();

    when(wrestlerService.findById(1L)).thenReturn(Optional.of(wrestler));
    when(statusCardService.findByKey("status_draw")).thenReturn(card);
    when(wrestlerStatusRepository.findByWrestlerAndStatusCard(wrestler, card))
        .thenReturn(Optional.of(existingStatus));

    wrestlerStatusService.assignStatus(1L, "status_draw");

    verify(wrestlerStatusRepository).save(existingStatus);
    assertEquals(2, existingStatus.getLevel());

    ArgumentCaptor<WrestlerStatusHistory> historyCaptor =
        ArgumentCaptor.forClass(WrestlerStatusHistory.class);
    verify(wrestlerStatusHistoryRepository).save(historyCaptor.capture());
    assertEquals(WrestlerStatusAction.FLIP, historyCaptor.getValue().getAction());
    assertEquals(1, historyCaptor.getValue().getOldLevel());
    assertEquals(2, historyCaptor.getValue().getNewLevel());
  }

  @Test
  void testAssignStatus_AlreadyLevel2_Ignored() {
    mockEnabled(true);
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    StatusCard card = new StatusCard();
    card.setId(10L);
    card.setKey("status_draw");
    WrestlerStatus existingStatus =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(2).build();

    when(wrestlerService.findById(1L)).thenReturn(Optional.of(wrestler));
    when(statusCardService.findByKey("status_draw")).thenReturn(card);
    when(wrestlerStatusRepository.findByWrestlerAndStatusCard(wrestler, card))
        .thenReturn(Optional.of(existingStatus));

    wrestlerStatusService.assignStatus(1L, "status_draw");

    verify(wrestlerStatusRepository, never()).save(any(WrestlerStatus.class));
    verify(wrestlerStatusHistoryRepository, never()).save(any(WrestlerStatusHistory.class));
  }

  @Test
  void testRemoveStatus() {
    mockEnabled(true);
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    StatusCard card = new StatusCard();
    card.setId(10L);
    card.setKey("status_draw");
    WrestlerStatus existingStatus =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(1).build();

    when(wrestlerService.findById(1L)).thenReturn(Optional.of(wrestler));
    when(statusCardService.findByKey("status_draw")).thenReturn(card);
    when(wrestlerStatusRepository.findByWrestlerAndStatusCard(wrestler, card))
        .thenReturn(Optional.of(existingStatus));

    wrestlerStatusService.removeStatus(1L, "status_draw");

    verify(wrestlerStatusRepository).delete(existingStatus);

    ArgumentCaptor<WrestlerStatusHistory> historyCaptor =
        ArgumentCaptor.forClass(WrestlerStatusHistory.class);
    verify(wrestlerStatusHistoryRepository).save(historyCaptor.capture());
    assertEquals(WrestlerStatusAction.LOSS, historyCaptor.getValue().getAction());
    assertEquals(1, historyCaptor.getValue().getOldLevel());
  }

  @Test
  void testEvaluateTriggerConditions_FlipUp() {
    mockEnabled(true);
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    StatusCard card =
        StatusCard.builder().key("status_draw").flipUpCondition("momentum >= 5").build();
    WrestlerStatus status =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(1).build();

    when(campaignScriptService.evaluateSnippet(anyString(), anyMap())).thenReturn(true);

    wrestlerStatusService.evaluateTriggerConditions(status, 6, false);

    assertEquals(2, status.getLevel());
    verify(wrestlerStatusRepository).save(status);
    verify(wrestlerStatusHistoryRepository).save(any(WrestlerStatusHistory.class));
  }

  @Test
  void testEvaluateTriggerConditions_FlipDown() {
    mockEnabled(true);
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    StatusCard card =
        StatusCard.builder().key("status_draw").flipDownCondition("momentum < 3").build();
    WrestlerStatus status =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(2).build();

    when(campaignScriptService.evaluateSnippet(anyString(), anyMap())).thenReturn(true);

    wrestlerStatusService.evaluateTriggerConditions(status, 2, false);

    assertEquals(1, status.getLevel());
    verify(wrestlerStatusRepository).save(status);
    verify(wrestlerStatusHistoryRepository).save(any(WrestlerStatusHistory.class));
  }

  @Test
  void testEvaluateTriggerConditions_Discard() {
    mockEnabled(true);
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    StatusCard card =
        StatusCard.builder().key("status_draw").discardCondition("loss == true").build();
    WrestlerStatus status =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(1).build();

    when(campaignScriptService.evaluateSnippet(eq("loss == true"), anyMap())).thenReturn(true);

    wrestlerStatusService.evaluateTriggerConditions(status, 0, true);

    verify(wrestlerStatusRepository).delete(status);
    verify(wrestlerStatusHistoryRepository).save(any(WrestlerStatusHistory.class));
  }
}
