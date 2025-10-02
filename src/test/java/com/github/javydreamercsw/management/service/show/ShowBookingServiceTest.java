package com.github.javydreamercsw.management.service.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Show Booking Service Tests")
class ShowBookingServiceTest {

  @Mock private ShowRepository showRepository;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SeasonService seasonService;
  @Mock private RivalryService rivalryService;
  @Mock private NPCSegmentResolutionService npcSegmentResolutionService;
  @Mock private PromoBookingService promoBookingService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private Clock clock;
  @Mock private Random random;

  @InjectMocks private ShowBookingService showBookingService;

  @BeforeEach
  void setUp() {
    ShowType weeklyShowType = new ShowType();
    weeklyShowType.setName("Weekly");
    when(showTypeRepository.findByName("Weekly")).thenReturn(Optional.of(weeklyShowType));

    when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  @DisplayName("Test booking a regular show")
  void testBookShow() {
    // Given
    List<Wrestler> wrestlers = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      wrestlers.add(new Wrestler());
    }
    when(wrestlerRepository.findAll()).thenReturn(wrestlers);
    when(rivalryService.getActiveRivalries()).thenReturn(new ArrayList<>());

    SegmentType segmentType = new SegmentType();
    segmentType.setName("One on One");
    when(segmentTypeRepository.findByName("One on One")).thenReturn(Optional.of(segmentType));

    when(npcSegmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), anyString()))
        .thenReturn(new Segment());

    // When
    Optional<Show> result = showBookingService.bookShow("Test Show", "A test show", "Weekly", 5);

    // Then
    assertTrue(result.isPresent());
    Show show = result.get();
    assertEquals("Test Show", show.getName());
  }
}
