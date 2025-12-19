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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.SegmentNarrationConfig;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NarrationDialogTest {

  @Mock private NpcService npcService;
  @Mock private WrestlerService wrestlerService;
  @Mock private ShowService showService;
  @Mock private RivalryService rivalryService;
  @Mock private LocalAIStatusService localAIStatusService;
  @Mock private SegmentNarrationConfig segmentNarrationConfig;

  private NarrationDialog narrationDialog;

  private Segment segment;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Type");

    Segment segment1 = new Segment();
    segment1.setId(1L);
    segment1.setSegmentOrder(1);
    segment1.setNarration("Segment 1 Narration");
    segment1.setSummary("Segment 1 Summary");
    segment1.setShow(show);
    segment1.setSegmentType(segmentType);

    segment = new Segment();
    segment.setId(2L);
    segment.setSegmentOrder(2);
    segment.setShow(show);
    segment.setSegmentType(segmentType);

    List<Segment> segments = new ArrayList<>();
    segments.add(segment1);
    segments.add(segment);

    when(showService.getSegments(show)).thenReturn(segments);
    when(localAIStatusService.isReady()).thenReturn(true);

    narrationDialog =
        new NarrationDialog(
            segment,
            npcService,
            wrestlerService,
            showService,
            s -> {},
            rivalryService,
            localAIStatusService,
            segmentNarrationConfig);
  }

  @Test
  void testBuildSegmentContext_withPreviousSegments() {
    // When
    SegmentNarrationService.SegmentNarrationContext context = narrationDialog.buildSegmentContext();

    // Then
    assertNotNull(context.getPreviousSegments());
    assertEquals(1, context.getPreviousSegments().size());

    SegmentNarrationService.SegmentNarrationContext previousSegmentContext =
        context.getPreviousSegments().get(0);
    assertEquals("Segment 1 Narration", previousSegmentContext.getNarration());
    assertEquals("Segment 1 Summary", previousSegmentContext.getDeterminedOutcome());
  }

  @Test
  void testBuildSegmentContext_withChampionshipTitle() {
    // Given
    Title title = new Title();
    title.setName("World Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    Wrestler champion = new Wrestler();
    champion.setName("John Cena");
    title.getChampion().add(champion);
    segment.getTitles().add(title);

    // When
    SegmentNarrationService.SegmentNarrationContext context = narrationDialog.buildSegmentContext();

    // Then
    assertNotNull(context.getTitles());
    assertEquals(1, context.getTitles().size());
    SegmentNarrationService.TitleContext titleContext = context.getTitles().get(0);
    assertEquals("World Championship", titleContext.getName());
    assertEquals("John Cena", titleContext.getCurrentHolderName());
    assertEquals("MAIN_EVENTER", titleContext.getTier());
  }
}
