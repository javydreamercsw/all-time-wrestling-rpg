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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationController;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NarrationDialogTest {

  private Segment segment;
  private Wrestler wrestler;
  private NarrationDialog narrationDialog;

  @Mock private NpcService npcService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;
  @Mock private RivalryService rivalryService;
  @Mock private SegmentNarrationController segmentNarrationController;
  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private NotificationService notificationService;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private UniverseContextService universeContextService;
  @Mock private MultiSelectComboBox<WrestlerDTO> mockWrestlersCombo;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    segment = new Segment();
    segment.setId(1L);
    SegmentType type = new SegmentType();
    type.setName("Match");
    segment.setSegmentType(type);

    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Roman Reigns");
    wrestler.setGender(Gender.MALE);

    when(segmentService.findByIdWithDetails(anyLong())).thenReturn(Optional.of(segment));
    when(npcService.findAllByType("Referee")).thenReturn(new ArrayList<>());
    when(npcService.findAllByType("Commissioner")).thenReturn(new ArrayList<>());
    when(npcService.findAllByType("Commentator")).thenReturn(new ArrayList<>());
    when(npcService.findAllByType("Announcer")).thenReturn(new ArrayList<>());
    when(npcService.findAll()).thenReturn(new ArrayList<>());
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);

    narrationDialog =
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
            notificationService,
            wrestlerStatsService);
  }

  @Test
  void testDialogInitialization() {
    assertNotNull(narrationDialog);

    // Create mocks for the UI components that teamsLayout would contain
    VerticalLayout mockTeamsLayout = mock(VerticalLayout.class);
    HorizontalLayout mockTeamSelector = mock(HorizontalLayout.class);

    // Configure the mocked MultiSelectComboBox to return our wrestler
    WrestlerDTO wrestlerDTO = new WrestlerDTO(wrestler);

    when(mockWrestlersCombo.getValue()).thenReturn(new HashSet<>(List.of(wrestlerDTO)));

    // Configure the mocked HorizontalLayout to contain the MultiSelectComboBox
    when(mockTeamSelector.getComponentAt(0)).thenReturn(mockWrestlersCombo);

    // Configure the mocked teamsLayout to contain the HorizontalLayout
    when(mockTeamsLayout.getComponentCount()).thenReturn(1);
    when(mockTeamsLayout.getComponentAt(0)).thenReturn(mockTeamSelector);

    // Use reflection to set the private final teamsLayout field in narrationDialog
    try {
      Field teamsLayoutField = NarrationDialog.class.getDeclaredField("teamsLayout");
      teamsLayoutField.setAccessible(true);
      teamsLayoutField.set(narrationDialog, mockTeamsLayout);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail("Failed to inject mock teamsLayout into NarrationDialog", e);
    }
  }

  @Test
  void testBuildSegmentContext_withPreviousSegments() {
    // Given
    Segment prevSegment = new Segment();
    prevSegment.setId(99L);
    prevSegment.setSegmentOrder(0);
    prevSegment.setNarration("Segment 1 Narration");
    prevSegment.setSummary("Segment 1 Summary");
    SegmentType type = new SegmentType();
    type.setName("Match");
    prevSegment.setSegmentType(type);

    Show show = new Show();
    segment.setShow(show);
    segment.setSegmentOrder(1);
    prevSegment.setShow(show);

    when(showService.getSegments(show)).thenReturn(List.of(prevSegment, segment));
    when(segmentService.findByIdWithDetails(99L)).thenReturn(Optional.of(prevSegment));

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
    title.getCurrentChampions().add(champion);
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

  @Test
  void testBuildWrestlerContexts_withManager() {
    // Given
    // Mock the UI component via reflection
    VerticalLayout mockTeamsLayout = mock(VerticalLayout.class);
    HorizontalLayout mockTeamSelector = mock(HorizontalLayout.class);
    WrestlerDTO wrestlerDTO = new WrestlerDTO(wrestler);
    when(mockWrestlersCombo.getValue()).thenReturn(new HashSet<>(List.of(wrestlerDTO)));
    when(mockTeamSelector.getComponentAt(0)).thenReturn(mockWrestlersCombo);
    when(mockTeamsLayout.getComponentCount()).thenReturn(1);
    when(mockTeamsLayout.getComponentAt(0)).thenReturn(mockTeamSelector);

    try {
      Field teamsLayoutField = NarrationDialog.class.getDeclaredField("teamsLayout");
      teamsLayoutField.setAccessible(true);
      teamsLayoutField.set(narrationDialog, mockTeamsLayout);
    } catch (Exception e) {
      fail(e);
    }

    Npc manager = new Npc();
    manager.setName("Paul Heyman");
    when(wrestlerService.findByName(wrestler.getName())).thenReturn(Optional.of(wrestler));
    when(ringsideActionService.getBestSupporter(segment, wrestler)).thenReturn(manager);

    // When
    List<SegmentNarrationService.WrestlerContext> wrestlerContexts =
        narrationDialog.buildWrestlerContexts();

    // Then
    assertNotNull(wrestlerContexts);
    assertEquals(1, wrestlerContexts.size());
    SegmentNarrationService.WrestlerContext wrestlerContext = wrestlerContexts.get(0);
    assertEquals("Roman Reigns", wrestlerContext.getName());
    assertEquals("Paul Heyman", wrestlerContext.getManagerName());
  }

  @Test
  void testBuildWrestlerContexts_withRelationships() {
    // Given
    // Mock the UI component via reflection
    VerticalLayout mockTeamsLayout = mock(VerticalLayout.class);
    HorizontalLayout mockTeamSelector = mock(HorizontalLayout.class);
    WrestlerDTO wrestlerDTO = new WrestlerDTO(wrestler);
    when(mockWrestlersCombo.getValue()).thenReturn(new HashSet<>(List.of(wrestlerDTO)));
    when(mockTeamSelector.getComponentAt(0)).thenReturn(mockWrestlersCombo);
    when(mockTeamsLayout.getComponentCount()).thenReturn(1);
    when(mockTeamsLayout.getComponentAt(0)).thenReturn(mockTeamSelector);

    try {
      Field teamsLayoutField = NarrationDialog.class.getDeclaredField("teamsLayout");
      teamsLayoutField.setAccessible(true);
      teamsLayoutField.set(narrationDialog, mockTeamsLayout);
    } catch (Exception e) {
      fail(e);
    }

    Wrestler partner = new Wrestler();
    partner.setId(99L);
    partner.setName("Seth Rollins");

    com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship rel =
        new com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship();
    rel.setWrestler1(wrestler);
    rel.setWrestler2(partner);
    rel.setType(
        com.github.javydreamercsw.management.domain.relationship.RelationshipType.BEST_FRIEND);
    rel.setLevel(80);
    rel.setIsStoryline(true);

    when(wrestlerService.findById(wrestler.getId())).thenReturn(Optional.of(wrestler));
    when(relationshipService.getRelationshipsForWrestler(wrestler.getId()))
        .thenReturn(List.of(rel));

    // When
    List<SegmentNarrationService.WrestlerContext> wrestlerContexts =
        narrationDialog.buildWrestlerContexts();

    // Then
    assertNotNull(wrestlerContexts);
    assertFalse(wrestlerContexts.isEmpty());
    SegmentNarrationService.WrestlerContext wrestlerContext = wrestlerContexts.get(0);
    assertNotNull(wrestlerContext.getRelationships());
    assertEquals(1, wrestlerContext.getRelationships().size());
    String relText = wrestlerContext.getRelationships().get(0);
    assertTrue(relText.contains("Best Friend"));
    assertTrue(relText.contains("Seth Rollins"));
    assertTrue(relText.contains("80"));
    assertTrue(relText.contains("Storyline"));
  }
}
