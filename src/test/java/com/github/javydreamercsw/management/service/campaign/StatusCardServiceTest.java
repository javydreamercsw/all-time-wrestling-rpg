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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.StatusCardRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusCardServiceTest {

  @Mock private StatusCardRepository statusCardRepository;

  @InjectMocks private StatusCardService statusCardService;

  @Test
  void testCreateOrUpdateCard_New() {
    when(statusCardRepository.findByKey(anyString())).thenReturn(Optional.empty());

    statusCardService.createOrUpdateCard(
        "status_draw",
        "Draw",
        "Main Eventer",
        "Description",
        true,
        "L1 Effect",
        "L2 Effect",
        "Up",
        "Down",
        "Discard");

    ArgumentCaptor<StatusCard> captor = ArgumentCaptor.forClass(StatusCard.class);
    verify(statusCardRepository).save(captor.capture());
    StatusCard saved = captor.getValue();

    assertEquals("status_draw", saved.getKey());
    assertEquals("Draw", saved.getLevel1Name());
    assertEquals("Main Eventer", saved.getLevel2Name());
    assertEquals("Description", saved.getDescription());
    assertTrue(saved.isPositive());
    assertEquals("L1 Effect", saved.getLevel1Effect());
    assertEquals("L2 Effect", saved.getLevel2Effect());
    assertEquals("Up", saved.getFlipUpCondition());
    assertEquals("Down", saved.getFlipDownCondition());
    assertEquals("Discard", saved.getDiscardCondition());
  }

  @Test
  void testCreateOrUpdateCard_Existing() {
    StatusCard existing = new StatusCard();
    existing.setId(1L);
    existing.setKey("status_existing");
    existing.setLevel1Name("Old L1 Name");
    existing.setDescription("Old Description");

    when(statusCardRepository.findByKey(anyString())).thenReturn(Optional.of(existing));

    statusCardService.createOrUpdateCard(
        "status_existing",
        "New L1 Name",
        "New L2 Name",
        "New Description",
        false,
        "New L1",
        "New L2",
        "New Up",
        "New Down",
        "New Discard");

    verify(statusCardRepository).save(existing);
    assertEquals("New L1 Name", existing.getLevel1Name());
    assertEquals("New Description", existing.getDescription());
    assertEquals("New L1", existing.getLevel1Effect());
    assertEquals(false, existing.isPositive());
  }
}
