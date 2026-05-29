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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.SmartPromoService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class PromoViewTest extends AbstractViewTest {

  @Mock private SmartPromoService smartPromoService;
  @Mock private CampaignRepository campaignRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private SegmentService segmentService;
  @Mock private NotificationService notificationService;

  private PromoView view;

  @BeforeEach
  void setup() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    view =
        new PromoView(
            smartPromoService,
            campaignRepository,
            wrestlerRepository,
            securityUtils,
            segmentService,
            notificationService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the promo view title")
  void shouldRenderTitle() {
    H2 title = _get(view, H2.class, spec -> spec.withId("promo-view-title"));
    assertTrue(title.isVisible());
  }
}
