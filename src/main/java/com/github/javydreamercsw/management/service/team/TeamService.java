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
package com.github.javydreamercsw.management.service.team;

import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamMember;
import com.github.javydreamercsw.management.domain.team.TeamOrigin;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.expansion.ExpansionToggledEvent;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing teams (tag teams) in the ATW RPG system. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamService {

  @Autowired private TeamRepository teamRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private NpcRepository npcRepository;
  @Autowired private ExpansionService expansionService;
  @Autowired private UniverseContextService universeContextService;
  @Autowired private UniverseSettingsService universeSettingsService;
  @Autowired private DefaultImageService imageService;

  private Set<String> enabledExpansionCodes() {
    return universeContextService
        .getCurrentUniverse()
        .map(universeSettingsService::getEnabledExpansionCodesForUniverse)
        .orElseGet(() -> new HashSet<>(expansionService.getEnabledExpansionCodes()));
  }

  // ==================== CRUD OPERATIONS ====================

  /** Get all teams with pagination. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Team> getAllTeams(@NonNull final Pageable pageable) {
    Set<String> enabledExpansions = enabledExpansionCodes();

    // Fetch all since we need to filter based on member properties not in the Team table directly
    // and then manually paginate.
    List<Team> allFiltered =
        teamRepository.findAll().stream()
            .filter(
                team ->
                    enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                        && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
            .peek(
                team -> {
                  // Hide manager if their expansion is disabled
                  if (team.getManager() != null
                      && !enabledExpansions.contains(team.getManager().getExpansionCode())) {
                    team.setManager(null);
                  }
                })
            .collect(Collectors.toList());

    if (pageable.isUnpaged()) {
      return new PageImpl<>(allFiltered, pageable, allFiltered.size());
    }

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), allFiltered.size());

    List<Team> pageContent = new ArrayList<>();
    if (start < allFiltered.size()) {
      pageContent = allFiltered.subList(start, end);
    }

    return new PageImpl<>(pageContent, pageable, allFiltered.size());
  }

  /** Count all teams. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countAllTeams() {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return teamRepository.findAll().stream()
        .filter(
            team ->
                enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                    && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
        .count();
  }

  /** Count all teams. */
  @Transactional(readOnly = true)
  public long count() {
    return teamRepository.count();
  }

  /** Get team by ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Team> getTeamById(final Long id) {
    return teamRepository.findById(id);
  }

  /** Get team by name. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Team> getTeamByName(final String name) {
    return teamRepository.findByName(name);
  }

  /**
   * Create a new duo team from two wrestler IDs. Delegates to {@link #createTeam(String, String,
   * List, TeamSize, Long, Long, TeamOrigin)}.
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<Team> createTeam(
      final String name,
      final String description,
      final Long wrestler1Id,
      final Long wrestler2Id,
      final Long factionId,
      final Long managerId) {
    if (wrestler1Id == null || wrestler2Id == null) {
      log.debug("Cannot create team: Wrestler IDs cannot be null");
      return Optional.empty();
    }
    return createTeam(
        name,
        description,
        List.of(wrestler1Id, wrestler2Id),
        TeamSize.DUO,
        factionId,
        managerId,
        TeamOrigin.REGISTRY);
  }

  /**
   * Create a new team from an ordered member-ID list.
   *
   * @param name unique team name
   * @param description optional description
   * @param memberIds ordered wrestler IDs (slot 0..n-1)
   * @param teamSize declared size; must match {@code memberIds.size()}
   * @param factionId optional faction
   * @param managerId optional manager (NPC)
   * @param origin where this team came from (registry seed vs one-off assembly)
   * @return the saved team, or empty when validation fails
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<Team> createTeam(
      final String name,
      final String description,
      final List<Long> memberIds,
      final TeamSize teamSize,
      final Long factionId,
      final Long managerId,
      final TeamOrigin origin) {
    if (memberIds == null || memberIds.isEmpty() || memberIds.stream().anyMatch(Objects::isNull)) {
      log.debug("Cannot create team: Member IDs must be provided and non-null");
      return Optional.empty();
    }
    if (teamSize == null || memberIds.size() != teamSize.getMemberCount()) {
      log.debug(
          "Cannot create team: Member count {} does not match declared team size {}",
          memberIds.size(),
          teamSize);
      return Optional.empty();
    }

    // Validate wrestlers exist
    List<Wrestler> members = new ArrayList<>();
    for (Long memberId : memberIds) {
      Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(memberId);
      if (wrestlerOpt.isEmpty()) {
        log.debug("Cannot create team: Wrestler not found (ID: {})", memberId);
        return Optional.empty();
      }
      members.add(wrestlerOpt.get());
    }

    // Check for duplicate members
    Set<Long> distinctIds = new HashSet<>(memberIds);
    if (distinctIds.size() != memberIds.size()) {
      log.debug("Cannot create team: Duplicate member in list {}", memberIds);
      return Optional.empty();
    }

    // Check if team name already exists
    if (teamRepository.existsByName(name)) {
      log.debug("Cannot create team: Name '{}' already exists", name);
      return Optional.empty();
    }

    // Check if an active team with the same member set already exists
    Optional<Team> existingTeam = findActiveTeamByMembers(members);
    if (existingTeam.isPresent()) {
      log.debug(
          "Cannot create team: Active team '{}' already has the same members",
          existingTeam.get().getName());
      return Optional.empty();
    }

    Team team = new Team();
    team.setName(name);
    team.setDescription(description);
    team.setMembers(members);
    team.setTeamSize(teamSize);
    team.setOrigin(origin != null ? origin : TeamOrigin.REGISTRY);
    team.setStatus(TeamStatus.ACTIVE);

    // Set faction if provided
    if (factionId != null) {
      team.setFaction(factionRepository.findById(factionId).orElse(null));
    }

    // Set manager if provided
    if (managerId != null) {
      team.setManager(npcRepository.findById(managerId).orElse(null));
    }

    Team savedTeam = teamRepository.saveAndFlush(team);
    log.debug("Created team: {} with members {}", savedTeam.getName(), savedTeam.getMemberNames());

    return Optional.of(savedTeam);
  }

  /**
   * Finds an active team whose member set matches the given wrestlers exactly, regardless of order
   * or declared size.
   *
   * @param members the wrestlers to match
   * @return the matching active team, if any
   */
  private Optional<Team> findActiveTeamByMembers(final List<Wrestler> members) {
    Set<Long> memberIds = members.stream().map(Wrestler::getId).collect(Collectors.toSet());
    return teamRepository.findByWrestlerAndStatus(members.getFirst(), TeamStatus.ACTIVE).stream()
        .filter(
            team -> {
              List<TeamMember> teamMembers = team.getMembers();
              if (teamMembers.size() != members.size()) {
                return false;
              }
              Set<Long> teamMemberIds =
                  teamMembers.stream()
                      .map(TeamMember::getWrestler)
                      .map(Wrestler::getId)
                      .collect(Collectors.toSet());
              return teamMemberIds.equals(memberIds);
            })
        .findFirst();
  }

  /** Update an existing team. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<Team> updateTeam(
      final Long teamId,
      final String name,
      final String description,
      final TeamStatus status,
      final Long factionId,
      final Long managerId) {
    Optional<Team> teamOpt = teamRepository.findById(teamId);
    if (teamOpt.isEmpty()) {
      return Optional.empty();
    }

    Team team = teamOpt.get();

    // Check if new name conflicts with existing team (excluding current team)
    if (name != null && !name.equals(team.getName()) && teamRepository.existsByName(name)) {
      log.debug("Cannot update team: Name '{}' already exists", name);
      return Optional.empty();
    }

    if (name != null) {
      team.setName(name);
    }
    if (description != null) {
      team.setDescription(description);
    }
    if (status != null) {
      team.setStatus(status);
      if (status == TeamStatus.DISBANDED) {
        team.disband();
      } else if (status == TeamStatus.ACTIVE && team.getDisbandedDate() != null) {
        team.reactivate();
      }
    }

    // Set manager if provided
    if (managerId != null) {
      team.setManager(npcRepository.findById(managerId).orElse(null));
    }

    Team savedTeam = teamRepository.saveAndFlush(team);
    log.debug("Updated team: {}", savedTeam.getName());

    return Optional.of(savedTeam);
  }

  /** Delete a team. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public boolean deleteTeam(final Long teamId) {
    if (!teamRepository.existsById(teamId)) {
      return false;
    }

    teamRepository.deleteById(teamId);
    log.info("Deleted team with ID: {}", teamId);
    return true;
  }

  // ==================== QUERY OPERATIONS ====================

  /** Get all active teams. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Team> getActiveTeams() {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return teamRepository.findByStatus(TeamStatus.ACTIVE).stream()
        .filter(
            team ->
                enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                    && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
        .peek(
            team -> {
              // Hide manager if their expansion is disabled
              if (team.getManager() != null
                  && !enabledExpansions.contains(team.getManager().getExpansionCode())) {
                team.setManager(null);
              }
            })
        .collect(Collectors.toList());
  }

  @EventListener
  public void onExpansionToggled(final ExpansionToggledEvent event) {
    log.info("Expansion '{}' toggled, clear team caches if any.", event.getExpansionCode());
  }

  /** Get teams by faction. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Team> getTeamsByFaction(final Faction faction) {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return teamRepository.findByFaction(faction).stream()
        .filter(
            team ->
                enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                    && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
        .peek(
            team -> {
              if (team.getManager() != null
                  && !enabledExpansions.contains(team.getManager().getExpansionCode())) {
                team.setManager(null);
              }
            })
        .collect(Collectors.toList());
  }

  /** Get teams where a wrestler is a member. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Team> getTeamsByWrestler(final Wrestler wrestler) {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return teamRepository.findByWrestler(wrestler).stream()
        .filter(
            team ->
                enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                    && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
        .peek(
            team -> {
              if (team.getManager() != null
                  && !enabledExpansions.contains(team.getManager().getExpansionCode())) {
                team.setManager(null);
              }
            })
        .collect(Collectors.toList());
  }

  /** Get active teams where a wrestler is a member. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Team> getActiveTeamsByWrestler(final Wrestler wrestler) {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return teamRepository.findByWrestlerAndStatus(wrestler, TeamStatus.ACTIVE).stream()
        .filter(
            team ->
                enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                    && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
        .peek(
            team -> {
              if (team.getManager() != null
                  && !enabledExpansions.contains(team.getManager().getExpansionCode())) {
                team.setManager(null);
              }
            })
        .collect(Collectors.toList());
  }

  /** Find team by both wrestlers. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Team> findTeamByWrestlers(final Wrestler wrestler1, final Wrestler wrestler2) {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return teamRepository
        .findByBothWrestlers(wrestler1, wrestler2)
        .filter(
            team ->
                enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                    && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
        .map(
            team -> {
              if (team.getManager() != null
                  && !enabledExpansions.contains(team.getManager().getExpansionCode())) {
                team.setManager(null);
              }
              return team;
            });
  }

  /** Find active team by both wrestlers. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Team> findActiveTeamByWrestlers(
      final Wrestler wrestler1, final Wrestler wrestler2) {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return teamRepository
        .findActiveTeamByBothWrestlers(wrestler1, wrestler2)
        .filter(
            team ->
                enabledExpansions.contains(team.getWrestler1().getExpansionCode())
                    && enabledExpansions.contains(team.getWrestler2().getExpansionCode()))
        .map(
            team -> {
              if (team.getManager() != null
                  && !enabledExpansions.contains(team.getManager().getExpansionCode())) {
                team.setManager(null);
              }
              return team;
            });
  }

  /** Count active teams. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countActiveTeams() {
    return getActiveTeams().size();
  }

  // ==================== BUSINESS OPERATIONS ====================

  /**
   * Saves a one-off ({@link TeamOrigin#AD_HOC}) team to the registry by flipping its origin to
   * {@link TeamOrigin#REGISTRY}. The team row itself is kept — this is a transition, not a
   * re-create.
   *
   * @param team the team to register
   * @return the persisted team with {@code REGISTRY} origin
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Team saveToRegistry(final Team team) {
    team.setOrigin(TeamOrigin.REGISTRY);
    Team saved = teamRepository.saveAndFlush(team);
    log.debug("Saved team {} to registry", saved.getName());
    return saved;
  }

  /**
   * Generates a unique team name from the given members: "A & B", then "A & B (2)", "A & B (3)", …
   * as needed to satisfy the unique {@code team.name} constraint.
   *
   * @param members the ordered team members
   * @return a team name not currently taken
   */
  @Transactional(readOnly = true)
  public String generateUniqueName(final List<Wrestler> members) {
    String baseName = members.stream().map(Wrestler::getName).collect(Collectors.joining(" & "));
    String candidate = baseName;
    int suffix = 2;
    while (teamRepository.existsByName(candidate)) {
      candidate = baseName + " (" + suffix + ")";
      suffix++;
    }
    return candidate;
  }

  /** Disband a team. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<Team> disbandTeam(final Long teamId) {
    return updateTeam(teamId, null, null, TeamStatus.DISBANDED, null, null);
  }

  /** Reactivate a disbanded team. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<Team> reactivateTeam(final Long teamId) {
    return updateTeam(teamId, null, null, TeamStatus.ACTIVE, null, null);
  }

  /**
   * Resolves the image URL for a team, using the default image system if no specific URL is set.
   *
   * @param team The team entity.
   * @return The resolved image URL.
   */
  public String resolveTeamImage(final Team team) {
    if (team.getImageUrl() != null && !team.getImageUrl().isBlank()) {
      return team.getImageUrl();
    }
    return imageService.resolveImage(team.getName(), ImageCategory.TEAM).url();
  }
}
