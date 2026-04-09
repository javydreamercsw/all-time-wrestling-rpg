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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WrestlerRelationshipServiceTest {

  @Mock private WrestlerRelationshipRepository relationshipRepository;
  @Mock private WrestlerRepository wrestlerRepository;

  @InjectMocks private WrestlerRelationshipService relationshipService;

  private Wrestler w1;
  private Wrestler w2;

  @BeforeEach
  void setUp() {
    w1 = new Wrestler();
    w1.setId(1L);
    w1.setName("Johnny All Time");

    w2 = new Wrestler();
    w2.setId(2L);
    w2.setName("Taya");
  }

  @Test
  void testCreateRelationship() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(w1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(w2));
    when(relationshipRepository.findBetweenWrestlers(w1, w2)).thenReturn(List.of());
    when(relationshipRepository.save(any(WrestlerRelationship.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    WrestlerRelationship rel =
        relationshipService.createOrUpdateRelationship(
            1L, 2L, RelationshipType.SPOUSE, 100, false, "Married");

    assertNotNull(rel);
    assertEquals(RelationshipType.SPOUSE, rel.getType());
    assertEquals(100, rel.getLevel());
    assertEquals(w1, rel.getWrestler1());
    assertEquals(w2, rel.getWrestler2());
    verify(relationshipRepository).save(any(WrestlerRelationship.class));
  }

  @Test
  void testCalculateChemistryBonus() {
    WrestlerRelationship rel = new WrestlerRelationship();
    rel.setWrestler1(w1);
    rel.setWrestler2(w2);
    rel.setType(RelationshipType.SPOUSE);
    rel.setLevel(100);

    when(relationshipRepository.findBetweenWrestlers(w1, w2)).thenReturn(List.of(rel));

    double bonus = relationshipService.calculateChemistryBonus(List.of(w1, w2));

    // Spouse at Level 100 multiplier is 0.15
    assertEquals(0.15, bonus, 0.001);
  }
}
