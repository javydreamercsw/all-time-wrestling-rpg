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

import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
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
@Slf4j
@Transactional
public class FactionService {

  private final FactionRepository factionRepository;
  private final WrestlerRepository wrestlerRepository;
  private final UniverseRepository universeRepository;
  private final com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository
      wrestlerStateRepository;
  private final ExpansionService expansionService;
  private final Clock clock;
  private final DefaultImageService imageService;

  @org.springframework.beans.factory.annotation.Autowired
  public FactionService(
      FactionRepository factionRepository,
      WrestlerRepository wrestlerRepository,
      UniverseRepository universeRepository,
      com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository
          wrestlerStateRepository,
      ExpansionService expansionService,
      Clock clock,
      DefaultImageService imageService) {
    this.factionRepository = factionRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.universeRepository = universeRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.expansionService = expansionService;
    this.clock = clock;
    this.imageService = imageService;
  }

  /** Get all factions. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> findAll() {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return factionRepository.findAll().stream()
        .filter(
            faction ->
                faction.getMembers().stream()
                    .allMatch(
                        member ->
                            enabledExpansions.contains(member.getWrestler().getExpansionCode())))
        .peek(
            faction -> {
              if (faction.getManager() != null
                  && !enabledExpansions.contains(faction.getManager().getExpansionCode())) {
                faction.setManager(null);
              }
            })
        .collect(Collectors.toList());
  }

  /** Get all factions for a specific universe. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> findAllByUniverse(@NonNull Long universeId) {
    Universe universe = universeRepository.findById(universeId).orElseThrow();
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return factionRepository.findByUniverse(universe).stream()
        .filter(
            faction ->
                faction.getMembers().stream()
                    .allMatch(
                        member ->
                            enabledExpansions.contains(member.getWrestler().getExpansionCode())))
        .collect(Collectors.toList());
  }

  /** Get all factions (alias for findAll for UI compatibility). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> getAllFactions() {
    return findAll();
  }

  /** Get all factions with pagination. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Faction> getAllFactions(Pageable pageable) {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();

    List<Faction> allFiltered =
        factionRepository.findAll().stream()
            .filter(
                faction ->
                    faction.getMembers().stream()
                        .allMatch(
                            member ->
                                enabledExpansions.contains(
                                    member.getWrestler().getExpansionCode())))
            .collect(Collectors.toList());

    if (pageable.isUnpaged()) {
      return new org.springframework.data.domain.PageImpl<>(
          allFiltered, pageable, allFiltered.size());
    }

    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), allFiltered.size());

    List<Faction> pageContent = new java.util.ArrayList<>();
    if (start < allFiltered.size()) {
      pageContent = allFiltered.subList(start, end);
    }

    return new org.springframework.data.domain.PageImpl<>(
        pageContent, pageable, allFiltered.size());
  }

  /** Get faction by ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Faction> getFactionById(Long id) {
    return factionRepository.findById(id);
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
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return factionRepository.findByIsActiveTrue().stream()
        .filter(
            faction ->
                faction.getMembers().stream()
                    .allMatch(
                        member ->
                            enabledExpansions.contains(member.getWrestler().getExpansionCode())))
        .collect(Collectors.toList());
  }

  /** Create a new faction in a universe. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Faction> createFaction(
      @NonNull String name, String description, Long leaderId, @NonNull Long universeId) {
    if (factionRepository.existsByName(name)) {
      log.warn("Faction with name '{}' already exists", name);
      return Optional.empty();
    }

    Universe universe = universeRepository.findById(universeId).orElseThrow();

    Faction faction = Faction.builder().build();
    faction.setName(name);
    faction.setDescription(description);
    faction.setActive(true);
    faction.setFormedDate(clock.instant());
    faction.setCreationDate(clock.instant());
    faction.setUniverse(universe);

    Optional<Wrestler> leaderOpt = wrestlerRepository.findById(leaderId);
    if (leaderOpt.isPresent()) {
      Wrestler leader = leaderOpt.get();
      faction.setLeader(leader);
      com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
          wrestlerStateRepository
              .findByWrestlerIdAndUniverseId(leader.getId(), universeId)
              .orElseGet(
                  () ->
                      wrestlerStateRepository.save(
                          com.github.javydreamercsw.management.domain.wrestler.WrestlerState
                              .builder()
                              .wrestler(leader)
                              .universe(universe)
                              .tier(
                                  com.github.javydreamercsw.base.domain.wrestler.WrestlerTier
                                      .ROOKIE)
                              .build()));
      faction.addMember(state);
    }

    Faction savedFaction = factionRepository.saveAndFlush(faction);
    log.info(
        "Created faction: {} with {} members in universe {}",
        savedFaction.getName(),
        savedFaction.getMemberCount(),
        universeId);

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

    if (faction.getUniverse() == null) {
      return Optional.empty();
    }

    Long universeId = faction.getUniverse().getId();

    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        wrestlerStateRepository.findByWrestlerIdAndUniverseId(wrestlerId, universeId).orElseThrow();

    faction.addMember(state);
    return Optional.of(factionRepository.saveAndFlush(faction));
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

    if (faction.getUniverse() == null) return Optional.empty();
    Long universeId = faction.getUniverse().getId();

    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        wrestlerStateRepository.findByWrestlerIdAndUniverseId(wrestlerId, universeId).orElseThrow();

    faction.removeMember(state);
    if (wrestler.equals(faction.getLeader())) {
      faction.setLeader(null);
    }

    return Optional.of(factionRepository.saveAndFlush(faction));
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

    faction.setLeader(newLeader);
    return Optional.of(factionRepository.saveAndFlush(faction));
  }

  /** Disband a faction. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Faction> disbandFaction(@NonNull Long factionId, @NonNull String reason) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);

    if (factionOpt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction = factionOpt.get();
    faction.disband(reason);
    return Optional.of(factionRepository.saveAndFlush(faction));
  }

  /** Get faction for a wrestler. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Faction> getFactionForWrestler(@NonNull Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    return wrestlerOpt.flatMap(wrestler -> factionRepository.findActiveFactionByMember(wrestler));
  }

  /** Get factions with active rivalries. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Faction> getFactionsWithActiveRivalries() {
    return factionRepository.findFactionsWithActiveRivalries();
  }

  /** Get factions by type. */
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

