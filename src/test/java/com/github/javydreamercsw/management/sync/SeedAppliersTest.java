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
package com.github.javydreamercsw.management.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.seedreview.SeedUpdateDecisionService;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import com.github.javydreamercsw.management.sync.review.SeedFieldDecision;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Real-database apply-path tests for the three wired {@link SeedDomainApplier}s (seed-update review
 * feature, PR 4 fix round 1).
 *
 * <p>Each applier resolves its entity through the decision row's {@code entity_display_name} — the
 * exact seed name captured at detection time — because slug reversal is lossy: "Jessie McKay"
 * de-cases to "Jessie Mckay", "André the Giant" downcases mid-name words, "OMZ" becomes "Omz", "ATW
 * World" uppercases to "ATW WORLD", "No DQ" becomes "No Dq", and "Free-For-All" re-spaces to "Free
 * For All" — while the repository lookups are case-sensitive. The deleted {@code entityName}
 * helpers failed exactly those names, so the resolution tests here use the real seed names that
 * break the round-trip; removal tests use fresh unseeded rows with the same lossy shape (safe to
 * delete — no seed foreign keys point at them).
 *
 * <p>Runs against the real repositories and the REAL applier beans on in-memory H2 (same base as
 * {@code SeedUpdateDecisionServiceTest}) because the assertions are persistence-shaped: the entity
 * must actually change / be deleted / carry the stamp after the apply service's transaction.
 */
class SeedAppliersTest extends AbstractJpaTest {

  @Autowired private SeedUpdateDecisionRepository decisionRepository;
  @Autowired private WrestlerSeedApplier wrestlerApplier;
  @Autowired private SegmentRuleSeedApplier segmentRuleApplier;
  @Autowired private ChampionshipSeedApplier championshipApplier;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private EntityManager entityManager;

  @BeforeEach
  void resetDecisionState() {
    // Deterministic starting point: no parked decisions from the boot's data initializer.
    decisionRepository.deleteAllInBatch();
    entityManager.clear();
  }

  private SeedUpdateDecisionService service(SeedDomainApplier applier) {
    return new SeedUpdateDecisionService(
        decisionRepository, List.of(applier), new SeedReviewGate(decisionRepository));
  }

  /** Finds the row by exact seed name, creating it only when the seed data lacks it. */
  private Wrestler wrestler(String name) {
    return wrestlerRepository
        .findByName(name)
        .orElseGet(() -> wrestlerRepository.save(Wrestler.builder().name(name).build()));
  }

  private SegmentRule segmentRule(String name) {
    return segmentRuleRepository
        .findByName(name)
        .orElseGet(
            () -> {
              SegmentRule rule = new SegmentRule();
              rule.setName(name);
              return segmentRuleRepository.save(rule);
            });
  }

  private Title title(String name, WrestlerTier tier) {
    return titleRepository
        .findByName(name)
        .orElseGet(
            () -> {
              Title t = new Title();
              t.setName(name);
              t.setTier(tier);
              t.setChampionshipType(ChampionshipType.SINGLE);
              return titleRepository.save(t);
            });
  }

