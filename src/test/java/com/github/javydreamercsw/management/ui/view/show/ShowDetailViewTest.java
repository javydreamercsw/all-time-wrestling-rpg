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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationConfig;
import com.github.javydreamercsw.base.ai.SegmentNarrationController;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.controller.show.ShowController;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.export.ShowExportService;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.NarrationParserService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.BeforeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class ShowDetailViewTest extends AbstractViewTest {

  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;
  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NpcService npcService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private TitleService titleService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private RivalryService rivalryService;
  @Mock private ShowPlanningService showPlanningService;
  @Mock private SegmentNarrationConfig segmentNarrationConfig;
  @Mock private SegmentNarrationServiceFactory segmentNarrationServiceFactory;
  @Mock private Environment env;
  @Mock private SegmentNarrationController segmentNarrationController;
  @Mock private ShowController showController;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private ArenaService arenaService;
  @Mock private NotificationService notificationService;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private SecurityUtils securityUtils;
  @Mock private NarrationParserService narrationParserService;

  @BeforeEach
  public void setUp() {
    // mocks initialized by AbstractViewTest.setupKaribu()
  }

  @Test
  void testEditSegmentResetsAdjudicationStatus() {
    try (MockedStatic<Notification> mocked = Mockito.mockStatic(Notification.class)) {
      mocked
          .when(() -> Notification.show(anyString(), anyInt(), any(Notification.Position.class)))
          .thenReturn(mock(Notification.class));

      ShowType showType = new ShowType();
      showType.setName("Test Show Type");
      showType.setDescription("Test Description");

      Show show = new Show();
      show.setName("Test Show");
      show.setDescription("Test Description");
      show.setType(showType);

      SegmentType segmentType = new SegmentType();
      segmentType.setName("Test Segment Type");

      Segment segment = new Segment();
      segment.setId(10L);
      segment.setShow(show);
      segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
      segment.setSegmentType(segmentType);
      when(segmentService.updateSegment(any(Segment.class))).thenReturn(segment);

      Wrestler wrestler1 = new Wrestler();
      wrestler1.setId(1L);
      wrestler1.setName("Wrestler 1");

      Wrestler wrestler2 = new Wrestler();
      wrestler2.setId(2L);
      wrestler2.setName("Wrestler 2");

      Set<Wrestler> wrestlers = new HashSet<>(Arrays.asList(wrestler1, wrestler2));

      ShowExportService exportService = mock(ShowExportService.class);
      com.github.javydreamercsw.management.domain.league.LeagueRepository leagueRepository =
          mock(com.github.javydreamercsw.management.domain.league.LeagueRepository.class);

      ShowDetailView showDetailView =
          new ShowDetailView(
              showService,
              segmentService,
              segmentRepository,
              segmentTypeService,
              segmentRuleService,
              npcService,
              wrestlerService,
              wrestlerStatsService,
              titleService,
              showTypeService,
              seasonService,
              showTemplateService,
              rivalryService,
              showPlanningService,
              segmentNarrationServiceFactory,
              segmentNarrationController,
              showController,
              matchFulfillmentRepository,
              universeRepository,
              universeContextService,
              commentaryTeamRepository,
              ringsideActionService,
              arenaService,
              relationshipService,
              notificationService,
              exportService,
              leagueRepository,
              mock(SecurityUtils.class),
              mock(NarrationParserService.class));
      java.util.Map<Integer, java.util.List<Wrestler>> teamMap = new java.util.LinkedHashMap<>();
      teamMap.put(1, List.of(wrestler1));
      teamMap.put(2, List.of(wrestler2));
      ReflectionTestUtils.invokeMethod(
          showDetailView,
          "validateAndSaveSegment",
          show,
          segmentType,
          teamMap,
          Collections.emptySet(),
          Collections.emptySet(),
          segment);

      assertEquals(AdjudicationStatus.PENDING, segment.getAdjudicationStatus());
    }
  }

  @Test
  void testSegmentReordering() {
    try (MockedStatic<Notification> mocked = Mockito.mockStatic(Notification.class)) {
      mocked
          .when(() -> Notification.show(anyString(), anyInt(), any(Notification.Position.class)))
          .thenReturn(mock(Notification.class));

      ShowType showType = new ShowType();
      showType.setName("Test Show Type");
      showType.setDescription("Test Description");

      Show show = new Show();
      show.setId(1L);
      show.setName("Test Show");
      show.setDescription("Test Description");
      show.setType(showType);

      SegmentType segmentType = new SegmentType();
      segmentType.setName("Test Segment Type");

      Segment segment1 = new Segment();
      segment1.setId(10L);
      segment1.setShow(show);
      segment1.setSegmentOrder(1);
      segment1.setSegmentType(segmentType);
      when(segmentRepository.save(any(Segment.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(segmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

      Segment segment2 = new Segment();
      segment2.setId(11L);
      segment2.setShow(show);
      segment2.setSegmentOrder(2);
      segment2.setSegmentType(segmentType);

      List<Segment> initialSegments = new ArrayList<>(Arrays.asList(segment1, segment2));

      when(showService.getShowById(any())).thenReturn(Optional.of(show));
      when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
          .thenReturn(initialSegments);
      when(segmentRepository.findByShow(any(Show.class))).thenReturn(initialSegments);

      ShowExportService exportService = mock(ShowExportService.class);
      com.github.javydreamercsw.management.domain.league.LeagueRepository leagueRepository =
          mock(com.github.javydreamercsw.management.domain.league.LeagueRepository.class);

      ShowDetailView showDetailView =
          new ShowDetailView(
              showService,
              segmentService,
              segmentRepository,
              segmentTypeService,
              segmentRuleService,
              npcService,
              wrestlerService,
              wrestlerStatsService,
              titleService,
              showTypeService,
              seasonService,
              showTemplateService,
              rivalryService,
              showPlanningService,
              segmentNarrationServiceFactory,
              segmentNarrationController,
              showController,
              matchFulfillmentRepository,
              universeRepository,
              universeContextService,
              commentaryTeamRepository,
              ringsideActionService,
              arenaService,
              relationshipService,
              notificationService,
              exportService,
              leagueRepository,
              mock(SecurityUtils.class),
              mock(NarrationParserService.class));
      BeforeEvent beforeEvent = Mockito.mock(BeforeEvent.class);
      Mockito.when(beforeEvent.getLocation()).thenReturn(new com.vaadin.flow.router.Location(""));
      showDetailView.setParameter(beforeEvent, show.getId());

      ReflectionTestUtils.setField(showDetailView, "currentShow", show);
      ReflectionTestUtils.setField(showDetailView, "segmentsGrid", mock(Grid.class));
      ReflectionTestUtils.setField(
          showDetailView, "segmentOrder", new ArrayList<>(Arrays.asList(segment1, segment2)));

      // Reordering is instant — no DB call, just rearranges in-memory list
      showDetailView.moveSegmentInMemory(segment1, 1);

      @SuppressWarnings("unchecked")
      List<Segment> order =
          (List<Segment>) ReflectionTestUtils.getField(showDetailView, "segmentOrder");
      assertSame(segment2, order.get(0), "segment2 should now be first");
      assertSame(segment1, order.get(1), "segment1 should now be second");

      // Persisting writes the new order to the DB
      ReflectionTestUtils.invokeMethod(showDetailView, "persistSegmentOrder");

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<Segment>> captor = ArgumentCaptor.forClass(List.class);
      Mockito.verify(segmentRepository, Mockito.timeout(2000)).saveAll(captor.capture());
      List<Segment> saved = captor.getValue();
      assertSame(segment2, saved.get(0));
      assertSame(segment1, saved.get(1));
      assertEquals(1, saved.get(0).getSegmentOrder());
      assertEquals(2, saved.get(1).getSegmentOrder());
    }
  }

  @Test
  void viewerRole_adjudicateAndAddSegmentButtonsHidden() {
    when(securityUtils.isViewer()).thenReturn(true);

    ShowType showType = new ShowType();
    showType.setName("Test");
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setType(showType);

    when(showService.getShowById(any())).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(any(Show.class))).thenReturn(Collections.emptyList());
    when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
        .thenReturn(Collections.emptyList());

    ShowDetailView view = buildView(securityUtils);
    BeforeEvent event = Mockito.mock(BeforeEvent.class);
    Mockito.when(event.getLocation()).thenReturn(new com.vaadin.flow.router.Location(""));
    view.setParameter(event, 1L);

    assertThat(ReflectionTestUtils.getField(view, "adjudicateButton"))
        .as("adjudicateButton must not be created for VIEWER")
        .isNull();
    assertThat(ReflectionTestUtils.getField(view, "addSegmentButton"))
        .as("addSegmentButton must not be created for VIEWER")
        .isNull();
  }

  @Test
  void nonViewerRole_adjudicateAndAddSegmentButtonsVisible() {
    when(securityUtils.isViewer()).thenReturn(false);

    ShowType showType = new ShowType();
    showType.setName("Test");
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setType(showType);

    when(showService.getShowById(any())).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(any(Show.class))).thenReturn(Collections.emptyList());
    when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
        .thenReturn(Collections.emptyList());

    ShowDetailView view = buildView(securityUtils);
    BeforeEvent event = Mockito.mock(BeforeEvent.class);
    Mockito.when(event.getLocation()).thenReturn(new com.vaadin.flow.router.Location(""));
    view.setParameter(event, 1L);

    Button adjudicate = (Button) ReflectionTestUtils.getField(view, "adjudicateButton");
    Button addSegment = (Button) ReflectionTestUtils.getField(view, "addSegmentButton");
    assertThat(adjudicate.isVisible()).isTrue();
    assertThat(addSegment.isVisible()).isTrue();
  }

  private ShowDetailView buildView(final SecurityUtils su) {
    ShowExportService exportService = mock(ShowExportService.class);
    com.github.javydreamercsw.management.domain.league.LeagueRepository leagueRepository =
        mock(com.github.javydreamercsw.management.domain.league.LeagueRepository.class);
    return new ShowDetailView(
        showService,
        segmentService,
        segmentRepository,
        segmentTypeService,
        segmentRuleService,
        npcService,
        wrestlerService,
        wrestlerStatsService,
        titleService,
        showTypeService,
        seasonService,
        showTemplateService,
        rivalryService,
        showPlanningService,
        segmentNarrationServiceFactory,
        segmentNarrationController,
        showController,
        matchFulfillmentRepository,
        universeRepository,
        universeContextService,
        commentaryTeamRepository,
        ringsideActionService,
        arenaService,
        relationshipService,
        notificationService,
        exportService,
        leagueRepository,
        su,
        narrationParserService);
  }
}
