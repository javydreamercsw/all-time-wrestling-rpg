package com.github.javydreamercsw.management.service.faction;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class FactionService {
  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  public Faction save(@NonNull Faction faction) {
    return factionRepository.save(faction);
  }

  public void delete(@NonNull Faction faction) {
    factionRepository.delete(faction);
  }

  public List<Faction> findAll() {
    return factionRepository.findAll();
  }

  public Optional<Faction> findById(Long id) {
    return factionRepository.findById(id);
  }

  public Optional<Faction> findByName(String name) {
    return factionRepository.findByName(name);
  }

  public Optional<Faction> findByExternalId(String externalId) {
    return factionRepository.findByExternalId(externalId);
  }

  public Page<Faction> getAllFactions(Pageable pageable) {
    return factionRepository.findAll(pageable);
  }

  public Optional<Faction> getFactionById(Long id) {
    return factionRepository.findById(id);
  }

  public Optional<Faction> getFactionByName(String name) {
    return factionRepository.findByName(name);
  }

  public List<Faction> getActiveFactions() {
    return factionRepository.findByIsActive(true);
  }

  public List<Faction> getFactionsByType(String type) {
    return factionRepository.findAll().stream()
        .filter(faction -> faction.getFactionType().equalsIgnoreCase(type))
        .toList();
  }

  public List<Faction> getLargestFactions(int limit) {
    return factionRepository.findAll().stream()
        .sorted((f1, f2) -> Integer.compare(f2.getMemberCount(), f1.getMemberCount()))
        .limit(limit)
        .toList();
  }

  public List<Faction> getFactionsWithActiveRivalries() {
    return factionRepository.findAll().stream()
        .filter(faction -> !faction.getActiveRivalries().isEmpty())
        .toList();
  }

  public Optional<Faction> getFactionForWrestler(Long wrestlerId) {
    return wrestlerRepository.findById(wrestlerId).map(Wrestler::getFaction);
  }

  public Optional<Faction> createFaction(String name, String description, Long leaderId) {
    if (factionRepository.findByName(name).isPresent()) {
      return Optional.empty();
    }
    Wrestler leader = null;
    if (leaderId != null) {
      leader = wrestlerRepository.findById(leaderId).orElse(null);
    }
    Faction faction =
        Faction.builder()
            .name(name)
            .description(description)
            .leader(leader)
            .formedDate(Instant.now())
            .build();
    return Optional.of(factionRepository.save(faction));
  }

  public Optional<Faction> addMemberToFaction(Long factionId, Long wrestlerId) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    if (factionOpt.isPresent() && wrestlerOpt.isPresent()) {
      Faction faction = factionOpt.get();
      Wrestler wrestler = wrestlerOpt.get();
      faction.addMember(wrestler);
      return Optional.of(factionRepository.save(faction));
    }
    return Optional.empty();
  }

  public Optional<Faction> removeMemberFromFaction(
      Long factionId, Long wrestlerId, String reason) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    if (factionOpt.isPresent() && wrestlerOpt.isPresent()) {
      Faction faction = factionOpt.get();
      Wrestler wrestler = wrestlerOpt.get();
      faction.removeMember(wrestler);
      return Optional.of(factionRepository.save(faction));
    }
    return Optional.empty();
  }

  public Optional<Faction> changeFactionLeader(Long factionId, Long newLeaderId) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(newLeaderId);
    if (factionOpt.isPresent() && wrestlerOpt.isPresent()) {
      Faction faction = factionOpt.get();
      Wrestler newLeader = wrestlerOpt.get();
      if (faction.hasMember(newLeader)) {
        faction.setLeader(newLeader);
        return Optional.of(factionRepository.save(faction));
      }
    }
    return Optional.empty();
  }

  public Optional<Faction> disbandFaction(Long factionId, String reason) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);
    if (factionOpt.isPresent()) {
      Faction faction = factionOpt.get();
      faction.disband(reason);
      return Optional.of(factionRepository.save(faction));
    }
    return Optional.empty();
  }

  public List<Faction> list(@NonNull Pageable pageable) {
    return factionRepository.findAll(pageable).toList();
  }

  public long count() {
    return factionRepository.count();
  }

  public List<Faction> findAllWithMembersAndTeams() {
    return factionRepository.findAllWithMembersAndTeams();
  }

  public Optional<Faction> getFactionByIdWithMembers(Long id) {
    return factionRepository.findByIdWithMembers(id);
  }
}