  private SeedUpdateDecision park(
      String domain,
      String entityKey,
      String displayName,
      String field,
      String current,
      String newValue,
      String fingerprint) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domain);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(field);
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT);
    decision.setCurrentValue(current);
    decision.setNewValue(newValue);
    decision.setNewFingerprint(fingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decisionRepository.saveAndFlush(decision);
  }

  private SeedUpdateDecision parkRemoval(
      String domain, String entityKey, String displayName, String fingerprint) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domain);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(null); // NULL for removal decisions
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_REMOVAL);
    decision.setCurrentValue(null);
    decision.setNewValue(null);
    decision.setNewFingerprint(fingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decisionRepository.saveAndFlush(decision);
  }

  /** Forces the test transaction's writes to the database and re-reads entities fresh. */
  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  // ── WrestlerSeedApplier ──────────────────────────────────────────────────────

  @Test
  void wrestlerApplierAppliesUseNewToLossySeedNames() {
    // Lossy-name pins: case-only ("Jessie McKay"), non-ASCII ("André the Giant"), acronym ("OMZ")
    // — all survive in the DB but none survive a slug round-trip.
    Wrestler jessie = wrestler("Jessie McKay");
    Wrestler andre = wrestler("André the Giant");
    Wrestler omz = wrestler("OMZ");
    flushAndClear();

    SeedUpdateDecision d1 =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "description",
            "old",
            "new description for Jessie",
            "fp-jessie");
    SeedUpdateDecision d2 =
        park(
            "wrestler",
            "wrestler:andré-the-giant",
            "André the Giant",
            "brawl",
            "1",
            "6",
            "fp-andre");
    // OMZ is a real seeded row — its current heritageTag is whatever the seed loader wrote; the
    // decision carries that value as "current" so KEEP_MINE's assertion is value-agnostic.
    String omzCurrent = omz.getHeritageTag();
    SeedUpdateDecision d3 =
        park(
            "wrestler", "wrestler:omz", "OMZ", "heritageTag", omzCurrent, "The Mountain", "fp-omz");

    service(wrestlerApplier)
        .apply(
            Map.of(
                d1.getId(), SeedFieldDecision.USE_NEW,
                d2.getId(), SeedFieldDecision.USE_NEW,
                d3.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    assertEquals(
        "new description for Jessie",
        wrestlerRepository.findById(jessie.getId()).orElseThrow().getDescription(),
        "USE_NEW must reach the row resolved by display name (case-only slug loss)");
    assertEquals(
        6,
        wrestlerRepository.findById(andre.getId()).orElseThrow().getBrawl(),
        "USE_NEW must reach the row resolved by display name (non-ASCII slug loss)");
    Wrestler omzRow = wrestlerRepository.findById(omz.getId()).orElseThrow();
    assertEquals(omzCurrent, omzRow.getHeritageTag(), "KEEP_MINE must keep the admin's value");
    assertEquals(
        "fp-omz", omzRow.getSeedFingerprint(), "KEEP_MINE must stamp the detection fingerprint");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_NEW,
        decisionRepository.findById(d1.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(d3.getId()).orElseThrow().getStatus());
  }

  @Test
  void wrestlerApplierRemoveDeletesTheRow() {
    // Fresh unseeded row with the lossy acronym shape ("OMZ Prime" downcases to "Omz Prime" in a
    // slug round-trip) — safe to delete, no seed data references it.
    Wrestler doomed = wrestlerRepository.save(Wrestler.builder().name("OMZ Prime").build());
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("wrestler", "wrestler:omz-prime", "OMZ Prime", "fp-remove");
    service(wrestlerApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        wrestlerRepository.findById(doomed.getId()).isPresent(),
        "REMOVE must delete the row resolved by display name");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void wrestlerApplierRemoveRefusedWhenDependentsExist() {
    // A deck references the wrestler: the apply service's pre-dispatch hasDependents check must
    // refuse the removal BEFORE any dispatch, leaving the row and decision untouched.
    Wrestler referenced = wrestlerRepository.save(Wrestler.builder().name("OMZ Prime").build());
    Deck deck = new Deck();
    deck.setWrestler(referenced);
    deck.setCreationDate(Instant.now());
    deckRepository.save(deck);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("wrestler", "wrestler:omz-prime", "OMZ Prime", "fp-dep");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(wrestlerApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));

    assertTrue(
        exception.getMessage().contains("references it"),
        "removal with dependents must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        wrestlerRepository.findById(referenced.getId()).isPresent(),
        "the referenced wrestler must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  @Test
  void wrestlerApplierKeepAsCustomClearsStamp() {
    Wrestler omz = wrestler("OMZ");
    omz.setSeedFingerprint("fp-old");
    wrestlerRepository.save(omz);
    flushAndClear();

    SeedUpdateDecision decision =
        park("wrestler", "wrestler:omz", "OMZ", "description", "old", "seed", "fp-new");
    service(wrestlerApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    assertNull(
        wrestlerRepository.findById(omz.getId()).orElseThrow().getSeedFingerprint(),
        "KEEP_AS_CUSTOM must clear the stamp (the row becomes user-owned content)");
  }

  // ── SegmentRuleSeedApplier ──────────────────────────────────────────────────

  @Test
  void segmentRuleApplierAppliesUseNewToLossySeedNames() {
    // Lossy-name pins: case-only ("No DQ" — slug reversal de-cases to "No Dq") and hyphens
    // ("Free-For-All" — slug reversal re-spaces to "Free For All").
    SegmentRule noDq = segmentRule("No DQ");
    SegmentRule ffa = segmentRule("Free-For-All");
    flushAndClear();

    SeedUpdateDecision d1 =
        park(
            "segment_rule",
            "segment_rule:no-dq",
            "No DQ",
            "description",
            "old",
            "disqualification rules updated",
            "fp-nodq");
    SeedUpdateDecision d2 =
        park(
            "segment_rule",
            "segment_rule:free-for-all",
            "Free-For-All",
            "bumpAddition",
            BumpAddition.NONE.name(),
            BumpAddition.ALL.name(),
            "fp-ffa");

    service(segmentRuleApplier)
        .apply(
            Map.of(d1.getId(), SeedFieldDecision.USE_NEW, d2.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        "disqualification rules updated",
        segmentRuleRepository.findById(noDq.getId()).orElseThrow().getDescription(),
        "USE_NEW must reach the row resolved by display name (case-only slug loss)");
    assertEquals(
        BumpAddition.ALL,
        segmentRuleRepository.findById(ffa.getId()).orElseThrow().getBumpAddition(),
        "USE_NEW must reach the row resolved by display name (hyphen slug loss)");
  }

  @Test
  void segmentRuleApplierRemoveDeletesTheRow() {
    // Fresh unseeded row with the lossy acronym shape ("GM Office Brawl" downcases to "Gm Office
    // Brawl" in a slug round-trip) — safe to delete, no seed segment references it.
    SegmentRule doomed = segmentRule("GM Office Brawl");
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("segment_rule", "segment_rule:gm-office-brawl", "GM Office Brawl", "fp-remove");
    service(segmentRuleApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        segmentRuleRepository.findById(doomed.getId()).isPresent(),
        "REMOVE must delete the row resolved by display name");
  }

  @Test
  void segmentRuleApplierKeepMineStampsWithoutChangingValue() {
    SegmentRule rule = segmentRule("No DQ");
    String adminValue = rule.getDescription();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "segment_rule",
            "segment_rule:no-dq",
            "No DQ",
            "description",
            adminValue,
            "seed description",
            "fp-keep");
    service(segmentRuleApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    SegmentRule reloaded = segmentRuleRepository.findById(rule.getId()).orElseThrow();
    assertEquals(adminValue, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals(
        "fp-keep",
        reloaded.getSeedFingerprint(),
        "KEEP_MINE must stamp the detection fingerprint (re-collision on the next seed change)");
  }

  // ── ChampionshipSeedApplier ─────────────────────────────────────────────────

  @Test
  void championshipApplierAppliesUseNewToUppercasedSlugName() {
    // "ATW World" was uppercased to "ATW WORLD" by the old slug reversal — all 4 seeded
    // championships failed lookup. The display-name resolution must reach the real row.
    Title atwWorld = title("ATW World", WrestlerTier.MAIN_EVENTER);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "championship",
            "championship:atw-world",
            "ATW World",
            "description",
            "old",
            "The top title in All Time Wrestling.",
            "fp-atw");

    service(championshipApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    Title reloaded = titleRepository.findById(atwWorld.getId()).orElseThrow();
    assertEquals(
        "The top title in All Time Wrestling.",
        reloaded.getDescription(),
        "USE_NEW must reach the row resolved by display name (uppercase slug loss)");
    assertEquals(
        "fp-atw", reloaded.getSeedFingerprint(), "USE_NEW must stamp the detection fingerprint");
  }

  @Test
  void championshipApplierKeepMineStampsWithoutChangingValue() {
    Title atwWomen = title("ATW Women", WrestlerTier.MAIN_EVENTER);
    String adminValue = atwWomen.getDescription();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "championship",
            "championship:atw-women",
            "ATW Women",
            "description",
            adminValue,
            "seed description",
            "fp-women");

    service(championshipApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    Title reloaded = titleRepository.findById(atwWomen.getId()).orElseThrow();
    assertEquals(adminValue, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals(
        "fp-women",
        reloaded.getSeedFingerprint(),
        "KEEP_MINE must stamp the detection fingerprint (re-collision on the next seed change)");
  }

  @Test
  void championshipApplierRemoveDeletesVacantInactiveTitle() {
    // Fresh unseeded row with the lossy acronym shape — deleteTitle only removes vacant, inactive
    // titles (vacancy is derived from the empty champion set); the fail-safe direction keeps any
    // title with gameplay state.
    Title vacant = title("ATW Tag Team Gold", WrestlerTier.MIDCARDER);
    vacant.setIsActive(false);
    titleRepository.save(vacant);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "championship", "championship:atw-tag-team-gold", "ATW Tag Team Gold", "fp-rem");

    service(championshipApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        titleRepository.findById(vacant.getId()).isPresent(),
        "REMOVE must delete a vacant, inactive title");
  }

  @Test
  void championshipApplierRemoveRefusedWhenReignsExist() {
    // A title with an open reign has dependents: the pre-dispatch hasDependents check must refuse
    // before anything is dispatched; the row and decision survive.
    Title withReign = title("ATW Tag Team Gold", WrestlerTier.MIDCARDER);
    TitleReign reign = new TitleReign();
    reign.setTitle(withReign);
    reign.setStartDate(Instant.now());
    titleReignRepository.save(reign);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "championship", "championship:atw-tag-team-gold", "ATW Tag Team Gold", "fp-rem2");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(championshipApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));

    assertTrue(
        exception.getMessage().contains("references it"),
        "removal with dependents must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        titleRepository.findById(withReign.getId()).isPresent(),
        "the title with a reign must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }
}
