package com.github.javydreamercsw.management.service.feud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.FeudHeatChangeEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiWrestlerFeudService Tests")
class MultiWrestlerFeudServiceTest {

  @Mock private MultiWrestlerFeudRepository multiWrestlerFeudRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private Clock clock;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private MultiWrestlerFeudService multiWrestlerFeudService;

  @BeforeEach
  void setUp() {
    lenient().when(clock.instant()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
  }

  @Test
  void testCreateFeud() {
    when(clock.instant()).thenReturn(Instant.now());
    when(multiWrestlerFeudRepository.existsByName("TestFeud")).thenReturn(false);
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("TestFeud");
    feud.setIsActive(true);
    when(multiWrestlerFeudRepository.saveAndFlush(any())).thenReturn(feud);
    var result = multiWrestlerFeudService.createFeud("TestFeud", "Desc", "Notes");
    assertTrue(result.isPresent());
    assertEquals("TestFeud", result.get().getName());
  }

  @Test
  void testCreateFeudDuplicateName() {
    when(multiWrestlerFeudRepository.existsByName("TestFeud")).thenReturn(true);
    var result = multiWrestlerFeudService.createFeud("TestFeud", "Desc", "Notes");
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetAllFeuds() {
    Page<MultiWrestlerFeud> page = new PageImpl<>(java.util.List.of(new MultiWrestlerFeud()));
    when(multiWrestlerFeudRepository.findAllBy(any())).thenReturn(page);
    var result = multiWrestlerFeudService.getAllFeuds(PageRequest.of(0, 10));
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void testGetFeudById() {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("TestFeud");
    when(multiWrestlerFeudRepository.findById(anyLong())).thenReturn(Optional.of(feud));
    var result = multiWrestlerFeudService.getFeudById(1L);
    assertTrue(result.isPresent());
    assertEquals("TestFeud", result.get().getName());
  }

  @Test
  void testGetActiveFeuds() {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setIsActive(true);
    when(multiWrestlerFeudRepository.findByIsActiveTrue()).thenReturn(java.util.List.of(feud));
    var result = multiWrestlerFeudService.getActiveFeuds();
    assertEquals(1, result.size());
    assertTrue(result.get(0).getIsActive());
  }

  @Test
  void testGetFeudByName_found() {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("FeudName");
    when(multiWrestlerFeudRepository.findByName("FeudName")).thenReturn(Optional.of(feud));
    var result = multiWrestlerFeudService.getFeudByName("FeudName");
    assertTrue(result.isPresent());
    assertEquals("FeudName", result.get().getName());
  }

  @Test
  void testGetFeudByName_notFound() {
    when(multiWrestlerFeudRepository.findByName("MissingFeud")).thenReturn(Optional.empty());
    var result = multiWrestlerFeudService.getFeudByName("MissingFeud");
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetActiveFeudsForWrestler_wrestlerNotFound() {
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());
    var result = multiWrestlerFeudService.getActiveFeudsForWrestler(99L);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetActiveFeudsForWrestler_noFeuds() {
    Wrestler wrestler = mock(Wrestler.class);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(multiWrestlerFeudRepository.findActiveFeudsForWrestler(wrestler))
        .thenReturn(java.util.Collections.emptyList());
    var result = multiWrestlerFeudService.getActiveFeudsForWrestler(1L);
    assertTrue(result.isEmpty());
  }

  @Test
  void testAddParticipant_missingFeudOrWrestler() {
    when(multiWrestlerFeudRepository.findById(1L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.empty());
    var result = multiWrestlerFeudService.addParticipant(1L, 2L, FeudRole.PROTAGONIST);
    assertTrue(result.isEmpty());
  }

  @Test
  void testAddParticipant_nullRole_throws() {
    assertThrows(
        NullPointerException.class, () -> multiWrestlerFeudService.addParticipant(1L, 2L, null));
  }

  @Test
  void testAddParticipant_inactiveFeud() {
    MultiWrestlerFeud feud = mock(MultiWrestlerFeud.class);
    Wrestler wrestler = mock(Wrestler.class);
    when(feud.getIsActive()).thenReturn(false);
    when(multiWrestlerFeudRepository.findById(1L)).thenReturn(Optional.of(feud));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler));
    var result = multiWrestlerFeudService.addParticipant(1L, 2L, FeudRole.ANTAGONIST);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should publish FeudHeatChangeEvent with wrestlers")
  void shouldPublishFeudHeatChangeEventWithWrestlers() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    MultiWrestlerFeud feud = createFeud("Test Feud", 1L, wrestler1, wrestler2);

    when(multiWrestlerFeudRepository.findById(1L)).thenReturn(Optional.of(feud));
    when(multiWrestlerFeudRepository.saveAndFlush(any(MultiWrestlerFeud.class))).thenReturn(feud);

    // When
    multiWrestlerFeudService.addHeat(1L, 5, "Feud escalation");

    // Then
    verify(eventPublisher)
        .publishEvent(
            argThat(
                event ->
                    event instanceof FeudHeatChangeEvent
                        && ((FeudHeatChangeEvent) event).getSource() == multiWrestlerFeudService
                        && ((FeudHeatChangeEvent) event).getFeudId() == feud.getId()
                        && ((FeudHeatChangeEvent) event).getOldHeat() == 5
                        && ((FeudHeatChangeEvent) event).getReason().equals("Feud escalation")
                        && ((FeudHeatChangeEvent) event)
                            .getWrestlers()
                            .containsAll(List.of(wrestler1, wrestler2))
                        && List.of(wrestler1, wrestler2)
                            .containsAll(((FeudHeatChangeEvent) event).getWrestlers())));
  }

  private Wrestler createWrestler(String name, Long id) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private MultiWrestlerFeud createFeud(String name, Long id, Wrestler... wrestlers) {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setId(id);
    feud.setName(name);
    feud.setHeat(5);
    feud.setIsActive(true);
    feud.setStartedDate(Instant.now(clock));
    for (Wrestler wrestler : wrestlers) {
      feud.addParticipant(wrestler, FeudRole.PROTAGONIST);
    }
    return feud;
  }
}
