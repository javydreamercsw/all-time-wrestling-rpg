package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for WrestlerService ATW RPG functionality. Tests the service layer methods for fan
 * management, injury system, and wrestler filtering.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WrestlerService ATW RPG Tests")
class WrestlerServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private InjuryService injuryService;
  @Mock private Clock clock;
  @Mock private DramaEventRepository dramaEventRepository;

  @InjectMocks private WrestlerService wrestlerService;

  private Wrestler testWrestler;
  private final Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");

  @BeforeEach
  void setUp() {
    testWrestler = new Wrestler();
    testWrestler.setId(1L);
    testWrestler.setName("Test Wrestler");
    testWrestler.setFans(30000L);
    testWrestler.setBumps(0);
    testWrestler.setIsPlayer(true);
    testWrestler.updateTier();
  }

  @Test
  @DisplayName("Should award fans and update tier")
  void shouldAwardFansAndUpdateTier() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(testWrestler);

    // When
    Optional<Wrestler> result = wrestlerService.awardFans(1L, 20000L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getFans()).isEqualTo(50000L);
    assertThat(result.get().getTier()).isEqualTo(WrestlerTier.CONTENDER);
    verify(wrestlerRepository).saveAndFlush(testWrestler);
  }

  @Test
  @DisplayName("Should handle negative fan awards")
  void shouldHandleNegativeFanAwards() {
    // Given
    testWrestler.setFans(50000L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(testWrestler);

    // When
    Optional<Wrestler> result = wrestlerService.awardFans(1L, -20000L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getFans()).isEqualTo(30000L);
    assertThat(result.get().getTier()).isEqualTo(WrestlerTier.RISER);
  }

  @Test
  @DisplayName("Should return empty when wrestler not found for fan award")
  void shouldReturnEmptyWhenWrestlerNotFoundForFanAward() {
    // Given
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<Wrestler> result = wrestlerService.awardFans(999L, 10000L);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should add bump without injury")
  void shouldAddBumpWithoutInjury() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(testWrestler);

    // When
    Optional<Wrestler> result = wrestlerService.addBump(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getBumps()).isEqualTo(1);
    verify(wrestlerRepository).saveAndFlush(testWrestler);
  }

  @Test
  @DisplayName("Should add bump and trigger injury")
  void shouldAddBumpAndTriggerInjury() {
    // Given
    testWrestler.setBumps(2); // Already has 2 bumps
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(testWrestler);

    // Mock injury creation
    Injury mockInjury = new Injury();
    mockInjury.setName("Test Injury");
    mockInjury.setSeverity(InjurySeverity.MINOR);
    when(injuryService.createInjuryFromBumps(any())).thenReturn(Optional.of(mockInjury));

    // When
    Optional<Wrestler> result = wrestlerService.addBump(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getBumps()).isEqualTo(0); // Reset after injury
    verify(wrestlerRepository).saveAndFlush(testWrestler);
    verify(injuryService)
        .createInjuryFromBumps(testWrestler.getId()); // Verify injury service was called
  }

  @Test
  @DisplayName("Should return empty when wrestler not found for bump")
  void shouldReturnEmptyWhenWrestlerNotFoundForBump() {
    // Given
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<Wrestler> result = wrestlerService.addBump(999L);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should get eligible wrestlers for title")
  void shouldGetEligibleWrestlersForTitle() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 20000L); // Not eligible
    Wrestler wrestler2 = createWrestler("Wrestler 2", 30000L); // Eligible for Extreme
    Wrestler wrestler3 = createWrestler("Wrestler 3", 45000L); // Eligible for Extreme & Tag Team

    when(wrestlerRepository.findAll()).thenReturn(Arrays.asList(wrestler1, wrestler2, wrestler3));

    // When
    List<Wrestler> eligible = wrestlerService.getEligibleWrestlers(WrestlerTier.ROOKIE);

    // Then
    assertThat(eligible).hasSize(3);
    assertThat(eligible)
        .extracting(Wrestler::getName)
        .containsExactly("Wrestler 1", "Wrestler 2", "Wrestler 3");
  }

  @Test
  @DisplayName("Should get wrestlers by tier")
  void shouldGetWrestlersByTier() {
    // Given
    Wrestler rookie = createWrestler("Rookie", 10000L);
    Wrestler riser1 = createWrestler("Riser 1", 30000L);
    Wrestler riser2 = createWrestler("Riser 2", 35000L);
    Wrestler contender = createWrestler("Contender", 45000L);

    when(wrestlerRepository.findAll()).thenReturn(Arrays.asList(rookie, riser1, riser2, contender));

    // When
    List<Wrestler> risers = wrestlerService.getWrestlersByTier(WrestlerTier.RISER);

    // Then
    assertThat(risers).hasSize(2);
    assertThat(risers).extracting(Wrestler::getName).containsExactly("Riser 1", "Riser 2");
  }

  @Test
  @DisplayName("Should get player wrestlers")
  void shouldGetPlayerWrestlers() {
    // Given
    Wrestler player1 = createWrestler("Player 1", 30000L);
    player1.setIsPlayer(true);
    Wrestler npc1 = createWrestler("NPC 1", 40000L);
    npc1.setIsPlayer(false);
    Wrestler player2 = createWrestler("Player 2", 50000L);
    player2.setIsPlayer(true);

    when(wrestlerRepository.findAll()).thenReturn(Arrays.asList(player1, npc1, player2));

    // When
    List<Wrestler> players = wrestlerService.getPlayerWrestlers();

    // Then
    assertThat(players).hasSize(2);
    assertThat(players).extracting(Wrestler::getName).containsExactly("Player 1", "Player 2");
  }

  @Test
  @DisplayName("Should get NPC wrestlers")
  void shouldGetNpcWrestlers() {
    // Given
    Wrestler player1 = createWrestler("Player 1", 30000L);
    player1.setIsPlayer(true);
    Wrestler npc1 = createWrestler("NPC 1", 40000L);
    npc1.setIsPlayer(false);
    Wrestler npc2 = createWrestler("NPC 2", 50000L);
    npc2.setIsPlayer(false);

    when(wrestlerRepository.findAll()).thenReturn(Arrays.asList(player1, npc1, npc2));

    // When
    List<Wrestler> npcs = wrestlerService.getNpcWrestlers();

    // Then
    assertThat(npcs).hasSize(2);
    assertThat(npcs).extracting(Wrestler::getName).containsExactly("NPC 1", "NPC 2");
  }

  @Test
  @DisplayName("Should spend fans successfully")
  void shouldSpendFansSuccessfully() {
    // Given
    testWrestler.setFans(50000L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(testWrestler);

    // When
    boolean result = wrestlerService.spendFans(1L, 20000L);

    // Then
    assertThat(result).isTrue();
    assertThat(testWrestler.getFans()).isEqualTo(30000L);
    verify(wrestlerRepository).saveAndFlush(testWrestler);
  }

  @Test
  @DisplayName("Should fail to spend fans when insufficient")
  void shouldFailToSpendFansWhenInsufficient() {
    // Given
    testWrestler.setFans(10000L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));

    // When
    boolean result = wrestlerService.spendFans(1L, 20000L);

    // Then
    assertThat(result).isFalse();
    assertThat(testWrestler.getFans()).isEqualTo(10000L); // Unchanged
  }

  @Test
  @DisplayName("Should return false when wrestler not found for spending")
  void shouldReturnFalseWhenWrestlerNotFoundForSpending() {
    // Given
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    boolean result = wrestlerService.spendFans(999L, 10000L);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should create ATW wrestler with correct defaults")
  void shouldCreateWrestlerWithCorrectDefaults() {
    // Given
    when(clock.instant()).thenReturn(fixedInstant);
    Wrestler savedWrestler = new Wrestler();
    savedWrestler.setName("New Wrestler");
    savedWrestler.setFans(0L);
    savedWrestler.setIsPlayer(true);
    savedWrestler.setDescription("Test description");

    savedWrestler.setCreationDate(fixedInstant);

    // Mock the saveAndFlush method that's actually called by save()
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class)))
        .thenAnswer(
            invocation -> {
              Wrestler wrestler = invocation.getArgument(0);
              wrestler.setId(1L); // Simulate database ID assignment
              return wrestler;
            });

    // When
    Wrestler result = wrestlerService.createWrestler("New Wrestler", true, "Test description");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("New Wrestler");
    assertThat(result.getIsPlayer()).isTrue();
    assertThat(result.getDescription()).isEqualTo("Test description");

    assertThat(result.getFans()).isEqualTo(0L);
    assertThat(result.getBumps()).isEqualTo(0);
    assertThat(result.getDeckSize()).isEqualTo(15);
    assertThat(result.getStartingHealth()).isEqualTo(15);
    verify(wrestlerRepository).saveAndFlush(any(Wrestler.class));
  }

  @Test
  void testCreateCard() {
    wrestlerService.createCard("New Wrestler");
    verify(wrestlerRepository).saveAndFlush(any(Wrestler.class));
  }

  @Test
  void testList() {
    Wrestler wrestler = new Wrestler();
    when(wrestlerRepository.findAllBy(any()))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(wrestler)));
    List<Wrestler> result =
        wrestlerService.list(org.springframework.data.domain.PageRequest.of(0, 10));
    assertThat(result).hasSize(1);
  }

  @Test
  void testCount() {
    when(wrestlerRepository.count()).thenReturn(42L);
    assertThat(wrestlerService.count()).isEqualTo(42L);
  }

  @Test
  void testSave() {
    Wrestler wrestler = new Wrestler();
    when(clock.instant()).thenReturn(fixedInstant);
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);
    Wrestler result = wrestlerService.save(wrestler);
    assertThat(result.getCreationDate()).isEqualTo(fixedInstant);
    verify(wrestlerRepository).saveAndFlush(wrestler);
  }

  @Test
  void testDelete() {
    Wrestler wrestler = new Wrestler();
    wrestler.setName("DeleteMe");
    wrestler.setId(1L);
    wrestlerService.delete(wrestler);
    verify(dramaEventRepository).deleteByPrimaryWrestlerOrSecondaryWrestler(wrestler, wrestler);
    verify(wrestlerRepository).delete(wrestler);
  }

  @Test
  void testFindAll() {
    Wrestler wrestler = new Wrestler();
    when(wrestlerRepository.findAll(any(Sort.class))).thenReturn(List.of(wrestler));
    List<Wrestler> result = wrestlerService.findAll();
    assertThat(result).hasSize(1);
  }

  @Test
  void testGetWrestlerById_found() {
    Wrestler wrestler = new Wrestler();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    Optional<Wrestler> result = wrestlerService.getWrestlerById(1L);
    assertThat(result).isPresent();
  }

  @Test
  void testGetWrestlerById_notFound() {
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.empty());
    Optional<Wrestler> result = wrestlerService.getWrestlerById(2L);
    assertThat(result).isEmpty();
  }

  @Test
  void testFindByName_found() {
    Wrestler wrestler = new Wrestler();
    when(wrestlerRepository.findByName("Name")).thenReturn(Optional.of(wrestler));
    Optional<Wrestler> result = wrestlerService.findByName("Name");
    assertThat(result).isPresent();
  }

  @Test
  void testFindByName_notFound() {
    when(wrestlerRepository.findByName("Missing")).thenReturn(Optional.empty());
    Optional<Wrestler> result = wrestlerService.findByName("Missing");
    assertThat(result).isEmpty();
  }

  @Test
  void testFindByExternalId_found() {
    Wrestler wrestler = new Wrestler();
    when(wrestlerRepository.findByExternalId("extid")).thenReturn(Optional.of(wrestler));
    Optional<Wrestler> result = wrestlerService.findByExternalId("extid");
    assertThat(result).isPresent();
  }

  @Test
  void testFindByExternalId_notFound() {
    when(wrestlerRepository.findByExternalId("missing")).thenReturn(Optional.empty());
    Optional<Wrestler> result = wrestlerService.findByExternalId("missing");
    assertThat(result).isEmpty();
  }

  @Test
  void testAwardFans_found() {
    Wrestler wrestler = new Wrestler();
    wrestler.setFans(100L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);
    Optional<Wrestler> result = wrestlerService.awardFans(1L, 50L);
    assertThat(result).isPresent();
    assertThat(result.get().getFans()).isEqualTo(150L);
  }

  @Test
  void testAwardFans_notFound() {
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.empty());
    Optional<Wrestler> result = wrestlerService.awardFans(2L, 50L);
    assertThat(result).isEmpty();
  }

  @Test
  void testAddBump_found_noInjury() {
    Wrestler wrestler = new Wrestler();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);
    Optional<Wrestler> result = wrestlerService.addBump(1L);
    assertThat(result).isPresent();
  }

  @Test
  void testAddBump_found_withInjury() {
    Wrestler wrestler =
        new Wrestler() {
          @Override
          public boolean addBump() {
            return true;
          }
        };
    wrestler.setId(1L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    Injury injury = new Injury();
    injury.setSeverity(InjurySeverity.MINOR); // Ensure severity is set to avoid NPE
    when(injuryService.createInjuryFromBumps(any())).thenReturn(Optional.of(injury));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);
    Optional<Wrestler> result = wrestlerService.addBump(1L);
    assertThat(result).isPresent();
    verify(injuryService).createInjuryFromBumps(any());
  }

  @Test
  void testAddBump_notFound() {
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.empty());
    Optional<Wrestler> result = wrestlerService.addBump(2L);
    assertThat(result).isEmpty();
  }

  private Wrestler createWrestler(String name, Long fans) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    wrestler.setFans(fans);
    wrestler.setIsPlayer(true);
    wrestler.updateTier();
    return wrestler;
  }
}
