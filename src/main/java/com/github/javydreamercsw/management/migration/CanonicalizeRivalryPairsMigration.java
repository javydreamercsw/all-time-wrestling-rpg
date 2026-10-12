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
package com.github.javydreamercsw.management.migration;

import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time repair establishing the canonical rivalry pair order (ATW-8nc8): every rivalry stores
 * wrestler1 as the lower wrestler id and wrestler2 as the higher one. The grouped Rivalry List's
 * representative-per-pair query keys on (wrestler1, wrestler2) and is only correct under this
 * invariant; {@code createRivalry} maintains it for new rows, this migration for existing ones.
 *
 * <p>Merge rule, applied per (universe, lower-id, higher-id) group — rivalries are universe-scoped
 * so the same pair in two universes is two independent groups:
 *
 * <ol>
 *   <li>Canonicalize every row (swap wrestler1/wrestler2 where wrestler1_id &gt; wrestler2_id).
 *   <li>Exactly one ACTIVE row (the verified production state): done — ended rows are feud-cycle
 *       history, not duplicates, and are kept.
 *   <li>Multiple ACTIVE rows (not seen in production, handled defensively): keep the hottest active
 *       row, tie-break by earliest startedDate; every other active row is ended with reason
 *       "Duplicate pair — merged" (its heat is preserved as history). Rows are never deleted.
 * </ol>
 *
 * <p>After the swap a row may end up equal to itself in id terms (wrestler1 == wrestler2 never —
 * the CHECK constraint guards it) — no extra handling needed.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class CanonicalizeRivalryPairsMigration implements DataMigration {

  static final String MERGED_REASON = "Duplicate pair — merged";

  private final RivalryRepository rivalryRepository;
  private final CacheManager cacheManager;

  @Override
  public String id() {
    return "canonicalize-rivalry-pairs";
  }

  @Override
  @Transactional
  public void migrate() {
    List<Rivalry> all = rivalryRepository.findAll();
    Map<String, List<Rivalry>> groups = new LinkedHashMap<>();
    for (Rivalry rivalry : all) {
      groups.computeIfAbsent(pairKey(rivalry), k -> new ArrayList<>()).add(rivalry);
    }

    int canonicalized = 0;
    int merged = 0;
    for (List<Rivalry> group : groups.values()) {
      for (Rivalry rivalry : group) {
        if (rivalry.getWrestler1().getId() > rivalry.getWrestler2().getId()) {
          WrestlerSwap.swap(rivalry);
          canonicalized++;
        }
      }
      List<Rivalry> active = group.stream().filter(Rivalry::getIsActive).toList();
      if (active.size() > 1) {
        Rivalry keep =
            active.stream()
                .max(
                    Comparator.comparingInt(Rivalry::getHeat)
                        .thenComparing(
                            r ->
                                r.getStartedDate() == null
                                    ? java.time.Instant.EPOCH
                                    : r.getStartedDate(),
                            Comparator.reverseOrder()))
                .orElseThrow();
        for (Rivalry rivalry : active) {
          if (rivalry != keep) {
            rivalry.endRivalry(MERGED_REASON);
            merged++;
          }
        }
      }
      group.forEach(rivalryRepository::save);
    }

    if (canonicalized > 0 || merged > 0) {
      log.info(
          "Canonicalized {} rivalry rows ({} duplicate-actives merged into history) across {}"
              + " pairs",
          canonicalized,
          merged,
          groups.size());
      var cache = cacheManager.getCache(CacheConfig.RIVALRIES_CACHE);
      if (cache != null) {
        cache.clear();
      }
    } else {
      log.info("All rivalry pairs already canonical — nothing to repair.");
    }
  }

  /** Universe-scoped unordered pair key: universe + lower wrestler id + higher wrestler id. */
  private String pairKey(Rivalry rivalry) {
    long w1 = rivalry.getWrestler1().getId();
    long w2 = rivalry.getWrestler2().getId();
    long low = Math.min(w1, w2);
    long high = Math.max(w1, w2);
    return rivalry.getUniverse() != null
        ? rivalry.getUniverse().getId() + ":" + low + ":" + high
        : "null:" + low + ":" + high;
  }

  /** Swaps the wrestler positions of a rivalry row. */
  private static final class WrestlerSwap {
    private static void swap(Rivalry rivalry) {
      var w1 = rivalry.getWrestler1();
      rivalry.setWrestler1(rivalry.getWrestler2());
      rivalry.setWrestler2(w1);
    }
  }
}
