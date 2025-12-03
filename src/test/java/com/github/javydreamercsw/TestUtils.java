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
package com.github.javydreamercsw;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import lombok.NonNull;

public class TestUtils {
  /**
   * Create a wrestler with default values. Stil needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName(name);
    wrestler.setDescription("Test Wrestler");
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setLowHealth(4);
    wrestler.setLowStamina(2);
    wrestler.setTier(WrestlerTier.ROOKIE);
    wrestler.setCreationDate(Instant.now());
    return wrestler;
  }
}
