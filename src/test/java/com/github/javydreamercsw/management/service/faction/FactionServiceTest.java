package com.github.javydreamercsw.management.service.faction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactionService Tests")
class FactionServiceTest {

  @Mock private FactionRepository factionRepository;

  @Mock private WrestlerRepository wrestlerRepository;

  @Mock private Clock clock;

  @InjectMocks private FactionService factionService;

  private Faction testFaction;
  private Wrestler testWrestler;
  private List<Faction> testFactions;

  @BeforeEach
  void setUp() {
    // Create test faction
    testFaction = Faction.builder().build();
    testFaction.setId(1L);
    testFaction.setName("Test Faction");
    testFaction.setDescription("A test faction");
    testFaction.setIsActive(true);
    testFaction.setFormedDate(Instant.now());

    // Create test wrestler
    testWrestler = Wrestler.builder().build();
    testWrestler.setId(1L);
    testWrestler.setName("Test Wrestler");

    // Create test factions list
    testFactions = Arrays.asList(testFaction);
  }

  @Test
  @DisplayName("Should find all factions")
  void shouldFindAllFactions() {
    // Given
    when(factionRepository.findAll()).thenReturn(testFactions);

    // When
    List<Faction> result = factionService.findAll();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testFaction.getName(), result.get(0).getName());
    verify(factionRepository).findAll();
  }

  @Test
  @DisplayName("Should find all factions with members")
  void shouldFindAllFactionsWithMembers() {
    // Given
    when(factionRepository.findAllWithMembers()).thenReturn(testFactions);

    // When
    List<Faction> result = factionService.findAllWithMembers();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testFaction.getName(), result.get(0).getName());
    verify(factionRepository).findAllWithMembers();
  }

  @Test
  @DisplayName("Should get all factions (alias method)")
  void shouldGetAllFactions() {
    // Given
    when(factionRepository.findAll()).thenReturn(testFactions);

    // When
    List<Faction> result = factionService.getAllFactions();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testFaction.getName(), result.get(0).getName());
    verify(factionRepository).findAll();
  }

  @Test
  @DisplayName("Should find faction by ID")
  void shouldFindFactionById() {
    // Given
    when(factionRepository.findById(1L)).thenReturn(Optional.of(testFaction));

    // When
    Optional<Faction> result = factionService.getFactionById(1L);

    // Then
    assertTrue(result.isPresent());
    assertEquals(testFaction.getName(), result.get().getName());
    verify(factionRepository).findById(1L);
  }

  @Test
  @DisplayName("Should return empty when faction not found by ID")
  void shouldReturnEmptyWhenFactionNotFoundById() {
    // Given
    when(factionRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<Faction> result = factionService.getFactionById(999L);

    // Then
    assertFalse(result.isPresent());
    verify(factionRepository).findById(999L);
  }

  @Test
  @DisplayName("Should find faction by ID with members")
  void shouldFindFactionByIdWithMembers() {
    // Given
    List<Wrestler> members = new ArrayList<>();
    members.add(testWrestler);
    testFaction.setMembers(members);

    when(factionRepository.findByIdWithMembers(1L)).thenReturn(Optional.of(testFaction));

    // When
    Optional<Faction> result = factionService.getFactionByIdWithMembers(1L);

    // Then
    assertTrue(result.isPresent());
    assertEquals(testFaction.getName(), result.get().getName());
    // Verify that members collection was accessed (forcing initialization)
    assertEquals(1, result.get().getMembers().size());
    verify(factionRepository).findByIdWithMembers(1L);
  }

  @Test
  @DisplayName("Should save faction")
  void shouldSaveFaction() {
    // Given
    when(factionRepository.saveAndFlush(testFaction)).thenReturn(testFaction);

    // When
    Faction result = factionService.save(testFaction);

    // Then
    assertNotNull(result);
    assertEquals(testFaction.getName(), result.getName());
    verify(factionRepository).saveAndFlush(testFaction);
  }

  @Test
  @DisplayName("Should add member to faction")
  void shouldAddMemberToFaction() {
    // Given
    when(factionRepository.findById(1L)).thenReturn(Optional.of(testFaction));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(factionRepository.saveAndFlush(any(Faction.class))).thenReturn(testFaction);

    // When
    Optional<Faction> result = factionService.addMemberToFaction(1L, 1L);

    // Then
    assertTrue(result.isPresent());
    assertEquals(testFaction.getName(), result.get().getName());
    verify(factionRepository).findById(1L);
    verify(wrestlerRepository).findById(1L);
    verify(factionRepository).saveAndFlush(testFaction);
  }

  @Test
  @DisplayName("Should not add member when faction not found")
  void shouldNotAddMemberWhenFactionNotFound() {
    // Given
    when(factionRepository.findById(999L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));

    // When
    Optional<Faction> result = factionService.addMemberToFaction(999L, 1L);

    // Then
    assertFalse(result.isPresent());
    verify(factionRepository).findById(999L);
    verify(wrestlerRepository).findById(1L); // Service calls both repositories
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Should not add member when wrestler not found")
  void shouldNotAddMemberWhenWrestlerNotFound() {
    // Given
    when(factionRepository.findById(1L)).thenReturn(Optional.of(testFaction));
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<Faction> result = factionService.addMemberToFaction(1L, 999L);

    // Then
    assertFalse(result.isPresent());
    verify(factionRepository).findById(1L);
    verify(wrestlerRepository).findById(999L);
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Should remove member from faction")
  void shouldRemoveMemberFromFaction() {
    // Given
    List<Wrestler> members = new ArrayList<>();
    members.add(testWrestler);
    testFaction.setMembers(members);

    when(factionRepository.findById(1L)).thenReturn(Optional.of(testFaction));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(factionRepository.saveAndFlush(any(Faction.class))).thenReturn(testFaction);

    // When
    Optional<Faction> result = factionService.removeMemberFromFaction(1L, 1L, "Test removal");

    // Then
    assertTrue(result.isPresent());
    assertEquals(testFaction.getName(), result.get().getName());
    verify(factionRepository).findById(1L);
    verify(wrestlerRepository).findById(1L);
    verify(factionRepository).saveAndFlush(testFaction);
  }

  @Test
  @DisplayName("Should not remove member when faction not found")
  void shouldNotRemoveMemberWhenFactionNotFound() {
    // Given
    when(factionRepository.findById(999L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));

    // When
    Optional<Faction> result = factionService.removeMemberFromFaction(999L, 1L, "Test removal");

    // Then
    assertFalse(result.isPresent());
    verify(factionRepository).findById(999L);
    verify(wrestlerRepository).findById(1L); // Service calls both repositories
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Should not remove member when wrestler not found")
  void shouldNotRemoveMemberWhenWrestlerNotFound() {
    // Given
    when(factionRepository.findById(1L)).thenReturn(Optional.of(testFaction));
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<Faction> result = factionService.removeMemberFromFaction(1L, 999L, "Test removal");

    // Then
    assertFalse(result.isPresent());
    verify(factionRepository).findById(1L);
    verify(wrestlerRepository).findById(999L);
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Should delete faction by ID")
  void shouldDeleteFactionById() {
    // Given
    doNothing().when(factionRepository).deleteById(1L);

    // When
    factionService.deleteById(1L);

    // Then
    verify(factionRepository).deleteById(1L);
  }

  @Test
  @DisplayName("Should check if faction exists by ID")
  void shouldCheckIfFactionExistsById() {
    // Given
    when(factionRepository.existsById(1L)).thenReturn(true);

    // When
    boolean result = factionService.existsById(1L);

    // Then
    assertTrue(result);
    verify(factionRepository).existsById(1L);
  }

  @Test
  @DisplayName("Should return false when faction does not exist by ID")
  void shouldReturnFalseWhenFactionDoesNotExistById() {
    // Given
    when(factionRepository.existsById(999L)).thenReturn(false);

    // When
    boolean result = factionService.existsById(999L);

    // Then
    assertFalse(result);
    verify(factionRepository).existsById(999L);
  }

  @Test
  @DisplayName("Should count all factions")
  void shouldCountAllFactions() {
    // Given
    when(factionRepository.count()).thenReturn(5L);

    // When
    long result = factionService.count();

    // Then
    assertEquals(5L, result);
    verify(factionRepository).count();
  }
}