  /** Save a faction. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Faction save(@NonNull Faction faction) {
    if (faction.getId() == null) {
      faction.setCreationDate(clock.instant());
      if (faction.getFormedDate() == null) {
        faction.setFormedDate(clock.instant());
      }
    }
    return factionRepository.saveAndFlush(faction);
  }

  /** Delete a faction by ID. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void deleteById(@NonNull Long id) {
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
              int newAffinity = Math.min(100, oldAffinity + points);
              faction.setAffinity(newAffinity);
              factionRepository.saveAndFlush(faction);
            });
  }

  /** Gets the shared faction affinity between two wrestlers. */
  @Transactional(readOnly = true)
  public int getAffinityBetween(Wrestler w1, Wrestler w2) {
    if (w1 == null || w2 == null) {
      return 0;
    }
    Optional<Faction> f1 = factionRepository.findActiveFactionByMember(w1);
    Optional<Faction> f2 = factionRepository.findActiveFactionByMember(w2);

    if (f1.isPresent() && f2.isPresent() && f1.get().equals(f2.get())) {
      return f1.get().getAffinity();
    }
    return 0;
  }

  /** Resolves the image URL for a faction. */
  public String resolveFactionImage(Faction faction) {
    if (faction.getImageUrl() != null && !faction.getImageUrl().isBlank()) {
      return faction.getImageUrl();
    }
    return imageService.resolveImage(faction.getName(), ImageCategory.FACTION).url();
  }
}
