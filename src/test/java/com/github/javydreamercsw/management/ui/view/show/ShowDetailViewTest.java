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
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.export.ShowExportService;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonAwardsService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.NarrationParserService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowContextFacade;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.wrestler.AbilityReminderTextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStateHistoryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
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
  @Mock private ExpansionService expansionService;
  @Mock private ShowPlanningAiService showPlanningAiService;
  @Mock private TeamService teamService;
  @Mock private FeudScriptService feudScriptService;

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

      ShowDetailView showDetailView = buildView(mock(SecurityUtils.class));
      Map<Integer, List<Wrestler>> teamMap = new LinkedHashMap<>();
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

      ShowDetailView showDetailView = buildView(mock(SecurityUtils.class));
      BeforeEvent beforeEvent = Mockito.mock(BeforeEvent.class);
      Mockito.when(beforeEvent.getLocation()).thenReturn(new Location(""));
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
    Mockito.when(event.getLocation()).thenReturn(new Location(""));
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
    Mockito.when(event.getLocation()).thenReturn(new Location(""));
    view.setParameter(event, 1L);

    Button adjudicate = (Button) ReflectionTestUtils.getField(view, "adjudicateButton");
    Button addSegment = (Button) ReflectionTestUtils.getField(view, "addSegmentButton");
    assertThat(adjudicate.isVisible()).isTrue();
    assertThat(addSegment.isVisible()).isTrue();
  }

  @Test
  void deleteSegment_arcLinked_dialogMentionsArcName() {
    Segment segment = new Segment();
    segment.setId(77L);

    FeudScript script = new FeudScript();
    script.setName("The Bloodline Saga");

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setScript(script);
    beat.setBeatOrder(2);
    beat.setActualSegment(segment);

    Mockito.when(feudScriptService.findBeatForSegment(segment)).thenReturn(Optional.of(beat));

    ShowDetailView view = buildView(mock(SecurityUtils.class));
    ReflectionTestUtils.invokeMethod(view, "deleteSegment", segment);

    Dialog dialog = LocatorJ._get(Dialog.class);
    String dialogText =
        dialog
            .getChildren()
            .filter(c -> c instanceof Paragraph)
            .map(c -> ((Paragraph) c).getText())
            .collect(Collectors.joining(" "));
    assertThat(dialogText).contains("The Bloodline Saga");
    assertThat(dialogText).contains("beat #2");
  }

  @Test
  void deleteSegment_notArcLinked_genericConfirmDialog() {
    Segment segment = new Segment();
    segment.setId(88L);

    Mockito.when(feudScriptService.findBeatForSegment(segment)).thenReturn(Optional.empty());

    ShowDetailView view = buildView(mock(SecurityUtils.class));
    ReflectionTestUtils.invokeMethod(view, "deleteSegment", segment);

    Dialog dialog = LocatorJ._get(Dialog.class);
    String dialogText =
        dialog
            .getChildren()
            .filter(c -> c instanceof Paragraph)
            .map(c -> ((Paragraph) c).getText())
            .collect(Collectors.joining(" "));
    assertThat(dialogText).doesNotContain("Story Arc");
    assertThat(dialogText).contains("Are you sure");
  }

  private ShowDetailView buildView(final SecurityUtils su) {
    ShowFacade showFacade =
        new ShowFacade(
            showService,
            segmentService,
            segmentTypeService,
            segmentRuleService,
            segmentNarrationServiceFactory,
            narrationParserService,
            npcService,
            mock(DramaEventService.class),
            feudScriptService);
    ShowContextFacade showContextFacade =
        new ShowContextFacade(
            showTypeService,
            seasonService,
            mock(SeasonAwardsService.class),
            showTemplateService,
            showPlanningService,
            showPlanningAiService,
            arenaService);
    WrestlerFacade wrestlerFacade =
        new WrestlerFacade(
            wrestlerService,
            wrestlerStatsService,
            relationshipService,
            teamService,
            mock(InjuryService.class),
            mock(TitleService.class),
            mock(WrestlerStateHistoryService.class),
            mock(AbilityReminderTextService.class));
    ViewContext viewContext =
        new ViewContext(
            notificationService,
            su,
            universeContextService,
            expansionService,
            mock(GameSettingService.class));
    return new ShowDetailView(
        showFacade,
        showContextFacade,
        wrestlerFacade,
        viewContext,
        segmentRepository,
        titleService,
        rivalryService,
        segmentNarrationController,
        showController,
        matchFulfillmentRepository,
        universeRepository,
        commentaryTeamRepository,
        ringsideActionService,
        mock(ShowExportService.class),
        mock(LeagueRepository.class));
  }

  @Test
  void segmentGrid_mergedTypeDateColumnAndArcBadge() {
    ShowType showType = new ShowType();
    showType.setName("Test");
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setType(showType);

    Segment withArc = new Segment();
    withArc.setId(10L);
    withArc.setSegmentType(new SegmentType());
    withArc.setSegmentDate(Instant.parse("2026-09-01T00:00:00Z"));

    Segment plain = new Segment();
    plain.setId(11L);
    plain.setSegmentType(new SegmentType());
    plain.setSegmentDate(Instant.parse("2026-09-02T00:00:00Z"));

    FeudScript script = new FeudScript();
    script.setName("The Bloodline Saga");
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setScript(script);
    beat.setBeatOrder(1);

    Mockito.when(showService.getShowById(any())).thenReturn(Optional.of(show));
    Mockito.when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
        .thenReturn(List.of(withArc, plain));
    Mockito.when(segmentRepository.findByShow(any(Show.class))).thenReturn(List.of(withArc, plain));
    Mockito.when(feudScriptService.findBeatForSegment(withArc)).thenReturn(Optional.of(beat));
    Mockito.when(feudScriptService.findBeatForSegment(plain)).thenReturn(Optional.empty());

    ShowDetailView view = buildView(mock(SecurityUtils.class));
    BeforeEvent event = Mockito.mock(BeforeEvent.class);
    Mockito.when(event.getLocation()).thenReturn(new Location(""));
    view.setParameter(event, 1L);

    // Component-column renderers run at row-render time, not on data fetch, so
    // drive the renderer directly: find the Arc column and render both rows.
    Grid<Segment> grid = LocatorJ._get(view, Grid.class, spec -> spec.withId("segments-grid"));
    // Vaadin 25 has no getHeaderText(); find the Arc column by probing each
    // component renderer for the feudScriptService call (exactly one column does).
    ComponentRenderer<?, Segment> arcRenderer = null;
    for (var column : grid.getColumns()) {
      if (column.getRenderer() instanceof ComponentRenderer<?, ?> cr) {
        var probe = (ComponentRenderer<Component, Segment>) cr;
        Component test = probe.createComponent(withArc);
        if (test != null && test.getElement().getText().contains("The Bloodline Saga")) {
          arcRenderer = (ComponentRenderer<?, Segment>) column.getRenderer();
          break;
        }
      }
    }
    Assertions.assertThat(arcRenderer).as("Arc column").isNotNull();
    Component arcBadge = arcRenderer.createComponent(withArc);
    Component plainCell = arcRenderer.createComponent(plain);
    Assertions.assertThat(arcBadge.getElement().getText()).contains("The Bloodline Saga");
    // Plain segment renders an empty placeholder span, not a badge.
    Assertions.assertThat(plainCell.getElement().getText()).isEmpty();
    Mockito.verify(feudScriptService, Mockito.atLeastOnce()).findBeatForSegment(withArc);
    Mockito.verify(feudScriptService, Mockito.atLeastOnce()).findBeatForSegment(plain);
  }

  @Test
  void segmentGrid_stakesCellSkipsEmptyRuleAndTitleEntries() {
    ShowType showType = new ShowType();
    showType.setName("Test");
    Show show = new Show();
    show.setId(1L);
    show.setName("Stakes Show");
    show.setType(showType);

    Segment bare = new Segment();
    bare.setId(12L);
    bare.setSegmentType(new SegmentType());
    bare.setSegmentDate(Instant.now());
    bare.setSegmentRules(new HashSet<>());

    Mockito.when(showService.getShowById(any())).thenReturn(Optional.of(show));
    Mockito.when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
        .thenReturn(List.of(bare));
    Mockito.when(segmentRepository.findByShow(any(Show.class))).thenReturn(List.of(bare));
    Mockito.when(feudScriptService.findBeatForSegment(any())).thenReturn(Optional.empty());

    ShowDetailView view = buildView(mock(SecurityUtils.class));
    BeforeEvent event = Mockito.mock(BeforeEvent.class);
    Mockito.when(event.getLocation()).thenReturn(new Location(""));
    view.setParameter(event, 1L);

    // The bare segment renders without rules or titles; no exception and grid populated.
    var grid = LocatorJ._get(view, Grid.class);
    grid.getDataProvider().fetch(new Query<>());
    Assertions.assertThat(grid.getGenericDataView().getItems().count()).isEqualTo(1);
  }

  /** Builds a fully-loaded segment exercising every grid column's branches. */
  private Segment richSegment(
      final long id,
      final String typeName,
      final String ruleName,
      final Title title,
      final Integer rating,
      final String summary,
      final String narration) {
    Segment segment = new Segment();
    segment.setId(id);
    SegmentType type = new SegmentType();
    type.setName(typeName);
    segment.setSegmentType(type);
    segment.setSegmentDate(Instant.parse("2026-09-01T00:00:00Z"));
    if (ruleName != null) {
      SegmentRule rule = new SegmentRule();
      rule.setName(ruleName);
      segment.setSegmentRules(new HashSet<>(Set.of(rule)));
    }
    if (title != null) {
      segment.setIsTitleSegment(true);
      segment.setTitles(Set.of(title));
    }
    segment.setSegmentRating(rating);
    segment.setSummary(summary);
    segment.setNarration(narration);
    return segment;
  }

  /**
   * Renders one segment through every component column and returns the text of each produced cell
   * (value-provider columns are not included).
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<String> renderSegmentCells(final ShowDetailView view, final Segment segment) {
    Grid<Segment> grid = LocatorJ._get(view, Grid.class, spec -> spec.withId("segments-grid"));
    List<String> texts = new ArrayList<>();
    for (var column : grid.getColumns()) {
      if (column.getRenderer() instanceof ComponentRenderer<?, ?> cr) {
        Component cell = ((ComponentRenderer) cr).createComponent(segment);
        if (cell != null) {
          texts.add(flattenCellText(cell));
        }
      }
    }
    return texts;
  }

  /** Recursive text collection over a component subtree. */
  private String flattenCellText(final Component root) {
    StringBuilder sb = new StringBuilder();
    appendCellText(root, sb);
    return sb.toString();
  }

  private void appendCellText(final Component component, final StringBuilder sb) {
    sb.append(component.getElement().getText()).append(' ');
    component.getChildren().forEach(child -> appendCellText(child, sb));
  }

  @Test
  void segmentGrid_allCellColumnsRenderLoadedSegment() {
    ShowType showType = new ShowType();
    showType.setName("Test");
    Show show = new Show();
    show.setId(1L);
    show.setName("Rich Show");
    show.setType(showType);

    Title title = new Title();
    title.setId(3L);
    title.setName("World Title");

    Segment rich =
        richSegment(
            20L, "Singles Match", "No DQ", title, 85, "Big match summary", "Crowd goes wild");
    Segment unrated = richSegment(21L, "Promo", null, null, null, null, null);

    Mockito.when(showService.getShowById(any())).thenReturn(Optional.of(show));
    Mockito.when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
        .thenReturn(List.of(rich, unrated));
    Mockito.when(segmentRepository.findByShow(any(Show.class))).thenReturn(List.of(rich, unrated));
    Mockito.when(feudScriptService.findBeatForSegment(any())).thenReturn(Optional.empty());
    Mockito.when(titleService.getCurrentChampionNamesByTitleIds(any()))
        .thenReturn(Map.of(3L, "Champ"));

    ShowDetailView view = buildView(mock(SecurityUtils.class));
    BeforeEvent event = Mockito.mock(BeforeEvent.class);
    Mockito.when(event.getLocation()).thenReturn(new Location(""));
    view.setParameter(event, 1L);

    List<String> richCells = renderSegmentCells(view, rich);
    String richAll = String.join(" | ", richCells);
    // Merged Segment column: type name plus formatted date (local-zone render
    // of the UTC instant can fall on Aug 31).
    Assertions.assertThat(richAll).contains("Singles Match").contains(", 2026");
    // Score column: 85/100 renders as 4.5 stars ("85/100" lives in the tooltip).
    Assertions.assertThat(richAll).contains("★★★★½");
    // Stakes column: rule and title names, with the champion from the service.
    Assertions.assertThat(richAll).contains("No DQ").contains("World Title");
    // Summary column: summary text plus narration underneath.
    Assertions.assertThat(richAll).contains("Big match summary").contains("Crowd goes wild");

    // Unrated segment: Score renders a dash; stakes fall back to the em-dash.
    List<String> bareCells = renderSegmentCells(view, unrated);
    String bareAll = String.join(" | ", bareCells);
    Assertions.assertThat(bareAll).contains("-");
    Assertions.assertThat(bareAll).contains("Promo");
  }

  @Test
  void viewerFeed_rendersMainEventTitleAndContenderBadges() {
    ShowType showType = new ShowType();
    showType.setName("Test");
    Show show = new Show();
    show.setId(1L);
    show.setName("Viewer Show");
    show.setType(showType);

    Title title = new Title();
    title.setId(4L);
    title.setName("Tag Titles");

    // Promo first (so the later match is the main event), then the title match.
    Segment promo = richSegment(30L, "Promo", null, null, null, "Promo summary", null);
    Segment titleMatch = richSegment(31L, "Singles Match", null, title, 70, "Title summary", null);
    titleMatch.setContenderMatch(true);

    Mockito.when(showService.getShowById(any())).thenReturn(Optional.of(show));
    Mockito.when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
        .thenReturn(List.of(promo, titleMatch));
    Mockito.when(segmentRepository.findByShow(any(Show.class)))
        .thenReturn(List.of(promo, titleMatch));
    Mockito.when(feudScriptService.findBeatForSegment(any())).thenReturn(Optional.empty());
    Mockito.when(titleService.getCurrentChampionNamesByTitleIds(any()))
        .thenReturn(Map.of(4L, "Champ Two"));

    SecurityUtils su = mock(SecurityUtils.class);
    Mockito.when(su.isViewer()).thenReturn(true);
    ShowDetailView view = buildView(su);
    BeforeEvent event = Mockito.mock(BeforeEvent.class);
    Mockito.when(event.getLocation()).thenReturn(new Location(""));
    view.setParameter(event, 1L);

    // The viewer feed replaces the editing grid.
    Component feed = walkForId(view, "viewer-segment-feed");
    String feedText = flattenCellText(feed);
    Assertions.assertThat(feedText).contains("★ Main Event");
    Assertions.assertThat(feedText).contains("Tag Titles").contains("Champ Two");
    Assertions.assertThat(feedText).contains("#1 Contender Match");
    Assertions.assertThat(feedText).contains("Promo summary").contains("Title summary");
  }

  /** Tree-walks for a component by id (the feed may sit inside an INVIS container). */
  private Component walkForId(final Component root, final String id) {
    if (id.equals(root.getId().orElse(""))) {
      return root;
    }
    for (Component child : root.getChildren().toList()) {
      Component found = walkForId(child, id);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  @Test
  void segmentGrid_contenderStarAndQualityBadgeRender() {
    ShowType showType = new ShowType();
    showType.setName("Test");
    Show show = new Show();
    show.setId(1L);
    show.setName("Star Show");
    show.setType(showType);
    show.setQualityScore(4.5);

    // A contender match produced by a feud script: covers the star in the
    // merged Segment column and the full Arc badge (name + tooltip).
    Segment contender = richSegment(40L, "Singles Match", null, null, 100, "S", null);
    contender.setContenderMatch(true);

    FeudScript script = new FeudScript();
    script.setName("Rise of Dom");
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setScript(script);
    beat.setBeatOrder(3);

    Mockito.when(showService.getShowById(any())).thenReturn(Optional.of(show));
    Mockito.when(segmentRepository.findByShowOrderBySegmentOrderAsc(any(Show.class)))
        .thenReturn(List.of(contender));
    Mockito.when(segmentRepository.findByShow(any(Show.class))).thenReturn(List.of(contender));
    Mockito.when(feudScriptService.findBeatForSegment(contender)).thenReturn(Optional.of(beat));

    ShowDetailView view = buildView(mock(SecurityUtils.class));
    BeforeEvent event = Mockito.mock(BeforeEvent.class);
    Mockito.when(event.getLocation()).thenReturn(new Location(""));
    view.setParameter(event, 1L);

    List<String> cells = renderSegmentCells(view, contender);
    String all = String.join(" | ", cells);
    // Contender star rides in the Segment column with its tooltip text.
    Assertions.assertThat(all).contains("⭐");
    // Arc badge carries the script name; beat order lives in the tooltip.
    Assertions.assertThat(all).contains("Rise of Dom");
    // A 100 rating renders as a full 5 stars.
    Assertions.assertThat(all).contains("★★★★★");
  }
}
