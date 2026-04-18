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
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.SegmentNarrationController;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class NarrationDialogTest extends AbstractViewTest {

  @Mock private NpcService npcService;
  @Mock private WrestlerService wrestlerService;
  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;
  @Mock private RivalryService rivalryService;
  @Mock private SegmentNarrationController segmentNarrationController;
  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private UniverseContextService universeContextService;

  @Mock
  private com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService
      relationshipService;

  private Segment segment;

  @BeforeEach
  void setUp() {
    segment = new Segment();
    segment.setId(1L);
    SegmentType segmentType = new SegmentType();
    segmentType.setName("Match");
    segment.setSegmentType(segmentType);

    when(segmentService.findByIdWithDetails(anyLong())).thenReturn(Optional.of(segment));
    when(npcService.findAllByType(anyString())).thenReturn(new ArrayList<>());
    when(npcService.findAll()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAllBySegment(any(), anyLong())).thenReturn(new ArrayList<>());
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
  }

  @Test
  void testDialogInitialization() {
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
            universeContextService);

    assertNotNull(dialog);
  }
}
