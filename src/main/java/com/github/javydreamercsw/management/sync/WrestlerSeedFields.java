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
package com.github.javydreamercsw.management.sync;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;

/**
 * Canonical seed-owned field extraction for wrestler seed fingerprints. Field order here is the
 * fingerprint contract; changing it invalidates every stored fingerprint (they re-resolve as legacy
 * and are re-adopted on the next verify pass, so drift is safe but noisy).
 *
 * <p>Excluded by design (not seed-owned): fans, bumps, tier, active, isPlayer, state, contracts.
 */
final class WrestlerSeedFields {

  private WrestlerSeedFields() {}

  static Object[] canonical(Wrestler w) {
    return new Object[] {
      w.getDeckSize(),
      w.getStartingHealth(),
      w.getLowHealth(),
      w.getStartingStamina(),
      w.getLowStamina(),
      w.getDescription(),
      w.getGender(),
      w.getImageUrl(),
      w.getHeritageTag(),
      w.getExpansionCode(),
      w.getDrive(),
      w.getResilience(),
      w.getCharisma(),
      w.getBrawl(),
      w.getAlignment() == null ? null : w.getAlignment().getAlignmentType()
    };
  }
}
