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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignScriptServiceTest {

  private CampaignScriptService scriptService;
  private CampaignStateRepository stateRepository;

  @BeforeEach
  void setUp() {
    stateRepository = mock(CampaignStateRepository.class);
    when(stateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    scriptService = new CampaignScriptService(stateRepository, new ObjectMapper());
  }

  @Test
  void testEvaluateSnippet() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("val", 10);
    Object result = scriptService.evaluateSnippet("val * 2", variables);
    assertThat(result).isEqualTo(20);
  }

  @Test
  void testExecuteEffect() {
    CampaignState state = new CampaignState();
    Campaign campaign = new Campaign();
    campaign.setState(state);
    // Verifies the script runs and mutates state without crashing
    scriptService.executeEffect("spendStamina(1); gainInitiative()", campaign);
    assertThat(state.getStaminaPenalty()).isEqualTo(1);
  }
}
