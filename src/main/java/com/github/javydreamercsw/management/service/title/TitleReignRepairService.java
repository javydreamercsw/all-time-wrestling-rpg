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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * One-time startup repair: rebuilds title reigns from segment ground truth. Reigns predating the
 * {@code won_at_segment_id} column (added by a later migration and never backfilled) are genuine
 * history and are preserved as-is. Only segment-backed reigns are replaced — rebuilt by replaying
 * each title's {@code isTitleSegment} matches in chronological order — which fixes reigns corrupted
 * by the ATW-z2x2 bug (partial-champion tag team defenses recorded as title changes) and reigns
 * whose dates were stamped with the real-world adjudication time instead of the in-game show date.
 *
 * <p>Guarded by a {@code GameSetting} flag so it runs exactly once per database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TitleReignRepairService {

  private static final String GUARD_KEY = "migration.title_reign_rebuild_v1";

  private final GameSettingRepository gameSettingRepository;
  private final TitleRepository titleRepository;
  private final SegmentRepository segmentRepository;

  @Transactional
  public void repairIfNeeded() {
    if (gameSettingRepository.findGlobal(GUARD_KEY).isPresent()) {
      log.debug("Title reign rebuild already applied — skipping.");
      return;
    }
    log.info("Starting title reign rebuild from segment history (ATW-z2x2 data repair)...");
    List<Title> titles = titleRepository.findAll();
    for (Title title : titles) {
      rebuildReignsForTitle(title);
    }
    GameSetting guard = new GameSetting();
    guard.setSettingKey(GUARD_KEY);
    guard.setValue("applied");
    gameSettingRepository.save(guard);
    log.info("Title reign rebuild complete.");
  }

  private void rebuildReignsForTitle(Title title) {
    List<Segment> segments = segmentRepository.findByTitleOrderedByShowDate(title);
    if (segments.isEmpty()) {
      log.debug("No segments for title '{}' — skipping.", title.getName());
      return;
    }

    Title managed = titleRepository.findById(title.getId()).orElseThrow();

    // Reigns with no wonAtSegment link predate that column (added by a later migration and
    // never backfilled) and are genuine history — preserve them. Only segment-backed reigns
    // may be corrupted by the ATW-z2x2 bug or the real-date-vs-in-game-date bug, so only those
    // get wiped and rebuilt.
    Set<TitleReign> segmentBackedReigns =
        managed.getTitleReigns().stream()
            .filter(r -> r.getWonAtSegment() != null)
            .collect(Collectors.toSet());
    managed.getTitleReigns().removeAll(segmentBackedReigns);

    // Pre-migration reigns were re-saved with real-world timestamps every time the app synced
    // (e.g. loadShowTemplatesFromFile re-processing initial champions), fragmenting one real
    // reign into many back-to-back rows for the same champion(s). Collapse consecutive runs
    // held by the same champion(s) into a single reign spanning the earliest start to the
    // latest end, before the dates are used for anything below.
    mergeConsecutivePreservedReigns(managed);
    titleRepository.saveAndFlush(managed);

    // Seed initial champion state from the last open preserved reign (if any). If a title
    // change happens in the segment replay below, that reign's end date is set so the
    // lineage stays continuous.
    TitleReign openReign =
        managed.getTitleReigns().stream()
            .filter(r -> r.getEndDate() == null)
            .findFirst()
            .orElse(null);
    Set<Long> currentChampionIds =
        openReign == null
            ? new HashSet<>()
            : openReign.getChampions().stream().map(w -> w.getId()).collect(Collectors.toSet());
    int reignNumber =
        managed.getTitleReigns().stream().mapToInt(TitleReign::getReignNumber).max().orElse(0);
    int preservedCount = managed.getTitleReigns().size();
    int newReignCount = 0;

    for (Segment segment : segments) {
      Set<Long> winnerIds =
          segment.getParticipants().stream()
              .filter(p -> Boolean.TRUE.equals(p.getIsWinner()))
              .map(p -> p.getWrestler().getId())
              .collect(Collectors.toSet());

      if (winnerIds.isEmpty()) {
        continue; // draw — champions retain
      }

      boolean championDefended =
          !currentChampionIds.isEmpty()
              && currentChampionIds.stream().anyMatch(winnerIds::contains);
      if (championDefended) {
        continue;
      }

      // Title change: close the previous reign and open a new one.
      Instant segmentDate = showDateInstant(segment);
      if (openReign != null) {
        openReign.setEndDate(segmentDate);
      }

      reignNumber++;
      newReignCount++;
      TitleReign newReign = new TitleReign();
      newReign.setTitle(managed);
      newReign.setStartDate(segmentDate);
      newReign.setWonAtSegment(segment);
      newReign.setReignNumber(reignNumber);
      segment.getParticipants().stream()
          .filter(p -> Boolean.TRUE.equals(p.getIsWinner()))
          .forEach(p -> newReign.getChampions().add(p.getWrestler()));
      managed.getTitleReigns().add(newReign);

      openReign = newReign;
      currentChampionIds = new HashSet<>(winnerIds);
    }

    if (openReign != null) {
      managed.setChampion(new ArrayList<>(openReign.getChampions()));
    }

    titleRepository.save(managed);
    log.info(
        "Rebuilt {} segment-backed reign(s) for title '{}' ({} pre-migration reign(s) preserved).",
        newReignCount,
        managed.getName(),
        preservedCount);
  }

  /**
   * Collapses consecutive preserved reigns held by the same champion(s) into one. Renumbers the
   * survivors sequentially afterward so reign numbers stay gap-free.
   */
  private static void mergeConsecutivePreservedReigns(Title managed) {
    List<TitleReign> preserved =
        managed.getTitleReigns().stream()
            .filter(r -> r.getWonAtSegment() == null)
            .sorted(Comparator.comparing(TitleReign::getStartDate))
            .collect(Collectors.toList());

    TitleReign runStart = null;
    for (TitleReign reign : preserved) {
      if (runStart != null && sameChampions(runStart, reign)) {
        runStart.setEndDate(reign.getEndDate());
        managed.getTitleReigns().remove(reign);
      } else {
        runStart = reign;
      }
    }

    List<TitleReign> survivors =
        managed.getTitleReigns().stream()
            .filter(r -> r.getWonAtSegment() == null)
            .sorted(Comparator.comparing(TitleReign::getStartDate))
            .collect(Collectors.toList());
    for (int i = 0; i < survivors.size(); i++) {
      survivors.get(i).setReignNumber(i + 1);
    }
  }

  private static boolean sameChampions(TitleReign a, TitleReign b) {
    Set<Long> aIds = a.getChampions().stream().map(Wrestler::getId).collect(Collectors.toSet());
    Set<Long> bIds = b.getChampions().stream().map(Wrestler::getId).collect(Collectors.toSet());
    return aIds.equals(bIds);
  }

  private static Instant showDateInstant(Segment segment) {
    return segment.getShow().getShowDate().atStartOfDay(ZoneOffset.UTC).toInstant();
  }
}
