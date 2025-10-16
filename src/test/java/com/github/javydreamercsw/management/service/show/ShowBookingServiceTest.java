package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.PromoBookingService;
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
class ShowBookingServiceTest {

  @Mock private ShowRepository showRepository;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SeasonService seasonService;
  @Mock private RivalryService rivalryService;
  @Mock private NPCSegmentResolutionService npcSegmentResolutionService;
  @Mock private PromoBookingService promoBookingService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private Clock clock;
  @Mock private Random random;

  @InjectMocks private ShowBookingService showBookingService;

  private ShowType weeklyShowType;

  @BeforeEach
  void setUp() {
    weeklyShowType = new ShowType();
    weeklyShowType.setName("Weekly Show");
    weeklyShowType.setDescription("A weekly wrestling show");

    when(showTypeRepository.findByName("Weekly Show")).thenReturn(Optional.of(weeklyShowType));

    List<Wrestler> wrestlers = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Wrestler wrestler = new Wrestler();
      wrestler.setName("Wrestler " + i);
      wrestlers.add(wrestler);
    }
    when(wrestlerRepository.findAll()).thenReturn(wrestlers);

    SegmentType oneOnOne = new SegmentType();
    oneOnOne.setName("One on One");
    when(segmentTypeRepository.findByName("One on One")).thenReturn(Optional.of(oneOnOne));

    when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArguments()[0]);
    when(segmentRepository.save(any(Segment.class))).thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  @DisplayName("Should book regular show with specified segment count")
  void shouldBookRegularShowWithSpecifiedSegmentCount() {
    // Given
    String showName = "Monday Night Wrestling";
    String showDescription = "Weekly wrestling showcase";
    int segmentCount = 5;

    // When
    Optional<Show> result =
        showBookingService.bookShow(showName, showDescription, "Weekly Show", segmentCount);

    // Then
    assertThat(result).isPresent();
    Show show = result.get();
    assertThat(show.getName()).isEqualTo(showName);
    assertThat(show.getDescription()).isEqualTo(showDescription);
    assertThat(show.getType()).isEqualTo(weeklyShowType);
  }
}
