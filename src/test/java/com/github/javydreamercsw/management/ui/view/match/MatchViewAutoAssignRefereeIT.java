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
package com.github.javydreamercsw.management.ui.view.match;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.npc.NpcType;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests that verify the referee auto-assignment security model:
 *
 * <ol>
 *   <li>A PLAYER-role user who does not own the segment <em>cannot</em> call {@code
 *       segmentService.updateSegment} directly — the real {@code @PreAuthorize} gate throws {@link
 *       AuthorizationDeniedException}.
 *   <li>The same update <em>succeeds</em> when wrapped in {@link
 *       GeneralSecurityUtils#runAsAdmin(Runnable)}, which is the pattern used by {@code
 *       MatchView.autoAssignRefereeIfNeeded()}.
 * </ol>
 */
@Transactional
class MatchViewAutoAssignRefereeIT extends AbstractMockUserIntegrationTest {

  @Autowired private SegmentService segmentService;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private NpcRepository npcRepository;

  private Segment leagueSegment;
  private Npc referee;
  private Account playerAccount;

  @BeforeEach
  void setUpLeagueSegmentAndPlayerUser() {
    // Create a plain league segment (no universe, no campaign — canUserUpdateSegment returns false
    // for a PLAYER who doesn't own the segment, or any PLAYER on a plain league match).
    ShowType showType =
        showTypeRepository.findAll().stream()
            .findFirst()
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setDescription("Weekly show");
                  return showTypeRepository.saveAndFlush(st);
                });

    Show show = new Show();
    show.setName("Test League Show");
    show.setDescription("IT test show");
    show.setType(showType);
    show.setShowDate(LocalDate.now());
    show = showRepository.saveAndFlush(show);

    SegmentType matchType =
        segmentTypeRepository.findAll().stream()
            .filter(st -> !"Promo".equalsIgnoreCase(st.getName()))
            .findFirst()
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("Singles Match");
                  return segmentTypeRepository.saveAndFlush(st);
                });

    leagueSegment = new Segment();
    leagueSegment.setShow(show);
    leagueSegment.setSegmentType(matchType);
    leagueSegment = segmentRepository.saveAndFlush(leagueSegment);

    // Referee NPC used to verify the update path.
    Npc ref = new Npc();
    ref.setName("IT Test Referee");
    ref.setNpcType(NpcType.REFEREE.getName());
    ref.setExpansionCode("TEST");
    ref.setAlignment(AlignmentType.NEUTRAL);
    referee = npcRepository.saveAndFlush(ref);

    // Create a PLAYER-role account that owns no wrestler in this segment.
    playerAccount = createTestAccount("it-player-user", RoleName.PLAYER);
  }

  @Test
  void playerRole_directUpdateSegment_throwsAuthorizationDenied() {
    // Log in as the player who has no ownership of this segment.
    loginAs(playerAccount.getUsername());

    // Direct call must be denied: canUserUpdateSegment() returns false because
    // the segment is a plain league match (no universe, no campaign) and the
    // player is not a participant.
    leagueSegment.setReferee(referee);
    final Segment toUpdate = leagueSegment;
    assertThrows(AuthorizationDeniedException.class, () -> segmentService.updateSegment(toUpdate));
  }

  @Test
  void playerRole_updateSegmentViaRunAsAdmin_succeeds() {
    // Log in as the player who has no ownership of this segment.
    loginAs(playerAccount.getUsername());

    // Via runAsAdmin the security context is elevated → updateSegment must succeed.
    leagueSegment.setReferee(referee);
    final Segment toUpdate = leagueSegment;
    GeneralSecurityUtils.runAsAdmin(() -> segmentService.updateSegment(toUpdate));

    // Fetch fresh from DB to confirm the referee was persisted.
    Segment persisted =
        segmentRepository
            .findByIdWithDetails(leagueSegment.getId())
            .orElseThrow(() -> new AssertionError("Segment not found after runAsAdmin update"));
    assertNotNull(persisted.getReferee(), "Referee must be persisted even for PLAYER-role viewer");
  }
}
