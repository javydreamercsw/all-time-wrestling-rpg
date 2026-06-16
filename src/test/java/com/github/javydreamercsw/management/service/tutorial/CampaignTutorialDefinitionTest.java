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
package com.github.javydreamercsw.management.service.tutorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CampaignTutorialDefinitionTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private CampaignService campaignService;
  @Mock private CampaignChapterService campaignChapterService;
  @Mock private BackstageActionHistoryRepository backstageActionHistoryRepository;

  private CampaignTutorialDefinition definition;
  private Account account;
  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    definition =
        new CampaignTutorialDefinition(
            wrestlerService,
            wrestlerRepository,
            campaignService,
            campaignChapterService,
            backstageActionHistoryRepository);

    account = new Account();
    account.setId(1L);
    account.setUsername("test-player");

    wrestler = new Wrestler();
    ReflectionTestUtils.setField(wrestler, "id", 42L);
  }

  private TutorialStep step2() {
    return definition.getSteps().get(1);
  }

  @Test
  @DisplayName("Step 2 validate fails when no active wrestler selected")
  void step2_noActiveWrestler_fails() {
    String result = step2().validate(account);
    assertThat(result).contains("select a wrestler");
  }

  @Test
  @DisplayName("Step 2 validate passes when active wrestler has a campaign")
  void step2_activeWrestlerHasCampaign_passes() {
    account.setActiveWrestlerId(42L);
    when(wrestlerService.findByIdWithDetails(42L)).thenReturn(Optional.of(wrestler));
    when(campaignService.hasActiveCampaign(wrestler)).thenReturn(true);

    String result = step2().validate(account);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Step 2 validate falls back to account wrestlers and passes when one has campaign")
  void step2_fallbackAccountWrestlerHasCampaign_passes() {
    account.setActiveWrestlerId(42L);
    Wrestler otherWrestler = new Wrestler();
    ReflectionTestUtils.setField(otherWrestler, "id", 99L);

    when(wrestlerService.findByIdWithDetails(42L)).thenReturn(Optional.of(wrestler));
    when(campaignService.hasActiveCampaign(wrestler)).thenReturn(false);
    when(wrestlerRepository.findByAccountId(1L)).thenReturn(List.of(wrestler, otherWrestler));
    when(campaignService.hasActiveCampaign(otherWrestler)).thenReturn(true);

    String result = step2().validate(account);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName(
      "Step 2 validate fails when neither active wrestler nor any account wrestler has campaign")
  void step2_noCampaignAnywhere_fails() {
    account.setActiveWrestlerId(42L);
    when(wrestlerService.findByIdWithDetails(42L)).thenReturn(Optional.of(wrestler));
    when(campaignService.hasActiveCampaign(wrestler)).thenReturn(false);
    when(wrestlerRepository.findByAccountId(1L)).thenReturn(List.of(wrestler));

    String result = step2().validate(account);
    assertThat(result).isNotNull().contains("active campaign");
  }
}
