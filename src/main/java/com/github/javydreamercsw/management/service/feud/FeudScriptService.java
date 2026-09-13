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
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.feud.FeudBeatParticipantRole;
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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.FeudScriptCompletedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import com.github.javydreamercsw.management.service.title.ContenderSelectionService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
  private final SegmentService segmentService;
  private final ShowService showService;
  private final TitleService titleService;
  private final ApplicationEventPublisher eventPublisher;

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

  /** Returns every arc (any status, rivalry- or feud-linked) with beats eagerly loaded. */
  @Transactional(readOnly = true)
  public List<FeudScript> getAllScriptsWithBeats() {
    return feudScriptRepository.findAllWithBeats();
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

  /**
   * Creates a FeudScript from the wizard together with all its beats in a single transaction — a
   * failure on any beat leaves nothing persisted (the wizard previously committed the script and
   * each beat separately, leaving a half-built arc behind). Runs the same validation as {@link
   * #createFromWizard} and {@link #addBeat}.
   *
   * @param name booker-facing arc name
   * @param wrestlers 2 wrestlers → rivalry, 3+ → multi-wrestler feud
   * @param maxPleAppearances PLE appearance cap (1–3)
   * @param beats pre-built beats, added in list order
   * @return the persisted FeudScript
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public FeudScript createScriptWithBeats(
      @NonNull String name,
      @NonNull List<Wrestler> wrestlers,
      int maxPleAppearances,
      @NonNull List<FeudScriptBeat> beats) {
    FeudScript script = createFromWizard(name, wrestlers, maxPleAppearances);
    for (FeudScriptBeat beat : beats) {
      addBeat(script, beat);
    }
    return script;
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
   * Automatically finds and completes the first PENDING beat whose arc wrestlers (rivalry pair, or
   * all active feud members) appear in the segment's participants. Called after a segment is saved
   * with results. Before completing, the beat's title stakes (title match / #1 contender) are
   * copied onto the segment so show adjudication — which runs after the beat is linked — applies
   * the outcome once, through the normal {@code isTitleSegment}/{@code isContenderMatch} paths.
   * Returns the linked beat if one was matched and completed, otherwise empty.
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
    List<FeudScriptBeat> candidates =
        feudScriptBeatRepository.findPendingBeatsForWrestlers(wrestlerIds);
    // Rivalry arcs already match on both wrestlers (in the query); feud arcs surface on any
    // active member, so require full coverage of the arc's participants here.
    FeudScriptBeat beat =
        candidates.stream()
            .filter(b -> wrestlerIds.containsAll(participantIdsOf(b)))
            .findFirst()
            .orElse(null);
    if (beat == null) {
      return Optional.empty();
    }
    completeBeatInternal(beat, segment);
    return Optional.of(beat);
  }

  /**
   * Public entry point for completing a beat with a specific segment — used by the manual
   * add-segment flow in ShowDetailView, which previously never consulted pending beats.
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public Optional<FeudScriptBeat> resolveAndCompleteBeat(
      @NonNull FeudScript script, @NonNull FeudScriptBeat beat, @NonNull Segment segment) {
    completeBeatInternal(beat, segment);
    return Optional.of(beat);
  }

  /** Marks a beat as completed and checks if the whole script is now complete. */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public FeudScriptBeat completeBeat(@NonNull FeudScriptBeat beat, @NonNull Segment segment) {
    completeBeatInternal(beat, segment);
    return beat;
  }

  /**
   * Shared completion path: links the segment, copies title stakes onto a still-pending segment
   * (adjudication applies the outcome later — see {@link #copyTitleContextToSegment}), fills the
   * beat's PLE reservation, completes the beat and — when every beat is done — the whole arc
   * (publishing {@link FeudScriptCompletedEvent}).
   */
  private void completeBeatInternal(@NonNull FeudScriptBeat beat, @NonNull Segment segment) {
    boolean flagsCopied = copyTitleContextToSegment(beat, segment);
    if (!flagsCopied && segment.getAdjudicationStatus() == AdjudicationStatus.ADJUDICATED) {
      // Adjudication already ran on this segment (booker re-linked a finished segment): copy the
      // flags would be inert, so apply the contender outcome directly instead.
      applyContenderDesignation(beat, segment);
    }
    beat.setActualSegment(segment);
    beat.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    FeudScriptBeat saved = feudScriptBeatRepository.save(beat);
    if (saved.getReservation() != null) {
      reservationService.fillReservation(saved.getReservation(), segment);
    }

    FeudScript script = saved.getScript();
    if (isScriptComplete(script)) {
      script.setStatus(FeudScriptStatus.COMPLETED);
      feudScriptRepository.save(script);
      eventPublisher.publishEvent(new FeudScriptCompletedEvent(this, script));
      log.info("Story arc '{}' completed", script.getName());
    }
    log.info(
        "Completed beat #{} of arc '{}' for segment {}",
        saved.getBeatOrder(),
        script.getName(),
        segment.getId());
  }

  /**
   * Copies the beat's title stakes onto the segment when adjudication has not run yet, so the
   * normal segment adjudication path (title change/defense + contender outcomes) applies the
   * outcome exactly once. Returns false — leaving the beat-level {@link #applyContenderDesignation}
   * fallback in charge — when the segment was already adjudicated (booker re-linked a finished
   * segment; adjudication would not re-run on it).
   */
  private boolean copyTitleContextToSegment(FeudScriptBeat beat, Segment segment) {
    if (segment.getAdjudicationStatus() == AdjudicationStatus.ADJUDICATED) {
      return false;
    }
    boolean changed = false;
    if (beat.isTitleStakes() && !beat.getTitles().isEmpty()) {
      segment.setIsTitleSegment(true);
      segment.getTitles().clear();
      beat.getTitles().forEach(segment.getTitles()::add);
      changed = true;
    }
    if (beat.getContenderTitle() != null && !beat.isTitleStakes()) {
      segment.setContenderMatch(true);
      segment.getTitles().add(beat.getContenderTitle());
      changed = true;
    }
    if (changed) {
      segmentService.saveSegment(segment);
    }
    return changed;
  }

  /** True when every beat of the script is COMPLETED or SKIPPED. */
  private boolean isScriptComplete(FeudScript script) {
    return script.getBeats().stream()
        .allMatch(
            b ->
                b.getBeatStatus() == FeudScriptBeatStatus.COMPLETED
                    || b.getBeatStatus() == FeudScriptBeatStatus.SKIPPED);
  }

  /**
   * Marks the beat as SKIPPED — the booker is walking the arc past it (e.g. feud participants
   * injured). Cancels the beat's PLE reservation if any and completes the script when this was the
   * last outstanding beat.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public FeudScriptBeat skipBeat(@NonNull FeudScript script, @NonNull FeudScriptBeat beat) {
    script = reattachScript(script);
    if (beat.getId() != null) {
      beat = feudScriptBeatRepository.findById(beat.getId()).orElse(beat);
    }
    if (beat.getReservation() != null) {
      reservationService.cancelReservation(beat.getReservation());
      beat.setReservation(null);
    }
    beat.setBeatStatus(FeudScriptBeatStatus.SKIPPED);
    FeudScriptBeat saved = feudScriptBeatRepository.save(beat);
    if (isScriptComplete(script)) {
      script.setStatus(FeudScriptStatus.COMPLETED);
      feudScriptRepository.save(script);
      eventPublisher.publishEvent(new FeudScriptCompletedEvent(this, script));
      log.info("Story arc '{}' completed (last beat skipped)", script.getName());
    }
    log.info("Skipped beat #{} of arc '{}'", saved.getBeatOrder(), script.getName());
    return saved;
  }

  /**
   * Marks every PENDING beat targeted at {@code show} as BOOKED — called from show planning after
   * its segments are approved. Booked beats drop out of AI planning queries (PENDING-only) so the
   * slot is not double-booked, while the beat grid keeps rendering them (BOOKED is pending-like for
   * display and still editable).
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void markBeatsBookedForShow(@NonNull Show show) {
    if (show.getId() == null) {
      return;
    }
    List<FeudScriptBeat> targeted = feudScriptBeatRepository.findPendingBeatsForShow(show.getId());
    for (FeudScriptBeat beat : targeted) {
      beat.setBeatStatus(FeudScriptBeatStatus.BOOKED);
      feudScriptBeatRepository.save(beat);
    }
    if (!targeted.isEmpty()) {
      log.info(
          "Marked {} targeted beat(s) of show '{}' as BOOKED", targeted.size(), show.getName());
    }
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
   * winner, culmination, notes, target show, title stakes, contender designation, external
   * participants and custom team layout) from {@code edited}. Only pending beats may be edited —
   * completed/skipped beats are immutable history. Re-runs creation-time external participant
   * validation and the PLE appearance cap (relevant when the cap was lowered after the beat was
   * created). When the target show changes, the old PLE reservation is cancelled and — for a PLE —
   * a new one reserved; when the show is unchanged, the existing reservation is preserved.
   *
   * <p>UI dialogs hold entities detached from the render request's session; the script, existing
   * beat, wrestler references, target show and titles are re-attached by id so LAZY associations
   * resolve inside this transaction instead of throwing {@code no session}.
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
    // Resolve the incoming target show (may be null = show-agnostic) and swap reservations when
    // the beat moves between shows.
    Show newShow = reattachShow(edited.getTargetShow());
    Show currentShow = managed.getTargetShow();
    boolean showChanged =
        !Objects.equals(
            currentShow == null ? null : currentShow.getId(),
            newShow == null ? null : newShow.getId());
    if (showChanged && managed.getReservation() != null) {
      reservationService.cancelReservation(managed.getReservation());
      managed.setReservation(null);
    }
    edited.setTargetShow(newShow);
    edited.setPlannedWinner(reattachWrestler(edited.getPlannedWinner()));
    edited.setContenderTitle(reattachTitle(edited.getContenderTitle()));
    edited.setTitles(reattachTitles(edited.getTitles()));
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
    managed.setTargetShow(newShow);
    managed.setTitleStakes(edited.isTitleStakes());
    managed.setTitles(edited.getTitles());
    managed.setContenderTitle(edited.getContenderTitle());

    // Replace external participants wholesale (upsert + removal via orphanRemoval).
    managed.getExternalParticipants().clear();
    for (FeudScriptBeatParticipant external : edited.getExternalParticipants()) {
      managed.addExternalParticipant(
          external.getWrestler(), external.getRole(), external.getTeamNumber());
    }

    // (Re)create the PLE reservation after the cap validation, mirroring addBeat.
    if (newShow != null && newShow.isPremiumLiveEvent() && managed.getReservation() == null) {
      String label = script.getName() + " — " + managed.getSegmentType();
      var reservation =
          reservationService.reserveSlot(
              newShow, ShowSegmentReservationPurpose.FEUD_BLOWOFF, script.getId(), label);
      managed.setReservation(reservation);
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

  /** Show variant of {@link #reattachScript}: resolves a detached show by id. */
  private Show reattachShow(Show show) {
    if (show == null || show.getId() == null) {
      return show;
    }
    return showService.getShowById(show.getId()).orElse(show);
  }

  /** Title variant of {@link #reattachScript}: resolves a detached title by id. */
  private Title reattachTitle(Title title) {
    if (title == null || title.getId() == null) {
      return title;
    }
    return titleService.getTitleById(title.getId()).orElse(title);
  }

  /** Re-attaches every title of the beat's stakes set by id. */
  private Set<Title> reattachTitles(Set<Title> titles) {
    if (titles == null || titles.isEmpty()) {
      return new HashSet<>();
    }
    Set<Title> resolved = new HashSet<>();
    for (Title title : titles) {
      resolved.add(reattachTitle(title));
    }
    return resolved;
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
   * Validates the beat's participants: no null wrestler/role, no arc member doubling as an
   * OPPONENT/EXTRA, no wrestler on both external roles, and — when intergender matches are disabled
   * and the beat is not a promo — no external whose gender differs from a feud participant's. Under
   * a custom team layout (FEUD_MEMBER rows present), every arc participant must be placed exactly
   * once and no wrestler may sit on two teams. Show-template gender constraints remain the
   * authoritative check at card validation, because the target show is unknown at beat-creation
   * time.
   */
  private void validateExternals(FeudScript script, FeudScriptBeat beat) {
    Set<Long> feudParticipantIds = participantIdsOf(beat);
    Set<Long> seen = new HashSet<>();
    boolean customTeams = false;
    Set<Long> placed = new HashSet<>();
    for (FeudScriptBeatParticipant external : beat.getExternalParticipants()) {
      if (external.getWrestler() == null || external.getWrestler().getId() == null) {
        throw new IllegalStateException("External participant requires a wrestler");
      }
      if (external.getRole() == null) {
        throw new IllegalStateException(
            "External participant " + external.getWrestler().getName() + " requires a role");
      }
      if (external.getRole() == FeudBeatParticipantRole.FEUD_MEMBER) {
        customTeams = true;
        if (!feudParticipantIds.contains(external.getWrestler().getId())) {
          throw new IllegalStateException(
              external.getWrestler().getName()
                  + " is not part of this arc and cannot be marked as a feud member");
        }
        if (external.getTeamNumber() == null || external.getTeamNumber() < 1) {
          throw new IllegalStateException(
              external.getWrestler().getName() + " requires a team number in a custom layout");
        }
        if (!seen.add(external.getWrestler().getId())) {
          throw new IllegalStateException(
              external.getWrestler().getName() + " cannot be placed on more than one team");
        }
        placed.add(external.getWrestler().getId());
        continue;
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
    if (customTeams) {
      Set<Long> missing = new HashSet<>(feudParticipantIds);
      missing.removeAll(placed);
      if (!missing.isEmpty()) {
        throw new IllegalStateException(
            "Custom team layout must place every arc participant; missing: "
                + missing.stream()
                    .map(
                        id ->
                            feudParticipantsOf(beat).stream()
                                .filter(w -> w.getId().equals(id))
                                .findFirst()
                                .map(Wrestler::getName)
                                .orElse("wrestler #" + id))
                    .collect(Collectors.joining(", ")));
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
      if (feudGender != null
          && external.getRole() != FeudBeatParticipantRole.FEUD_MEMBER
          && external.getWrestler().getGender() != feudGender) {
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

  /**
   * Reuses an existing feud by name when one exists — {@link MultiWrestlerFeudService#createFeud}
   * refuses duplicate names, which previously surfaced as a raw failure; only throw when the feud
   * truly cannot be resolved.
   */
  private MultiWrestlerFeud findOrCreateFeud(String name, List<Wrestler> wrestlers) {
    List<Long> wrestlerIds = wrestlers.stream().map(Wrestler::getId).collect(Collectors.toList());
    return multiWrestlerFeudService
        .createFeud(name, "Script-driven feud", "Script-driven feud", wrestlerIds)
        .orElseGet(
            () ->
                multiWrestlerFeudService
                    .getFeudByName(name)
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "Failed to create or find multi-wrestler feud '" + name + "'")));
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
    List<String> participantNames = new ArrayList<>();
    List<Long> participantIds = new ArrayList<>();
    Long rivalryId = null;
    if (script.getRivalry() != null) {
      Rivalry r = script.getRivalry();
      participantNames = List.of(r.getWrestler1().getName(), r.getWrestler2().getName());
      participantIds = List.of(r.getWrestler1().getId(), r.getWrestler2().getId());
      rivalryId = r.getId();
    } else if (script.getFeud() != null) {
      List<Wrestler> activeMembers =
          script.getFeud().getParticipants().stream()
              .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
              .map(p -> p.getWrestler())
              .collect(Collectors.toList());
      participantNames = activeMembers.stream().map(Wrestler::getName).collect(Collectors.toList());
      participantIds = activeMembers.stream().map(Wrestler::getId).collect(Collectors.toList());
    }
    // Display string for prompts/UI hints — NOT a source of structured team data.
    dto.setParticipantNames(String.join(" vs ", participantNames));
    dto.setParticipantIds(participantIds);
    dto.setRivalryId(rivalryId);

    // Title stakes: title match (winner wins/retains) and #1 contender designation.
    if (beat.isTitleStakes()) {
      dto.setTitleSegment(true);
      dto.setTitles(new ArrayList<>(beat.getTitles()));
    }
    if (beat.getContenderTitle() != null) {
      dto.setContenderTitleId(beat.getContenderTitle().getId());
      dto.setContenderTitleName(beat.getContenderTitle().getName());
    }
    if (beat.getTargetShow() != null) {
      dto.setTargetShowId(beat.getTargetShow().getId());
      dto.setTargetShowName(beat.getTargetShow().getName());
      dto.setTargetShowDate(beat.getTargetShow().getShowDate());
    }

    if (beat.hasCustomTeams()) {
      // Explicit per-beat layout: every participant row carries its own team number.
      dto.setCustomTeams(true);
      Map<Integer, List<Wrestler>> layout = beat.getExplicitTeamLayout();
      List<List<String>> teams = new ArrayList<>();
      List<List<Long>> teamIds = new ArrayList<>();
      List<String> summary = new ArrayList<>();
      for (Map.Entry<Integer, List<Wrestler>> entry : layout.entrySet()) {
        teams.add(entry.getValue().stream().map(Wrestler::getName).collect(Collectors.toList()));
        teamIds.add(entry.getValue().stream().map(Wrestler::getId).collect(Collectors.toList()));
        summary.add(
            "Team "
                + entry.getKey()
                + ": "
                + entry.getValue().stream()
                    .map(Wrestler::getName)
                    .collect(Collectors.joining(", ")));
      }
      dto.setTeams(teams);
      dto.setTeamIds(teamIds);
      dto.setExternalSummary(String.join(" | ", summary));
    } else {
      // Quick-path: feud wrestlers = team 1, external opponent + extras = team 2.
      List<Wrestler> opponents = beat.getExternalOpponents();
      List<Wrestler> extras = beat.getExternalExtras();
      if (!opponents.isEmpty() || !extras.isEmpty()) {
        List<List<String>> teams = new ArrayList<>();
        teams.add(new ArrayList<>(participantNames));
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
    }
    return dto;
  }
}
