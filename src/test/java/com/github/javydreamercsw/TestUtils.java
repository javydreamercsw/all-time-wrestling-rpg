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

  private static Universe defaultUniverse;

  public static void setDefaultUniverse(Universe universe) {
    defaultUniverse = universe;
  }

  /**
   * Create a wrestler with default values. Still needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @param fans Initial fans.
   * @param universe Universe to associate with.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name, long fans, Universe universe) {
    Universe finalUniverse = universe != null ? universe : defaultUniverse;
    Wrestler wrestler = createWrestler(name, finalUniverse);
    setFans(wrestler, fans, finalUniverse);
    return wrestler;
  }

  /**
   * Create a wrestler with default values. Still needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name, long fans) {
    Wrestler wrestler = createWrestler(name);
    setFans(wrestler, fans, null);
    return wrestler;
  }

  /**
   * Create a wrestler with default values. Still needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name) {
    return createWrestler(name, (Universe) null);
  }

  /**
   * Create a wrestler with default values. Still needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @param universe Universe to associate with.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name, Universe universe) {
    Universe finalUniverse = universe != null ? universe : defaultUniverse;
    Wrestler wrestler =
        createWrestler(name, UUID.randomUUID().toString(), WrestlerTier.ROOKIE, null);
    wrestler.setDescription("Test Wrestler");
    setFans(wrestler, 1_000L, finalUniverse); // Default fan count
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
    w.setGender(Gender.MALE);
    w.setActive(true);
    // Note: fans/state not set here, must be done via setFans if needed
    return w;
  }

  public static void setFans(Wrestler wrestler, long fans, Universe universe) {
    WrestlerState state =
        wrestler
            .getDefaultState()
            .orElseGet(
                () -> {
                  if (universe == null) {
                    // Don't create a state if we don't have a universe
                    return null;
                  }
                  WrestlerState s =
                      WrestlerState.builder().wrestler(wrestler).universe(universe).build();
                  wrestler.getWrestlerStates().add(s);
                  return s;
                });

    if (state != null) {
      state.setFans(fans);
      state.setTier(WrestlerTier.fromFanCount(fans));
    }
  }

  @Deprecated
  private static void setFans(Wrestler wrestler, long fans) {
    setFans(wrestler, fans, null);
  }
}
