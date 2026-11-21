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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The cooldown expiry arithmetic is the single definition shared by RankingService's DTO annotation
 * and ContenderSelectionService's earned-shot gate — these tests pin it.
 */
class WrestlerTitleCooldownTest {

  private WrestlerTitleCooldown cooldownAt(long defenseCountAtChallenge) {
    return WrestlerTitleCooldown.builder().defenseCountAtChallenge(defenseCountAtChallenge).build();
  }

  @Test
  void isActive_whileDefensesUnderThreshold() {
    // Failed a challenge at defense 0; horizon is 2 — blocks until 2 defenses have passed.
    WrestlerTitleCooldown cooldown = cooldownAt(0L);
    assertTrue(cooldown.isActive(0, 2));
    assertTrue(cooldown.isActive(1, 2));
    assertFalse(cooldown.isActive(2, 2));
  }

  @Test
  void isActive_expiredStaysExpired() {
    // Defense count well past the horizon never re-activates (regression shape: 4-0 ≥ 2).
    assertFalse(cooldownAt(0L).isActive(4, 2));
    assertFalse(cooldownAt(3L).isActive(10, 2));
  }

  @Test
  void defensesRemaining_countsDownToZero() {
    WrestlerTitleCooldown cooldown = cooldownAt(0L);
    assertEquals(2, cooldown.defensesRemaining(0, 2));
    assertEquals(1, cooldown.defensesRemaining(1, 2));
    assertEquals(0, cooldown.defensesRemaining(2, 2));
    // Never negative.
    assertEquals(0, cooldown.defensesRemaining(5, 2));
  }
}
