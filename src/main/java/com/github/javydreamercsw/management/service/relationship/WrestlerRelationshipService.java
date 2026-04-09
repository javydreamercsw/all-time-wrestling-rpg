/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.relationship;

import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WrestlerRelationshipService {

  private final WrestlerRelationshipRepository relationshipRepository;
  private final WrestlerRepository wrestlerRepository;

  /** Get all relationships for a wrestler. */
  public List<WrestlerRelationship> getRelationshipsForWrestler(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(relationshipRepository::findAllByWrestler)
        .orElse(List.of());
  }

  /** Get relationships between two wrestlers. */
  public List<WrestlerRelationship> getRelationshipsBetween(Long w1Id, Long w2Id) {
    Optional<Wrestler> w1 = wrestlerRepository.findById(w1Id);
    Optional<Wrestler> w2 = wrestlerRepository.findById(w2Id);

    if (w1.isPresent() && w2.isPresent()) {
      return relationshipRepository.findBetweenWrestlers(w1.get(), w2.get());
    }
    return List.of();
  }

  /** Create or update a relationship. */
  @Transactional
  public WrestlerRelationship createOrUpdateRelationship(
      Long w1Id,
      Long w2Id,
      RelationshipType type,
      Integer level,
      boolean isStoryline,
      String notes) {

    Wrestler w1 =
        wrestlerRepository
            .findById(w1Id)
            .orElseThrow(() -> new IllegalArgumentException("Wrestler 1 not found: " + w1Id));
    Wrestler w2 =
        wrestlerRepository
            .findById(w2Id)
            .orElseThrow(() -> new IllegalArgumentException("Wrestler 2 not found: " + w2Id));

    if (w1.equals(w2)) {
      throw new IllegalArgumentException("Wrestler cannot have a relationship with themselves");
    }

    // Check for existing relationship of the same type
    List<WrestlerRelationship> existing = relationshipRepository.findBetweenWrestlers(w1, w2);
    WrestlerRelationship relationship =
        existing.stream()
            .filter(r -> r.getType() == type)
            .findFirst()
            .orElseGet(
                () -> {
                  WrestlerRelationship newRel = new WrestlerRelationship();
                  newRel.setWrestler1(w1);
                  newRel.setWrestler2(w2);
                  newRel.setType(type);
                  newRel.setStartedDate(Instant.now());
                  return newRel;
                });

    relationship.setLevel(level);
    relationship.setIsStoryline(isStoryline);
    relationship.setNotes(notes);

    WrestlerRelationship saved = relationshipRepository.save(relationship);
    log.info(
        "Saved relationship: {} {} {} (Level: {})",
        w1.getName(),
        type.getDisplayName(),
        w2.getName(),
        level);

    return saved;
  }

  /** Calculate chemistry bonus for a set of wrestlers. */
  public double calculateChemistryBonus(List<Wrestler> wrestlers) {
    if (wrestlers == null || wrestlers.size() < 2) {
      return 0.0;
    }

    double totalBonus = 0.0;
    int pairs = 0;

    for (int i = 0; i < wrestlers.size(); i++) {
      for (int j = i + 1; j < wrestlers.size(); j++) {
        List<WrestlerRelationship> relationships =
            relationshipRepository.findBetweenWrestlers(wrestlers.get(i), wrestlers.get(j));

        for (WrestlerRelationship rel : relationships) {
          totalBonus += calculatePairBonus(rel);
        }
        pairs++;
      }
    }

    // Return an average bonus based on pairs, or some other formula
    return totalBonus;
  }

  private double calculatePairBonus(WrestlerRelationship rel) {
    // Basic formula: Level (0-100) / 100 * multiplier based on type
    // Max bonus for a single pair is 0.15 (15% boost) for Spouse at Level 100.
    double multiplier =
        switch (rel.getType()) {
          case SPOUSE -> 0.15;
          case SIBLING -> 0.10;
          case BEST_FRIEND -> 0.08;
          case MENTOR, PROTEGE -> 0.05;
          case FAMILY -> 0.03;
          case ROMANCE -> 0.07;
        };

    return (rel.getLevel() / 100.0) * multiplier;
  }
}
