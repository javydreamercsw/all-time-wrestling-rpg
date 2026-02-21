/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.faction;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final Clock clock; // Injected via constructor

  /** Get all factions. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> findAll() {
    return factionRepository.findAll();
  }

  /** Get all factions (alias for findAll for UI compatibility). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> getAllFactions() {
    return findAll();
  }

  /** Get all factions with members eagerly loaded for UI display. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> findAllWithMembers() {
    return factionRepository.findAllWithMembers();
  }

  /** Get all factions with both members and teams eagerly loaded for UI display. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> findAllWithMembersAndTeams() {
    return factionRepository.findAllWithMembersAndTeams();
  }

  /** Get all factions with pagination. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Faction> getAllFactions(Pageable pageable) {
    return factionRepository.findAllBy(pageable);
  }

  /** Get faction by ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Faction> getFactionById(Long id) {
    return factionRepository.findById(id);
  }

  /** Get faction by ID with members eagerly loaded. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Faction> getFactionByIdWithMembers(Long id) {
    return factionRepository.findByIdWithMembers(id);
  }

  /** Get faction by name. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Faction> getFactionByName(String name) {
    return factionRepository.findByName(name);
  }

  /** Get faction by external ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Faction> findByExternalId(String externalId) {
    return factionRepository.findByExternalId(externalId);
  }

  /** Get all active factions. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> getActiveFactions() {
    return factionRepository.findByIsActiveTrue();
  }

  /** Create a new faction. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Faction> createFaction(@NonNull String name, String description, Long leaderId) {
    // Check if faction name already exists
    if (factionRepository.existsByName(name)) {
      log.warn("Faction with name '{}' already exists", name);
      return Optional.empty();
    }

    Faction faction = Faction.builder().build();
    faction.setName(name);
    faction.setDescription(description);
    faction.setActive(true);
    faction.setFormedDate(clock.instant());
    faction.setCreationDate(clock.instant());

    // Set leader if provided
    Optional<Wrestler> leaderOpt = wrestlerRepository.findById(leaderId);
    if (leaderOpt.isPresent()) {
      Wrestler leader = leaderOpt.get();
      faction.setLeader(leader);
      // Add leader as first member
      faction.addMember(leader);
    } else {
      log.warn("Leader with ID {} not found for faction '{}'", leaderId, name);
    }

    Faction savedFaction = factionRepository.saveAndFlush(faction);
    log.info(
        "Created faction: {} with {} members",
        savedFaction.getName(),
        savedFaction.getMemberCount());

    return Optional.of(savedFaction);
  }

  /** Add a member to a faction. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Faction> addMemberToFaction(@NonNull Long factionId, @NonNull Long wrestlerId) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (factionOpt.isEmpty() || wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction = factionOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    if (!faction.isActive()) {
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
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Faction> removeMemberFromFaction(
      @NonNull Long factionId, @NonNull Long wrestlerId, @NonNull String reason) {
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
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Faction> changeFactionLeader(@NonNull Long factionId, @NonNull Long newLeaderId) {
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
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Faction> disbandFaction(@NonNull Long factionId, @NonNull String reason) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);

    if (factionOpt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction = factionOpt.get();

    if (!faction.isActive()) {
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
  @PreAuthorize("isAuthenticated()")
  public Optional<Faction> getFactionForWrestler(@NonNull Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    return factionRepository.findActiveFactionByMember(wrestlerOpt.get());
  }

  /** Get factions with active rivalries. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> getFactionsWithActiveRivalries() {
    return factionRepository.findFactionsWithActiveRivalries();
  }

  /** Get factions by type (singles, tag team, stable). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> getFactionsByType(@NonNull String type) {
    return switch (type.toLowerCase()) {
      case "singles" -> factionRepository.findSinglesFactions();
      case "tag", "tagteam", "tag_team" -> factionRepository.findTagTeamFactions();
      case "stable" -> factionRepository.findStableFactions();
      default -> List.of();
    };
  }

  /** Get largest active factions. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> getLargestFactions(int limit) {
    return factionRepository.findLargestFactions(Pageable.ofSize(limit));
  }

  /** Check if two factions can have a rivalry. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean canHaveRivalry(@NonNull Long faction1Id, @NonNull Long faction2Id) {
    Optional<Faction> faction1Opt = factionRepository.findById(faction1Id);
    Optional<Faction> faction2Opt = factionRepository.findById(faction2Id);

    if (faction1Opt.isEmpty() || faction2Opt.isEmpty()) {
      return false;
    }

    Faction faction1 = faction1Opt.get();
    Faction faction2 = faction2Opt.get();

    // Both factions must be active
    if (!faction1.isActive() || !faction2.isActive()) {
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
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Faction save(@NonNull Faction faction) {
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
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void delete(@NonNull Faction faction) {
    log.info("Deleting faction: {}", faction.getName());
    factionRepository.delete(faction);
  }

  /** Delete a faction by ID. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void deleteById(@NonNull Long id) {
    log.info("Deleting faction with ID: {}", id);
    factionRepository.deleteById(id);
  }

  /** Check if a faction exists by ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean existsById(@NonNull Long id) {
    return factionRepository.existsById(id);
  }

  /** Count all factions. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long count() {
    return factionRepository.count();
  }

  /** Add affinity points to a faction. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void addAffinity(@NonNull Long factionId, int points) {
    factionRepository
        .findById(factionId)
        .ifPresent(
            faction -> {
              int oldAffinity = faction.getAffinity();
              faction.setAffinity(oldAffinity + points);
              factionRepository.saveAndFlush(faction);
              log.info(
                  "Added {} affinity to faction: {} (Total: {})",
                  points,
                  faction.getName(),
                  faction.getAffinity());
            });
  }
}
