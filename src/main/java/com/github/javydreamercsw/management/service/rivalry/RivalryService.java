package com.github.javydreamercsw.management.service.rivalry;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryIntensity;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing rivalries in the ATW RPG system. Handles heat management, rivalry
 * resolution, and storyline progression.
 */
@Service
@Transactional
public class RivalryService {

  private final RivalryRepository rivalryRepository;
  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;
  private final Random random;

  public RivalryService(
      RivalryRepository rivalryRepository, WrestlerRepository wrestlerRepository, Clock clock) {
    this.rivalryRepository = rivalryRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.clock = clock;
    this.random = new Random();
  }

  /** Create a new rivalry between two wrestlers. */
  public Optional<Rivalry> createRivalry(
      Long wrestler1Id, Long wrestler2Id, String storylineNotes) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return Optional.empty();
    }

    Wrestler wrestler1 = wrestler1Opt.get();
    Wrestler wrestler2 = wrestler2Opt.get();

    // Check if rivalry already exists
    Optional<Rivalry> existingRivalry =
        rivalryRepository.findActiveRivalryBetween(wrestler1, wrestler2);
    if (existingRivalry.isPresent()) {
      return existingRivalry; // Return existing rivalry
    }

    // Create new rivalry
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(0);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now(clock));
    rivalry.setStorylineNotes(storylineNotes);
    rivalry.setCreationDate(Instant.now(clock));

    return Optional.of(rivalryRepository.saveAndFlush(rivalry));
  }

  /** Add heat to a rivalry. */
  public Optional<Rivalry> addHeat(Long rivalryId, int heatGain, String reason) {
    return rivalryRepository
        .findById(rivalryId)
        .filter(Rivalry::getIsActive)
        .map(
            rivalry -> {
              rivalry.addHeat(heatGain, reason);
              return rivalryRepository.saveAndFlush(rivalry);
            });
  }

  /** Add heat between two specific wrestlers. */
  public Optional<Rivalry> addHeatBetweenWrestlers(
      Long wrestler1Id, Long wrestler2Id, int heatGain, String reason) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return Optional.empty();
    }

    // Find or create rivalry
    Optional<Rivalry> rivalryOpt =
        rivalryRepository.findActiveRivalryBetween(wrestler1Opt.get(), wrestler2Opt.get());

    if (rivalryOpt.isEmpty()) {
      // Create new rivalry if none exists
      rivalryOpt = createRivalry(wrestler1Id, wrestler2Id, "Auto-generated from heat event");
    }

    return rivalryOpt.flatMap(rivalry -> addHeat(rivalry.getId(), heatGain, reason));
  }

  /** Attempt to resolve a rivalry with dice rolls. */
  public ResolutionResult attemptResolution(
      Long rivalryId, Integer wrestler1Roll, Integer wrestler2Roll) {
    Optional<Rivalry> rivalryOpt = rivalryRepository.findById(rivalryId);

    if (rivalryOpt.isEmpty()) {
      return new ResolutionResult(false, "Rivalry not found", null, 0, 0, 0);
    }

    Rivalry rivalry = rivalryOpt.get();

    if (!rivalry.canAttemptResolution()) {
      return new ResolutionResult(
          false,
          String.format(
              "Rivalry needs at least 20 heat to attempt resolution (current: %d)",
              rivalry.getHeat()),
          rivalry,
          0,
          0,
          0);
    }

    // Use provided rolls or generate random ones
    int roll1 = wrestler1Roll != null ? wrestler1Roll : random.nextInt(20) + 1;
    int roll2 = wrestler2Roll != null ? wrestler2Roll : random.nextInt(20) + 1;
    int total = roll1 + roll2;

    boolean resolved = rivalry.attemptResolution(roll1, roll2);

    if (resolved) {
      rivalryRepository.saveAndFlush(rivalry);
    }

    return new ResolutionResult(
        resolved,
        resolved ? "Rivalry resolved successfully" : "Resolution attempt failed",
        rivalry,
        roll1,
        roll2,
        total);
  }

  /** End a rivalry manually. */
  public Optional<Rivalry> endRivalry(Long rivalryId, String reason) {
    return rivalryRepository
        .findById(rivalryId)
        .filter(Rivalry::getIsActive)
        .map(
            rivalry -> {
              rivalry.endRivalry(reason);
              return rivalryRepository.saveAndFlush(rivalry);
            });
  }

  /** Get rivalry by ID. */
  @Transactional(readOnly = true)
  public Optional<Rivalry> getRivalryById(Long rivalryId) {
    return rivalryRepository.findById(rivalryId);
  }

  /** Get all rivalries with pagination. */
  @Transactional(readOnly = true)
  public Page<Rivalry> getAllRivalries(Pageable pageable) {
    return rivalryRepository.findAllBy(pageable);
  }

  /** Get active rivalries. */
  @Transactional(readOnly = true)
  public List<Rivalry> getActiveRivalries() {
    return rivalryRepository.findByIsActiveTrue();
  }

  /** Get rivalries for a specific wrestler. */
  @Transactional(readOnly = true)
  public List<Rivalry> getRivalriesForWrestler(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(rivalryRepository::findActiveRivalriesForWrestler)
        .orElse(List.of());
  }

  /** Get rivalries requiring matches (10+ heat). */
  @Transactional(readOnly = true)
  public List<Rivalry> getRivalriesRequiringMatches() {
    return rivalryRepository.findRivalriesRequiringMatches();
  }

  /** Get rivalries eligible for resolution (20+ heat). */
  @Transactional(readOnly = true)
  public List<Rivalry> getRivalriesEligibleForResolution() {
    return rivalryRepository.findRivalriesEligibleForResolution();
  }

  /** Get rivalries requiring stipulation matches (30+ heat). */
  @Transactional(readOnly = true)
  public List<Rivalry> getRivalriesRequiringStipulationMatches() {
    return rivalryRepository.findRivalriesRequiringStipulationMatches();
  }

  /** Get rivalries by intensity level. */
  @Transactional(readOnly = true)
  public List<Rivalry> getRivalriesByIntensity(RivalryIntensity intensity) {
    return rivalryRepository.findByHeatRange(
        intensity.getMinHeat(),
        intensity.getMaxHeat() == Integer.MAX_VALUE ? 999 : intensity.getMaxHeat());
  }

  /** Get the hottest rivalries. */
  @Transactional(readOnly = true)
  public List<Rivalry> getHottestRivalries(int limit) {
    return rivalryRepository.findHottestRivalries(
        org.springframework.data.domain.PageRequest.of(0, limit));
  }

  /** Update rivalry storyline notes. */
  public Optional<Rivalry> updateStorylineNotes(Long rivalryId, String storylineNotes) {
    return rivalryRepository
        .findById(rivalryId)
        .map(
            rivalry -> {
              rivalry.setStorylineNotes(storylineNotes);
              return rivalryRepository.saveAndFlush(rivalry);
            });
  }

  /** Get rivalry statistics. */
  @Transactional(readOnly = true)
  public RivalryStats getRivalryStats(Long rivalryId) {
    return rivalryRepository
        .findById(rivalryId)
        .map(
            rivalry ->
                new RivalryStats(
                    rivalry.getId(),
                    rivalry.getWrestler1().getName(),
                    rivalry.getWrestler2().getName(),
                    rivalry.getHeat(),
                    rivalry.getIntensity(),
                    rivalry.mustWrestleNextShow(),
                    rivalry.canAttemptResolution(),
                    rivalry.requiresStipulationMatch(),
                    rivalry.getDurationDays(),
                    rivalry.getIsActive(),
                    rivalry.getHeatEvents().size()))
        .orElse(null);
  }

  /** Get rivalry between two wrestlers. */
  @Transactional(readOnly = true)
  public Optional<Rivalry> getRivalryBetweenWrestlers(Long wrestler1Id, Long wrestler2Id) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return Optional.empty();
    }

    return rivalryRepository.findActiveRivalryBetween(wrestler1Opt.get(), wrestler2Opt.get());
  }

  /** Check if two wrestlers have rivalry history. */
  @Transactional(readOnly = true)
  public boolean hasRivalryHistory(Long wrestler1Id, Long wrestler2Id) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return false;
    }

    return rivalryRepository.hasRivalryHistory(wrestler1Opt.get(), wrestler2Opt.get());
  }

  /** Resolution result data class. */
  public record ResolutionResult(
      boolean resolved,
      String message,
      Rivalry rivalry,
      int wrestler1Roll,
      int wrestler2Roll,
      int totalRoll) {}

  /** Rivalry statistics data class. */
  public record RivalryStats(
      Long rivalryId,
      String wrestler1Name,
      String wrestler2Name,
      int heat,
      RivalryIntensity intensity,
      boolean mustWrestleNextShow,
      boolean canAttemptResolution,
      boolean requiresStipulationMatch,
      long durationDays,
      boolean isActive,
      int heatEventCount) {}
}
