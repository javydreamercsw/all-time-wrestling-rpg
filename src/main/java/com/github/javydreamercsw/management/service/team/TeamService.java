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

import com.github.javydreamercsw.base.domain.faction.Faction;
import com.github.javydreamercsw.base.domain.faction.FactionRepository;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  // ==================== CRUD OPERATIONS ====================

  /** Get all teams with pagination. */
  @Transactional(readOnly = true)
  public Page<Team> getAllTeams(Pageable pageable) {
    return teamRepository.findAll(pageable);
  }

  /** Get team by ID. */
  @Transactional(readOnly = true)
  public Optional<Team> getTeamById(Long id) {
    return teamRepository.findById(id);
  }

  /** Get team by name. */
  @Transactional(readOnly = true)
  public Optional<Team> getTeamByName(String name) {
    return teamRepository.findByName(name);
  }

  /** Get team by external ID (for Notion sync). */
  @Transactional(readOnly = true)
  public Optional<Team> getTeamByExternalId(String externalId) {
    return teamRepository.findByExternalId(externalId);
  }

  /** Create a new team. */
  public Optional<Team> createTeam(
      String name, String description, Long wrestler1Id, Long wrestler2Id, Long factionId) {
    if (wrestler1Id == null || wrestler2Id == null) {
      log.warn("Cannot create team: Wrestler IDs cannot be null");
      return Optional.empty();
    }
    // Validate wrestlers exist
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      log.warn(
          "Cannot create team: One or both wrestlers not found (IDs: {}, {})",
          wrestler1Id,
          wrestler2Id);
      return Optional.empty();
    }

    Wrestler wrestler1 = wrestler1Opt.get();
    Wrestler wrestler2 = wrestler2Opt.get();

    // Check if wrestlers are the same
    if (wrestler1.equals(wrestler2)) {
      log.warn("Cannot create team: Both wrestlers are the same ({})", wrestler1.getName());
      return Optional.empty();
    }

    // Check if team name already exists
    if (teamRepository.existsByName(name)) {
      log.warn("Cannot create team: Name '{}' already exists", name);
      return Optional.empty();
    }

    // Check if these wrestlers already have an active team together
    Optional<Team> existingTeam =
        teamRepository.findActiveTeamByBothWrestlers(wrestler1, wrestler2);
    if (existingTeam.isPresent()) {
      log.warn(
          "Cannot create team: {} and {} already have an active team: {}",
          wrestler1.getName(),
          wrestler2.getName(),
          existingTeam.get().getName());
      return Optional.empty();
    }

    Team team = new Team();
    team.setName(name);
    team.setDescription(description);
    team.setWrestler1(wrestler1);
    team.setWrestler2(wrestler2);
    team.setStatus(TeamStatus.ACTIVE);

    // Set faction if provided
    if (factionId != null) {
      // Note: We'd need FactionRepository here, but keeping it simple for now
      team.setFaction(factionRepository.findById(factionId).orElse(null));
    }

    Team savedTeam = teamRepository.saveAndFlush(team);
    log.info(
        "Created team: {} with members {} & {}",
        savedTeam.getName(),
        wrestler1.getName(),
        wrestler2.getName());

    return Optional.of(savedTeam);
  }

  /** Update an existing team. */
  public Optional<Team> updateTeam(
      Long teamId, String name, String description, TeamStatus status, Long factionId) {
    Optional<Team> teamOpt = teamRepository.findById(teamId);
    if (teamOpt.isEmpty()) {
      return Optional.empty();
    }

    Team team = teamOpt.get();

    // Check if new name conflicts with existing team (excluding current team)
    if (name != null && !name.equals(team.getName()) && teamRepository.existsByName(name)) {
      log.warn("Cannot update team: Name '{}' already exists", name);
      return Optional.empty();
    }

    if (name != null) team.setName(name);
    if (description != null) team.setDescription(description);
    if (status != null) {
      team.setStatus(status);
      if (status == TeamStatus.DISBANDED) {
        team.disband();
      } else if (status == TeamStatus.ACTIVE && team.getDisbandedDate() != null) {
        team.reactivate();
      }
    }

    Team savedTeam = teamRepository.saveAndFlush(team);
    log.info("Updated team: {}", savedTeam.getName());

    return Optional.of(savedTeam);
  }

  /** Delete a team. */
  public boolean deleteTeam(Long teamId) {
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
  public List<Team> getActiveTeams() {
    return teamRepository.findByStatus(TeamStatus.ACTIVE);
  }

  /** Get teams by faction. */
  @Transactional(readOnly = true)
  public List<Team> getTeamsByFaction(Faction faction) {
    return teamRepository.findByFaction(faction);
  }

  /** Get teams where a wrestler is a member. */
  @Transactional(readOnly = true)
  public List<Team> getTeamsByWrestler(Wrestler wrestler) {
    return teamRepository.findByWrestler(wrestler);
  }

  /** Get active teams where a wrestler is a member. */
  @Transactional(readOnly = true)
  public List<Team> getActiveTeamsByWrestler(Wrestler wrestler) {
    return teamRepository.findByWrestlerAndStatus(wrestler, TeamStatus.ACTIVE);
  }

  /** Find team by both wrestlers. */
  @Transactional(readOnly = true)
  public Optional<Team> findTeamByWrestlers(Wrestler wrestler1, Wrestler wrestler2) {
    return teamRepository.findByBothWrestlers(wrestler1, wrestler2);
  }

  /** Find active team by both wrestlers. */
  @Transactional(readOnly = true)
  public Optional<Team> findActiveTeamByWrestlers(Wrestler wrestler1, Wrestler wrestler2) {
    return teamRepository.findActiveTeamByBothWrestlers(wrestler1, wrestler2);
  }

  /** Count active teams. */
  @Transactional(readOnly = true)
  public long countActiveTeams() {
    return teamRepository.countByStatus(TeamStatus.ACTIVE);
  }

  // ==================== BUSINESS OPERATIONS ====================

  /** Disband a team. */
  public Optional<Team> disbandTeam(Long teamId) {
    return updateTeam(teamId, null, null, TeamStatus.DISBANDED, null);
  }

  /** Reactivate a disbanded team. */
  public Optional<Team> reactivateTeam(Long teamId) {
    return updateTeam(teamId, null, null, TeamStatus.ACTIVE, null);
  }
}
