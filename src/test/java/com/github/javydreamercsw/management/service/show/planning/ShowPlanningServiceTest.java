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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningDtoMapper;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowPlanningServiceTest {

  @MockitoBean private SegmentRepository segmentRepository;
  @MockitoBean private RivalryService rivalryService;
  @MockitoBean private TitleService titleService;
  @MockitoBean private ShowService showService;
  @MockitoBean private SegmentService segmentService;
  @MockitoBean private SegmentTypeRepository segmentTypeRepository;
  @MockitoBean private WrestlerService wrestlerService;
  @MockitoBean private FactionService factionService;

  @MockitoSpyBean
  private ShowPlanningDtoMapper showPlanningDtoMapper; // IDE shows no usages, but it is needed.

  @MockitoSpyBean private Clock clock;

  @MockitoBean private PromoBookingService promoBookingService;
  @Autowired private ShowPlanningService showPlanningService;

  @Test
  void getShowPlanningContext() {
    // Given
    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");
    when(show.getShowDate()).thenReturn(LocalDate.now());
    when(show.getId()).thenReturn(1L);

    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setName("Wrestler 1");
    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setName("Wrestler 2");

    SegmentType promoSegmentType = new SegmentType();
    promoSegmentType.setName("Promo");
    SegmentType matchSegmentType = new SegmentType();
    matchSegmentType.setName("One on One");

    Segment match = new Segment();
    match.setId(1L);
    match.setNarration("Test narration");
    Show segmentShow = new Show();
    segmentShow.setId(1L);
    segmentShow.setName("Segment Show");
    match.setShow(segmentShow);
    match.setSegmentType(matchSegmentType);
    SegmentParticipant p1 = new SegmentParticipant();
    p1.setWrestler(wrestler1);
    p1.setIsWinner(true);
    SegmentParticipant p2 = new SegmentParticipant();
    p2.setWrestler(wrestler2);
    match.setParticipants(Arrays.asList(p1, p2));

    Segment promo = new Segment();
    promo.setId(2L);
    promo.setNarration("Test promo narration");
    promo.setShow(segmentShow);
    promo.setSegmentType(promoSegmentType);
    SegmentParticipant p3 = new SegmentParticipant();
    p3.setWrestler(wrestler1);
    promo.setParticipants(Collections.singletonList(p3));

    when(segmentRepository.findBySegmentDateBetween(any(), any()))
        .thenReturn(Arrays.asList(match, promo));
    when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.of(promoSegmentType));
    when(promoBookingService.isPromoSegment(any(Segment.class)))
        .thenAnswer(
            invocation -> {
              Segment s = invocation.getArgument(0);
              return s.getSegmentType() != null && "Promo".equals(s.getSegmentType().getName());
            });

    when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.of(promoSegmentType));
    when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.of(promoSegmentType));
    when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.of(promoSegmentType));

    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(10);
    when(rivalryService.getActiveRivalriesBetween(any(), any()))
        .thenReturn(Collections.singletonList(rivalry));

    Title title = new Title();
    title.setId(1L);
    title.setName("Test Title");
    Wrestler champion = Wrestler.builder().build();
    champion.setName("Champion");
    title.getCurrentChampions().add(champion);
    when(titleService.getActiveTitles()).thenReturn(Collections.singletonList(title));
    Wrestler contender = Wrestler.builder().build();
    contender.setName("Contender");
    when(titleService.getEligibleChallengers(anyLong()))
        .thenReturn(Collections.singletonList(contender));

    when(wrestlerService.findAll()).thenReturn(Collections.emptyList());
    when(factionService.findAll()).thenReturn(Collections.emptyList());

    ShowType pleShowType = new ShowType();
    com.github.javydreamercsw.management.domain.show.template.ShowTemplate pleTemplate =
        mock(com.github.javydreamercsw.management.domain.show.template.ShowTemplate.class);
    when(pleTemplate.isPremiumLiveEvent()).thenReturn(true);
    pleTemplate.setShowType(pleShowType);

    Show ple = new Show();
    ple.setName("Test PLE");
    ple.setTemplate(pleTemplate);
    ple.setShowDate(LocalDate.now().plusMonths(1));
    ple.setDescription("Test PLE Description");
    when(showService.getUpcomingShows(anyInt())).thenReturn(Collections.singletonList(ple));

    when(segmentService.findById(anyLong()))
        .thenAnswer(
            invocation -> {
              Long id = invocation.getArgument(0);
              if (id == 1L) {
                Segment s = new Segment();
                s.setId(1L);
                s.setNarration("Test narration");
                s.setSummary("Test summary for match");
                return Optional.of(s);
              } else if (id == 2L) {
                Segment s = new Segment();
                s.setId(2L);
                s.setNarration("Test promo narration");
                s.setSummary("Test summary for promo");
                return Optional.of(s);
              }
              return Optional.empty();
            });
  }

  @Test
  void getShowPlanningContext_shouldCorrectlyHandleNextPle() {
    // Given
    LocalDate futureShowDate = LocalDate.now(clock).plusMonths(3);
    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Future Show");
    when(show.getShowDate()).thenReturn(futureShowDate);
    when(show.getId()).thenReturn(1L);
    ShowType currentShowType = new ShowType(); // Create a ShowType for the 'show' object
    currentShowType.setName("Regular Show Type");
    currentShowType.setExpectedMatches(5); // Set some default values
    currentShowType.setExpectedPromos(3); // Set some default values
    when(show.getType()).thenReturn(currentShowType); // Mock getType() to return this ShowType

    ShowType pleShowType = new ShowType();
    pleShowType.setName("Premium Live Event (PLE)");
    pleShowType.setExpectedMatches(7);
    pleShowType.setExpectedPromos(2);
    com.github.javydreamercsw.management.domain.show.template.ShowTemplate pleTemplate =
        new com.github.javydreamercsw.management.domain.show.template.ShowTemplate();
    pleTemplate.setShowType(pleShowType);

    Show upcomingPle = new Show();
    upcomingPle.setId(2L);
    upcomingPle.setName("Upcoming PLE");
    upcomingPle.setTemplate(pleTemplate);
    upcomingPle.setType(pleShowType);
    upcomingPle.setShowDate(futureShowDate.plusWeeks(2));
    upcomingPle.setDescription("Test PLE Description");

    when(showService.getUpcomingShows(eq(futureShowDate), anyInt()))
        .thenReturn(Collections.singletonList(upcomingPle));

    // Mock other dependencies to avoid NullPointerExceptions
    when(segmentRepository.findBySegmentDateBetween(any(), any()))
        .thenReturn(Collections.emptyList());
    when(rivalryService.getActiveRivalriesBetween(any(), any()))
        .thenReturn(Collections.emptyList());
    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(titleService.getActiveTitles()).thenReturn(Collections.emptyList());
    when(segmentService.findById(anyLong())).thenReturn(Optional.empty());
    when(wrestlerService.findAll()).thenReturn(Collections.emptyList());
    when(factionService.findAll()).thenReturn(Collections.emptyList());

    // Act
    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);

    // Assert
    assertNotNull(context);
    assertNotNull(context.getNextPle());
    assertEquals("Upcoming PLE", context.getNextPle().getPleName());
    assertEquals(
        upcomingPle.getShowDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
        context.getNextPle().getPleDate());
  }

  @Test
  void getShowPlanningContext_shouldUseShowDateForUpcomingShowsAndSegments() {
    // Given
    LocalDate futureShowDate = LocalDate.now(clock).plusMonths(3);
    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Future Show");
    when(show.getShowDate()).thenReturn(futureShowDate);
    when(show.getId()).thenReturn(1L);
    ShowType showType = new ShowType(); // Create a ShowType for the 'show' object
    showType.setName("Regular Show Type");
    showType.setExpectedMatches(5); // Set some default values
    showType.setExpectedPromos(2); // Set some default values
    when(show.getType()).thenReturn(showType); // Mock getType() to return this ShowType

    // Mock an upcoming PLE relative to futureShowDate
    ShowType pleShowType = new ShowType();
    pleShowType.setName("Premium Live Event (PLE)");
    pleShowType.setExpectedMatches(7);
    pleShowType.setExpectedPromos(2);
    com.github.javydreamercsw.management.domain.show.template.ShowTemplate pleTemplate =
        new com.github.javydreamercsw.management.domain.show.template.ShowTemplate();
    pleTemplate.setShowType(pleShowType);

    Show upcomingPle = mock(Show.class);
    when(upcomingPle.getId()).thenReturn(2L);
    when(upcomingPle.getName()).thenReturn("Upcoming PLE");
    when(upcomingPle.getTemplate()).thenReturn(pleTemplate);
    when(upcomingPle.getType()).thenReturn(pleShowType);
    when(upcomingPle.getShowDate()).thenReturn(futureShowDate.plusWeeks(2));
    when(upcomingPle.getDescription()).thenReturn("Test PLE Description");
    when(upcomingPle.isPremiumLiveEvent()).thenReturn(true);
    when(showService.getUpcomingShows(eq(futureShowDate), anyInt()))
        .thenReturn(Collections.singletonList(upcomingPle));

    // Mock segments within 30 days before futureShowDate
    Segment segment1 = new Segment();
    segment1.setId(1L);
    segment1.setSegmentDate(
        futureShowDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant());
    Segment segment2 = new Segment();
    segment2.setId(2L);
    segment2.setSegmentDate(
        futureShowDate.minusDays(20).atStartOfDay(ZoneId.systemDefault()).toInstant());

    Show segmentShow = new Show();
    segmentShow.setId(3L);
    segmentShow.setName("Segment Show");
    segmentShow.setShowDate(futureShowDate.minusDays(10));
    segment1.setShow(segmentShow);
    segment2.setShow(segmentShow);

    when(segmentRepository.findBySegmentDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(Arrays.asList(segment1, segment2));

    // Mock other dependencies to avoid NullPointerExceptions
    when(rivalryService.getActiveRivalriesBetween(any(), any()))
        .thenReturn(Collections.emptyList());
    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(titleService.getActiveTitles()).thenReturn(Collections.emptyList());
    when(segmentService.findById(anyLong())).thenReturn(Optional.empty());
    when(wrestlerService.findAll()).thenReturn(Collections.emptyList());
    when(factionService.findAll()).thenReturn(Collections.emptyList());

    // Act
    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);

    // Assert
    assertNotNull(context);

    // Verify next PLE
    assertNotNull(context.getNextPle());
    assertEquals("Upcoming PLE", context.getNextPle().getPleName());
    assertEquals(
        upcomingPle.getShowDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
        context.getNextPle().getPleDate());

    // Verify last month segments
    assertNotNull(context.getRecentSegments());
    assertEquals(2, context.getRecentSegments().size());
    assertTrue(context.getRecentSegments().stream().anyMatch(s -> s.getId().equals(1L)));
    assertTrue(context.getRecentSegments().stream().anyMatch(s -> s.getId().equals(2L)));

    // Verify that getUpcomingShows was called with the correct reference date
    verify(showService, times(1)).getUpcomingShows(eq(futureShowDate), anyInt());
  }

  @Test
  void getShowPlanningContext_shouldIncludeOnlyNumberOneContenders() {
    // Given
    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");
    when(show.getShowDate()).thenReturn(LocalDate.now());
    when(show.getId()).thenReturn(1L);
    ShowType showType = new ShowType(); // Create a ShowType for the 'show' object
    showType.setName("Regular Show Type");
    showType.setExpectedMatches(5); // Set some default values
    showType.setExpectedPromos(3); // Set some default values
    when(show.getType()).thenReturn(showType); // Mock getType() to return this ShowType

    Title title = new Title();
    title.setId(1L);
    title.setName("Test Title");
    Wrestler champion = Wrestler.builder().build();
    champion.setName("Champion");

    title.getCurrentChampions().add(champion);

    Wrestler numberOneContender = Wrestler.builder().build();
    numberOneContender.setName("Number One Contender");
    title.setContender(Collections.singletonList(numberOneContender));

    when(titleService.getActiveTitles()).thenReturn(Collections.singletonList(title));
    when(titleService.getEligibleChallengers(anyLong()))
        .thenReturn(Collections.singletonList(numberOneContender));
    when(wrestlerService.findAll()).thenReturn(Collections.emptyList());
    when(factionService.findAll()).thenReturn(Collections.emptyList());

    // Act
    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);

    // Assert
    assertNotNull(context);
    assertFalse(context.getChampionships().isEmpty());
    var championship = context.getChampionships().get(0);
    assertEquals("Test Title", championship.getChampionshipName());
    assertEquals("Champion", championship.getChampionName());
    assertEquals("Number One Contender", championship.getContenderName());
  }

  @Test
  void getShowPlanningContext_shouldIncludeWrestlerHeats() {
    // Given
    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");
    when(show.getShowDate()).thenReturn(LocalDate.now());
    when(show.getId()).thenReturn(1L);
    ShowType showType = new ShowType();
    showType.setName("Regular Show Type");
    showType.setExpectedMatches(5);
    showType.setExpectedPromos(3);
    when(show.getType()).thenReturn(showType);

    // Create wrestlers with rivalries
    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");
    wrestler1.setTier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MAIN_EVENTER);

    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");
    wrestler2.setTier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MAIN_EVENTER);

    Wrestler wrestler3 = Wrestler.builder().build();
    wrestler3.setId(3L);
    wrestler3.setName("Wrestler 3");
    wrestler3.setTier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MIDCARDER);

    // Create rivalries
    Rivalry rivalry1 = new Rivalry();
    rivalry1.setId(1L);
    rivalry1.setWrestler1(wrestler1);
    rivalry1.setWrestler2(wrestler2);
    rivalry1.setHeat(75);

    Rivalry rivalry2 = new Rivalry();
    rivalry2.setId(2L);
    rivalry2.setWrestler1(wrestler1);
    rivalry2.setWrestler2(wrestler3);
    rivalry2.setHeat(50);

    // Mock the wrestler service to return our wrestlers
    when(wrestlerService.findAll()).thenReturn(Arrays.asList(wrestler1, wrestler2, wrestler3));

    // Mock the rivalry service to return appropriate rivalries for each wrestler
    when(rivalryService.getRivalriesForWrestler(1L)).thenReturn(Arrays.asList(rivalry1, rivalry2));
    when(rivalryService.getRivalriesForWrestler(2L))
        .thenReturn(Collections.singletonList(rivalry1));
    when(rivalryService.getRivalriesForWrestler(3L))
        .thenReturn(Collections.singletonList(rivalry2));

    // Mock other dependencies
    when(segmentRepository.findBySegmentDateBetween(any(), any()))
        .thenReturn(Collections.emptyList());
    when(rivalryService.getActiveRivalriesBetween(any(), any()))
        .thenReturn(Collections.emptyList());
    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(titleService.getActiveTitles()).thenReturn(Collections.emptyList());
    when(segmentService.findById(anyLong())).thenReturn(Optional.empty());
    when(factionService.findAll()).thenReturn(Collections.emptyList());

    // Act
    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);

    // Assert
    assertNotNull(context);
    assertNotNull(context.getWrestlerHeats(), "Wrestler heats should not be null");
    assertFalse(context.getWrestlerHeats().isEmpty(), "Wrestler heats should not be empty");

    // Should have 4 heat entries total (2 for wrestler1, 1 for wrestler2, 1 for wrestler3)
    assertEquals(4, context.getWrestlerHeats().size(), "Should have 4 wrestler heat entries");

    // Verify wrestler1's heats
    long wrestler1Heats =
        context.getWrestlerHeats().stream()
            .filter(h -> "Wrestler 1".equals(h.getWrestlerName()))
            .count();
    assertEquals(2, wrestler1Heats, "Wrestler 1 should have 2 feuds");

    // Verify specific heat values
    assertTrue(
        context.getWrestlerHeats().stream()
            .anyMatch(
                h ->
                    "Wrestler 1".equals(h.getWrestlerName())
                        && "Wrestler 2".equals(h.getOpponentName())
                        && h.getHeat() == 75),
        "Should have heat entry for Wrestler 1 vs Wrestler 2 with heat 75");

    assertTrue(
        context.getWrestlerHeats().stream()
            .anyMatch(
                h ->
                    "Wrestler 1".equals(h.getWrestlerName())
                        && "Wrestler 3".equals(h.getOpponentName())
                        && h.getHeat() == 50),
        "Should have heat entry for Wrestler 1 vs Wrestler 3 with heat 50");
  }

  @Test
  void getShowPlanningContext_shouldHandleWrestlersWithNoRivalries() {
    // Given
    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");
    when(show.getShowDate()).thenReturn(LocalDate.now());
    when(show.getId()).thenReturn(1L);
    ShowType showType = new ShowType();
    showType.setName("Regular Show Type");
    showType.setExpectedMatches(5);
    showType.setExpectedPromos(3);
    when(show.getType()).thenReturn(showType);

    // Create wrestlers with no rivalries
    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");
    wrestler1.setTier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MAIN_EVENTER);

    // Mock the wrestler service to return our wrestler
    when(wrestlerService.findAll()).thenReturn(Collections.singletonList(wrestler1));

    // Mock the rivalry service to return empty list (no rivalries)
    when(rivalryService.getRivalriesForWrestler(1L)).thenReturn(Collections.emptyList());

    // Mock other dependencies
    when(segmentRepository.findBySegmentDateBetween(any(), any()))
        .thenReturn(Collections.emptyList());
    when(rivalryService.getActiveRivalriesBetween(any(), any()))
        .thenReturn(Collections.emptyList());
    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(titleService.getActiveTitles()).thenReturn(Collections.emptyList());
    when(segmentService.findById(anyLong())).thenReturn(Optional.empty());
    when(factionService.findAll()).thenReturn(Collections.emptyList());

    // Act
    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);

    // Assert
    assertNotNull(context);
    assertNotNull(context.getWrestlerHeats(), "Wrestler heats should not be null");
    assertTrue(
        context.getWrestlerHeats().isEmpty(),
        "Wrestler heats should be empty when no rivalries exist");
  }
}
