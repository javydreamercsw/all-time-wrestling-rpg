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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves the {@code data.initializer.skip-if-not-empty} seed sync mode and provides the shared
 * seed-fingerprint helpers used by verify-mode reconciliation.
 *
 * <p>Mode mapping: falsy → FULL, "true" → PROTECT, "verify" (case-insensitive, trimmed) → VERIFY,
 * any other truthy value → PROTECT with a one-time warning.
 */
@Slf4j
public class SeedSyncSupport {

  private final String rawValue;
  private SeedSyncMode resolved;
  private boolean warnedUnknown;

  public SeedSyncSupport(String skipIfNotEmptyRaw) {
    this.rawValue = skipIfNotEmptyRaw;
  }

  public SeedSyncMode mode() {
    if (resolved == null) {
      resolved = resolve(rawValue);
    }
    return resolved;
  }

  public boolean isFull() {
    return mode() == SeedSyncMode.FULL;
  }

  public boolean isVerify() {
    return mode() == SeedSyncMode.VERIFY;
  }

  private SeedSyncMode resolve(String raw) {
    if (raw == null || raw.isBlank()) {
      return SeedSyncMode.FULL;
    }
    String value = raw.trim();
    if ("true".equalsIgnoreCase(value)) {
      return SeedSyncMode.PROTECT;
    }
    if ("false".equalsIgnoreCase(value)) {
      return SeedSyncMode.FULL;
    }
    if ("verify".equalsIgnoreCase(value)) {
      return SeedSyncMode.VERIFY;
    }
    warnUnknown(value);
    return SeedSyncMode.PROTECT;
  }

  private void warnUnknown(String value) {
    if (!warnedUnknown) {
      log.warn(
          "Unknown data.initializer.skip-if-not-empty value '{}'; treating as protect. "
              + "Supported values: false, true, verify.",
          value);
      warnedUnknown = true;
    }
  }

  /**
   * Computes the seed fingerprint: SHA-256 hex over the given seed-owned values joined by newlines,
   * in the entity's canonical field order. {@code null} serializes as the literal text "null".
   *
   * @param seedOwnedValues values in the entity's canonical field order
   * @return 64-character lowercase hex fingerprint
   */
  public String fingerprint(Object... seedOwnedValues) {
    StringBuilder canonical = new StringBuilder();
    for (int i = 0; i < seedOwnedValues.length; i++) {
      if (i > 0) {
        canonical.append('\n');
      }
      canonical.append(seedOwnedValues[i]);
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm unavailable", e);
    }
  }

  /**
   * @param fingerprint stored fingerprint value
   * @return true when the value is null or blank — i.e. a pre-fingerprint-era (legacy) row
   */
  public static boolean isBlankFingerprint(String fingerprint) {
    return fingerprint == null || fingerprint.isBlank();
  }

  /** Standard summary line for a verify-mode reconcile pass. */
  public String reconcileLog(String entity, int reconciled, int preserved) {
    return String.format(
        "verify(%s): reconciled %d, preserved %d edited", entity, reconciled, preserved);
  }
}
