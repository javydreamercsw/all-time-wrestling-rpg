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
package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"notion.sync.enabled=true", "test.mock.notion-handler=false"})
class NotionTokenSettingTest extends AbstractMockUserIntegrationTest {

  @Autowired private GameSettingService gameSettingService;
  @Autowired private NotionHandler notionHandler;

  @Test
  @DisplayName("Should use Notion token from GameSettingService when available")
  void shouldUserTokenFromGameSetting() {
    String testToken = "secret_test_token_123";

    // Given: A token is saved in game settings
    gameSettingService.setNotionToken(testToken);

    String savedToken = gameSettingService.getNotionToken();
    assertEquals(testToken, savedToken);

    // Then: NotionHandler should be able to see it via its provider
    assertTrue(
        notionHandler.isNotionTokenAvailable(),
        "Token should be available from GameSettingService");
  }
}
