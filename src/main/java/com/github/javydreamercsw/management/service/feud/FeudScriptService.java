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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.feud.FeudParticipant;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatParticipant;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationPurpose;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import com.github.javydreamercsw.management.service.title.ContenderSelectionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
  private final ContenderSelectionService contenderSelectionService;
  private final SegmentTypeService segmentTypeService;
  private final WrestlerService wrestlerService;

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

  /**
   * Returns pending beats to inject into show planning context: beats explicitly targeted at the
   * show, plus — for beats with no target show (the common case; the wizard has no show picker) —
   * the next pending beat of every active script whose participants are all on {@code rosterIds}.
   */
  @Transactional(readOnly = true)
  public List<FeudScriptBeat> getUpcomingBeatsForShow(
      @NonNull Show show, @NonNull Set<Long> rosterIds) {
    if (show.getId() == null) {
      return List.of();
    }
    List<FeudScriptBeat> beats =
        new ArrayList<>(feudScriptBeatRepository.findPendingBeatsForShow(show.getId()));
    Set<Long> present = beats.stream().map(FeudScriptBeat::getId).collect(Collectors.toSet());
    List<FeudScriptBeat> fallback = feudScriptBeatRepository.findNextPendingBeatPerActiveScript();
    log.info(
        "Beat injection for show {}: {} targeted beat(s), {} fallback candidate(s), roster of {}"
            + " available wrestler(s)",
        show.getId(),
        beats.size(),
        fallback.size(),
        rosterIds.size());
    for (FeudScriptBeat next : fallback) {
      if (!present.add(next.getId())) {
        continue;
      }
      Set<Long> participantIds = participantIdsOf(next);
      if (!rosterIds.containsAll(participantIds)) {
        Set<Long> missing = new HashSet<>(participantIds);
        missing.removeAll(rosterIds);
        log.info(
            "Beat #{} of arc '{}' excluded: feud participant(s) {} not on the available roster"
                + " (injured, low condition, or filtered by the show's constraints)",
            next.getBeatOrder(),
            next.getScript().getName(),
            missing);
        continue;
      }
      beats.add(next);
    }
    return beats;
  }

  /** Wrestler IDs the beat's script involves (rivalry pair or active feud members). */
  private Set<Long> participantIdsOf(@NonNull FeudScriptBeat beat) {
    FeudScript script = beat.getScript();
    if (script.getRivalry() != null) {
      Rivalry rivalry = script.getRivalry();
      return Set.of(rivalry.getWrestler1().getId(), rivalry.getWrestler2().getId());
    }
    if (script.getFeud() != null) {
      return script.getFeud().getParticipants().stream()
          .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
          .map(p -> p.getWrestler().getId())
          .collect(Collectors.toSet());
    }
    return Set.of();
  }

  /** Maps pending beats for a show to DTOs suitable for the AI prompt. */
  public List<FeudScriptBeatDTO> getUpcomingBeatDTOsForShow(
      @NonNull Show show, @NonNull Set<Long> rosterIds) {
    return getUpcomingBeatsForShow(show, rosterIds).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Test-visible DTO mapping (toDTO is shared with the show-planning context builder). */
  FeudScriptBeatDTO toDTOForTest(FeudScriptBeat beat) {
    return toDTO(beat);
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
    script = reattachScript(script);
    validatePleCap(script, beat);
    beat.setScript(script);
    validateExternals(script, beat);
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

  /**
   * Automatically finds and completes the first PENDING beat whose rivalry wrestlers both appear in
   * the segment's participants. Called after a segment is saved with results. Returns the linked
   * beat if one was matched and completed, otherwise empty.
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public Optional<FeudScriptBeat> autoCompleteBeatForSegment(@NonNull Segment segment) {
    List<Long> wrestlerIds =
        segment.getParticipants().stream()
            .map(p -> p.getWrestler().getId())
            .collect(Collectors.toList());
    if (wrestlerIds.size() < 2) {
      return Optional.empty();
    }
    List<FeudScriptBeat> matches =
        feudScriptBeatRepository.findPendingBeatsForWrestlers(wrestlerIds);
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    FeudScriptBeat beat = matches.get(0);
    beat.setActualSegment(segment);
    beat.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    FeudScriptBeat saved = feudScriptBeatRepository.save(beat);
    applyContenderDesignation(saved, segment);

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
    log.info(
        "Auto-completed beat #{} of arc '{}' for segment {}",
        saved.getBeatOrder(),
        script.getName(),
        segment.getId());
    return Optional.of(saved);
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
    applyContenderDesignation(saved, segment);

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

  /**
   * CONTENDER_DESIGNATION beat outcome: when the beat carries a contender title, its segment winner
   * becomes the #1 contender for that title. Null-safe on the winner list (no declared winner → no
   * designation) and on the injected service (unit tests).
   */
  private void applyContenderDesignation(
      @NonNull final FeudScriptBeat beat, @NonNull final Segment segment) {
    if (beat.getContenderTitle() == null
        || contenderSelectionService == null
        || segment.getWinners().isEmpty()) {
      return;
    }
    Wrestler winner = segment.getWinners().get(0);
    log.info(
        "Beat #{} carries a contender designation: {} -> #1 contender for {}",
        beat.getBeatOrder(),
        winner.getName(),
        beat.getContenderTitle().getName());
    contenderSelectionService.designateAsContender(beat.getContenderTitle(), winner);
  }

  /** Returns the default PLE cap from settings (1–3, defaults to 3). */
  public int getDefaultMaxPleAppearances() {
    return gameSettingService.getMaxPleFeudAppearances();
  }

  // ── Edit / cancel ─────────────────────────────────────────────────────────

  /** Updates the arc name and PLE cap. */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public FeudScript updateScript(
      @NonNull FeudScript script, @NonNull String name, int maxPleAppearances) {
    script.setName(name.trim());
    script.setMaxPleAppearances(Math.min(Math.max(maxPleAppearances, 1), 3));
    return feudScriptRepository.save(script);
  }

  /**
   * Updates a PENDING beat's editable fields (match type, stipulation, winner control, planned
   * winner, culmination, notes, external participants) from {@code edited}. Only pending beats may
   * be edited — completed/skipped beats are immutable history. Re-runs creation-time external
   * participant validation and the PLE appearance cap (relevant when the cap was lowered after the
   * beat was created). The target show and its PLE reservation are preserved: the beat editor has
   * no show picker, so an edit never moves a beat between shows.
   *
   * <p>UI dialogs hold entities detached from the render request's session; the script, existing
   * beat and every wrestler reference are re-attached by id so LAZY associations resolve inside
   * this transaction instead of throwing {@code no session}.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public FeudScriptBeat updateBeat(
      @NonNull FeudScript script,
      @NonNull FeudScriptBeat existing,
      @NonNull FeudScriptBeat edited) {
    script = reattachScript(script);
    FeudScriptBeat managed =
        existing.getId() == null
            ? existing
            : feudScriptBeatRepository.findById(existing.getId()).orElse(existing);
    if (managed.getBeatStatus() != FeudScriptBeatStatus.PENDING) {
      throw new IllegalStateException(
          "Only pending beats can be edited (beat #"
              + managed.getBeatOrder()
              + " is "
              + managed.getBeatStatus()
              + ")");
    }
    // The beat editor has no target-show picker: an edit never moves the beat to another
    // show, so preserve the persisted assignment (and its reservation linkage) as-is.
    edited.setTargetShow(managed.getTargetShow());
    edited.setPlannedWinner(reattachWrestler(edited.getPlannedWinner()));
    for (FeudScriptBeatParticipant external : edited.getExternalParticipants()) {
      external.setWrestler(reattachWrestler(external.getWrestler()));
    }

    edited.setScript(script);
    validateExternals(script, edited);
    validatePleCapExcluding(script, edited, managed);

    managed.setSegmentType(edited.getSegmentType());
    managed.setSegmentRule(edited.getSegmentRule());
    managed.setWinnerControl(edited.getWinnerControl());
    managed.setPlannedWinner(edited.getPlannedWinner());
    managed.setCulmination(edited.isCulmination());
    managed.setNotes(edited.getNotes());

    // Replace external participants wholesale (upsert + removal via orphanRemoval).
    managed.getExternalParticipants().clear();
    for (FeudScriptBeatParticipant external : edited.getExternalParticipants()) {
      managed.addExternalParticipant(external.getWrestler(), external.getRole());
    }

    FeudScriptBeat saved = feudScriptBeatRepository.save(managed);
    log.info("Updated beat #{} of arc '{}'", saved.getBeatOrder(), script.getName());
    return saved;
  }

  /**
   * Removes a beat and renumbers the remaining beats in order. External participant rows go with it
   * via the {@code ON DELETE CASCADE} FK and {@code orphanRemoval} on the collection; the PLE
   * reservation, if any, is cancelled so the show's auto-booked segment count recovers. UI-supplied
   * script and beat are re-attached by id first (they arrive detached; see {@link #updateBeat}).
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void removeBeat(@NonNull FeudScript script, @NonNull FeudScriptBeat beat) {
    script = reattachScript(script);
    if (beat.getId() != null) {
      beat = feudScriptBeatRepository.findById(beat.getId()).orElse(beat);
    }
    if (beat.getReservation() != null) {
      reservationService.cancelReservation(beat.getReservation());
      beat.setReservation(null);
    }
    script.getBeats().remove(beat);
    feudScriptBeatRepository.delete(beat);
    int order = 1;
    for (FeudScriptBeat remaining :
        script.getBeats().stream()
            .sorted(Comparator.comparing(FeudScriptBeat::getBeatOrder))
            .collect(Collectors.toList())) {
      remaining.setBeatOrder(order++);
      feudScriptBeatRepository.save(remaining);
    }
  }

  /** Marks the arc as CANCELLED. Beats remain for historical reference. */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void cancelScript(@NonNull FeudScript script) {
    script.setStatus(FeudScriptStatus.CANCELLED);
    feudScriptRepository.save(script);
  }

  // ── Internal helpers ─────────────────────────────────────────────────────

  /**
   * Re-attaches a UI-supplied (detached) script to the current transaction. {@code open-in-view} is
   * off, so a {@link FeudScript} held by a Vaadin dialog carries LAZY proxies (rivalry, feud) bound
   * to the closed render-request session; touching them throws {@code no session}. Reloading by id
   * returns a managed instance with fresh proxies. Falls back to the argument when it is transient
   * (unit tests) or already managed.
   */
  private FeudScript reattachScript(FeudScript script) {
    return script.getId() == null
        ? script
        : feudScriptRepository.findById(script.getId()).orElse(script);
  }

  /** Wrestler variant of {@link #reattachScript}: resolves a detached wrestler by id. */
  private Wrestler reattachWrestler(Wrestler wrestler) {
    if (wrestler == null || wrestler.getId() == null) {
      return wrestler;
    }
    return wrestlerService.findById(wrestler.getId()).orElse(wrestler);
  }

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

  /** PLE-cap validation that skips {@code excluded} (the beat being edited) in the count. */
  private void validatePleCapExcluding(
      FeudScript script, FeudScriptBeat newBeat, FeudScriptBeat excluded) {
    if (newBeat.getTargetShow() == null || !newBeat.getTargetShow().isPremiumLiveEvent()) {
      return;
    }
    long existingPleBeats =
        script.getBeats().stream()
            .filter(b -> b.getTargetShow() != null && b.getTargetShow().isPremiumLiveEvent())
            .filter(b -> !b.getId().equals(excluded.getId()))
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

  /**
   * Validates the beat's external (non-feud) participants: no null wrestler/role, no feud member
   * doubling as an external, no wrestler on both external roles, and — when intergender matches are
   * disabled and the beat is not a promo — no external whose gender differs from a feud
   * participant's. Show-template gender constraints remain the authoritative check at card
   * validation, because the target show is unknown at beat-creation time.
   */
  private void validateExternals(FeudScript script, FeudScriptBeat beat) {
    Set<Long> feudParticipantIds = participantIdsOf(beat);
    Set<Long> seen = new HashSet<>();
    for (FeudScriptBeatParticipant external : beat.getExternalParticipants()) {
      if (external.getWrestler() == null || external.getWrestler().getId() == null) {
        throw new IllegalStateException("External participant requires a wrestler");
      }
      if (external.getRole() == null) {
        throw new IllegalStateException(
            "External participant " + external.getWrestler().getName() + " requires a role");
      }
      if (feudParticipantIds.contains(external.getWrestler().getId())) {
        throw new IllegalStateException(
            external.getWrestler().getName()
                + " is part of this arc and cannot be added as an external participant");
      }
      if (!seen.add(external.getWrestler().getId())) {
        throw new IllegalStateException(
            external.getWrestler().getName() + " cannot be added more than once to the same beat");
      }
    }
    if (beat.getExternalParticipants().isEmpty()
        || gameSettingService.isIntergenderMatchesEnabled()
        || isPromoSegmentType(beat.getSegmentType())) {
      return;
    }
    Set<Gender> feudGenders =
        feudParticipantsOf(beat).stream().map(Wrestler::getGender).collect(Collectors.toSet());
    if (feudGenders.size() > 1) {
      throw new IllegalStateException(
          "Intergender matches are disabled; the arc's participants are mixed-gender");
    }
    Gender feudGender = feudGenders.isEmpty() ? null : feudGenders.iterator().next();
    for (FeudScriptBeatParticipant external : beat.getExternalParticipants()) {
      if (feudGender != null && external.getWrestler().getGender() != feudGender) {
        throw new IllegalStateException(
            "Intergender matches are disabled; "
                + external.getWrestler().getName()
                + " cannot face the arc's participants");
      }
    }
  }

  /** True when the segment type name refers to a promo (no physical match, intergender moot). */
  private boolean isPromoSegmentType(String segmentTypeName) {
    if (segmentTypeName == null || segmentTypeName.isBlank()) {
      return false;
    }
    return segmentTypeService
        .findByName(segmentTypeName)
        .map(type -> WellKnownSegmentType.PROMO.matches(type))
        .orElseGet(() -> segmentTypeName.toLowerCase().contains("promo"));
  }

  /** The beat's feud wrestlers (rivalry pair or active feud members); empty when unresolvable. */
  private List<Wrestler> feudParticipantsOf(FeudScriptBeat beat) {
    FeudScript script = beat.getScript();
    if (script.getRivalry() != null) {
      return List.of(script.getRivalry().getWrestler1(), script.getRivalry().getWrestler2());
    }
    if (script.getFeud() != null) {
      return script.getFeud().getParticipants().stream()
          .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
          .map(FeudParticipant::getWrestler)
          .collect(Collectors.toList());
    }
    return List.of();
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
    List<Long> participantIds = List.of();
    Long rivalryId = null;
    if (script.getRivalry() != null) {
      Rivalry r = script.getRivalry();
      participants = r.getWrestler1().getName() + " vs " + r.getWrestler2().getName();
      participantIds = List.of(r.getWrestler1().getId(), r.getWrestler2().getId());
      rivalryId = r.getId();
    } else if (script.getFeud() != null) {
      List<Wrestler> activeMembers =
          script.getFeud().getParticipants().stream()
              .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
              .map(p -> p.getWrestler())
              .collect(Collectors.toList());
      participants =
          activeMembers.stream().map(Wrestler::getName).collect(Collectors.joining(", "));
      participantIds = activeMembers.stream().map(Wrestler::getId).collect(Collectors.toList());
    }
    dto.setParticipantNames(participants);
    dto.setParticipantIds(participantIds);
    dto.setRivalryId(rivalryId);

    // Explicit team layout: feud wrestlers = team 1, external opponent + extras = team 2.
    List<Wrestler> opponents = beat.getExternalOpponents();
    List<Wrestler> extras = beat.getExternalExtras();
    if (!opponents.isEmpty() || !extras.isEmpty()) {
      List<List<String>> teams = new ArrayList<>();
      teams.add(
          Arrays.stream(participants.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .collect(Collectors.toList()));
      List<List<Long>> teamIds = new ArrayList<>();
      teamIds.add(new ArrayList<>(participantIds));
      List<String> team2Names = new ArrayList<>();
      List<Long> team2Ids = new ArrayList<>();
      for (FeudScriptBeatParticipant external : beat.getExternalParticipants()) {
        team2Names.add(external.getWrestler().getName());
        team2Ids.add(external.getWrestler().getId());
      }
      teams.add(team2Names);
      teamIds.add(team2Ids);
      dto.setTeams(teams);
      dto.setTeamIds(teamIds);
      dto.setExternalSummary(
          beat.getExternalParticipants().stream()
              .map(p -> p.getWrestler().getName() + " (" + p.getRole().getDisplayName() + ")")
              .collect(Collectors.joining(", ")));
    }
    return dto;
  }
}
