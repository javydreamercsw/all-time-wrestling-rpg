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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class InjuryTypeNotionSyncServiceTest extends AbstractSyncTest {
  private InjuryTypeNotionSyncService injuryTypeNotionSyncService;
  @Captor private ArgumentCaptor<InjuryType> injuryTypeCaptor;

  @BeforeEach
  protected void setUp() {
    super.setUp();
    injuryTypeNotionSyncService =
        new InjuryTypeNotionSyncService(
            injuryTypeRepository, syncServiceDependencies, notionApiExecutor);
  }

  @Test
  @DisplayName("Test Sync to Notion with a single new Injury Type")
  void testSyncToNotionSingleNew() {
    // Given
    String operationId = UUID.randomUUID().toString();
    InjuryType injuryType = new InjuryType();
    injuryType.setInjuryName("Test Injury");
    injuryType.setHealthEffect(10);
    injuryType.setStaminaEffect(5);
    injuryType.setCardEffect(1);
    injuryType.setSpecialEffects("Test Special Effects");

    NotionClient client = mock(NotionClient.class);

    when(injuryTypeRepository.findAll()).thenReturn(List.of(injuryType));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    Page page = mock(Page.class);

    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    // When
    var result = injuryTypeNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(injuryTypeRepository, times(1)).saveAndFlush(injuryTypeCaptor.capture());
    InjuryType savedInjuryType = injuryTypeCaptor.getValue();
    assertNotNull(savedInjuryType.getExternalId());
    assertNotNull(savedInjuryType.getLastSync());
  }

  @Test
  @DisplayName("Test Sync to Notion with a single existing Injury Type")
  void testSyncToNotionSingleExisting() throws Exception {
    // Given
    String operationId = UUID.randomUUID().toString();

    InjuryType injuryType = new InjuryType();
    injuryType.setInjuryName("Test Injury");
    injuryType.setHealthEffect(10);
    injuryType.setStaminaEffect(5);
    injuryType.setCardEffect(1);
    injuryType.setSpecialEffects("Test Special Effects");
    injuryType.setExternalId(UUID.randomUUID().toString());

    NotionClient client = mock(NotionClient.class);

    when(injuryTypeRepository.findAll()).thenReturn(List.of(injuryType));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    // When
    var result = injuryTypeNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(1, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(injuryTypeRepository, times(1)).saveAndFlush(injuryTypeCaptor.capture());
    InjuryType savedInjuryType = injuryTypeCaptor.getValue();
    assertNotNull(savedInjuryType.getLastSync());
  }

  @Test
  @DisplayName("Test Sync to Notion with no Injury Types")
  void testSyncToNotionEmpty() {
    // Given
    String operationId = UUID.randomUUID().toString();

    when(injuryTypeRepository.findAll()).thenReturn(Collections.emptyList());

    NotionClient client = mock(NotionClient.class);

    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    // When
    var result = injuryTypeNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(injuryTypeRepository, times(0)).save(any());
  }
}
