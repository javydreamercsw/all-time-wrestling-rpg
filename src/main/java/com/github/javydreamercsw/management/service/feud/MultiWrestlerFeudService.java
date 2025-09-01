package com.github.javydreamercsw.management.service.feud;

import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing multi-wrestler feuds in the ATW RPG system. Handles feud creation,
 * participant management, and heat tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MultiWrestlerFeudService {

  private final MultiWrestlerFeudRepository multiWrestlerFeudRepository;
  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;

  /** Get all multi-wrestler feuds with pagination. */
  @Transactional(readOnly = true)
  public Page<MultiWrestlerFeud> getAllFeuds(Pageable pageable) {
    return multiWrestlerFeudRepository.findAllBy(pageable);
  }

  /** Get feud by ID. */
  @Transactional(readOnly = true)
  public Optional<MultiWrestlerFeud> getFeudById(Long id) {
    return multiWrestlerFeudRepository.findById(id);
  }

  /** Get feud by name. */
  @Transactional(readOnly = true)
  public Optional<MultiWrestlerFeud> getFeudByName(String name) {
    return multiWrestlerFeudRepository.findByName(name);
  }

  /** Get all active feuds. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getActiveFeuds() {
    return multiWrestlerFeudRepository.findByIsActiveTrue();
  }

  /** Get active feuds for a specific wrestler. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getActiveFeudsForWrestler(Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (wrestlerOpt.isEmpty()) {
      return List.of();
    }

    return multiWrestlerFeudRepository.findActiveFeudsForWrestler(wrestlerOpt.get());
  }

  /** Create a new multi-wrestler feud. */
  public Optional<MultiWrestlerFeud> createFeud(
      String name, String description, String storylineNotes) {
    // Check if feud name already exists
    if (multiWrestlerFeudRepository.existsByName(name)) {
      log.warn("Feud with name '{}' already exists", name);
      return Optional.empty();
    }

    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName(name);
    feud.setDescription(description);
    feud.setStorylineNotes(storylineNotes);
    feud.setHeat(0);
    feud.setIsActive(true);
    feud.setStartedDate(clock.instant());
    feud.setCreationDate(clock.instant());

    MultiWrestlerFeud savedFeud = multiWrestlerFeudRepository.saveAndFlush(feud);

    log.info("Created multi-wrestler feud: {} (ID: {})", savedFeud.getName(), savedFeud.getId());

    return Optional.of(savedFeud);
  }

  /** Add a participant to a feud. */
  public Optional<MultiWrestlerFeud> addParticipant(Long feudId, Long wrestlerId, FeudRole role) {
    Optional<MultiWrestlerFeud> feudOpt = multiWrestlerFeudRepository.findById(feudId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (feudOpt.isEmpty() || wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    MultiWrestlerFeud feud = feudOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    if (!feud.getIsActive()) {
      log.warn("Cannot add participant to inactive feud: {}", feud.getName());
      return Optional.empty();
    }

    if (feud.hasParticipant(wrestler)) {
      log.warn(
          "Wrestler {} is already participating in feud: {}", wrestler.getName(), feud.getName());
      return Optional.empty();
    }

    feud.addParticipant(wrestler, role != null ? role : FeudRole.NEUTRAL);
    MultiWrestlerFeud savedFeud = multiWrestlerFeudRepository.saveAndFlush(feud);

    log.info(
        "Added {} to feud {} as {} (now {} participants)",
        wrestler.getName(),
        feud.getName(),
        role,
        feud.getActiveParticipantCount());

    return Optional.of(savedFeud);
  }

  /** Remove a participant from a feud. */
  public Optional<MultiWrestlerFeud> removeParticipant(
      Long feudId, Long wrestlerId, String reason) {
    Optional<MultiWrestlerFeud> feudOpt = multiWrestlerFeudRepository.findById(feudId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (feudOpt.isEmpty() || wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    MultiWrestlerFeud feud = feudOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    if (!feud.hasParticipant(wrestler)) {
      log.warn("Wrestler {} is not participating in feud: {}", wrestler.getName(), feud.getName());
      return Optional.empty();
    }

    feud.removeParticipant(wrestler, reason);
    MultiWrestlerFeud savedFeud = multiWrestlerFeudRepository.saveAndFlush(feud);

    log.info(
        "Removed {} from feud {} (reason: {}, now {} participants)",
        wrestler.getName(),
        feud.getName(),
        reason,
        feud.getActiveParticipantCount());

    return Optional.of(savedFeud);
  }

  /** Add heat to a feud. */
  public Optional<MultiWrestlerFeud> addHeat(Long feudId, int heatGain, String reason) {
    return multiWrestlerFeudRepository
        .findById(feudId)
        .filter(feud -> feud.getIsActive())
        .map(
            feud -> {
              feud.addHeat(heatGain, reason);

              MultiWrestlerFeud savedFeud = multiWrestlerFeudRepository.saveAndFlush(feud);

              log.info(
                  "Added {} heat to feud {} (total: {}, reason: {})",
                  heatGain,
                  feud.getName(),
                  feud.getHeat(),
                  reason);

              return savedFeud;
            });
  }

  /** End a feud. */
  public Optional<MultiWrestlerFeud> endFeud(Long feudId, String reason) {
    Optional<MultiWrestlerFeud> feudOpt = multiWrestlerFeudRepository.findById(feudId);

    if (feudOpt.isEmpty()) {
      return Optional.empty();
    }

    MultiWrestlerFeud feud = feudOpt.get();

    if (!feud.getIsActive()) {
      return Optional.of(feud); // Already ended
    }

    feud.endFeud(reason);
    MultiWrestlerFeud savedFeud = multiWrestlerFeudRepository.saveAndFlush(feud);

    log.info("Ended feud: {} (reason: {})", feud.getName(), reason);

    return Optional.of(savedFeud);
  }

  /** Get feuds requiring matches at next show. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getFeudsRequiringMatches() {
    return multiWrestlerFeudRepository.findFeudsRequiringMatches();
  }

  /** Get feuds eligible for resolution. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getFeudsEligibleForResolution() {
    return multiWrestlerFeudRepository.findFeudsEligibleForResolution();
  }

  /** Get feuds requiring stipulation matches. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getFeudsRequiringStipulationMatches() {
    return multiWrestlerFeudRepository.findFeudsRequiringStipulationMatches();
  }

  /** Get hottest feuds. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getHottestFeuds(int limit) {
    return multiWrestlerFeudRepository.findHottestFeuds(Pageable.ofSize(limit));
  }

  /** Get valid multi-wrestler feuds (3+ participants). */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getValidMultiWrestlerFeuds() {
    return multiWrestlerFeudRepository.findValidMultiWrestlerFeuds();
  }

  /** Get feuds with both protagonists and antagonists. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getFeudsWithProtagonistsAndAntagonists() {
    return multiWrestlerFeudRepository.findFeudsWithProtagonistsAndAntagonists();
  }

  /** Get feuds with wild card participants. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getFeudsWithWildCards() {
    return multiWrestlerFeudRepository.findFeudsWithWildCards();
  }

  /** Get largest feuds by participant count. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getLargestFeuds(int limit) {
    return multiWrestlerFeudRepository.findLargestFeuds(Pageable.ofSize(limit));
  }

  /** Get inter-faction feuds. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getInterFactionFeuds() {
    return multiWrestlerFeudRepository.findInterFactionFeuds();
  }

  /** Get independent wrestler feuds (no faction members). */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getIndependentWrestlerFeuds() {
    return multiWrestlerFeudRepository.findIndependentWrestlerFeuds();
  }

  /** Get feuds by participant count range. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getFeudsByParticipantCount(
      int minParticipants, int maxParticipants) {
    return multiWrestlerFeudRepository.findByParticipantCountRange(
        minParticipants, maxParticipants);
  }

  /** Get feuds with specific role. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getFeudsWithRole(FeudRole role) {
    return multiWrestlerFeudRepository.findFeudsWithRole(role);
  }

  /** Count active feuds for a wrestler. */
  @Transactional(readOnly = true)
  public long countActiveFeudsForWrestler(Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (wrestlerOpt.isEmpty()) {
      return 0;
    }

    return multiWrestlerFeudRepository.countActiveFeudsForWrestler(wrestlerOpt.get());
  }

  /** Get recent feuds. */
  @Transactional(readOnly = true)
  public List<MultiWrestlerFeud> getRecentFeuds(int days) {
    Instant sinceDate = clock.instant().minusSeconds(days * 24L * 3600L);
    return multiWrestlerFeudRepository.findRecentFeuds(sinceDate);
  }

  /** Check if a feud is valid (has 3+ participants). */
  @Transactional(readOnly = true)
  public boolean isValidMultiWrestlerFeud(Long feudId) {
    Optional<MultiWrestlerFeud> feudOpt = multiWrestlerFeudRepository.findById(feudId);

    if (feudOpt.isEmpty()) {
      return false;
    }

    return feudOpt.get().isValidMultiWrestlerFeud();
  }

  /** Get feud statistics. */
  @Transactional(readOnly = true)
  public FeudStatistics getFeudStatistics() {
    List<MultiWrestlerFeud> activeFeuds = getActiveFeuds();
    List<MultiWrestlerFeud> validFeuds = getValidMultiWrestlerFeuds();

    int totalActiveFeuds = activeFeuds.size();
    int validMultiWrestlerFeuds = validFeuds.size();
    int totalParticipants =
        activeFeuds.stream().mapToInt(MultiWrestlerFeud::getActiveParticipantCount).sum();

    double averageParticipants =
        totalActiveFeuds > 0 ? (double) totalParticipants / totalActiveFeuds : 0.0;

    return new FeudStatistics(
        totalActiveFeuds, validMultiWrestlerFeuds, totalParticipants, averageParticipants);
  }

  /** Record class for feud statistics. */
  public record FeudStatistics(
      int totalActiveFeuds,
      int validMultiWrestlerFeuds,
      int totalParticipants,
      double averageParticipants) {}
}
