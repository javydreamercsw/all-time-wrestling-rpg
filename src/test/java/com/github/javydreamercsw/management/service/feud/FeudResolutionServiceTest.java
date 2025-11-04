package com.github.javydreamercsw.management.service.feud;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.FeudResolvedEvent;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class FeudResolutionServiceTest {

  @Mock private MultiWrestlerFeudRepository feudRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private Random random;
  @InjectMocks private FeudResolutionService feudResolutionService;

  private MultiWrestlerFeud activeFeud;
  private MultiWrestlerFeud inactiveFeud;

  @BeforeEach
  void setUp() {
    activeFeud = new MultiWrestlerFeud();
    activeFeud.setId(1L);
    activeFeud.setName("Test Feud");
    activeFeud.setHeat(25);
    activeFeud.setIsActive(true);
    activeFeud.setStartedDate(Instant.now());
    activeFeud.setCreationDate(Instant.now());

    inactiveFeud = new MultiWrestlerFeud();
    inactiveFeud.setId(2L);
    inactiveFeud.setName("Inactive Feud");
    inactiveFeud.setHeat(30);
    inactiveFeud.setIsActive(false);
    inactiveFeud.setStartedDate(Instant.now());
    inactiveFeud.setCreationDate(Instant.now());
  }

  @Test
  void testAttemptFeudResolution_notEligible() {
    activeFeud.setHeat(10); // Not enough heat
    feudResolutionService.attemptFeudResolution(activeFeud);

    verify(feudRepository, never()).save(any(MultiWrestlerFeud.class));
    verify(eventPublisher, never()).publishEvent(any(FeudResolvedEvent.class));
  }

  @Test
  void testAttemptFeudResolution_resolved() {

    when(random.nextInt(anyInt())).thenReturn(15); // Simulate a roll that resolves the feud

    activeFeud.addParticipant(mock(Wrestler.class), FeudRole.PROTAGONIST);

    activeFeud.addParticipant(mock(Wrestler.class), FeudRole.ANTAGONIST);

    activeFeud.addParticipant(mock(Wrestler.class), FeudRole.NEUTRAL);

    feudResolutionService.attemptFeudResolution(activeFeud);

    assertFalse(activeFeud.getIsActive());

    verify(feudRepository, times(1)).save(activeFeud);

    ArgumentCaptor<FeudResolvedEvent> eventCaptor =
        ArgumentCaptor.forClass(FeudResolvedEvent.class);

    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    assertEquals(activeFeud, eventCaptor.getValue().getFeud());
  }

  @Test
  void testAttemptFeudResolution_notResolved() {
    when(random.nextInt(anyInt())).thenReturn(5); // Simulate a roll that does not resolve the feud
    activeFeud.addParticipant(mock(Wrestler.class), FeudRole.PROTAGONIST);
    activeFeud.addParticipant(mock(Wrestler.class), FeudRole.ANTAGONIST);
    activeFeud.addParticipant(mock(Wrestler.class), FeudRole.NEUTRAL);

    feudResolutionService.attemptFeudResolution(activeFeud);

    assertTrue(activeFeud.getIsActive());
    verify(feudRepository, never()).save(any(MultiWrestlerFeud.class));
    verify(eventPublisher, never()).publishEvent(any(FeudResolvedEvent.class));
  }

  @Test
  void testAttemptFeudResolution_inactiveFeud() {
    feudResolutionService.attemptFeudResolution(inactiveFeud);

    verify(feudRepository, never()).save(any(MultiWrestlerFeud.class));
    verify(eventPublisher, never()).publishEvent(any(FeudResolvedEvent.class));
  }

  @Test
  void testAttemptFeudResolution_noParticipants() {
    feudResolutionService.attemptFeudResolution(activeFeud);

    verify(feudRepository, never()).save(any(MultiWrestlerFeud.class));
    verify(eventPublisher, never()).publishEvent(any(FeudResolvedEvent.class));
  }
}
