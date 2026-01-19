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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignScriptServiceTest {

  @InjectMocks private CampaignScriptService campaignScriptService;

  @Test
  void testExecuteScriptUpdatesState() {
    // Arrange
    CampaignState state = new CampaignState();
    state.setCurrentChapter(0);
    state.setVictoryPoints(10); // Should be reset

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");

    Map<String, Object> context = new HashMap<>();
    context.put("campaignState", state);
    context.put("wrestler", wrestler);

    // Act
    Object result = campaignScriptService.executeScript("chapter1_start.groovy", context);

    // Assert
    assertThat(result).isEqualTo("Chapter 1 Started");
    assertThat(state.getCurrentChapter()).isEqualTo(1);
    assertThat(state.getVictoryPoints()).isEqualTo(0);
    assertThat(state.getBumps()).isEqualTo(0);
  }
}
