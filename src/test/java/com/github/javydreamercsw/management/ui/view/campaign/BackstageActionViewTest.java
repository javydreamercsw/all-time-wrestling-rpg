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
package com.github.javydreamercsw.management.ui.view.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.BackstageActionService;
import com.github.javydreamercsw.management.service.campaign.BackstageEncounterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackstageActionViewTest {

  private BackstageActionService backstageActionService;
  private CampaignRepository campaignRepository;
  private WrestlerRepository wrestlerRepository;
  private WrestlerService wrestlerService;
  private SecurityUtils securityUtils;
  private CampaignService campaignService;
  private BackstageEncounterService backstageEncounterService;

  @BeforeEach
  void setUp() {
    backstageActionService = mock(BackstageActionService.class);
    campaignRepository = mock(CampaignRepository.class);
    wrestlerRepository = mock(WrestlerRepository.class);
    wrestlerService = mock(WrestlerService.class);
    securityUtils = mock(SecurityUtils.class);
    campaignService = mock(CampaignService.class);
    backstageEncounterService = mock(BackstageEncounterService.class);

    when(backstageActionService.getBackstageEncounterService())
        .thenReturn(backstageEncounterService);
  }

  @Test
  void testNoCampaignRedirection() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    BackstageActionView view =
        new BackstageActionView(
            backstageActionService,
            campaignRepository,
            wrestlerRepository,
            wrestlerService,
            securityUtils,
            campaignService);

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    view.beforeEnter(event);

    assertThat(
            view.getChildren()
                .anyMatch(c -> c instanceof H2 && ((H2) c).getText().equals("Backstage Actions")))
        .isTrue();
  }

  @Test
  void testEncounterTriggered() {
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    when(backstageEncounterService.shouldTriggerEncounter(campaign)).thenReturn(true);

    // Setup authenticated user and campaign
    var user = mock(com.github.javydreamercsw.base.security.CustomUserDetails.class);
    var account = mock(com.github.javydreamercsw.base.domain.account.Account.class);
    when(user.getAccount()).thenReturn(account);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(user));

    Wrestler active = new Wrestler();
    active.setId(1L);
    when(account.getActiveWrestlerId()).thenReturn(1L);
    when(wrestlerRepository.findByAccount(account)).thenReturn(java.util.List.of(active));
    when(campaignService.getCampaignForWrestler(active)).thenReturn(Optional.of(campaign));

    BackstageActionView view =
        new BackstageActionView(
            backstageActionService,
            campaignRepository,
            wrestlerRepository,
            wrestlerService,
            securityUtils,
            campaignService);

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    view.beforeEnter(event);

    org.mockito.Mockito.verify(event).forwardTo(BackstageEncounterView.class);
  }
}
