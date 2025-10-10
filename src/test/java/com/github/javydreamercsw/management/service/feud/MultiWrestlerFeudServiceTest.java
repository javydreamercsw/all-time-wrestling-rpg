package com.github.javydreamercsw.management.service.feud;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
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
}
