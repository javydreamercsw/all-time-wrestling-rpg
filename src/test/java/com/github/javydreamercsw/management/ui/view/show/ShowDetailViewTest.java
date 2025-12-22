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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.SegmentNarrationConfig;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class ShowDetailViewTest {

  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;
  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NpcService npcService;
  @Mock private WrestlerService wrestlerService;
  @Mock private TitleService titleService;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private RivalryService rivalryService;
  @Mock private LocalAIStatusService localAIStatusService;
  @Mock private SegmentNarrationConfig segmentNarrationConfig;
  @Mock private SegmentNarrationServiceFactory segmentNarrationServiceFactory;
  @Mock private Environment env;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
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
      when(segmentRepository.save(any(Segment.class))).thenReturn(segment);

      Wrestler wrestler1 = new Wrestler();
      wrestler1.setId(1L);
      wrestler1.setName("Wrestler 1");

      Wrestler wrestler2 = new Wrestler();
      wrestler2.setId(2L);
      wrestler2.setName("Wrestler 2");

      Set<Wrestler> wrestlers = new HashSet<>(Arrays.asList(wrestler1, wrestler2));

      ShowDetailView showDetailView =
          new ShowDetailView(
              showService,
              segmentService,
              segmentRepository,
              segmentTypeRepository,
              wrestlerRepository,
              npcService,
              wrestlerService,
              titleService,
              segmentRuleRepository,
              showTypeService,
              seasonService,
              showTemplateService,
              rivalryService,
              localAIStatusService,
              segmentNarrationConfig,
              segmentNarrationServiceFactory,
              mock(WebClient.Builder.class),
              env);

      ReflectionTestUtils.invokeMethod(
          showDetailView,
          "validateAndSaveSegment",
          show,
          segmentType,
          wrestlers,
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

      ShowDetailView showDetailView =
          new ShowDetailView(
              showService,
              segmentService,
              segmentRepository,
              segmentTypeRepository,
              wrestlerRepository,
              npcService,
              wrestlerService,
              titleService,
              segmentRuleRepository,
              showTypeService,
              seasonService,
              showTemplateService,
              rivalryService,
              localAIStatusService,
              segmentNarrationConfig,
              segmentNarrationServiceFactory,
              mock(WebClient.Builder.class),
              env);

      BeforeEvent beforeEvent = Mockito.mock(BeforeEvent.class);
      Mockito.when(beforeEvent.getLocation()).thenReturn(new Location(""));
      showDetailView.setParameter(beforeEvent, show.getId());

      ReflectionTestUtils.setField(showDetailView, "currentShow", show);
      ReflectionTestUtils.setField(showDetailView, "segmentsGrid", mock(Grid.class));

      showDetailView.moveSegment(segment1, 1);

      assertEquals(2, segment1.getSegmentOrder());
      assertEquals(1, segment2.getSegmentOrder());
    }
  }
}
