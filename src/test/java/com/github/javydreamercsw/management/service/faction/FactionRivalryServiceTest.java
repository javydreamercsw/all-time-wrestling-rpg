package com.github.javydreamercsw.management.service.faction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.FactionHeatChangeEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactionRivalryService Tests")
class FactionRivalryServiceTest {

  @Mock private FactionRivalryRepository factionRivalryRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private Clock clock;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private Random random;

  @InjectMocks private FactionRivalryService factionRivalryService;

  @BeforeEach
  void setUp() {
    lenient().when(clock.instant()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
  }

  private static Stream<Arguments> heatChangeScenarios() {
    return Stream.of(
        Arguments.of(0, 5, 0, 5), // SIMMERING
        Arguments.of(10, 5, 10, 16), // HEATED
        Arguments.of(20, 5, 20, 28), // INTENSE
        Arguments.of(30, 5, 30, 40) // EXPLOSIVE
        );
  }

  @ParameterizedTest
  @MethodSource("heatChangeScenarios")
  @DisplayName("Should publish FactionHeatChangeEvent with wrestlers for different heat levels")
  void shouldPublishFactionHeatChangeEventWithWrestlers(
      int initialHeat, int heatGain, int expectedOldHeat, int expectedNewHeat) {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Wrestler wrestler3 = createWrestler("Wrestler 3", 3L);
    Wrestler wrestler4 = createWrestler("Wrestler 4", 4L);

    Faction faction1 = createFaction("Faction 1", 1L, List.of(wrestler1, wrestler2));
    Faction faction2 = createFaction("Faction 2", 2L, List.of(wrestler3, wrestler4));

    FactionRivalry rivalry = createFactionRivalry(faction1, faction2, initialHeat);

    Assertions.assertNotNull(rivalry.getId());
    when(factionRivalryRepository.findById(rivalry.getId())).thenReturn(Optional.of(rivalry));
    when(factionRivalryRepository.saveAndFlush(any(FactionRivalry.class))).thenReturn(rivalry);

    // When
    factionRivalryService.addHeat(rivalry.getId(), heatGain, "Faction brawl");

    // Then
    List<Wrestler> expectedWrestlers = List.of(wrestler1, wrestler2, wrestler3, wrestler4);
    verify(eventPublisher)
        .publishEvent(
            argThat(
                event ->
                    event instanceof FactionHeatChangeEvent
                        && ((FactionHeatChangeEvent) event)
                            .getFactionRivalryId()
                            .equals(rivalry.getId())
                        && ((FactionHeatChangeEvent) event).getOldHeat() == expectedOldHeat
                        && ((FactionHeatChangeEvent) event).getNewHeat() == expectedNewHeat
                        && ((FactionHeatChangeEvent) event).getReason().equals("Faction brawl")
                        && ((FactionHeatChangeEvent) event)
                            .getWrestlers()
                            .containsAll(expectedWrestlers)
                        && expectedWrestlers.containsAll(
                            ((FactionHeatChangeEvent) event).getWrestlers())));
  }

  private Wrestler createWrestler(@NonNull String name, @NonNull Long id) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private Faction createFaction(
      @NonNull String name, @NonNull Long id, @NonNull List<Wrestler> members) {
    Faction faction = Faction.builder().build();
    faction.setId(id);
    faction.setName(name);
    faction.getMembers().addAll(members);
    return faction;
  }

  private FactionRivalry createFactionRivalry(
      @NonNull Faction faction1, @NonNull Faction faction2, int heat) {
    FactionRivalry rivalry = new FactionRivalry();
    rivalry.setId(1L);
    rivalry.setFaction1(faction1);
    rivalry.setFaction2(faction2);
    rivalry.setHeat(heat);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now(clock));
    return rivalry;
  }
}
