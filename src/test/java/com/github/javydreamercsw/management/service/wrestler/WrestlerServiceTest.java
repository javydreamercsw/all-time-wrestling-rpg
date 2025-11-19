package com.github.javydreamercsw.management.service.wrestler;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WrestlerServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private InjuryService injuryService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private WrestlerService wrestlerService;

  private Wrestler wrestler;

  @Mock private DiceBag diceBag;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
  }

  @Test
  void testAddBump_PublishesEvent() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);

    // When
    wrestlerService.addBump(1L);

    // Then
    ArgumentCaptor<WrestlerBumpEvent> eventCaptor =
        ArgumentCaptor.forClass(WrestlerBumpEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
  }

  @Test
  void testHealChance_PublishesEvent() {
    // Given
    wrestler.setBumps(1);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);
    when(diceBag.roll()).thenReturn(4); // Ensure bump is healed

    // When
    wrestlerService.healChance(1L, diceBag);

    // Then
    ArgumentCaptor<WrestlerBumpHealedEvent> eventCaptor =
        ArgumentCaptor.forClass(WrestlerBumpHealedEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
  }
}
