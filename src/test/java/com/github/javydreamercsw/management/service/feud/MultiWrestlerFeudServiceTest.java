package com.github.javydreamercsw.management.service.feud;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class MultiWrestlerFeudServiceTest {

  @Mock private MultiWrestlerFeudRepository feudRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private Clock clock;
  private MultiWrestlerFeudService service;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    service = new MultiWrestlerFeudService();
    // Inject mocks via reflection
    Field feudRepoField =
        MultiWrestlerFeudService.class.getDeclaredField("multiWrestlerFeudRepository");
    feudRepoField.setAccessible(true);
    feudRepoField.set(service, feudRepository);
    Field wrestlerRepoField = MultiWrestlerFeudService.class.getDeclaredField("wrestlerRepository");
    wrestlerRepoField.setAccessible(true);
    wrestlerRepoField.set(service, wrestlerRepository);
    Field clockField = MultiWrestlerFeudService.class.getDeclaredField("clock");
    clockField.setAccessible(true);
    clockField.set(service, clock);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mocks != null) mocks.close();
  }

  @Test
  void testCreateFeud() {
    when(clock.instant()).thenReturn(Instant.now());
    when(feudRepository.existsByName("TestFeud")).thenReturn(false);
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("TestFeud");
    feud.setIsActive(true);
    when(feudRepository.saveAndFlush(any())).thenReturn(feud);
    var result = service.createFeud("TestFeud", "Desc", "Notes");
    assertTrue(result.isPresent());
    assertEquals("TestFeud", result.get().getName());
  }

  @Test
  void testCreateFeudDuplicateName() {
    when(feudRepository.existsByName("TestFeud")).thenReturn(true);
    var result = service.createFeud("TestFeud", "Desc", "Notes");
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetAllFeuds() {
    Page<MultiWrestlerFeud> page = new PageImpl<>(java.util.List.of(new MultiWrestlerFeud()));
    when(feudRepository.findAllBy(any())).thenReturn(page);
    var result = service.getAllFeuds(PageRequest.of(0, 10));
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void testGetFeudById() {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("TestFeud");
    when(feudRepository.findById(anyLong())).thenReturn(Optional.of(feud));
    var result = service.getFeudById(1L);
    assertTrue(result.isPresent());
    assertEquals("TestFeud", result.get().getName());
  }

  @Test
  void testGetActiveFeuds() {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setIsActive(true);
    when(feudRepository.findByIsActiveTrue()).thenReturn(java.util.List.of(feud));
    var result = service.getActiveFeuds();
    assertEquals(1, result.size());
    assertTrue(result.get(0).getIsActive());
  }

  @Test
  void testGetFeudByName_found() {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("FeudName");
    when(feudRepository.findByName("FeudName")).thenReturn(Optional.of(feud));
    var result = service.getFeudByName("FeudName");
    assertTrue(result.isPresent());
    assertEquals("FeudName", result.get().getName());
  }

  @Test
  void testGetFeudByName_notFound() {
    when(feudRepository.findByName("MissingFeud")).thenReturn(Optional.empty());
    var result = service.getFeudByName("MissingFeud");
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetActiveFeudsForWrestler_wrestlerNotFound() {
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());
    var result = service.getActiveFeudsForWrestler(99L);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetActiveFeudsForWrestler_noFeuds() {
    Wrestler wrestler = mock(Wrestler.class);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(feudRepository.findActiveFeudsForWrestler(wrestler))
        .thenReturn(java.util.Collections.emptyList());
    var result = service.getActiveFeudsForWrestler(1L);
    assertTrue(result.isEmpty());
  }

  @Test
  void testAddParticipant_missingFeudOrWrestler() {
    when(feudRepository.findById(1L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.empty());
    var result = service.addParticipant(1L, 2L, FeudRole.PROTAGONIST);
    assertTrue(result.isEmpty());
  }

  @Test
  void testAddParticipant_nullRole_throws() {
    when(feudRepository.findById(1L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.empty());
    assertThrows(NullPointerException.class, () -> service.addParticipant(1L, 2L, null));
  }

  @Test
  void testAddParticipant_inactiveFeud() {
    MultiWrestlerFeud feud = mock(MultiWrestlerFeud.class);
    Wrestler wrestler = mock(Wrestler.class);
    when(feud.getIsActive()).thenReturn(false);
    when(feudRepository.findById(1L)).thenReturn(Optional.of(feud));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler));
    var result = service.addParticipant(1L, 2L, FeudRole.ANTAGONIST);
    assertTrue(result.isEmpty());
  }

  @Test
  void testAddParticipant_alreadyParticipant() {
    MultiWrestlerFeud feud = mock(MultiWrestlerFeud.class);
    Wrestler wrestler = mock(Wrestler.class);
    when(feud.getIsActive()).thenReturn(true);
    when(feud.hasParticipant(wrestler)).thenReturn(true);
    when(feudRepository.findById(1L)).thenReturn(Optional.of(feud));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler));
    var result = service.addParticipant(1L, 2L, FeudRole.PROTAGONIST);
    assertTrue(result.isEmpty());
  }
}
