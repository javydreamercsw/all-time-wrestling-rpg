package com.github.javydreamercsw.management.service.faction;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing factions in the ATW RPG system. Handles faction creation, member management,
 * and faction operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FactionService {

  private final FactionRepository factionRepository;
  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;

  /** Get all factions. */
  @Transactional(readOnly = true)
  public List<Faction> findAll() {
    return factionRepository.findAll();
  }

  /** Get all factions (alias for findAll for UI compatibility). */
  @Transactional(readOnly = true)
  public List<Faction> getAllFactions() {
    return findAll();
  }

  /** Get all factions with members eagerly loaded for UI display. */
  @Transactional(readOnly = true)
  public List<Faction> findAllWithMembers() {
    return factionRepository.findAllWithMembers();
  }

  /** Get all factions with pagination. */
  @Transactional(readOnly = true)
  public Page<Faction> getAllFactions(Pageable pageable) {
    return factionRepository.findAllBy(pageable);
  }

  /** Get faction by ID. */
  @Transactional(readOnly = true)
  public Optional<Faction> getFactionById(Long id) {
    return factionRepository.findById(id);
  }

  /** Get faction by ID with members eagerly loaded. */
  @Transactional(readOnly = true)
  public Optional<Faction> getFactionByIdWithMembers(Long id) {
    return factionRepository.findByIdWithMembers(id);
  }

  /** Get faction by name. */
  @Transactional(readOnly = true)
  public Optional<Faction> getFactionByName(String name) {
    return factionRepository.findByName(name);
  }

  /** Get all active factions. */
  @Transactional(readOnly = true)
  public List<Faction> getActiveFactions() {
    return factionRepository.findByIsActiveTrue();
  }

  /** Create a new faction. */
  public Optional<Faction> createFaction(String name, String description, Long leaderId) {
    // Check if faction name already exists
    if (factionRepository.existsByName(name)) {
      log.warn("Faction with name '{}' already exists", name);
      return Optional.empty();
    }

    Faction faction = new Faction();
    faction.setName(name);
    faction.setDescription(description);
    faction.setIsActive(true);
    faction.setFormedDate(clock.instant());
    faction.setCreationDate(clock.instant());

    // Set leader if provided
    if (leaderId != null) {
      Optional<Wrestler> leaderOpt = wrestlerRepository.findById(leaderId);
      if (leaderOpt.isPresent()) {
        Wrestler leader = leaderOpt.get();
        faction.setLeader(leader);
        // Add leader as first member
        faction.addMember(leader);
      } else {
        log.warn("Leader with ID {} not found for faction '{}'", leaderId, name);
      }
    }

    Faction savedFaction = factionRepository.saveAndFlush(faction);
    log.info(
        "Created faction: {} with {} members",
        savedFaction.getName(),
        savedFaction.getMemberCount());

    return Optional.of(savedFaction);
  }

  /** Add a member to a faction. */
  public Optional<Faction> addMemberToFaction(Long factionId, Long wrestlerId) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (factionOpt.isEmpty() || wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction = factionOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    if (!faction.getIsActive()) {
      log.warn("Cannot add member to inactive faction: {}", faction.getName());
      return Optional.empty();
    }

    // Check if wrestler is already in another faction
    Optional<Faction> currentFaction = factionRepository.findActiveFactionByMember(wrestler);
    if (currentFaction.isPresent() && !currentFaction.get().equals(faction)) {
      log.warn(
          "Wrestler {} is already in faction: {}",
          wrestler.getName(),
          currentFaction.get().getName());
      return Optional.empty();
    }

    faction.addMember(wrestler);
    Faction savedFaction = factionRepository.saveAndFlush(faction);

    log.info(
        "Added {} to faction: {} (now {} members)",
        wrestler.getName(),
        faction.getName(),
        faction.getMemberCount());

    return Optional.of(savedFaction);
  }

  /** Remove a member from a faction. */
  public Optional<Faction> removeMemberFromFaction(Long factionId, Long wrestlerId, String reason) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (factionOpt.isEmpty() || wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction = factionOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    if (!faction.hasMember(wrestler)) {
      log.warn("Wrestler {} is not a member of faction: {}", wrestler.getName(), faction.getName());
      return Optional.empty();
    }

    faction.removeMember(wrestler);

    // If removing the leader, clear the leader
    if (wrestler.equals(faction.getLeader())) {
      faction.setLeader(null);
    }

    Faction savedFaction = factionRepository.saveAndFlush(faction);

    log.info(
        "Removed {} from faction: {} (reason: {}, now {} members)",
        wrestler.getName(),
        faction.getName(),
        reason,
        faction.getMemberCount());

    return Optional.of(savedFaction);
  }

  /** Change faction leader. */
  public Optional<Faction> changeFactionLeader(Long factionId, Long newLeaderId) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    Optional<Wrestler> newLeaderOpt = wrestlerRepository.findById(newLeaderId);

    if (factionOpt.isEmpty() || newLeaderOpt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction = factionOpt.get();
    Wrestler newLeader = newLeaderOpt.get();

    if (!faction.hasMember(newLeader)) {
      log.warn(
          "Cannot make {} leader of faction {} - not a member",
          newLeader.getName(),
          faction.getName());
      return Optional.empty();
    }

    Wrestler oldLeader = faction.getLeader();
    faction.setLeader(newLeader);

    Faction savedFaction = factionRepository.saveAndFlush(faction);

    log.info(
        "Changed leader of faction {} from {} to {}",
        faction.getName(),
        oldLeader != null ? oldLeader.getName() : "None",
        newLeader.getName());

    return Optional.of(savedFaction);
  }

  /** Disband a faction. */
  public Optional<Faction> disbandFaction(Long factionId, String reason) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);

    if (factionOpt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction = factionOpt.get();

    if (!faction.getIsActive()) {
      log.warn("Faction {} is already disbanded", faction.getName());
      return Optional.of(faction);
    }

    faction.disband(reason);
    Faction savedFaction = factionRepository.saveAndFlush(faction);

    log.info("Disbanded faction: {} (reason: {})", faction.getName(), reason);

    return Optional.of(savedFaction);
  }

  /** Get faction for a wrestler. */
  @Transactional(readOnly = true)
  public Optional<Faction> getFactionForWrestler(Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    return factionRepository.findActiveFactionByMember(wrestlerOpt.get());
  }

  /** Get factions with active rivalries. */
  @Transactional(readOnly = true)
  public List<Faction> getFactionsWithActiveRivalries() {
    return factionRepository.findFactionsWithActiveRivalries();
  }

  /** Get factions by type (singles, tag team, stable). */
  @Transactional(readOnly = true)
  public List<Faction> getFactionsByType(String type) {
    return switch (type.toLowerCase()) {
      case "singles" -> factionRepository.findSinglesFactions();
      case "tag", "tagteam", "tag_team" -> factionRepository.findTagTeamFactions();
      case "stable" -> factionRepository.findStableFactions();
      default -> List.of();
    };
  }

  /** Get largest active factions. */
  @Transactional(readOnly = true)
  public List<Faction> getLargestFactions(int limit) {
    return factionRepository.findLargestFactions(Pageable.ofSize(limit));
  }

  /** Check if two factions can have a rivalry. */
  @Transactional(readOnly = true)
  public boolean canHaveRivalry(Long faction1Id, Long faction2Id) {
    Optional<Faction> faction1Opt = factionRepository.findById(faction1Id);
    Optional<Faction> faction2Opt = factionRepository.findById(faction2Id);

    if (faction1Opt.isEmpty() || faction2Opt.isEmpty()) {
      return false;
    }

    Faction faction1 = faction1Opt.get();
    Faction faction2 = faction2Opt.get();

    // Both factions must be active
    if (!faction1.getIsActive() || !faction2.getIsActive()) {
      return false;
    }

    // Cannot have rivalry with self
    if (faction1.equals(faction2)) {
      return false;
    }

    // Both factions must have at least one member
    return faction1.getMemberCount() > 0 && faction2.getMemberCount() > 0;
  }

  /** Save a faction. */
  public Faction save(Faction faction) {
    if (faction.getId() == null) {
      // New faction
      faction.setCreationDate(clock.instant());
      if (faction.getFormedDate() == null) {
        faction.setFormedDate(clock.instant());
      }
    }
    return factionRepository.saveAndFlush(faction);
  }

  /** Delete a faction. */
  public void delete(Faction faction) {
    log.info("Deleting faction: {}", faction.getName());
    factionRepository.delete(faction);
  }

  /** Delete a faction by ID. */
  public void deleteById(Long id) {
    log.info("Deleting faction with ID: {}", id);
    factionRepository.deleteById(id);
  }

  /** Check if a faction exists by ID. */
  @Transactional(readOnly = true)
  public boolean existsById(Long id) {
    return factionRepository.existsById(id);
  }

  /** Count all factions. */
  @Transactional(readOnly = true)
  public long count() {
    return factionRepository.count();
  }
}
