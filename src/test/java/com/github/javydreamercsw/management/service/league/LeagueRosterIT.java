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
package com.github.javydreamercsw.management.service.league;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class LeagueRosterIT extends ManagementIntegrationTest {

  @Autowired private LeagueRepository leagueRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private LeagueRosterRepository leagueRosterRepository;
  @Autowired private SegmentAdjudicationService segmentAdjudicationService;

  @Test
  @Transactional
  void testAdjudicationUpdatesStats() {
    Account admin = new Account("admin_roster_test", "password123", "roster@test.com");
    accountRepository.save(admin);

    League league = new League();
    league.setName("Stats League");
    league.setCommissioner(admin);
    leagueRepository.save(league);

    ShowType type = new ShowType();
    type.setName("League Show");
    type.setDescription("Desc");
    showTypeRepository.save(type);

    Show show = new Show();
    show.setName("Show 1");
    show.setDescription("Desc");
    show.setType(type);
    show.setLeague(league);
    show.setShowDate(LocalDate.now());
    showRepository.save(show);

    Wrestler winner = new Wrestler();
    winner.setName("Winner");
    wrestlerRepository.save(winner);

    Wrestler loser = new Wrestler();
    loser.setName("Loser");
    wrestlerRepository.save(loser);

    LeagueRoster winnerRoster = new LeagueRoster();
    winnerRoster.setLeague(league);
    winnerRoster.setOwner(admin);
    winnerRoster.setWrestler(winner);
    leagueRosterRepository.save(winnerRoster);

    LeagueRoster loserRoster = new LeagueRoster();
    loserRoster.setLeague(league);
    loserRoster.setOwner(admin);
    loserRoster.setWrestler(loser);
    leagueRosterRepository.save(loserRoster);

    SegmentType oneOnOne =
        segmentTypeRepository
            .findByName("One on One")
            .orElseGet(
                () -> {
                  SegmentType type1 = new SegmentType();
                  type1.setName("One on One");
                  return segmentTypeRepository.save(type1);
                });

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(oneOnOne);
    segment.addParticipant(winner);
    segment.addParticipant(loser);
    segment.setWinners(List.of(winner));
    segmentRepository.save(segment);

    // Adjudicate
    segmentAdjudicationService.adjudicateMatch(segment);

    // Verify Stats
    LeagueRoster winnerStats =
        leagueRosterRepository.findByLeagueAndWrestler(league, winner).orElseThrow();
    assertThat(winnerStats.getWins()).isEqualTo(1);
    assertThat(winnerStats.getLosses()).isEqualTo(0);

    LeagueRoster loserStats =
        leagueRosterRepository.findByLeagueAndWrestler(league, loser).orElseThrow();
    assertThat(loserStats.getWins()).isEqualTo(0);
    assertThat(loserStats.getLosses()).isEqualTo(1);
  }
}
