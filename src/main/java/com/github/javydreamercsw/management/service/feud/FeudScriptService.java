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
package com.github.javydreamercsw.management.service.feud;

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationPurpose;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeudScriptService {

  private final FeudScriptRepository feudScriptRepository;
  private final FeudScriptBeatRepository feudScriptBeatRepository;
  private final RivalryService rivalryService;
  private final MultiWrestlerFeudService multiWrestlerFeudService;
  private final ShowSegmentReservationService reservationService;
  private final GameSettingService gameSettingService;
  private final UniverseContextService universeContextService;

  // ── Query ────────────────────────────────────────────────────────────────

  public List<FeudScript> getActiveScriptsForRivalry(@NonNull Rivalry rivalry) {
    return feudScriptRepository.findByRivalryAndStatus(rivalry, FeudScriptStatus.ACTIVE);
  }

  /** Returns all scripts for a rivalry (any status) with beats eagerly loaded. */
  @Transactional(readOnly = true)
  public List<FeudScript> getScriptsWithBeatsForRivalry(@NonNull Rivalry rivalry) {
    return feudScriptRepository.findByRivalryWithBeats(rivalry);
  }

  public List<FeudScript> getActiveScriptsForFeud(@NonNull MultiWrestlerFeud feud) {
    return feudScriptRepository.findByFeudAndStatus(feud, FeudScriptStatus.ACTIVE);
  }

  /** Returns pending beats targeting the given show, used to inject into show planning context. */
  public List<FeudScriptBeat> getUpcomingBeatsForShow(@NonNull Show show) {
    if (show.getId() == null) {
      return List.of();
    }
    return feudScriptBeatRepository.findPendingBeatsForShow(show.getId());
  }

  /** Maps pending beats for a show to DTOs suitable for the AI prompt. */
  public List<FeudScriptBeatDTO> getUpcomingBeatDTOsForShow(@NonNull Show show) {
    return getUpcomingBeatsForShow(show).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Returns the beat that produced a given segment, if any (used for UI warnings). */
  public Optional<FeudScriptBeat> findBeatForSegment(@NonNull Segment segment) {
    if (segment.getId() == null) {
      return Optional.empty();
    }
    return feudScriptBeatRepository.findByActualSegment(segment);
  }

  // ── Creation from wizard ─────────────────────────────────────────────────

  /**
   * Creates a FeudScript from the wizard, auto-creating the underlying Rivalry or MultiWrestlerFeud
   * if one does not already exist between the given wrestlers.
   *
   * @param name booker-facing arc name
   * @param wrestlers 2 wrestlers → rivalry, 3+ → multi-wrestler feud
   * @param maxPleAppearances PLE appearance cap (1–3)
   * @return the persisted FeudScript
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public FeudScript createFromWizard(
      @NonNull String name, @NonNull List<Wrestler> wrestlers, int maxPleAppearances) {
    if (wrestlers.size() < 2) {
      throw new IllegalArgumentException("A feud script requires at least 2 wrestlers");
    }
    int clampedMax = Math.min(Math.max(maxPleAppearances, 1), 3);

    FeudScript script = new FeudScript();
    script.setName(name);
    script.setMaxPleAppearances(clampedMax);
    script.setStatus(FeudScriptStatus.ACTIVE);

    if (wrestlers.size() == 2) {
      Rivalry rivalry = findOrCreateRivalry(wrestlers.get(0), wrestlers.get(1));
      script.setRivalry(rivalry);
    } else {
      MultiWrestlerFeud feud = findOrCreateFeud(name, wrestlers);
      script.setFeud(feud);
    }

    return feudScriptRepository.save(script);
  }

  // ── Beat management ──────────────────────────────────────────────────────

  /**
   * Adds a beat to a script. Validates the PLE appearance cap and auto-creates a
   * ShowSegmentReservation when the beat targets a PLE show.
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public FeudScriptBeat addBeat(@NonNull FeudScript script, @NonNull FeudScriptBeat beat) {
    validatePleCap(script, beat);
    beat.setScript(script);
    beat.setBeatOrder(script.getBeats().size() + 1);
    FeudScriptBeat saved = feudScriptBeatRepository.save(beat);
    script.getBeats().add(saved);

    if (saved.getTargetShow() != null && saved.getTargetShow().isPremiumLiveEvent()) {
      String label = script.getName() + " — " + saved.getSegmentType();
      var reservation =
          reservationService.reserveSlot(
              saved.getTargetShow(),
              ShowSegmentReservationPurpose.FEUD_BLOWOFF,
              script.getId(),
              label);
      saved.setReservation(reservation);
      saved = feudScriptBeatRepository.save(saved);
    }
    return saved;
  }

  /** Marks a beat as completed and checks if the whole script is now complete. */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public FeudScriptBeat completeBeat(@NonNull FeudScriptBeat beat, @NonNull Segment segment) {
    beat.setActualSegment(segment);
    beat.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    FeudScriptBeat saved = feudScriptBeatRepository.save(beat);

    FeudScript script = saved.getScript();
    boolean allDone =
        script.getBeats().stream()
            .allMatch(
                b ->
                    b.getBeatStatus() == FeudScriptBeatStatus.COMPLETED
                        || b.getBeatStatus() == FeudScriptBeatStatus.SKIPPED);
    if (allDone) {
      script.setStatus(FeudScriptStatus.COMPLETED);
      feudScriptRepository.save(script);
    }
    return saved;
  }

  /** Returns the default PLE cap from settings (1–3, defaults to 3). */
  public int getDefaultMaxPleAppearances() {
    return gameSettingService.getMaxPleFeudAppearances();
  }

  // ── Internal helpers ─────────────────────────────────────────────────────

  private void validatePleCap(FeudScript script, FeudScriptBeat newBeat) {
    if (newBeat.getTargetShow() == null || !newBeat.getTargetShow().isPremiumLiveEvent()) {
      return;
    }
    long existingPleBeats =
        script.getBeats().stream()
            .filter(b -> b.getTargetShow() != null && b.getTargetShow().isPremiumLiveEvent())
            .count();
    if (existingPleBeats >= script.getMaxPleAppearances()) {
      throw new IllegalStateException(
          "PLE appearance cap of "
              + script.getMaxPleAppearances()
              + " already reached for script '"
              + script.getName()
              + "'");
    }
  }

  private Rivalry findOrCreateRivalry(Wrestler w1, Wrestler w2) {
    Long universeId = universeContextService.getCurrentUniverseId();
    return rivalryService
        .getRivalryBetweenWrestlers(w1.getId(), w2.getId())
        .orElseGet(
            () ->
                rivalryService
                    .createRivalry(w1.getId(), w2.getId(), "Script-driven feud", universeId)
                    .orElseThrow(
                        () -> new IllegalStateException("Failed to create rivalry for script")));
  }

  private MultiWrestlerFeud findOrCreateFeud(String name, List<Wrestler> wrestlers) {
    List<Long> wrestlerIds = wrestlers.stream().map(Wrestler::getId).collect(Collectors.toList());
    return multiWrestlerFeudService
        .createFeud(name, "Script-driven feud", "Script-driven feud", wrestlerIds)
        .orElseThrow(() -> new IllegalStateException("Failed to create multi-wrestler feud"));
  }

  private FeudScriptBeatDTO toDTO(FeudScriptBeat beat) {
    FeudScriptBeatDTO dto = new FeudScriptBeatDTO();
    dto.setBeatId(beat.getId());
    dto.setScriptName(beat.getScript().getName());
    dto.setSegmentType(beat.getSegmentType());
    dto.setSegmentRule(beat.getSegmentRule());
    dto.setWinnerControl(beat.getWinnerControl().name());
    dto.setPlannedWinnerName(
        beat.getPlannedWinner() != null ? beat.getPlannedWinner().getName() : null);
    dto.setCulmination(beat.isCulmination());
    dto.setNotes(beat.getNotes());

    FeudScript script = beat.getScript();
    String participants = "";
    if (script.getRivalry() != null) {
      Rivalry r = script.getRivalry();
      participants = r.getWrestler1().getName() + " vs " + r.getWrestler2().getName();
    } else if (script.getFeud() != null) {
      participants =
          script.getFeud().getParticipants().stream()
              .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
              .map(p -> p.getWrestler().getName())
              .collect(Collectors.joining(", "));
    }
    dto.setParticipantNames(participants);
    return dto;
  }
}
