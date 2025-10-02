package com.github.javydreamercsw.management.service.drama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DramaEventServiceTest {

  @Mock private DramaEventRepository dramaEventRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private Random random;
  @Mock private Clock clock;

  @InjectMocks private DramaEventService dramaEventService;

  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");

    when(clock.instant()).thenReturn(Instant.now());
  }

  @Test
  void testGenerateRandomDramaEvent() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    // Control randomness to get a POSITIVE backstage incident without a secondary wrestler
    when(random.nextInt(DramaEventType.values().length)).thenReturn(0); // BACKSTAGE_INCIDENT
    // 1st call for severity, 2nd for multi-wrestler, 3rd for fan impact, 4th for rivalry, 5th for
    // injury
    when(random.nextDouble()).thenReturn(0.8, 0.8, 0.5, 0.5, 0.5);

    when(dramaEventRepository.save(any(DramaEvent.class)))
        .thenAnswer(
            invocation -> {
              DramaEvent event = invocation.getArgument(0);
              event.setId(100L);
              return event;
            });

    // When
    Optional<DramaEvent> result = dramaEventService.generateRandomDramaEvent(1L);

    // Then
    assertTrue(result.isPresent());
    DramaEvent event = result.get();

    assertEquals(wrestler, event.getPrimaryWrestler());
    assertEquals(DramaEventType.BACKSTAGE_INCIDENT, event.getEventType());
    assertEquals(DramaEventSeverity.POSITIVE, event.getSeverity());
    assertNotNull(event.getTitle());
    assertNotNull(event.getDescription());
  }
}
