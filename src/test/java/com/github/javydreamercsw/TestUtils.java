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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import java.util.UUID;
import lombok.NonNull;

public class TestUtils {
  /**
   * Create a wrestler with default values. Stil needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name, long fans) {
    Wrestler wrestler = createWrestler(name);
    setFans(wrestler, fans);
    return wrestler;
  }

  /**
   * Create a wrestler with default values. Stil needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name) {
    Wrestler wrestler =
        createWrestler(name, UUID.randomUUID().toString(), WrestlerTier.ROOKIE, null);
    wrestler.setDescription("Test Wrestler");
    setFans(wrestler, 1_000L); // Default fan count
    return wrestler;
  }

  public static Wrestler createWrestler(
      @NonNull String name,
      @NonNull String description,
      @NonNull WrestlerTier tier,
      Account account) {
    Wrestler w = new Wrestler();
    w.setName(name);
    if (account != null) {
      w.setAccount(account);
    }
    w.setDescription(description);
    w.setIsPlayer(account != null);
    w.setTier(tier);
    w.setDescription(description);
    // Defaults
    w.setDeckSize(15);
    w.setStartingHealth(15);
    w.setLowHealth(4);
    w.setStartingStamina(15);
    w.setLowStamina(2);
    setFans(w, 0L);
    w.setGender(Gender.MALE);
    w.setBumps(0);
    w.setActive(true);
    return w;
  }

  private static void setFans(Wrestler wrestler, long fans) {
    WrestlerState state =
        wrestler
            .getDefaultState()
            .orElseGet(
                () -> {
                  WrestlerState s =
                      WrestlerState.builder()
                          .wrestler(wrestler)
                          .universe(Universe.builder().id(1L).name("Global").build())
                          .build();
                  wrestler.getWrestlerStates().add(s);
                  return s;
                });
    state.setFans(fans);
    state.setTier(WrestlerTier.fromFanCount(fans));
  }
}
