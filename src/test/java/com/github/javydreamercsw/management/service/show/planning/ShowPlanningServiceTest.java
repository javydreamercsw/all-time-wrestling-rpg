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
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningDtoMapper;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowPlanningServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private RivalryService rivalryService;
  @Mock private TitleService titleService;
  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentRuleService segmentRuleService;

  @Spy private ShowPlanningDtoMapper mapper;
  @Spy private Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

  @Mock private PromoBookingService promoBookingService;
  @InjectMocks private ShowPlanningService showPlanningService;

  @Test
  void getShowPlanningContext() {
    // Given
    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");
    when(show.getShowDate()).thenReturn(LocalDate.now());
    when(show.getId()).thenReturn(1L);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Wrestler 1");
    Wrestler wrestler2 = new Wrestler();
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
    Wrestler champion = new Wrestler();
    champion.setName("Champion");
    title.setCurrentChampion(champion);
    when(titleService.getActiveTitles()).thenReturn(Collections.singletonList(title));
    Wrestler contender = new Wrestler();
    contender.setName("Contender");
    when(titleService.getEligibleChallengers(anyLong()))
        .thenReturn(Collections.singletonList(contender));

    ShowType pleShowType = new ShowType();
    pleShowType.setName("Premium Live Event (PLE)");

    com.github.javydreamercsw.management.domain.show.template.ShowTemplate pleTemplate =
        new com.github.javydreamercsw.management.domain.show.template.ShowTemplate();
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
}
