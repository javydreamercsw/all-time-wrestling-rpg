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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeedSyncSupportTest {

  private final SeedSyncSupport support = new SeedSyncSupport("verify");

  @Test
  void modeResolvesFalseToFull() {
    assertEquals(SeedSyncMode.FULL, new SeedSyncSupport("false").mode());
  }

  @Test
  void modeResolvesTrueToProtect() {
    assertEquals(SeedSyncMode.PROTECT, new SeedSyncSupport("true").mode());
  }

  @Test
  void modeResolvesVerifyCaseInsensitiveWithTrim() {
    assertEquals(SeedSyncMode.VERIFY, new SeedSyncSupport(" Verify ").mode());
  }

  @Test
  void unknownTruthyResolvesToProtect() {
    assertEquals(SeedSyncMode.PROTECT, new SeedSyncSupport("yes").mode());
  }

  @Test
  void fingerprintIsDeterministicHex() {
    String a = support.fingerprint("x", 1, true);
    assertEquals(a, support.fingerprint("x", 1, true));
    assertEquals(64, a.length());
  }

  @Test
  void fingerprintDistinguishesValues() {
    assertNotEquals(support.fingerprint("a", 1), support.fingerprint("a", 2));
  }

  @Test
  void blankOrNullMeansLegacy() {
    assertTrue(SeedSyncSupport.isBlankFingerprint(null));
    assertTrue(SeedSyncSupport.isBlankFingerprint("  "));
    assertFalse(SeedSyncSupport.isBlankFingerprint("ab12"));
  }

  @Test
  void fingerprintSerializesNullAsLiteralText() {
    // Spec: null serializes as the literal text "null" — so it hashes identically to the
    // four-character string.
    assertEquals(support.fingerprint("a", (Object) null), support.fingerprint("a", "null"));
  }
}
