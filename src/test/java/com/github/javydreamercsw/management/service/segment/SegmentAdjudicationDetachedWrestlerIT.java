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
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.StatusCardRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatus;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Regression test for: LazyInitializationException when adjudicating a segment whose wrestler
 * participants are detached (no open Hibernate session) and the status-cards mechanic is enabled.
 *
 * <p>Real-world repro: MatchView loads a Segment in one HTTP request/session, stores it in a Vaadin
 * component field, then calls adjudicateMatch() from a button-click handler in a later request. By
 * then the session is closed, and {@code Wrestler.statuses} (FetchType.LAZY) cannot be initialized,
 * causing the crash.
 *
 * <p>This class is intentionally NOT annotated with {@code @Transactional} so that each {@code
 * transactionTemplate.execute()} call creates a real committed transaction and the entities are
 * truly detached between steps.
 */
class SegmentAdjudicationDetachedWrestlerIT extends AbstractMockUserIntegrationTest {

  @Autowired private SegmentAdjudicationService segmentAdjudicationService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private StatusCardRepository statusCardRepository;
  @Autowired private WrestlerStatusRepository wrestlerStatusRepository;
  @Autowired private GameSettingService gameSettingService;

  /**
   * Adjudicating a match must succeed even when the segment's wrestlers are detached (i.e. their
   * lazy {@code statuses} collection was never initialized in the calling session), as long as the
   * status-cards mechanic is enabled and at least one wrestler has a status.
   *
   * <p>Before the fix this throws {@code LazyInitializationException} from {@code
   * Wrestler.getEffectiveStartingMomentum()}.
   */
  @Test
  void testAdjudicateMatchSucceedsWithDetachedWrestlerHavingStatus() {
    // ── Step 1: Persist all required data in a committed transaction ──────────────────────────
    Long[] segmentIdHolder = new Long[1];

    transactionTemplate.execute(
        txStatus -> {
          // Enable the status-cards mechanic so the trigger-evaluation block executes
          gameSettingService.setStatusCardsEnabled(true);

          // Wrestler WITH a status (the one that triggers the lazy-load bug)
          Wrestler winner =
              Wrestler.builder()
                  .name("Detach Test Winner")
                  .startingHealth(13)
                  .startingStamina(14)
                  .lowHealth(2)
                  .lowStamina(2)
                  .deckSize(16)
                  .isPlayer(false)
                  .build();
          winner = wrestlerRepository.save(winner);
          wrestlerService.getOrCreateState(winner.getId(), defaultUniverse.getId());

          // Assign a status card with a momentum effect so getEffectiveStartingMomentum()
          // actually iterates the statuses collection
          StatusCard statusCard =
              StatusCard.builder()
                  .key("test-momentum-boost")
                  .level1Name("Focused")
                  .level1Effect("momentum:1")
                  .description("Test status card for regression")
                  .positive(true)
                  .build();
          statusCardRepository.save(statusCard);

          WrestlerStatus wrestlerStatus =
              WrestlerStatus.builder().wrestler(winner).statusCard(statusCard).level(1).build();
          wrestlerStatusRepository.save(wrestlerStatus);

          // Opponent (no status — just needs to exist for the segment)
          Wrestler loser =
              Wrestler.builder()
                  .name("Detach Test Loser")
                  .startingHealth(13)
                  .startingStamina(14)
                  .lowHealth(2)
                  .lowStamina(2)
                  .deckSize(16)
                  .isPlayer(false)
                  .build();
          loser = wrestlerRepository.save(loser);
          wrestlerService.getOrCreateState(loser.getId(), defaultUniverse.getId());

          // Show + segment scaffolding
          ShowType showType = new ShowType();
          showType.setName("Detach Regression Show Type");
          showType.setDescription("Regression test");
          showTypeRepository.save(showType);

          Show show = new Show();
          show.setName("Detach Regression Show");
          show.setDescription("Regression test");
          show.setShowDate(LocalDate.now());
          show.setType(showType);
          show.setUniverse(defaultUniverse);
          showRepository.save(show);

          SegmentType segmentType = new SegmentType();
          segmentType.setName("Detach Regression Match");
          segmentTypeRepository.save(segmentType);

          Segment segment = new Segment();
          segment.setShow(show);
          segment.setSegmentType(segmentType);
          segment.addParticipant(winner);
          segment.addParticipant(loser);

          // Capture winner's ID for the lambda (must be effectively final)
          final Wrestler finalWinner = winner;
          segment.setWinners(List.of(finalWinner));
          segmentRepository.save(segment);

          segmentIdHolder[0] = segment.getId();
          return null;
        });

    // ── Step 2: Load the segment the way MatchView does ───────────────────────────────────────
    // A separate (committed) transaction loads the segment and initialises the participants
    // collection, but deliberately does NOT touch each wrestler's statuses.  After this
    // transaction commits the session is gone and the wrestlers are detached.
    Segment[] detachedSegment = new Segment[1];
    transactionTemplate.execute(
        txStatus -> {
          Segment segment = segmentRepository.findById(segmentIdHolder[0]).orElseThrow();
          // Touch participants so that collection is initialised (MatchView renders them),
          // but do NOT access wrestler.getStatuses() — that collection stays uninitialized.
          int participantCount = segment.getWrestlers().size();
          assert participantCount == 2 : "expected 2 participants";
          detachedSegment[0] = segment;
          return null;
        });
    // detachedSegment[0] is now detached: its wrestlers' statuses PersistentBag is uninitialized.

    // ── Step 3: Adjudicate by ID (as MatchView does — no surrounding transaction) ─────────────
    // MatchView calls adjudicateMatch(segment.getId()) after saving winners so the service loads
    // the segment fresh inside @Transactional — no lazy proxy issues.
    // Before the fix: LazyInitializationException from Wrestler.getEffectiveStartingMomentum()
    // After the fix:  succeeds cleanly
    assertDoesNotThrow(
        () -> segmentAdjudicationService.adjudicateMatch(segmentIdHolder[0]),
        "adjudicateMatch(Long) must not throw LazyInitializationException for detached wrestlers");
  }
}
