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
package com.github.javydreamercsw.management.domain.wrestler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import java.util.List;
import org.junit.jupiter.api.Test;

class WrestlerTest {

  @Test
  void testGetAllRelationships() {
    Wrestler w = new Wrestler();
    w.setName("Johnny All Time");

    Wrestler partner1 = new Wrestler();
    partner1.setName("Taya");

    Wrestler partner2 = new Wrestler();
    partner2.setName("The Beast");

    // Relationship as wrestler1
    WrestlerRelationship rel1 = new WrestlerRelationship();
    rel1.setWrestler1(w);
    rel1.setWrestler2(partner1);
    rel1.setType(RelationshipType.SPOUSE);
    w.getRelationshipsAsWrestler1().add(rel1);

    // Relationship as wrestler2
    WrestlerRelationship rel2 = new WrestlerRelationship();
    rel2.setWrestler1(partner2);
    rel2.setWrestler2(w);
    rel2.setType(RelationshipType.BEST_FRIEND);
    w.getRelationshipsAsWrestler2().add(rel2);

    List<WrestlerRelationship> all = w.getAllRelationships();

    assertEquals(2, all.size());
    assertTrue(all.contains(rel1));
    assertTrue(all.contains(rel2));
  }
}
