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
package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.feud.FeudParticipant;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking engine for Faction War shows. Generates match segments driven by faction-vs-faction
 * rivalries and feuds instead of the individual-rivalry pipeline used by standard shows.
 *
 * <p>Priority order (highest fills slots first):
 *
 * <ol>
 *   <li>Active faction rivalry segments sorted by heat desc — up to 60% of requested slots.
 *   <li>Inter-faction feud singles — one match per feud that has members from ≥ 2 factions.
 *   <li>Cross-faction random singles — filler using any two factions with unbooked members.
 * </ol>
 *
 * <p>Segment type selection per rivalry: both factions with ≥ 2 available members → TAG_TEAM;
 * otherwise ONE_ON_ONE (leader vs leader, falling back to first available member).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FactionWarBookingService {

  private final FactionRivalryService factionRivalryService;
  private final FactionService factionService;
  private final MultiWrestlerFeudService multiWrestlerFeudService;
  private final SegmentTypeRepository segmentTypeRepository;
  private final NPCSegmentResolutionService npcSegmentResolutionService;

  /**
   * Generate match segments for a Faction War show. Promos are handled separately by the caller.
   *
   * @param show saved show to attach segments to
   * @param segmentCount number of match slots to fill
   * @return resolved segments, may be fewer than {@code segmentCount} if factions are scarce
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public List<Segment> generateFactionWarSegments(
      @NonNull final Show show, final int segmentCount) {

    Optional<SegmentType> oneOnOneOpt =
        segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode());
    if (oneOnOneOpt.isEmpty()) {
      log.warn(
          "ONE_ON_ONE segment type not found — Faction War booking skipped for '{}'",
          show.getName());
      return List.of();
    }
    SegmentType oneOnOne = oneOnOneOpt.get();
    SegmentType tagTeam =
        segmentTypeRepository.findByCode(WellKnownSegmentType.TAG_TEAM.getCode()).orElse(oneOnOne);

    List<Segment> segments = new ArrayList<>();
    Set<Long> bookedIds = new HashSet<>();

    // --- Priority 1: active faction rivalries (up to 60% of slots) ---
    int rivalrySlots = Math.max(1, (segmentCount * 6) / 10);
    List<FactionRivalry> rivalries =
        factionRivalryService.getActiveFactionRivalries().stream()
            .sorted((a, b) -> Integer.compare(b.getHeat(), a.getHeat()))
            .toList();

    for (FactionRivalry rivalry : rivalries) {
      if (segments.size() >= rivalrySlots) {
        break;
      }
      Faction f1 = rivalry.getFaction1();
      Faction f2 = rivalry.getFaction2();
      if (!f1.isActive() || !f2.isActive()) {
        continue;
      }
      bookFactionRivalrySegment(show, f1, f2, rivalry.getHeat(), bookedIds, oneOnOne, tagTeam)
          .ifPresent(segments::add);
    }

    // --- Priority 2: inter-faction feud singles ---
    List<MultiWrestlerFeud> feuds = multiWrestlerFeudService.getInterFactionFeuds();
    for (MultiWrestlerFeud feud : feuds) {
      if (segments.size() >= segmentCount) {
        break;
      }
      List<Wrestler> feudWrestlers =
          feud.getParticipants().stream()
              .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && p.getWrestler() != null)
              .map(FeudParticipant::getWrestler)
              .filter(w -> w.getId() != null && !bookedIds.contains(w.getId()))
              .collect(Collectors.toList());

      if (feudWrestlers.size() < 2) {
        continue;
      }
      Wrestler w1 = feudWrestlers.get(0);
      Wrestler w2 = feudWrestlers.get(1);
      Optional<Segment> seg =
          bookSinglesSegment(show, w1, w2, "Inter-Faction Feud: " + feud.getName(), oneOnOne);
      if (seg.isPresent()) {
        segments.add(seg.get());
        bookedIds.add(w1.getId());
        bookedIds.add(w2.getId());
      }
    }

    // --- Priority 3: cross-faction random filler ---
    List<Faction> allFactions =
        factionService.findAll().stream()
            .filter(Faction::isActive)
            .filter(f -> f.getMemberCount() >= 1)
            .toList();

    outer:
    for (int i = 0; i < allFactions.size(); i++) {
      for (int j = i + 1; j < allFactions.size(); j++) {
        if (segments.size() >= segmentCount) {
          break outer;
        }
        Faction fa = allFactions.get(i);
        Faction fb = allFactions.get(j);
        Optional<Wrestler> wa = pickUnbooked(fa, bookedIds);
        Optional<Wrestler> wb = pickUnbooked(fb, bookedIds);
        if (wa.isEmpty() || wb.isEmpty()) {
          continue;
        }
        Optional<Segment> seg =
            bookSinglesSegment(show, wa.get(), wb.get(), "Cross-Faction Match", oneOnOne);
        if (seg.isPresent()) {
          segments.add(seg.get());
          bookedIds.add(wa.get().getId());
          bookedIds.add(wb.get().getId());
        }
      }
    }

    log.info(
        "Faction War booking complete: {} segment(s) generated for show '{}'",
        segments.size(),
        show.getName());
    return segments;
  }

  // ==================== HELPERS ====================

  private Optional<Segment> bookFactionRivalrySegment(
      final Show show,
      final Faction f1,
      final Faction f2,
      final int heat,
      final Set<Long> bookedIds,
      final SegmentType oneOnOne,
      final SegmentType tagTeam) {

    List<Wrestler> m1 = availableMembers(f1, bookedIds);
    List<Wrestler> m2 = availableMembers(f2, bookedIds);
    if (m1.isEmpty() || m2.isEmpty()) {
      return Optional.empty();
    }

    String label =
        "Faction War: "
            + f1.getName()
            + " vs "
            + f2.getName()
            + (heat > 0 ? " (Heat: " + heat + ")" : "");

    try {
      if (m1.size() >= 2 && m2.size() >= 2) {
        SegmentTeam team1 = new SegmentTeam(List.of(m1.get(0), m1.get(1)), f1.getName());
        SegmentTeam team2 = new SegmentTeam(List.of(m2.get(0), m2.get(1)), f2.getName());
        Segment s =
            npcSegmentResolutionService.resolveTeamSegment(team1, team2, tagTeam, show, label);
        bookedIds.add(m1.get(0).getId());
        bookedIds.add(m1.get(1).getId());
        bookedIds.add(m2.get(0).getId());
        bookedIds.add(m2.get(1).getId());
        return Optional.of(s);
      } else {
        Wrestler w1 =
            f1.getLeader() != null && m1.contains(f1.getLeader()) ? f1.getLeader() : m1.get(0);
        Wrestler w2 =
            f2.getLeader() != null && m2.contains(f2.getLeader()) ? f2.getLeader() : m2.get(0);
        Segment s =
            npcSegmentResolutionService.resolveTeamSegment(
                new SegmentTeam(w1), new SegmentTeam(w2), oneOnOne, show, label);
        bookedIds.add(w1.getId());
        bookedIds.add(w2.getId());
        return Optional.of(s);
      }
    } catch (Exception e) {
      log.error("Error booking faction rivalry segment: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  private Optional<Segment> bookSinglesSegment(
      final Show show,
      final Wrestler w1,
      final Wrestler w2,
      final String label,
      final SegmentType type) {
    try {
      return Optional.of(
          npcSegmentResolutionService.resolveTeamSegment(
              new SegmentTeam(w1), new SegmentTeam(w2), type, show, label));
    } catch (Exception e) {
      log.error("Error booking singles segment: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  private List<Wrestler> availableMembers(final Faction faction, final Set<Long> bookedIds) {
    return faction.getMembers().stream()
        .map(WrestlerState::getWrestler)
        .filter(w -> w != null && w.getId() != null && !bookedIds.contains(w.getId()))
        .collect(Collectors.toList());
  }

  private Optional<Wrestler> pickUnbooked(final Faction faction, final Set<Long> bookedIds) {
    if (faction.getLeader() != null
        && faction.getLeader().getId() != null
        && !bookedIds.contains(faction.getLeader().getId())) {
      return Optional.of(faction.getLeader());
    }
    return availableMembers(faction, bookedIds).stream().findFirst();
  }
}
