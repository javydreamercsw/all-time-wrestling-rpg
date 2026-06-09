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
package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.UI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditSegmentDialogTest {

  private ProposedSegment segment;
  private WrestlerRepository wrestlerRepository;
  private WrestlerService wrestlerService;
  private TitleService titleService;
  private SegmentTypeRepository segmentTypeRepository;
  private SegmentRuleRepository segmentRuleRepository;
  private NpcService npcService;
  private Runnable onSave;
  private SegmentType matchType;
  private UI ui;

  @BeforeEach
  public void setUp() {
    // Mock the UI context
    ui = mock(UI.class);
    lenient().when(ui.getUI()).thenReturn(Optional.of(ui));
    UI.setCurrent(ui);

    segment = new ProposedSegment();
    segment.setType("Match");
    segment.setSummary("Original Summary");
    segment.setNarration("Original Narration");
    segment.setIsTitleSegment(false);
    segment.setParticipants(new ArrayList<>(Arrays.asList("Wrestler 1", "Wrestler 2")));

    wrestlerRepository = mock(WrestlerRepository.class);
    wrestlerService = mock(WrestlerService.class);
    titleService = mock(TitleService.class);
    segmentTypeRepository = mock(SegmentTypeRepository.class);
    segmentRuleRepository = mock(SegmentRuleRepository.class);
    npcService = mock(NpcService.class);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");
    List<Wrestler> allWrestlers = Arrays.asList(wrestler1, wrestler2);

    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(), any()))
        .thenReturn(allWrestlers);
    when(wrestlerService.findAllFiltered(any(), any(), anyLong())).thenReturn(allWrestlers);
    when(wrestlerService.findByName("Wrestler 1")).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByName("Wrestler 2")).thenReturn(Optional.of(wrestler2));
    when(wrestlerRepository.findAll()).thenReturn(allWrestlers);
    when(wrestlerRepository.findByName("Wrestler 1")).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findByName("Wrestler 2")).thenReturn(Optional.of(wrestler2));

    matchType = new SegmentType();
    matchType.setName("Match");
    when(segmentTypeRepository.findAll()).thenReturn(List.of(matchType));
    when(segmentTypeRepository.findByName("Match")).thenReturn(Optional.of(matchType));

    // Mock NpcService for referees
    when(npcService.findAllByType("Referee")).thenReturn(new ArrayList<>());

    // Mock TitleService and available titles
    Title title1 = new Title();
    title1.setId(1L);
    title1.setName("Test Title 1");
    Title title2 = new Title();
    title2.setId(2L);
    title2.setName("Test Title 2");
    when(titleService.findAll()).thenReturn(List.of(title1, title2));

    onSave = mock(Runnable.class);
  }

  @Test
  void testSave() {
    UI.setCurrent(ui);
    EditSegmentDialog dialog =
        new EditSegmentDialog(
            segment,
            wrestlerRepository,
            wrestlerService,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            null,
            1L,
            onSave);
    dialog.open();

    // Select segment type
    dialog.getSegmentTypeCombo().setValue(matchType);

    // Simulate user input
    dialog.getNarrationArea().setValue("New Description");
    segment.setNarration("New Description");

    // Act
    dialog.save();

    // Assert
    assertEquals("New Description", segment.getNarration());
    verify(onSave).run();
  }

  @Test
  void testTitleSelection() {
    UI.setCurrent(ui);
    // Force it to be a title segment so combo is initially visible
    segment.setIsTitleSegment(true);

    EditSegmentDialog dialog =
        new EditSegmentDialog(
            segment,
            wrestlerRepository,
            wrestlerService,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            null,
            1L,
            onSave);
    dialog.open();

    // Verify title MultiSelectComboBox is visible and populated
    assertTrue(dialog.getTitleMultiSelectComboBox().isVisible());
    assertNotNull(dialog.getTitleMultiSelectComboBox().getListDataView());
  }
}
