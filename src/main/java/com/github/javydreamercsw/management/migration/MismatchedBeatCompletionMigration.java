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

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationStatus;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time repair for the scripted-beat mis-completion bug (ATW-lxnn): {@code
 * autoCompleteBeatForSegment} used to complete a pending arc beat on participant overlap alone, so
 * a promo featuring both rivalry wrestlers could consume a One-on-One title culmination targeted at
 * a later PLE. Production evidence: a "lay down the law" promo (segment 1217, Timeless) was linked
 * to the Ladder Match beat of the "Bobby Lashley vs Shelton Benjamin Arc", the beat's title stakes
 * were copied onto the promo (inert — adjudication skips title changes on promos), the PLE played
 * with no payoff, and the arc falsely completed.
 *
 * <p>The repair unlinks every COMPLETED beat whose linked segment would now be rejected by the
 * corrected guards ({@code segmentFitsBeat}): wrong show, wrong segment type, or an untargeted
 * culmination completed off a PLE. The beat returns to PENDING so the arc re-plans it; the
 * reservation is reset to PENDING (it will be filled when the beat actually books); the copied
 * title flags are removed from the segment; and a script left COMPLETED with a newly-pending beat
 * reopens as ACTIVE. Beats whose match is legitimate (the manual link dialog or a now-matching
 * segment) are left untouched.
 */
@Slf4j
@Component
public class MismatchedBeatCompletionMigration implements DataMigration {

  private final FeudScriptBeatRepository beatRepository;
  private final FeudScriptRepository scriptRepository;
  private final SegmentRepository segmentRepository;

  public MismatchedBeatCompletionMigration(
      FeudScriptBeatRepository beatRepository,
      FeudScriptRepository scriptRepository,
      SegmentRepository segmentRepository) {
    this.beatRepository = beatRepository;
    this.scriptRepository = scriptRepository;
    this.segmentRepository = segmentRepository;
  }

  @Override
  public String id() {
    return "unlink-mismatched-beat-completions";
  }

  @Override
  @Transactional
  public void migrate() {
    List<FeudScriptBeat> completed =
        beatRepository.findAll().stream()
            .filter(beat -> beat.getBeatStatus() == FeudScriptBeatStatus.COMPLETED)
            .toList();
    int unlinked = 0;
    for (FeudScriptBeat beat : completed) {
      Segment segment = beat.getActualSegment();
      if (segment == null || segmentFitsBeat(beat, segment)) {
        continue; // nothing linked, or the link is legitimate under the corrected guards
      }
      log.info(
          "Unlinking beat #{} of arc '{}': segment {} ({} on '{}') does not fit the beat"
              + " (books {}{}{})",
          beat.getBeatOrder(),
          beat.getScript().getName(),
          segment.getId(),
          segment.getSegmentType() != null ? segment.getSegmentType().getName() : "unknown type",
          segment.getShow() != null ? segment.getShow().getName() : "no show",
          beat.getSegmentType(),
          beat.getTargetShow() != null ? ", targeted at " + beat.getTargetShow().getName() : "",
          beat.isCulmination() && beat.getTargetShow() == null ? ", culmination" : "");
      // Strip the title flags the completion copied onto the segment (inert on promos, but a
      // wrongly-flagged match would change a championship on re-adjudication).
      segment.setIsTitleSegment(false);
      segment.setContenderMatch(false);
      segment.getTitles().clear();
      segmentRepository.save(segment);
      beat.setActualSegment(null);
      beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
      if (beat.getReservation() != null) {
        beat.getReservation().setSegment(null);
        beat.getReservation().setStatus(ShowSegmentReservationStatus.PENDING);
      }
      beatRepository.save(beat);
      unlinked++;
    }
    if (unlinked == 0) {
      log.info("No mismatched beat completions found — nothing to repair.");
      return;
    }
    // A script the bug marked COMPLETED reopens when it has a pending beat again.
    List<FeudScript> scripts =
        scriptRepository.findAll().stream()
            .filter(script -> script.getStatus() == FeudScriptStatus.COMPLETED)
            .toList();
    for (FeudScript script : scripts) {
      boolean hasPending =
          script.getBeats().stream()
              .anyMatch(beat -> beat.getBeatStatus() == FeudScriptBeatStatus.PENDING);
      if (hasPending) {
        log.info("Reopening arc '{}' — a beat was unlinked back to PENDING.", script.getName());
        script.setStatus(FeudScriptStatus.ACTIVE);
        scriptRepository.save(script);
      }
    }
    log.info("Mismatched-beat-completion repair done: {} beat(s) unlinked.", unlinked);
  }

  /** Mirrors FeudScriptService.segmentFitsBeat (the corrected auto-complete guards). */
  private boolean segmentFitsBeat(FeudScriptBeat beat, Segment segment) {
    if (beat.getTargetShow() != null
        && (segment.getShow() == null
            || !beat.getTargetShow().getId().equals(segment.getShow().getId()))) {
      return false;
    }
    if (beat.getSegmentType() != null
        && !beat.getSegmentType().isBlank()
        && (segment.getSegmentType() == null
            || !segment
                .getSegmentType()
                .getName()
                .equalsIgnoreCase(beat.getSegmentType().trim()))) {
      return false;
    }
    return !beat.isCulmination()
        || beat.getTargetShow() != null
        || (segment.getShow() != null && segment.getShow().isPremiumLiveEvent());
  }
}
