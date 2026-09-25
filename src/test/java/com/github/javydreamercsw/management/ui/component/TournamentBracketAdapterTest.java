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
package com.github.javydreamercsw.management.ui.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchParticipant;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.service.tournament.QualifierGroupsFormat;
import com.github.javydreamercsw.management.service.tournament.RoundRobinFormat;
import com.github.javydreamercsw.management.service.tournament.SingleEliminationFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import com.github.javydreamercsw.management.ui.component.TournamentBracketModel.MatchModel;
import com.github.javydreamercsw.management.ui.component.TournamentBracketModel.MatchModel.ExtraEntrant;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TournamentBracketAdapterTest {

  // ── TournamentDTOAdapter ──────────────────────────────────────────────────

  @Test
  void dtoAdapter_renderModeIsAlwaysTree() {
    TournamentDTOAdapter adapter = new TournamentDTOAdapter(dtoWith1Match());
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.TREE);
  }

  @Test
  void dtoAdapter_forwardsRoundAndEntrantInfo() {
    TournamentDTO dto = dtoWith1Match();
    TournamentDTOAdapter adapter = new TournamentDTOAdapter(dto);

    assertThat(adapter.getTotalRounds()).isEqualTo(2);
    assertThat(adapter.getCurrentRound()).isEqualTo(1);

    List<MatchModel> matches = adapter.getMatches();
    assertThat(matches).hasSize(1);

    MatchModel m = matches.get(0);
    assertThat(m.getRound()).isEqualTo(1);
    assertThat(m.getWrestler1Id()).isEqualTo(10L);
    assertThat(m.getWrestler1Name()).isEqualTo("Alpha");
    assertThat(m.getWrestler2Id()).isEqualTo(20L);
    assertThat(m.getWrestler2Name()).isEqualTo("Beta");
    assertThat(m.getWinnerId()).isEqualTo(10L);
    assertThat(m.isPlayerMatch()).isTrue();
  }

  // ── TournamentEntityAdapter ───────────────────────────────────────────────

  @Test
  void entityAdapter_usesFormatRenderMode_singleElimination() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");
    List<TournamentFormat> formats = List.of(new SingleEliminationFormat(), new RoundRobinFormat());

    TournamentEntityAdapter adapter = new TournamentEntityAdapter(t, formats);
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.TREE);
  }

  @Test
  void entityAdapter_usesFormatRenderMode_roundRobin() {
    Tournament t = tournamentEntity("ROUND_ROBIN");
    List<TournamentFormat> formats = List.of(new SingleEliminationFormat(), new RoundRobinFormat());

    TournamentEntityAdapter adapter = new TournamentEntityAdapter(t, formats);
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.ROUND_ROBIN_GRID);
  }

  @Test
  void entityAdapter_defaultsToTreeForUnknownFormat() {
    Tournament t = tournamentEntity("UNKNOWN_FORMAT");
    TournamentEntityAdapter adapter = new TournamentEntityAdapter(t, List.of());
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.TREE);
  }

  @Test
  void entityAdapter_mapsMatchesCorrectly() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    Wrestler w1 = wrestler(1L, "Rocky");
    Wrestler w2 = wrestler(2L, "Austin");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);

    TournamentMatch match = TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();

    TournamentRound round =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .matches(new ArrayList<>(List.of(match)))
            .build();

    t.setRounds(new ArrayList<>(List.of(round)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));

    assertThat(adapter.getTotalRounds()).isEqualTo(1);

    List<MatchModel> matches = adapter.getMatches();
    assertThat(matches).hasSize(1);

    MatchModel m = matches.get(0);
    assertThat(m.getRound()).isEqualTo(1);
    assertThat(m.getWrestler1Id()).isEqualTo(1L);
    assertThat(m.getWrestler1Name()).isEqualTo("Rocky");
    assertThat(m.getWrestler2Id()).isEqualTo(2L);
    assertThat(m.getWrestler2Name()).isEqualTo("Austin");
    assertThat(m.getWinnerId()).isEqualTo(1L);
    assertThat(m.isPlayerMatch()).isFalse();
  }

  @Test
  void entityAdapter_getCurrentRound_inProgressRound() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    TournamentRound r1 = roundWithStatus(1, TournamentRoundStatus.COMPLETE);
    TournamentRound r2 = roundWithStatus(2, TournamentRoundStatus.IN_PROGRESS);
    t.setRounds(new ArrayList<>(List.of(r1, r2)));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));
    assertThat(adapter.getCurrentRound()).isEqualTo(2);
  }

  @Test
  void entityAdapter_getCurrentRound_fallsBackToLastComplete() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    TournamentRound r1 = roundWithStatus(1, TournamentRoundStatus.COMPLETE);
    TournamentRound r2 = roundWithStatus(2, TournamentRoundStatus.COMPLETE);
    t.setRounds(new ArrayList<>(List.of(r1, r2)));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));
    assertThat(adapter.getCurrentRound()).isEqualTo(2);
  }

  @Test
  void entityAdapter_multiEntrantMatch_exposesExtraEntrantNames() {
    // ATW-oloa: a 3-entrant qualifier renders its first two entrants through the classic lines
    // and the rest through the extra-entrants list.
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    Wrestler w1 = wrestler(1L, "Rocky");
    Wrestler w2 = wrestler(2L, "Austin");
    Wrestler w3 = wrestler(3L, "Triple H");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);
    TournamentEntry e3 = entry(w3);

    TournamentMatch match = TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();
    match.setParticipants(
        new ArrayList<>(
            List.of(
                participant(match, e1, 0), participant(match, e2, 1), participant(match, e3, 2))));

    TournamentRound round =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(match)))
            .build();

    t.setRounds(new ArrayList<>(List.of(round)));
    t.setEntries(List.of(e1, e2, e3));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));

    List<MatchModel> matches = adapter.getMatches();
    assertThat(matches).hasSize(1);
    MatchModel m = matches.get(0);
    assertThat(m.getWrestler1Name()).isEqualTo("Rocky");
    assertThat(m.getWrestler2Name()).isEqualTo("Austin");
    assertThat(m.getExtraEntrants()).extracting(ExtraEntrant::name).containsExactly("Triple H");
    assertThat(m.getExtraEntrants()).extracting(ExtraEntrant::wrestlerId).containsExactly(3L);
  }

  @Test
  void entityAdapter_singlesMatch_hasNoExtraEntrants() {
    // The classic two-entrant shape renders no extra lines.
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    TournamentEntry e1 = entry(wrestler(1L, "Rocky"));
    TournamentEntry e2 = entry(wrestler(2L, "Austin"));
    TournamentMatch match = TournamentMatch.builder().entrant1(e1).entrant2(e2).build();
    TournamentRound round =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Round 1")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(match)))
            .build();

    t.setRounds(new ArrayList<>(List.of(round)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));

    assertThat(adapter.getMatches().get(0).getExtraEntrants()).isEmpty();
  }

  private static TournamentMatchParticipant participant(
      TournamentMatch match, TournamentEntry entry, int slot) {
    TournamentMatchParticipant p = new TournamentMatchParticipant();
    p.setMatch(match);
    p.setEntry(entry);
    p.setSlot(slot);
    return p;
  }

  // ── Lazy-bracket regression (sandbox bug): qualifiers ≠ Finals, no premature champion ──

  @Test
  void entityAdapter_incompleteLazyBracket_reportsNotComplete() {
    // Round 1 (Qualifiers) exists with a DECIDED match; the Final still generates lazily.
    // The sandbox bug: the adapter reported the only round as the final → premature champion.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    Wrestler w1 = wrestler(1L, "Shumba");
    Wrestler w2 = wrestler(2L, "Johnny");
    Wrestler w3 = wrestler(3L, "Aldis");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);
    TournamentEntry e3 = entry(w3);
    TournamentMatch decided =
        TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();
    decided.setParticipants(
        new ArrayList<>(
            List.of(
                participant(decided, e1, 0),
                participant(decided, e2, 1),
                participant(decided, e3, 2))));
    TournamentMatch open = TournamentMatch.builder().entrant1(e2).entrant2(e3).build();

    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(decided, open)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2, e3));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    assertThat(adapter.isComplete())
        .as("A bracket with only its qualifier round is NOT complete")
        .isFalse();
    assertThat(adapter.getRoundName(1)).isEqualTo("Qualifiers");
  }

  @Test
  void entityAdapter_completedBracket_reportsComplete() {
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    Wrestler w1 = wrestler(1L, "Shumba");
    Wrestler w2 = wrestler(2L, "Aldis");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);
    TournamentMatch finalMatch =
        TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();
    TournamentRound finalRound =
        TournamentRound.builder()
            .roundNumber(2)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .matches(new ArrayList<>(List.of(finalMatch)))
            .build();
    t.setRounds(new ArrayList<>(List.of(finalRound)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    assertThat(adapter.isComplete()).isTrue();
    assertThat(adapter.getRoundName(2)).isEqualTo("Final");
  }

  @Test
  void entityAdapter_qualifierGroupsRenderMode() {
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.TREE);
  }

  // ── Full-bracket projection (sandbox request): projected rounds + placeholders ──

  @Test
  void entityAdapter_projectsCompleteBracket_forQualifierGroups() {
    // 6 entrants → 2 qualifiers + a 2-slot final, even though no round is persisted yet
    // (lazy generation) — the adapter serves the format's projection.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setEntries(
        List.of(
            entry(wrestler(1L, "Shumba")),
            entry(wrestler(2L, "Johnny")),
            entry(wrestler(3L, "Aldis")),
            entry(wrestler(4L, "Rocky")),
            entry(wrestler(5L, "Austin")),
            entry(wrestler(6L, "Triple H"))));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    Optional<TournamentFormat.BracketProjection> projection = adapter.getProjection();
    assertThat(projection).isPresent();
    TournamentFormat.BracketProjection bracket = projection.orElseThrow();
    assertThat(bracket.roundNames()).containsExactly("Qualifiers", "Final");
    assertThat(bracket.matches()).hasSize(3);
    // Final slots are placeholders: "Winner of Match 1" / "Winner of Match 2".
    assertThat(bracket.matches().get(2).slots())
        .extracting(TournamentFormat.ProjectedSlot::sourceMatchNumber)
        .containsExactly(1, 2);
  }

  @Test
  void component_projectedBracket_rendersPlaceholderFinalBeforeItExists() {
    // THE sandbox case: a bracket whose final hasn't generated shows the full shape —
    // match numbers, "Winner of Match N" placeholders, and no premature champion.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    Wrestler w1 = wrestler(1L, "Shumba");
    Wrestler w2 = wrestler(2L, "Johnny");
    Wrestler w3 = wrestler(3L, "Aldis");
    Wrestler w4 = wrestler(4L, "Rocky");
    Wrestler w5 = wrestler(5L, "Austin");
    Wrestler w6 = wrestler(6L, "Triple H");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);
    TournamentEntry e3 = entry(w3);
    TournamentEntry e4 = entry(w4);
    TournamentEntry e5 = entry(w5);
    TournamentEntry e6 = entry(w6);
    // Match 1 (Shumba/Johnny/Aldis) decided; match 2 (Rocky/Austin/Triple H) still open.
    TournamentMatch decided =
        TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();
    decided.setParticipants(
        new ArrayList<>(
            List.of(
                participant(decided, e1, 0),
                participant(decided, e2, 1),
                participant(decided, e3, 2))));
    TournamentMatch open = TournamentMatch.builder().entrant1(e4).entrant2(e5).build();
    open.setParticipants(
        new ArrayList<>(
            List.of(participant(open, e4, 0), participant(open, e5, 1), participant(open, e6, 2))));
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(decided, open)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2, e3, e4, e5, e6));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    List<String> texts = descendantTexts(component);
    // Round names for BOTH rounds render — the persisted one and the projected final.
    assertThat(texts).contains("Qualifiers", "Final");
    // Match-number chips 1..3 (2 qualifiers + the final).
    assertThat(texts).contains("Match 1", "Match 2", "Match 3");
    // Match 1 is decided — its winner "Shumba" replaces "Winner of Match 1" in the final's slot.
    assertThat(texts).contains("Shumba");
    // Match 2 is still open — the final keeps its "Winner of Match 2" placeholder.
    assertThat(texts).contains("Winner of Match 2");
    assertThat(texts).doesNotContain("Winner of Match 1");
    // Still no champion: the projected final is undecided.
    assertThat(texts).doesNotContain("CHAMPION");
  }

  @Test
  void component_decidedMultiEntrantMatch_strikesAllNonWinners() {
    // THE FFA bug: with Shumba winning a 3-man qualifier, Johnny AND Aldis must both show as
    // losers — everyone who didn't win a decided match, not just the slot-1 entrant.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    Wrestler w1 = wrestler(1L, "Shumba");
    Wrestler w2 = wrestler(2L, "Johnny");
    Wrestler w3 = wrestler(3L, "Aldis");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);
    TournamentEntry e3 = entry(w3);
    TournamentMatch decided =
        TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();
    decided.setParticipants(
        new ArrayList<>(
            List.of(
                participant(decided, e1, 0),
                participant(decided, e2, 1),
                participant(decided, e3, 2))));
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(decided)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2, e3));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    assertThat(strikingNames(component))
        .as("Both non-winners of the decided Free-for-All strike through")
        .containsExactlyInAnyOrder("Johnny", "Aldis");
  }

  @Test
  void component_projectedBracket_rendersConnectorOverlay() {
    // The projection carries sourceMatchNumber edges; the component must attach a connector
    // overlay whose data-bracket-edges lists every src:dst pair (1->3, 2->3 for 2 qualifiers).
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setEntries(
        List.of(
            entry(wrestler(1L, "Shumba")),
            entry(wrestler(2L, "Johnny")),
            entry(wrestler(3L, "Aldis")),
            entry(wrestler(4L, "Rocky")),
            entry(wrestler(5L, "Austin")),
            entry(wrestler(6L, "Triple H"))));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    Element host = findConnectorHost(component);
    assertThat(host).as("Projected bracket renders a connector overlay host").isNotNull();
    assertThat(host.getAttribute("data-bracket-edges")).isEqualTo("1:3,2:3");
    // Cards expose their match number for the client-side measurement.
    assertThat(countDescendantsWithAttribute(component, "data-match-number")).isEqualTo(3);
  }

  @Test
  void component_noProjection_noConnectorOverlay() {
    // The campaign DTO path has no projection (no edges to draw) — no overlay host renders.
    TournamentDTO dto = dtoWith1Match();
    TournamentBracketComponent component = new TournamentBracketComponent(dto);
    assertThat(findConnectorHost(component)).isNull();
  }

  @Test
  void component_projectedBracket_rendersZoomControls() {
    // Projected brackets route their columns through the zoom canvas and get the Fit/100%
    // controls; the toolbar labels are the server-side handle for the client-side scaling.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setEntries(
        List.of(
            entry(wrestler(1L, "Shumba")),
            entry(wrestler(2L, "Johnny")),
            entry(wrestler(3L, "Aldis")),
            entry(wrestler(4L, "Rocky"))));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    List<String> texts = descendantTexts(component);
    assertThat(texts).contains("Fit", "100%");
    // The zoom canvas wraps all round columns (2 qualifiers + final = 3 tagged cards inside).
    assertThat(countDescendantsWithAttribute(component, "data-match-number")).isEqualTo(3);
  }

  @Test
  void component_dtoPath_noZoomControls() {
    // No canvas on the DTO path — no zoom toolbar either.
    TournamentBracketComponent component = new TournamentBracketComponent(dtoWith1Match());
    assertThat(descendantTexts(component)).doesNotContain("Fit");
  }

  @Test
  void entityAdapter_finalRoundLabel_usesPayoffRule() {
    // THE sandbox case: Time Vault's payoff rule is TLC — the Final's label shows
    // "Free-for-All · TLC" (payoff books with the tournament's payoff rule), while the
    // qualifiers keep their pool/format rule.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setPayoffSegmentRule(rule(14L, "Tables, Ladders and Chairs (TLC)"));
    t.setAllowedRules(new ArrayList<>(List.of(rule(21L, "No DQ"), rule(22L, "Gauntlet"))));
    TournamentEntry e1 = entry(wrestler(1L, "Shumba"));
    TournamentEntry e2 = entry(wrestler(2L, "Aldis"));
    TournamentEntry e3 = entry(wrestler(3L, "Rocky"));
    TournamentEntry e4 = entry(wrestler(4L, "Austin"));
    TournamentMatch booked =
        TournamentMatch.builder()
            .entrant1(e1)
            .entrant2(e2)
            .segment(segmentWithRules(rule(22L, "Gauntlet")))
            .build();
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(booked)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2, e3, e4));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    // Round 1 = qualifiers: booked segment rule wins.
    assertThat(adapter.getRoundTypeRuleLabel(1)).isEqualTo("Free-for-All · Gauntlet");
    // Round 2 = the final: the payoff rule applies even though no round-2 row exists yet.
    assertThat(adapter.getRoundTypeRuleLabel(2))
        .isEqualTo("Free-for-All · Tables, Ladders and Chairs (TLC)");
  }

  @Test
  void entityAdapter_finalRound_noPayoffRule_fallsBackToPoolSilence() {
    // Without a payoff rule the final behaves like any unbooked pool round: type only.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setAllowedRules(new ArrayList<>(List.of(rule(21L, "No DQ"))));
    TournamentEntry e1 = entry(wrestler(1L, "Shumba"));
    TournamentEntry e2 = entry(wrestler(2L, "Aldis"));
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>())
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    assertThat(adapter.getRoundTypeRuleLabel(2)).isEqualTo("Free-for-All");
  }

  @Test
  void component_multiRoundSingleElimination_connectorEdgesSpanAllRounds() {
    // An 8-entrant single-elimination bracket projects 3 rounds: edges cascade across two
    // hops (4 quarter-finals → 2 semi-finals → 1 final), 7 tagged cards, 6 edges.
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");
    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 1; i < 8 + 1; i++) {
      entries.add(entry(wrestler((long) i, "Wrestler " + i)));
    }
    t.setEntries(entries);

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    Element host = findConnectorHost(component);
    assertThat(host).as("Projected single-elimination bracket renders the overlay").isNotNull();
    // Quarter-finals 1-4 feed semis 5 (1+2) and 6 (3+4); semis feed the final (match 7).
    assertThat(host.getAttribute("data-bracket-edges")).isEqualTo("1:5,2:5,3:6,4:6,5:7,6:7");
    // Every projected match renders a card — 4 QF + 2 SF + 1 final.
    assertThat(countDescendantsWithAttribute(component, "data-match-number")).isEqualTo(7);
  }

  /** Depth-first search for the bracket-connector-host element. */
  private static Element findConnectorHost(Component root) {
    if (root.getClassNames().contains("bracket-connector-host")) {
      return root.getElement();
    }
    for (Component child : root.getChildren().toList()) {
      Element found = findConnectorHost(child);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /** Counts descendant elements carrying {@code attribute}. */
  private static int countDescendantsWithAttribute(Component root, String attribute) {
    int count =
        root.getElement().getAttribute(attribute) != null
                && root.getElement().hasAttribute(attribute)
            ? 1
            : 0;
    for (Component child : root.getChildren().toList()) {
      count += countDescendantsWithAttribute(child, attribute);
    }
    return count;
  }

  @Test
  void component_undecidedMatch_noLoserStyling() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");
    TournamentEntry e1 = entry(wrestler(1L, "Rocky"));
    TournamentEntry e2 = entry(wrestler(2L, "Austin"));
    TournamentMatch open = TournamentMatch.builder().entrant1(e1).entrant2(e2).build();
    TournamentRound round =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Round 1")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(open)))
            .build();
    t.setRounds(new ArrayList<>(List.of(round)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    assertThat(strikingNames(component)).isEmpty();
  }

  @Test
  void entityAdapter_typeRuleLabel_freeForAllWithRule() {
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    // Format round type + default rule (no pool, no round fixedRule), resolved to display
    // names: "Free-for-All · No DQ".
    assertThat(adapter.getRoundTypeRuleLabel(1)).isEqualTo("Free-for-All · No DQ");
  }

  @Test
  void entityAdapter_typeRuleLabel_roundFixedRuleWins() {
    // A booker-set round rule outranks the format default — the label mirrors what booking
    // would resolve (round fixedRule → pool → format default).
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    SegmentRule cage = new SegmentRule();
    cage.setId(9L);
    cage.setName("Cage");
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .fixedRule(cage)
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    assertThat(adapter.getRoundTypeRuleLabel(1)).isEqualTo("Free-for-All · Cage");
  }

  @Test
  void entityAdapter_typeRuleLabel_nullForUnknownFormat() {
    Tournament t = tournamentEntity("UNKNOWN_FORMAT");
    TournamentEntityAdapter adapter = new TournamentEntityAdapter(t, List.of());
    assertThat(adapter.getRoundTypeRuleLabel(1)).isNull();
  }

  @Test
  void entityAdapter_typeRuleLabel_bookedSegmentRuleWinsOverEmptyPoolClaim() {
    // THE sandbox case: a tournament WITH a rule pool booked its qualifiers onto segments —
    // the pool's random pick (Gauntlet) landed on each segment, so the round label shows the
    // rule actually applied instead of staying silent ("Free-for-All" only).
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setAllowedRules(new ArrayList<>(List.of(rule(21L, "No DQ"), rule(22L, "Gauntlet"))));
    TournamentEntry e1 = entry(wrestler(1L, "Shumba"));
    TournamentEntry e2 = entry(wrestler(2L, "Aldis"));
    Segment gauntletMatch = segmentWithRules(rule(22L, "Gauntlet"));
    TournamentMatch match =
        TournamentMatch.builder().entrant1(e1).entrant2(e2).segment(gauntletMatch).build();
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(match)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    assertThat(adapter.getRoundTypeRuleLabel(1)).isEqualTo("Free-for-All · Gauntlet");
  }

  @Test
  void entityAdapter_typeRuleLabel_unbookedPoolRound_staysTypeOnly() {
    // With a pool and nothing booked yet, the pick is random — the label claims no rule.
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setAllowedRules(new ArrayList<>(List.of(rule(21L, "No DQ"))));
    TournamentEntry e1 = entry(wrestler(1L, "Shumba"));
    TournamentEntry e2 = entry(wrestler(2L, "Aldis"));
    TournamentMatch match = TournamentMatch.builder().entrant1(e1).entrant2(e2).build();
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(match)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    assertThat(adapter.getRoundTypeRuleLabel(1)).isEqualTo("Free-for-All");
  }

  @Test
  void component_bookedQualifier_showsItsOwnRuleAmongUnbooked() {
    // THE sandbox case: 6 qualifiers, only match 1 booked (pool pick: Gauntlet). The round label
    // must not generalize — but match 1's own card shows "Free-for-All · Gauntlet".
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    t.setAllowedRules(new ArrayList<>(List.of(rule(21L, "No DQ"), rule(22L, "Gauntlet"))));
    TournamentEntry e1 = entry(wrestler(1L, "Shumba"));
    TournamentEntry e2 = entry(wrestler(2L, "Aldis"));
    TournamentEntry e3 = entry(wrestler(3L, "Rocky"));
    TournamentEntry e4 = entry(wrestler(4L, "Austin"));
    TournamentMatch booked =
        TournamentMatch.builder()
            .entrant1(e1)
            .entrant2(e2)
            .segment(segmentWithRules(rule(22L, "Gauntlet")))
            .build();
    TournamentMatch unbooked = TournamentMatch.builder().entrant1(e3).entrant2(e4).build();
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(booked, unbooked)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2, e3, e4));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));

    // Per-match: the booked one carries its applied rule; the unbooked one shows just the type.
    assertThat(adapter.getMatches().get(0).getTypeRuleLabel()).isEqualTo("Free-for-All · Gauntlet");
    assertThat(adapter.getRoundTypeRuleLabel(1))
        .as("Round-level label stays silent: only some matches are booked and a pool exists")
        .isEqualTo("Free-for-All");
  }

  @Test
  void component_typeRuleLabel_rendersInlineOnCard() {
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    TournamentEntry e1 = entry(wrestler(1L, "Shumba"));
    TournamentEntry e2 = entry(wrestler(2L, "Aldis"));
    TournamentMatch match = TournamentMatch.builder().entrant1(e1).entrant2(e2).build();
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(match)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    assertThat(descendantTexts(component)).contains("Free-for-All · No DQ");
  }

  /** Names of spans styled struck-through anywhere under {@code root}. */
  private static List<String> strikingNames(Component root) {
    List<String> struck = new ArrayList<>();
    collectStruck(root, struck);
    return struck;
  }

  private static void collectStruck(Component node, List<String> out) {
    if (node instanceof Span span
        && "line-through".equals(span.getElement().getStyle().get("text-decoration"))) {
      out.add(span.getText());
    }
    node.getChildren().forEach(child -> collectStruck(child, out));
  }

  // ── TournamentBracketComponent: champion gating + persisted round labels ──

  @Test
  void component_incompleteBracketWithDecidedQualifier_noChampionBox() {
    // THE sandbox bug: a two-round bracket renders only its qualifiers round (lazy generation),
    // one qualifier has a winner — the component must NOT crown a champion and must label the
    // round "Qualifiers", not "Finals".
    Tournament t = tournamentEntity("QUALIFIER_GROUPS");
    Wrestler w1 = wrestler(1L, "Shumba");
    Wrestler w2 = wrestler(2L, "Johnny");
    Wrestler w3 = wrestler(3L, "Aldis");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);
    TournamentEntry e3 = entry(w3);
    TournamentMatch decided =
        TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();
    decided.setParticipants(
        new ArrayList<>(
            List.of(
                participant(decided, e1, 0),
                participant(decided, e2, 1),
                participant(decided, e3, 2))));
    TournamentMatch open = TournamentMatch.builder().entrant1(e2).entrant2(e3).build();
    TournamentRound qualifiers =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .matches(new ArrayList<>(List.of(decided, open)))
            .build();
    t.setRounds(new ArrayList<>(List.of(qualifiers)));
    t.setEntries(List.of(e1, e2, e3));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    List<String> texts = descendantTexts(component);
    assertThat(texts)
        .as("The single existing round must use its persisted name")
        .contains("Qualifiers");
    assertThat(texts).doesNotContain("Finals");
    assertThat(texts)
        .as("No champion while later rounds still generate lazily")
        .doesNotContain("CHAMPION");
  }

  @Test
  void component_completeBracket_showsChampionBox() {
    // A finished bracket (single-elimination final decided) still crowns its champion.
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");
    Wrestler w1 = wrestler(1L, "Rocky");
    Wrestler w2 = wrestler(2L, "Austin");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);
    TournamentMatch finalMatch =
        TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();
    TournamentRound finalRound =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .matches(new ArrayList<>(List.of(finalMatch)))
            .build();
    t.setRounds(new ArrayList<>(List.of(finalRound)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));
    TournamentBracketComponent component = new TournamentBracketComponent(adapter);

    List<String> texts = descendantTexts(component);
    assertThat(texts).contains("CHAMPION");
    assertThat(texts).contains("Rocky");
  }

  /** Depth-first walk over all descendant text nodes (component tree, not just direct children). */
  private static List<String> descendantTexts(Component root) {
    ArrayList<String> texts = new ArrayList<>();
    collectTexts(root, texts);
    return texts;
  }

  private static void collectTexts(Component node, List<String> out) {
    if (node instanceof HasText
        && ((HasText) node).getText() != null
        && !((HasText) node).getText().isBlank()) {
      out.add(((HasText) node).getText());
    }
    node.getChildren().forEach(child -> collectTexts(child, out));
  }

  // ── TournamentBracketComponent round-robin rendering ─────────────────────
  @Test
  void component_roundRobinGridRendersWithoutError() {
    TournamentDTO dto = new TournamentDTO();
    dto.setTotalRounds(2);
    dto.setCurrentRound(1);

    TournamentDTO.TournamentMatch m1 = new TournamentDTO.TournamentMatch();
    m1.setRound(1);
    m1.setWrestler1Id(1L);
    m1.setWrestler1Name("A");
    m1.setWrestler2Id(2L);
    m1.setWrestler2Name("B");

    dto.setMatches(List.of(m1));

    // Use a model that returns ROUND_ROBIN_GRID
    TournamentBracketModel model =
        new TournamentBracketModel() {
          @Override
          public int getTotalRounds() {
            return 1;
          }

          @Override
          public int getCurrentRound() {
            return 1;
          }

          @Override
          public RenderMode getRenderMode() {
            return RenderMode.ROUND_ROBIN_GRID;
          }

          @Override
          public List<MatchModel> getMatches() {
            return new TournamentDTOAdapter(dto).getMatches();
          }
        };

    TournamentBracketComponent component = new TournamentBracketComponent(model);
    assertThat(component.getChildren()).isNotEmpty();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static TournamentDTO dtoWith1Match() {
    TournamentDTO dto = new TournamentDTO();
    dto.setTotalRounds(2);
    dto.setCurrentRound(1);

    TournamentDTO.TournamentMatch m = new TournamentDTO.TournamentMatch();
    m.setRound(1);
    m.setWrestler1Id(10L);
    m.setWrestler1Name("Alpha");
    m.setWrestler2Id(20L);
    m.setWrestler2Name("Beta");
    m.setWinnerId(10L);
    m.setPlayerMatch(true);
    dto.setMatches(List.of(m));
    return dto;
  }

  private static Tournament tournamentEntity(String formatId) {
    Tournament t = new Tournament();
    t.setFormatId(formatId);
    t.setRounds(new ArrayList<>());
    t.setEntries(new ArrayList<>());
    return t;
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    return w;
  }

  private static TournamentEntry entry(Wrestler w) {
    return TournamentEntry.builder().wrestler(w).seed(1).build();
  }

  private static TournamentRound roundWithStatus(int number, TournamentRoundStatus status) {
    TournamentRound r = new TournamentRound();
    r.setRoundNumber(number);
    r.setStatus(status);
    r.setMatches(new ArrayList<>());
    return r;
  }

  private static SegmentRule rule(Long id, String name) {
    SegmentRule r = new SegmentRule();
    r.setId(id);
    r.setName(name);
    return r;
  }

  /** A segment carrying one applied rule — the booked-match shape. */
  private static Segment segmentWithRules(SegmentRule... rules) {
    Segment segment = new Segment();
    segment.setId(500L);
    for (SegmentRule r : rules) {
      segment.addSegmentRule(r);
    }
    return segment;
  }
}
