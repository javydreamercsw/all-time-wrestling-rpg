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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.github.javydreamercsw.management.service.campaign.WrestlerStatusService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

class WrestlerProfileViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleService titleService;
  @Mock private RankingService rankingService;
  @Mock private SegmentService segmentService;
  @Mock private MultiWrestlerFeudService multiWrestlerFeudService;
  @Mock private RivalryService rivalryService;
  @Mock private SeasonService seasonService;
  @Mock private InjuryService injuryService;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private NpcService npcService;
  @Mock private AccountService accountService;
  @Mock private CampaignService campaignService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private UniverseContextService universeContextService;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private StatusCardService statusCardService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private AlignmentService alignmentService;
  @Mock private SecurityUtils securityUtils;

  private WrestlerProfileView view;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    when(seasonService.getAllSeasons(any())).thenReturn(Page.empty());

    view =
        new WrestlerProfileView(
            wrestlerService,
            wrestlerStatsService,
            wrestlerRepository,
            titleService,
            rankingService,
            segmentService,
            multiWrestlerFeudService,
            rivalryService,
            seasonService,
            injuryService,
            injuryTypeService,
            npcService,
            accountService,
            campaignService,
            imageStorageService,
            universeContextService,
            relationshipService,
            wrestlerStatusService,
            statusCardService,
            wrestlerStateRepository,
            alignmentService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the wrestler profile toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }
}
