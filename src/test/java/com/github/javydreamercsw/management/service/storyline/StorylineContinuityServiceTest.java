package com.github.javydreamercsw.management.service.storyline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class StorylineContinuityServiceTest {

  @Mock private SeasonRepository seasonRepository;
  @Mock private ShowRepository showRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private DramaEventRepository dramaEventRepository;
  @Mock private RivalryService rivalryService;
  @Mock private Clock clock;
  private StorylineContinuityService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service =
        new StorylineContinuityService(
            seasonRepository,
            showRepository,
            segmentRepository,
            dramaEventRepository,
            rivalryService,
            clock);
  }

  @Test
  void testGetActiveStorylinesNoSeason() {
    when(seasonRepository.findActiveSeason()).thenReturn(Optional.empty());
    var result = service.getActiveStorylines();
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetStorylineSuggestionsNoRivalries() {
    when(rivalryService.getActiveRivalries()).thenReturn(java.util.List.of());
    var result = service.getStorylineSuggestions();
    assertTrue(result.isEmpty());
  }
}
