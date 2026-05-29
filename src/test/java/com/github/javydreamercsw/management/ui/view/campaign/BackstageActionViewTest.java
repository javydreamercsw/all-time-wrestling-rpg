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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.BackstageActionService;
import com.github.javydreamercsw.management.service.campaign.BackstageEncounterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class BackstageActionViewTest extends AbstractViewTest {

  @Mock private BackstageActionService backstageActionService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private InjuryService injuryService;
  @Mock private UniverseContextService universeContextService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignService campaignService;
  @Mock private BackstageEncounterService backstageEncounterService;

  @BeforeEach
  void setup() {
    when(backstageActionService.getBackstageEncounterService())
        .thenReturn(backstageEncounterService);
  }

  @Test
  @DisplayName("Should render the Backstage Actions heading when unauthenticated")
  void shouldRenderHeadingWhenUnauthenticated() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    BackstageActionView view =
        new BackstageActionView(
            backstageActionService,
            wrestlerRepository,
            wrestlerService,
            injuryService,
            universeContextService,
            securityUtils,
            campaignService);

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    view.beforeEnter(event);

    assertThat(
            view.getChildren()
                .anyMatch(c -> c instanceof H2 && "Backstage Actions".equals(((H2) c).getText())))
        .isTrue();
  }

  @Test
  @DisplayName("Should forward to BackstageEncounterView when encounter triggers")
  void shouldForwardWhenEncounterTriggered() {
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    when(backstageEncounterService.shouldTriggerEncounter(campaign)).thenReturn(true);

    CustomUserDetails user = mock(CustomUserDetails.class);
    Account account = mock(Account.class);
    when(user.getAccount()).thenReturn(account);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(user));

    Wrestler active = new Wrestler();
    active.setId(1L);
    when(account.getActiveWrestlerId()).thenReturn(1L);
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(active));
    when(campaignService.getCampaignForWrestler(active)).thenReturn(Optional.of(campaign));

    BackstageActionView view =
        new BackstageActionView(
            backstageActionService,
            wrestlerRepository,
            wrestlerService,
            injuryService,
            universeContextService,
            securityUtils,
            campaignService);

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    view.beforeEnter(event);

    verify(event).forwardTo(BackstageEncounterView.class);
  }
}
