/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.segment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationController;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NarrationDialogTest {

  private Segment segment;
  @Mock private NpcService npcService;
  @Mock private WrestlerService wrestlerService;
  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;
  @Mock private RivalryService rivalryService;
  @Mock private SegmentNarrationController segmentNarrationController;
  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private RingsideActionService ringsideActionService;

  @Mock
  private com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService
      relationshipService;

  @Mock private UniverseContextService universeContextService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    segment = new Segment();
    segment.setId(1L);
    com.github.javydreamercsw.management.domain.show.segment.type.SegmentType type =
        new com.github.javydreamercsw.management.domain.show.segment.type.SegmentType();
    type.setName("Match");
    segment.setSegmentType(type);

    when(segmentService.findByIdWithDetails(anyLong())).thenReturn(Optional.of(segment));
    when(npcService.findAllByType("Referee")).thenReturn(new ArrayList<>());
    when(npcService.findAllByType("Commissioner")).thenReturn(new ArrayList<>());
    when(npcService.findAllByType("Commentator")).thenReturn(new ArrayList<>());
    when(npcService.findAllByType("Announcer")).thenReturn(new ArrayList<>());
    when(npcService.findAll()).thenReturn(new ArrayList<>());
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
  }

  @Test
  void testDialogInitialization() {
    com.github.javydreamercsw.base.ui.service.NotificationService notificationService =
        mock(com.github.javydreamercsw.base.ui.service.NotificationService.class);

    NarrationDialog dialog =
        new NarrationDialog(
            segment,
            npcService,
            wrestlerService,
            showService,
            segmentService,
            s -> {},
            rivalryService,
            segmentNarrationController,
            aiFactory,
            ringsideActionService,
            relationshipService,
            universeContextService,
            notificationService);
    assertNotNull(dialog);
  }
}
