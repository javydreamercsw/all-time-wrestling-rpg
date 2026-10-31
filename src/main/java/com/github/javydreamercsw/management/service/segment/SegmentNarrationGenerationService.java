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
package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentResult;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates AI narration for a segment as part of the Adjudicate Show flow (2.13.0). One call = one
 * segment = one committed transaction (REQUIRES_NEW): a crash keeps already-generated narrations,
 * and no DB connection is held across the minutes an AI provider call can take.
 *
 * <p>Before building the narration context the service enriches the segment the way a booker would:
 * auto-assigns a referee to non-promo segments missing one (reusing the same avoid-double-booking
 * logic as MatchView), and supplies the announce team — ring announcer and commentators — as
 * context-only NPCs (they are not persisted on the segment).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SegmentNarrationGenerationService {

  private final SegmentRepository segmentRepository;
  private final NpcService npcService;
  private final WrestlerStatsService wrestlerStatsService;
  private final RivalryService rivalryService;
  private final UniverseContextService universeContextService;
  private final SegmentNarrationServiceFactory narrationServiceFactory;

  /** What the generation pass produced; null fields mean "not generated" (nothing changed). */
  public record GeneratedNarration(
      String narration, String summary, Npc referee, int refereeAwareness) {}

  /**
   * Whether the narration pass would actually have a provider to call. Backs the Adjudicate
   * pre-flight: the dialog says up front when segments will be adjudicated with their booked
   * descriptions because no AI provider is configured.
   */
  public boolean isNarrationProviderAvailable() {
    SegmentNarrationService service = narrationServiceFactory.getBestAvailableService();
    return service != null && service.isAvailable();
  }

  /**
   * Generates narration for the segment (if a provider is available), persists it with {@code
   * narrationGenerated} set, fills a blank summary from the fresh narration, and returns what was
   * produced so the caller can apply the same values to its own (possibly stale) entity instance.
   *
   * <p>Returning the generated state (instead of making the caller re-read the segment) matters on
   * MySQL: the caller's transaction pinned a REPEATABLE READ snapshot before this REQUIRES_NEW
   * commit, so a re-read there would still see the pre-generation row.
   *
   * <p>Failure posture: throws on AI failure so the caller (ShowService) can warn-and-continue per
   * segment; returns {@code null} when nothing was generated (segment missing/already flagged or no
   * provider available), leaving the segment untouched.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public GeneratedNarration generateForSegment(@NonNull final Long segmentId) {
    Segment segment = segmentRepository.findByIdWithDetails(segmentId).orElse(null);
    if (segment == null) {
      log.warn("Cannot generate narration: segment {} not found", segmentId);
      return null;
    }
    if (segment.isNarrationGenerated()) {
      return null;
    }

    SegmentNarrationService narrationService = narrationServiceFactory.getBestAvailableService();
    if (narrationService == null || !narrationService.isAvailable()) {
      log.info("No AI narration provider available — skipping narration for segment {}", segmentId);
      return null;
    }

    enrichSegment(segment);
    SegmentNarrationContext context = buildContext(segment);

    String narration = narrationService.narrateSegment(context);
    if (narration == null || narration.isBlank()) {
      log.warn("AI provider returned empty narration for segment {}", segmentId);
      return null;
    }
    segment.setNarration(narration);
    segment.setNarrationGenerated(true);

    String summary = null;
    if (segment.getSummary() == null || segment.getSummary().isBlank()) {
      try {
        summary = narrationServiceFactory.summarizeNarration(narration);
        segment.setSummary(summary);
      } catch (Exception e) {
        // Summary is a nice-to-have; narration failure posture applies to it too.
        log.warn("Failed to summarize narration for segment {}: {}", segmentId, e.getMessage());
      }
    }
    segmentRepository.save(segment);
    log.info("Generated narration for segment {} ({} chars)", segmentId, narration.length());
    return new GeneratedNarration(
        narration,
        summary,
        segment.getReferee(),
        segment.getReferee() != null ? segment.getRefereeAwarenessLevel() : 0);
  }

  /**
   * Auto-assigns a referee (non-promo only) — announce-team NPCs are context-only, not persisted.
   */
  private void enrichSegment(@NonNull final Segment segment) {
    boolean isPromo =
        segment.getSegmentType() != null
            && WellKnownSegmentType.PROMO.matches(segment.getSegmentType());
    if (isPromo || segment.getReferee() != null) {
      return;
    }
    List<Npc> referees = npcService.findAllByType("Referee");
    if (referees.isEmpty()) {
      return;
    }
    List<Npc> assignedReferees = new ArrayList<>();
    if (segment.getShow() != null) {
      for (Segment sibling : segmentRepository.findByShow(segment.getShow())) {
        if (Objects.equals(sibling.getId(), segment.getId())) {
          continue;
        }
        Npc used = sibling.getReferee();
        if (used != null) {
          assignedReferees.add(used);
        }
      }
    }
    Npc referee = npcService.pickUnusedReferee(referees, assignedReferees);
    if (referee != null) {
      segment.setReferee(referee);
      segment.setRefereeAwarenessLevel(npcService.getAwareness(referee));
    }
  }

  private SegmentNarrationContext buildContext(@NonNull final Segment segment) {
    SegmentNarrationContext context = new SegmentNarrationContext();
    context.setUniverseId(universeContextService.getCurrentUniverseId());
    context.setSegmentOrder(segment.getSegmentOrder());
    context.setMainEvent(segment.isMainEvent());
    context.setNarration(segment.getNarration());
    context.setInstructions(segment.getNotes());

    if (segment.getTitles() != null && !segment.getTitles().isEmpty()) {
      context.setSegmentChampionship(
          segment.getTitles().stream()
              .map(t -> t.getName())
              .reduce((a, b) -> a + " and " + b)
              .orElse(null));
      context.setTitleMatch(true);
    }

    // Declared result feeds the AI so a booked draw/no-contest is narrated as one
    // instead of the AI inventing a winner.
    SegmentResult result = segment.getResult();
    if (result == SegmentResult.DRAW) {
      context.setDeterminedOutcome("This match is booked to end in a draw (no winner).");
    } else if (result == SegmentResult.NO_CONTEST) {
      context.setDeterminedOutcome("This match is booked to end in a no-contest (no winner).");
    } else if (segment.getSummary() != null && !segment.getSummary().isBlank()) {
      context.setDeterminedOutcome(segment.getSummary());
    }

    List<WrestlerContext> wrestlerContexts = new ArrayList<>();
    List<WrestlerDTO> wrestlers =
        wrestlerStatsService.findAllBySegment(
            segment, universeContextService.getCurrentUniverseId());
    for (WrestlerDTO wrestler : wrestlers) {
      WrestlerContext wc = new WrestlerContext();
      wc.setName(wrestler.getName());
      wc.setDescription(wrestler.getDescription());
      wc.setTeam("Team " + (wrestlerContexts.size() + 1));
      wc.setGender(wrestler.getGender());
      wc.setTier(wrestler.getTier() != null ? wrestler.getTier().name() : "ROOKIE");
      wc.setMoveSet(wrestler.getMoveSet());
      List<String> feuds = new ArrayList<>();
      for (RivalryDTO rivalry :
          rivalryService.getActiveRivalriesForWrestlerAsDTO(wrestler.getId())) {
        WrestlerDTO opponent = rivalry.getOpponent(wrestler);
        feuds.add("Feuding with %s (Heat: %d)".formatted(opponent.getName(), rivalry.getHeat()));
      }
      wc.setFeudsAndHeat(feuds);
      wrestlerContexts.add(wc);
    }
    context.setWrestlers(wrestlerContexts);

    SegmentNarrationService.SegmentTypeContext mtc =
        new SegmentNarrationService.SegmentTypeContext();
    mtc.setSegmentType(
        segment.getSegmentType() != null ? segment.getSegmentType().getName() : "Segment");
    context.setSegmentType(mtc);

    context.setReferee(toRefereeContext(segment.getReferee()));
    context.setNpcs(buildAnnounceTeam(segment));
    return context;
  }

  private SegmentNarrationService.RefereeContext toRefereeContext(final Npc referee) {
    if (referee == null) {
      return null;
    }
    SegmentNarrationService.RefereeContext rc = new SegmentNarrationService.RefereeContext();
    rc.setName(referee.getName());
    rc.setDescription("Referee");
    rc.setAttributes(referee.getAttributes());
    return rc;
  }

  /**
   * The announce team as context-only NPCs: one ring announcer from the Announcer pool and the
   * show's commentary team (from its template) when one is assigned — falling back to the full
   * Commentator pool when the show has none, mirroring NarrationDialog's defaults.
   */
  private List<NPCContext> buildAnnounceTeam(@NonNull final Segment segment) {
    List<NPCContext> npcs = new ArrayList<>();
    List<Npc> announcers = npcService.findAllByTypeUnfiltered("Announcer");
    if (!announcers.isEmpty()) {
      Npc announcer = announcers.get(ThreadLocalRandom.current().nextInt(announcers.size()));
      NPCContext npc = new NPCContext();
      npc.setName(announcer.getName());
      npc.setRole("Ring Announcer");
      npc.setDescription("Wrestling ring announcer");
      npcs.add(npc);
    }
    CommentaryTeam team = segment.getShow() != null ? segment.getShow().getCommentaryTeam() : null;
    if (team != null && team.getCommentators() != null && !team.getCommentators().isEmpty()) {
      // The show's assigned announce team speaks this card.
      for (Commentator commentator : team.getCommentators()) {
        if (commentator.getNpc() == null) {
          continue;
        }
        NPCContext npc = new NPCContext();
        npc.setName(commentator.getNpc().getName());
        npc.setRole("Commentator");
        npc.setDescription(
            commentator.getStyle() != null
                ? commentator.getStyle() + " commentator"
                : "Wrestling commentator");
        npcs.add(npc);
      }
    } else {
      for (Npc commentator : npcService.findAllByTypeUnfiltered("Commentator")) {
        NPCContext npc = new NPCContext();
        npc.setName(commentator.getName());
        npc.setRole("Commentator");
        npc.setDescription("Wrestling commentator");
        npcs.add(npc);
      }
    }
    return npcs;
  }
}
