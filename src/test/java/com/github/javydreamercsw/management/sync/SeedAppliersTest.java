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

import com.github.javydreamercsw.base.domain.account.AccountAchievementRepository;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementCategory;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionRepository;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionTypeRepository;
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
 * Real-database apply-path tests for the wired {@link SeedDomainApplier}s (seed-update review
 * feature, PR 4 fix round 1 + Step 4 slices).
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
  @Autowired private CardSeedApplier cardApplier;
  @Autowired private CardSetSeedApplier cardSetApplier;
  @Autowired private DeckSeedApplier deckApplier;
  @Autowired private NpcSeedApplier npcApplier;
  @Autowired private AchievementSeedApplier achievementApplier;
  @Autowired private RingsideActionSeedApplier ringsideActionApplier;
  @Autowired private SegmentTypeSeedApplier segmentTypeApplier;
  @Autowired private ShowTypeSeedApplier showTypeApplier;
  @Autowired private ShowTemplateSeedApplier showTemplateApplier;
  @Autowired private StatusCardSeedApplier statusCardApplier;
  @Autowired private TournamentSeedApplier tournamentApplier;
  @Autowired private CampaignAbilityCardSeedApplier campaignAbilityCardApplier;
  @Autowired private FactionSeedApplier factionApplier;
  @Autowired private ArenaSeedApplier arenaApplier;
  @Autowired private LocationSeedApplier locationApplier;
  @Autowired private TeamSeedApplier teamApplier;
  @Autowired private CommentatorSeedApplier commentatorApplier;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private DeckCardRepository deckCardRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private RingsideActionRepository ringsideActionRepository;
  @Autowired private RingsideActionTypeRepository ringsideActionTypeRepository;
  @Autowired private NpcRepository npcRepository;
  @Autowired private AchievementRepository achievementRepository;
  @Autowired private AccountAchievementRepository accountAchievementRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository
      segmentTypeRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository
      showTypeRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository
      showTemplateRepository;

  @Autowired private com.github.javydreamercsw.management.domain.show.ShowRepository showRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.StatusCardRepository
      statusCardRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.WrestlerStatusRepository
      wrestlerStatusRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.tournament.TournamentRepository
      tournamentRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository
      campaignAbilityCardRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository
      wrestlerStateRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignRepository
      campaignRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository
      campaignStateRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.faction.FactionRepository factionRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository
      factionRivalryRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.world.ArenaRepository arenaRepositoryTest;

  @Autowired
  private com.github.javydreamercsw.management.domain.world.LocationRepository
      locationRepositoryTest;

  @Autowired private com.github.javydreamercsw.management.domain.team.TeamRepository teamRepoTest;

  @Autowired
  private com.github.javydreamercsw.management.domain.commentator.CommentatorRepository
      commentatorRepositoryTest;

  @Autowired
  private com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository
      commentaryTeamRepositoryTest;

  @Autowired
  private com.github.javydreamercsw.management.domain.universe.UniverseRepository
      universeRepositoryTest;

  @Autowired
  private com.github.javydreamercsw.management.domain.npc.NpcRepository npcRepositoryTest;

  @Autowired
  private com.github.javydreamercsw.management.domain.tournament.TrophyReignRepository
      trophyReignRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository
      tournamentEntryRepository;

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

  // ── CardSeedApplier ─────────────────────────────────────────────────────────

  private Card card(String setCode, int number, String name) {
    CardSet set =
        cardSetRepository
            .findByCode(setCode)
            .orElseGet(
                () -> {
                  CardSet s = new CardSet();
                  s.setCode(setCode);
                  s.setName(setCode + " Set");
                  return cardSetRepository.save(s);
                });
    Card c = new Card();
    c.setSet(set);
    c.setNumber(number);
    c.setName(name);
    c.setType("Strike");
    c.setDamage(1);
    c.setStamina(1);
    c.setTarget(2);
    c.setMomentum(1);
    c.setSignature(false);
    c.setFinisher(false);
    c.setTaunt(false);
    c.setRecover(false);
    c.setPin(false);
    c.setCreationDate(Instant.now());
    return cardRepository.save(c);
  }

  @Test
  void cardApplierAppliesUseNewAndResolvesBySetNumberKey() {
    // Cards resolve by the reminted set#number entity key (display names are not unique across
    // sets): two same-named cards in different sets must both stay resolvable. Fresh fixture sets
    // (no seed catalog overlap — the boot seeds ATW etc.).
    Card atwCard = card("TST1", 60, "Shoulder Tackle");
    Card otherSetSameName = card("TST2", 1, "Shoulder Tackle");
    otherSetSameName.setType("Throw");
    cardRepository.save(otherSetSameName);
    flushAndClear();

    SeedUpdateDecision d1 =
        park(
            "card",
            "card:tst1#60",
            "Shoulder Tackle",
            "name",
            "Shoulder Tackle",
            "Shoulder Tackle Renamed",
            "fp-card1");
    SeedUpdateDecision d2 =
        park("card", "card:tst2#1", "Shoulder Tackle", "type", "Throw", "Grapple", "fp-card2");

    service(cardApplier)
        .apply(
            Map.of(d1.getId(), SeedFieldDecision.USE_NEW, d2.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        "Shoulder Tackle Renamed",
        cardRepository.findById(atwCard.getId()).orElseThrow().getName(),
        "USE_NEW must reach the TST1 row resolved through its set#number key");
    assertEquals(
        "Grapple",
        cardRepository.findById(otherSetSameName.getId()).orElseThrow().getType(),
        "USE_NEW must reach the TST2 row resolved through its set#number key");
    assertEquals(
        "fp-card1",
        cardRepository.findById(atwCard.getId()).orElseThrow().getSeedFingerprint(),
        "USE_NEW must stamp the detection fingerprint");
  }

  @Test
  void cardApplierKeepMineStampsWithoutChangingValue() {
    Card c = card("TST1", 59, "Gori Special");
    Integer adminValue = c.getMomentum();
    flushAndClear();

    SeedUpdateDecision decision =
        park("card", "card:tst1#59", "Gori Special", "momentum", "7", "9", "fp-keep");
    service(cardApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    Card reloaded = cardRepository.findById(c.getId()).orElseThrow();
    assertEquals(adminValue, reloaded.getMomentum(), "KEEP_MINE must keep the admin's value");
    assertEquals(
        "fp-keep",
        reloaded.getSeedFingerprint(),
        "KEEP_MINE must stamp the detection fingerprint (re-collision on the next seed change)");
  }

  @Test
  void cardApplierKeepAsCustomClearsStamp() {
    Card c = card("TST1", 58, "Irish Whip");
    c.setSeedFingerprint("fp-old");
    cardRepository.save(c);
    flushAndClear();

    SeedUpdateDecision decision =
        park("card", "card:tst1#58", "Irish Whip", "name", "old", "seed", "fp-new");
    service(cardApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    assertNull(
        cardRepository.findById(c.getId()).orElseThrow().getSeedFingerprint(),
        "KEEP_AS_CUSTOM must clear the stamp (the row becomes user-owned content)");
  }

  @Test
  void cardApplierRemoveDeletesTheRow() {
    // Unheld card with no other pending decisions: REMOVE deletes directly.
    Card doomed = card("TST1", 57, "Chop");
    flushAndClear();

    SeedUpdateDecision free = parkRemoval("card", "card:tst1#57", "Chop", "fp-rem-free");
    service(cardApplier).apply(Map.of(free.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        cardRepository.findById(doomed.getId()).isPresent(),
        "REMOVE must delete the card no deck holds");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(free.getId()).orElseThrow().getStatus());
  }

  @Test
  void cardApplierRemoveRefusedWhenDeckHoldsTheCard() {
    // A deck holds the card: the pre-dispatch hasDependents check must refuse the removal BEFORE
    // any dispatch, leaving the row and decision untouched.
    Card held = card("TST1", 56, "Stomp");
    Wrestler holder = wrestlerRepository.save(Wrestler.builder().name("OMZ Prime").build());
    Deck deck = new Deck();
    deck.setWrestler(holder);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);
    DeckCard dc = new DeckCard();
    dc.setDeck(deck);
    dc.setCard(held);
    dc.setSet(held.getSet());
    dc.setAmount(1);
    deckCardRepository.save(dc);
    flushAndClear();

    SeedUpdateDecision blocked = parkRemoval("card", "card:tst1#56", "Stomp", "fp-rem-blocked");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service(cardApplier).apply(Map.of(blocked.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a deck-held card must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        cardRepository.findById(held.getId()).isPresent(),
        "the deck-held card must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(blocked.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  // ── CardSetSeedApplier ──────────────────────────────────────────────────────

  private CardSet cardSet(String code, String name) {
    CardSet cs =
        cardSetRepository
            .findByCode(code)
            .orElseGet(
                () -> {
                  CardSet s = new CardSet();
                  s.setCode(code);
                  s.setName(name);
                  return cardSetRepository.save(s);
                });
    return cs;
  }

  @Test
  void cardSetApplierAppliesUseNewToLossySeedName() {
    // "Sgt. Slaughter" de-cases mid-name words in a slug round-trip; the display-name resolution
    // must reach the real row.
    CardSet sgt = cardSet("SGT", "Sgt. Slaughter");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "card_set",
            "card_set:sgt",
            "Sgt. Slaughter",
            "name",
            "old",
            "Sergeant Slaughter",
            "fp-set");

    service(cardSetApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        "Sergeant Slaughter",
        cardSetRepository.findById(sgt.getId()).orElseThrow().getName(),
        "USE_NEW must reach the row resolved by display name (case-only slug loss)");
    assertEquals(
        "fp-set",
        cardSetRepository.findById(sgt.getId()).orElseThrow().getSeedFingerprint(),
        "USE_NEW must stamp the detection fingerprint");
  }

  @Test
  void cardSetApplierKeepMineStampsWithoutChangingValue() {
    CardSet cs = cardSet("ATW", "All Time Wrestling");
    String adminValue = cs.getName();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "card_set",
            "card_set:atw",
            "All Time Wrestling",
            "name",
            adminValue,
            "seed",
            "fp-keep");
    service(cardSetApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    CardSet reloaded = cardSetRepository.findById(cs.getId()).orElseThrow();
    assertEquals(adminValue, reloaded.getName(), "KEEP_MINE must keep the admin's value");
    assertEquals(
        "fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the detection fingerprint");
  }

  @Test
  void cardSetApplierResolvesRenamedRowThroughEntityKey() {
    // Tier-2 NAME-collision shape (the domain's only seed-owned field IS the name): the admin
    // renamed the row, so the display name detection parked — the NEW seed name — can never match
    // the row by name. Resolution must go through the decision's entity key, exactly as
    // hasDependents remints it.
    CardSet cs = cardSet("REN1", "Admin Renamed Set");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "card_set",
            "card_set:ren1",
            "Seed Renamed Set", // the NEW seed name — the row's current name differs
            "name",
            "Admin Renamed Set",
            "Seed Renamed Set",
            "fp-ren");

    service(cardSetApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    CardSet reloaded = cardSetRepository.findById(cs.getId()).orElseThrow();
    assertEquals(
        "Seed Renamed Set",
        reloaded.getName(),
        "USE_NEW must reach the admin-renamed row through the entity key, not the seed name");
    assertEquals(
        "fp-ren", reloaded.getSeedFingerprint(), "USE_NEW must stamp the detection fingerprint");
  }

  @Test
  void cardSetApplierRemoveRefusedWhenCardsExist() {
    // A card references the set: the pre-dispatch hasDependents check must refuse the removal
    // BEFORE any dispatch. Fresh fixture set (no seed catalog overlap — the boot seeds ATW etc.).
    CardSet cs = cardSet("TST1", "Test Set One");
    card("TST1", 60, "Shoulder Tackle");
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("card_set", "card_set:tst1", "Test Set One", "fp-rem");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(cardSetApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));

    assertTrue(
        exception.getMessage().contains("references it"),
        "removal with dependents must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        cardSetRepository.findById(cs.getId()).isPresent(),
        "the referenced set must survive the refused removal");
  }

  // ── DeckSeedApplier ─────────────────────────────────────────────────────────

  @Test
  void deckApplierKeepMineStampsWithoutChangingComposition() {
    Wrestler holder = wrestlerRepository.save(Wrestler.builder().name("Deck Holder").build());
    Deck deck = new Deck();
    deck.setWrestler(holder);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);
    Card card = card("TST1", 1, "Frog Splash");
    DeckCard dc = new DeckCard();
    dc.setDeck(deck);
    dc.setCard(card);
    dc.setSet(card.getSet());
    dc.setAmount(1);
    deck.getCards().add(dc);
    deckCardRepository.save(dc);
    int cardsBefore = deck.getCards().size();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "deck",
            "deck:deck-holder",
            "Deck Holder",
            "composition",
            "EDDIE#1x1",
            "seed",
            "fp-deck");
    service(deckApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    Deck reloaded = deckRepository.findById(deck.getId()).orElseThrow();
    assertEquals(cardsBefore, reloaded.getCards().size(), "KEEP_MINE must keep the composition");
    assertEquals("fp-deck", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp");
  }

  @Test
  void deckApplierRemoveIsNeverDispatchedAsDependent() {
    // A deck is its wrestler's gameplay content: hasDependents answers true, so the apply service
    // refuses the removal before dispatch and the row stays pending.
    Wrestler holder = wrestlerRepository.save(Wrestler.builder().name("Deck Holder").build());
    Deck deck = new Deck();
    deck.setWrestler(holder);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);
    flushAndClear();

    SeedUpdateDecision decision = parkRemoval("deck", "deck:deck-holder", "Deck Holder", "fp-rem");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service(deckApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));

    assertTrue(
        exception.getMessage().contains("references it"),
        "deck removal must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        deckRepository.findById(deck.getId()).isPresent(),
        "the deck must survive the refused removal");
  }

  @Test
  void deckApplierUseNewRebuildsCompositionAndStamps() {
    // The apply path no detection test reaches: USE_NEW parses the decision's captured canonical
    // composition, clears the old DeckCards (orphanRemoval), rebuilds from the card catalog, and
    // stamps the row — asserted against the reloaded deck.
    Wrestler holder = wrestlerRepository.save(Wrestler.builder().name("Deck Holder").build());
    Deck deck = new Deck();
    deck.setWrestler(holder);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);
    // OLD composition: TST1#56 x2 + TST1#57 x1 — both entries must be gone after the apply.
    Card oldCardA = card("TST1", 56, "Old Card A");
    Card oldCardB = card("TST1", 57, "Old Card B");
    for (Card old : List.of(oldCardA, oldCardB)) {
      DeckCard dc = new DeckCard();
      dc.setDeck(deck);
      dc.setCard(old);
      dc.setSet(old.getSet());
      dc.setAmount(old == oldCardA ? 2 : 1);
      deck.getCards().add(dc);
      deckCardRepository.save(dc);
    }
    // NEW seed composition: TST1#58 x3 + TST1#59 x1.
    Card newCardA = card("TST1", 58, "New Card A");
    Card newCardB = card("TST1", 59, "New Card B");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "deck",
            "deck:deck-holder",
            "Deck Holder",
            "composition",
            "TST1#56x2\nTST1#57x1",
            "TST1#58x3\nTST1#59x1",
            "fp-deck-new");

    service(deckApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    Deck reloaded = deckRepository.findById(deck.getId()).orElseThrow();
    Map<String, Integer> composition = new java.util.HashMap<>();
    reloaded
        .getCards()
        .forEach(
            dc ->
                composition.put(
                    dc.getSet().getCode() + "#" + dc.getCard().getNumber(), dc.getAmount()));
    assertEquals(
        Map.of("TST1#58", 3, "TST1#59", 1),
        composition,
        "USE_NEW must rebuild the deck to exactly the decision's new seed composition");
    assertEquals(
        "fp-deck-new",
        reloaded.getSeedFingerprint(),
        "USE_NEW must stamp the detection fingerprint");
  }

  // ── NpcSeedApplier ──────────────────────────────────────────────────────────

  private Npc npc(String name) {
    return npcRepository
        .findByName(name)
        .orElseGet(
            () -> {
              Npc n = new Npc();
              n.setName(name);
              n.setDescription("original description");
              n.setNpcType("Manager");
              return npcRepository.save(n);
            });
  }

  @Test
  void npcApplierAppliesUseNewToLossySeedName() {
    // Case-only loss pin: a lowercase-mid-word name survives only via the display name.
    Npc charles = npc("Charles");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "npc",
            "npc:charles",
            "Charles",
            "description",
            "old",
            "The mysterious owner of ATW, updated.",
            "fp-npc");

    service(npcApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        "The mysterious owner of ATW, updated.",
        npcRepository.findById(charles.getId()).orElseThrow().getDescription(),
        "USE_NEW must reach the row resolved by display name");
    assertEquals(
        "fp-npc",
        npcRepository.findById(charles.getId()).orElseThrow().getSeedFingerprint(),
        "USE_NEW must stamp the detection fingerprint");
  }

  @Test
  void npcApplierRemoveRefusedWhenReferenced() {
    // A wrestler state references the NPC as manager: the pre-dispatch hasDependents check must
    // refuse the removal BEFORE any dispatch.
    Npc manager = npc("Charles");
    Wrestler managed = wrestlerRepository.save(Wrestler.builder().name("Managed Wrestler").build());
    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        com.github.javydreamercsw.management.domain.wrestler.WrestlerState.builder()
            .wrestler(managed)
            .universe(defaultUniverse)
            .build();
    state.setManager(manager);
    wrestlerStateRepository.save(state);
    flushAndClear();

    SeedUpdateDecision decision = parkRemoval("npc", "npc:charles", "Charles", "fp-rem");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service(npcApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));

    assertTrue(
        exception.getMessage().contains("references it"),
        "removal with dependents must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        npcRepository.findById(manager.getId()).isPresent(),
        "the referenced NPC must survive the refused removal");
  }

  // ── AchievementSeedApplier ──────────────────────────────────────────────────

  private Achievement achievement(String key, String name) {
    return achievementRepository
        .findByKey(key)
        .orElseGet(
            () -> {
              Achievement a = new Achievement();
              a.setKey(key);
              a.setName(name);
              a.setDescription("original description");
              a.setXpValue(50);
              a.setCategory(AchievementCategory.COLLECTION);
              return achievementRepository.save(a);
            });
  }

  @Test
  void achievementApplierAppliesUseNewToUppercasedKey() {
    Achievement first = achievement("FIRST_WRESTLER", "Rookie Manager");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "achievement",
            "achievement:first_wrestler",
            // The display name is the achievement key — the applier resolves findByKey with it
            // (the human name field is seed-owned content, not a resolution handle).
            "FIRST_WRESTLER",
            "description",
            "old",
            "Manage your first wrestler, updated.",
            "fp-ach");

    service(achievementApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        "Manage your first wrestler, updated.",
        achievementRepository.findById(first.getId()).orElseThrow().getDescription(),
        "USE_NEW must reach the row resolved by key");
    assertEquals(
        "fp-ach",
        achievementRepository.findById(first.getId()).orElseThrow().getSeedFingerprint(),
        "USE_NEW must stamp the detection fingerprint");
  }

  @Test
  void achievementApplierKeepMineStampsWithoutChangingValue() {
    Achievement ach = achievement("FIRST_WRESTLER", "Rookie Manager");
    Integer adminValue = ach.getXpValue();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "achievement",
            "achievement:first_wrestler",
            "FIRST_WRESTLER",
            "xpValue",
            "500",
            "150",
            "fp-keep");
    service(achievementApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    Achievement reloaded = achievementRepository.findById(ach.getId()).orElseThrow();
    assertEquals(adminValue, reloaded.getXpValue(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp");
  }

  @Test
  void achievementApplierRemoveRefusedWhenAccountProgressExists() {
    // An account references the achievement: the pre-dispatch hasDependents check must refuse.
    Achievement ach = achievement("FIRST_WRESTLER", "Rookie Manager");
    com.github.javydreamercsw.base.domain.account.Account account =
        createTestAccount(
            "seed-achiever", com.github.javydreamercsw.base.domain.account.RoleName.PLAYER);
    account.getAchievements().add(ach);
    accountRepository.save(account);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("achievement", "achievement:first_wrestler", "FIRST_WRESTLER", "fp-rem");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(achievementApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));

    assertTrue(
        exception.getMessage().contains("references it"),
        "removal with dependents must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        achievementRepository.findById(ach.getId()).isPresent(),
        "the earned achievement must survive the refused removal");
  }

  // ── RingsideActionSeedApplier ───────────────────────────────────────────────

  private RingsideAction ringsideAction(String name, String typeName) {
    return ringsideActionRepository
        .findByName(name)
        .orElseGet(
            () -> {
              RingsideActionType type =
                  ringsideActionTypeRepository
                      .findByName(typeName)
                      .orElseGet(
                          () -> {
                            RingsideActionType t = new RingsideActionType();
                            t.setName(typeName);
                            return ringsideActionTypeRepository.save(t);
                          });
              RingsideAction a = new RingsideAction();
              a.setName(name);
              a.setType(type);
              a.setDescription("original description");
              a.setImpact(1);
              a.setRisk(1);
              return ringsideActionRepository.save(a);
            });
  }

  @Test
  void ringsideActionApplierAppliesUseNewToLossySeedName() {
    RingsideAction action = ringsideAction("Coaching Advice", "Legal Support");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "ringside_action",
            "ringside_action:coaching-advice",
            "Coaching Advice",
            "description",
            "old",
            "Shout technical advice, updated.",
            "fp-ra");

    service(ringsideActionApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        "Shout technical advice, updated.",
        ringsideActionRepository.findById(action.getId()).orElseThrow().getDescription(),
        "USE_NEW must reach the row resolved by display name");
    assertEquals(
        "fp-ra",
        ringsideActionRepository.findById(action.getId()).orElseThrow().getSeedFingerprint(),
        "USE_NEW must stamp the detection fingerprint");
  }

  @Test
  void ringsideActionApplierRemoveDeletesTheRow() {
    // No persisted FK points at a ringside action — removal deletes directly.
    RingsideAction doomed = ringsideAction("GM Office Brawl", "Tactical Distraction");
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "ringside_action", "ringside_action:gm-office-brawl", "GM Office Brawl", "fp-rem");
    service(ringsideActionApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        ringsideActionRepository.findById(doomed.getId()).isPresent(),
        "REMOVE must delete the row resolved by display name");
  }

  // ── SegmentTypeSeedApplier ──────────────────────────────────────────────────

  private com.github.javydreamercsw.management.domain.show.segment.type.SegmentType segmentType(
      String name) {
    return segmentTypeRepository
        .findByName(name)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.show.segment.type.SegmentType st =
                  new com.github.javydreamercsw.management.domain.show.segment.type.SegmentType();
              st.setName(name);
              return segmentTypeRepository.save(st);
            });
  }

  private com.github.javydreamercsw.management.domain.show.segment.type.SegmentType
      segmentTypeFixture(String name, String description) {
    return segmentTypeRepository
        .findByName(name)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.show.segment.type.SegmentType st =
                  new com.github.javydreamercsw.management.domain.show.segment.type.SegmentType();
              st.setName(name);
              st.setDescription(description);
              return segmentTypeRepository.save(st);
            });
  }

  @Test
  void segmentTypeApplierAppliesUseNewThroughEntityKey() {
    // Resolution pins to the decision's reminted entity key: the row's name slugs to exactly the
    // key detection minted, while the display name (the UI-facing string, which may drift from the
    // row name across a seed rename) never takes part in resolution.
    var row = segmentTypeFixture("Entity Key Type", "old description");
    row.setSeedFingerprint("fp-old");
    segmentTypeRepository.save(row);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "segment_type",
            SegmentTypeSync.entityKeyFor("Entity Key Type"),
            "Seed Name Type", // drifted display name — resolution must ignore it
            "description",
            "old description",
            "fresh seed description",
            "fp-st");

    service(segmentTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = segmentTypeRepository.findById(row.getId()).orElseThrow();
    assertEquals(
        "fresh seed description",
        reloaded.getDescription(),
        "USE_NEW must reach the row through the reminted entity key, not the display name");
    assertEquals("fp-st", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void segmentTypeApplierKeepMineStampsWithoutChangingValue() {
    var st = segmentTypeFixture("Keep Mine Type", "admin's description");
    String adminValue = st.getDescription();
    st.setSeedFingerprint("fp-old");
    segmentTypeRepository.save(st);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "segment_type",
            SegmentTypeSync.entityKeyFor("Keep Mine Type"),
            "Keep Mine Type",
            "description",
            adminValue,
            "seed description",
            "fp-keep");
    service(segmentTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = segmentTypeRepository.findById(st.getId()).orElseThrow();
    assertEquals(adminValue, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals(
        "fp-keep",
        reloaded.getSeedFingerprint(),
        "KEEP_MINE must stamp the detection fingerprint (re-collision on the next seed change)");
  }

  @Test
  void segmentTypeApplierRemoveRefusedWhenBooked() {
    // A booked segment uses the type: the pre-dispatch hasDependents check must refuse the
    // removal BEFORE any dispatch. (The boot seeds the Promo type and its show types/templates;
    // no segment books a fresh fixture type, so the removal of an unbooked row is safe.)
    var free = segmentTypeFixture("Removable Type", "unused");
    var booked = segmentTypeFixture("Booked Type", "used");
    // A template assignment row references the booked type (ShowTemplateSegmentAssignment FK).
    com.github.javydreamercsw.management.domain.show.template.ShowTemplate template =
        showTemplateFixture();
    com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment row =
        new com.github.javydreamercsw.management.domain.show.template
            .ShowTemplateSegmentAssignment();
    row.setTemplate(template);
    row.setSegmentType(booked);
    template.getSegmentAssignments().add(row);
    showTemplateRepository.save(template);
    flushAndClear();

    SeedUpdateDecision blocked =
        parkRemoval(
            "segment_type",
            SegmentTypeSync.entityKeyFor("Booked Type"),
            "Booked Type",
            "fp-rem-blocked");
    SeedUpdateDecision freeDecision =
        parkRemoval(
            "segment_type",
            SegmentTypeSync.entityKeyFor("Removable Type"),
            "Removable Type",
            "fp-rem-free");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(segmentTypeApplier)
                    .apply(
                        Map.of(
                            blocked.getId(),
                            SeedFieldDecision.REMOVE,
                            freeDecision.getId(),
                            SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a referenced type must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        segmentTypeRepository.findById(booked.getId()).isPresent(),
        "the referenced type must survive the refused removal");
  }

  // ── ShowTypeSeedApplier ─────────────────────────────────────────────────────

  private com.github.javydreamercsw.management.domain.show.type.ShowType showTypeFixture(
      String name) {
    return showTypeRepository
        .findByName(name)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.show.type.ShowType st =
                  new com.github.javydreamercsw.management.domain.show.type.ShowType();
              st.setName(name);
              st.setDescription("original");
              st.setCategory(
                  com.github.javydreamercsw.management.domain.show.type.ShowCategory.OTHER);
              return showTypeRepository.save(st);
            });
  }

  @Test
  void showTypeApplierAppliesUseNewAndStamps() {
    var st = showTypeFixture("Weekly");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "show_type",
            ShowTypeSync.entityKeyFor("Weekly"),
            "Weekly",
            "description",
            "old",
            "Weekly Event, refreshed",
            "fp-showtype");
    service(showTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = showTypeRepository.findById(st.getId()).orElseThrow();
    assertEquals(
        "Weekly Event, refreshed", reloaded.getDescription(), "USE_NEW must reach the row");
    assertEquals("fp-showtype", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void showTypeApplierRemoveRefusedWhenShowsExist() {
    var referenced = showTypeFixture("Referenced Type");
    var show = new com.github.javydreamercsw.management.domain.show.Show();
    show.setName("Seed Applier Show");
    show.setDescription("a show");
    show.setType(referenced);
    show.setCreationDate(Instant.now());
    showRepository.save(show);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "show_type", ShowTypeSync.entityKeyFor("Referenced Type"), "Referenced Type", "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(showTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a show-booked type must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        showTypeRepository.findById(referenced.getId()).isPresent(),
        "the referenced type must survive the refused removal");
  }

  // ── ShowTemplateSeedApplier ─────────────────────────────────────────────────

  private com.github.javydreamercsw.management.domain.show.template.ShowTemplate
      showTemplateFixture() {
    com.github.javydreamercsw.management.domain.show.template.ShowTemplate t =
        new com.github.javydreamercsw.management.domain.show.template.ShowTemplate();
    t.setName("Applier Fixture Template");
    t.setShowType(showTypeFixture("Weekly"));
    return showTemplateRepository.save(t);
  }

  @Test
  void showTemplateApplierAppliesUseNewThroughEntityKey() {
    // Resolution pins to the decision's reminted entity key: the row's name slugs to exactly the
    // key detection minted, while the display name never takes part in resolution.
    var template = showTemplateFixture();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "show_template",
            ShowTemplateSync.entityKeyFor("Applier Fixture Template"),
            "Applier Fixture Template",
            "description",
            "old",
            "fresh template description",
            "fp-template");
    service(showTemplateApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = showTemplateRepository.findById(template.getId()).orElseThrow();
    assertEquals(
        "fresh template description",
        reloaded.getDescription(),
        "USE_NEW must reach the row resolved through the reminted entity key");
    assertEquals("fp-template", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void showTemplateApplierRemoveRefusedWhenShowsExist() {
    var template = showTemplateFixture();
    var show = new com.github.javydreamercsw.management.domain.show.Show();
    show.setName("Template Show");
    show.setDescription("a show");
    show.setType(template.getShowType());
    show.setTemplate(template);
    show.setCreationDate(Instant.now());
    showRepository.save(show);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "show_template",
            ShowTemplateSync.entityKeyFor("Applier Fixture Template"),
            "Applier Fixture Template",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(showTemplateApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a template with shows must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        showTemplateRepository.findById(template.getId()).isPresent(),
        "the referenced template must survive the refused removal");
  }

  // ── StatusCardSeedApplier ───────────────────────────────────────────────────

  private com.github.javydreamercsw.management.domain.campaign.StatusCard statusCardFixture(
      String key) {
    return statusCardRepository
        .findByKey(key)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.campaign.StatusCard card =
                  new com.github.javydreamercsw.management.domain.campaign.StatusCard();
              card.setKey(key);
              card.setLevel1Name("Level One");
              card.setDescription("original");
              card.setPositive(true);
              return statusCardRepository.save(card);
            });
  }

  @Test
  void statusCardApplierAppliesUseNewThroughEntityKey() {
    var card = statusCardFixture("status_applier_test");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "status_card",
            StatusCardSync.entityKeyFor("status_applier_test"),
            "status_applier_test",
            "level1Effect",
            "old",
            "vpBonus: 2",
            "fp-sc");
    service(statusCardApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = statusCardRepository.findById(card.getId()).orElseThrow();
    assertEquals(
        "vpBonus: 2", reloaded.getLevel1Effect(), "USE_NEW must reach the row resolved by key");
    assertEquals("fp-sc", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void statusCardApplierRemoveRefusedWhenWrestlerStatusesExist() {
    var card = statusCardFixture("status_applier_dependent");
    Wrestler holder = wrestlerRepository.save(Wrestler.builder().name("Status Holder").build());
    com.github.javydreamercsw.management.domain.campaign.WrestlerStatus status =
        com.github.javydreamercsw.management.domain.campaign.WrestlerStatus.builder()
            .wrestler(holder)
            .statusCard(card)
            .build();
    wrestlerStatusRepository.save(status);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "status_card",
            StatusCardSync.entityKeyFor("status_applier_dependent"),
            "status_applier_dependent",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(statusCardApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of an applied status card must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        statusCardRepository.findById(card.getId()).isPresent(),
        "the applied card must survive the refused removal");
  }

  // ── TournamentSeedApplier ───────────────────────────────────────────────────

  private com.github.javydreamercsw.management.domain.tournament.Tournament tournamentFixture(
      String code) {
    return tournamentRepository
        .findByCodeWithRules(code)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.tournament.Tournament t =
                  new com.github.javydreamercsw.management.domain.tournament.Tournament();
              t.setCode(code);
              t.setName("Fixture " + code);
              t.setFormatId("SINGLE_ELIMINATION");
              return tournamentRepository.save(t);
            });
  }

  @Test
  void tournamentApplierAppliesUseNewThroughEntityKey() {
    var tournament = tournamentFixture("applier_fixture");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "tournament",
            TournamentSync.entityKeyFor("applier_fixture"),
            "Fixture applier_fixture", // the seed name — the row's current name may differ
            "name",
            "Fixture applier_fixture",
            "Renamed By Seed",
            "fp-t");
    service(tournamentApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = tournamentRepository.findById(tournament.getId()).orElseThrow();
    assertEquals(
        "Renamed By Seed",
        reloaded.getName(),
        "USE_NEW must reach the row resolved through the code-key, not the seed name");
    assertEquals("fp-t", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void tournamentApplierKeepMineStampsWithoutChangingValue() {
    var tournament = tournamentFixture("applier_keep");
    tournament.setDefaultEntrantCount(4);
    tournamentRepository.save(tournament);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "tournament",
            TournamentSync.entityKeyFor("applier_keep"),
            "Fixture applier_keep",
            "defaultEntrantCount",
            "4",
            "8",
            "fp-keep");
    service(tournamentApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = tournamentRepository.findById(tournament.getId()).orElseThrow();
    assertEquals(4, reloaded.getDefaultEntrantCount(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void tournamentApplierUseNewRebuildsRulesPoolAndStamps() {
    // The rules-pool apply path: USE_NEW resolves the decision's newline-joined rule names back
    // to entities through the rule catalog and replaces the row's pool.
    var tournament = tournamentFixture("applier_rules");
    SegmentRule cage = segmentRule("Cage");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "tournament",
            TournamentSync.entityKeyFor("applier_rules"),
            "Fixture applier_rules",
            "allowedRules",
            "No DQ",
            "Cage",
            "fp-rules");
    service(tournamentApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = tournamentRepository.findByCodeWithRules("applier_rules").orElseThrow();
    assertEquals(
        List.of(cage.getId()),
        reloaded.getAllowedRules().stream().map(SegmentRule::getId).toList(),
        "USE_NEW must rebuild the rules pool from the decision's captured names");
    assertEquals("fp-rules", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  // ── CampaignAbilityCardSeedApplier ──────────────────────────────────────────

  private com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard abilityCard(
      String name, int level) {
    return campaignAbilityCardRepository
        .findByNameAndAlignmentTypeAndLevel(
            name, com.github.javydreamercsw.management.domain.campaign.AlignmentType.HEEL, level)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard c =
                  new com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard();
              c.setName(name);
              c.setAlignmentType(
                  com.github.javydreamercsw.management.domain.campaign.AlignmentType.HEEL);
              c.setLevel(level);
              return campaignAbilityCardRepository.save(c);
            });
  }

  @Test
  void campaignAbilityCardApplierAppliesUseNewThroughCompositeKey() {
    // Two same-named cards at different levels: resolution must go through the composite
    // name|alignment|level key remint, not the display name.
    var level1 = abilityCard("Ambiguous Card", 1);
    var level2 = abilityCard("Ambiguous Card", 2);
    level2.setDescription("level-two row");
    campaignAbilityCardRepository.save(level2);
    flushAndClear();

    SeedUpdateDecision d1 =
        park(
            "campaign_ability_card",
            CampaignAbilityCardSync.entityKeyFor("Ambiguous Card|HEEL|1"),
            "Ambiguous Card",
            "description",
            "old",
            "level-one updated",
            "fp-c1");
    SeedUpdateDecision d2 =
        park(
            "campaign_ability_card",
            CampaignAbilityCardSync.entityKeyFor("Ambiguous Card|HEEL|2"),
            "Ambiguous Card",
            "description",
            "level-two row",
            "level-two updated",
            "fp-c2");

    service(campaignAbilityCardApplier)
        .apply(
            Map.of(d1.getId(), SeedFieldDecision.USE_NEW, d2.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        "level-one updated",
        campaignAbilityCardRepository.findById(level1.getId()).orElseThrow().getDescription(),
        "USE_NEW must reach the level-1 row through its composite key");
    assertEquals(
        "level-two updated",
        campaignAbilityCardRepository.findById(level2.getId()).orElseThrow().getDescription(),
        "USE_NEW must reach the level-2 row through its composite key");
  }

  @Test
  void campaignAbilityCardApplierKeepMineStamps() {
    var card = abilityCard("Keep Card", 1);
    String adminScript = card.getEffectScript();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "campaign_ability_card",
            CampaignAbilityCardSync.entityKeyFor("Keep Card|HEEL|1"),
            "Keep Card",
            "effectScript",
            adminScript,
            "seedScript()",
            "fp-keep");
    service(campaignAbilityCardApplier)
        .apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = campaignAbilityCardRepository.findById(card.getId()).orElseThrow();
    assertEquals(adminScript, reloaded.getEffectScript(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  // ── Fix-round coverage: the 24-path verb matrix for the six Slice B appliers ──
  // Per domain: USE_NEW (above), KEEP_MINE, KEEP_AS_CUSTOM, and REMOVE — positive where the
  // domain's hasDependents guard permits a genuinely orphaned row, plus the two refusal pins the
  // Slice B domains were missing (tournament, campaign ability card).

  @Test
  void segmentTypeApplierKeepAsCustomClearsStamp() {
    var st = segmentTypeFixture("Custom Type", "admin's description");
    String adminValue = st.getDescription();
    st.setSeedFingerprint("fp-old");
    segmentTypeRepository.save(st);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "segment_type",
            SegmentTypeSync.entityKeyFor("Custom Type"),
            "Custom Type",
            "description",
            adminValue,
            "seed description",
            "fp-new");
    service(segmentTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = segmentTypeRepository.findById(st.getId()).orElseThrow();
    assertEquals(
        adminValue, reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(
        reloaded.getSeedFingerprint(),
        "KEEP_AS_CUSTOM must clear the stamp (the row becomes user-owned content)");
  }

  @Test
  void segmentTypeApplierRemoveDeletesOrphanedRow() {
    // An unbooked type (no segments, no template assignments) is genuinely removable: the
    // hasDependents guard passes and the delete goes through.
    var orphan = segmentTypeFixture("Orphaned Type", "unused");
    orphan.setSeedFingerprint("fp-old");
    segmentTypeRepository.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "segment_type", SegmentTypeSync.entityKeyFor("Orphaned Type"), "Orphaned Type", "fp");
    service(segmentTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        segmentTypeRepository.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned segment type");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void showTypeApplierKeepMineStampsWithoutChangingValue() {
    var st = showTypeFixture("Keep Mine Weekly");
    String adminDescription = st.getDescription();
    st.setSeedFingerprint("fp-old");
    showTypeRepository.save(st);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "show_type",
            ShowTypeSync.entityKeyFor("Keep Mine Weekly"),
            "Keep Mine Weekly",
            "description",
            adminDescription,
            "seed description",
            "fp-keep");
    service(showTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = showTypeRepository.findById(st.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void showTypeApplierKeepAsCustomClearsStamp() {
    var st = showTypeFixture("Custom Weekly");
    String adminDescription = st.getDescription();
    st.setSeedFingerprint("fp-old");
    showTypeRepository.save(st);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "show_type",
            ShowTypeSync.entityKeyFor("Custom Weekly"),
            "Custom Weekly",
            "description",
            adminDescription,
            "seed description",
            "fp-new");
    service(showTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = showTypeRepository.findById(st.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(
        reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the stamp on the show type");
  }

  @Test
  void showTypeApplierRemoveDeletesOrphanedRow() {
    // A type with no shows and no templates referencing it is genuinely removable.
    var orphan = showTypeFixture("Orphaned Show Type");
    orphan.setSeedFingerprint("fp-old");
    showTypeRepository.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "show_type",
            ShowTypeSync.entityKeyFor("Orphaned Show Type"),
            "Orphaned Show Type",
            "fp");
    service(showTypeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        showTypeRepository.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned show type");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void showTemplateApplierKeepMineStampsWithoutChangingValue() {
    var template = showTemplateFixture();
    String adminDescription = template.getDescription();
    template.setSeedFingerprint("fp-old");
    showTemplateRepository.save(template);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "show_template",
            ShowTemplateSync.entityKeyFor("Applier Fixture Template"),
            "Applier Fixture Template",
            "description",
            adminDescription,
            "seed description",
            "fp-keep");
    service(showTemplateApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = showTemplateRepository.findById(template.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void showTemplateApplierKeepAsCustomClearsStamp() {
    var template = showTemplateFixture();
    String adminDescription = template.getDescription();
    template.setSeedFingerprint("fp-old");
    showTemplateRepository.save(template);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "show_template",
            ShowTemplateSync.entityKeyFor("Applier Fixture Template"),
            "Applier Fixture Template",
            "description",
            adminDescription,
            "seed description",
            "fp-new");
    service(showTemplateApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = showTemplateRepository.findById(template.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the template's stamp");
  }

  @Test
  void showTemplateApplierRemoveDeletesOrphanedRow() {
    // A template with no shows generated from it is genuinely removable.
    var orphan = showTemplateFixture();
    orphan.setName("Orphaned Fixture Template");
    orphan.setSeedFingerprint("fp-old");
    showTemplateRepository.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "show_template",
            ShowTemplateSync.entityKeyFor("Orphaned Fixture Template"),
            "Orphaned Fixture Template",
            "fp");
    service(showTemplateApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        showTemplateRepository.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned show template");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void statusCardApplierKeepMineStampsWithoutChangingValue() {
    var card = statusCardFixture("status_keep_mine");
    String adminEffect = card.getLevel1Effect();
    card.setSeedFingerprint("fp-old");
    statusCardRepository.save(card);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "status_card",
            StatusCardSync.entityKeyFor("status_keep_mine"),
            "status_keep_mine",
            "level1Effect",
            adminEffect,
            "vpBonus: 2",
            "fp-keep");
    service(statusCardApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = statusCardRepository.findById(card.getId()).orElseThrow();
    assertEquals(adminEffect, reloaded.getLevel1Effect(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void statusCardApplierKeepAsCustomClearsStamp() {
    var card = statusCardFixture("status_keep_custom");
    String adminEffect = card.getLevel1Effect();
    card.setSeedFingerprint("fp-old");
    statusCardRepository.save(card);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "status_card",
            StatusCardSync.entityKeyFor("status_keep_custom"),
            "status_keep_custom",
            "level1Effect",
            adminEffect,
            "vpBonus: 2",
            "fp-new");
    service(statusCardApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = statusCardRepository.findById(card.getId()).orElseThrow();
    assertEquals(
        adminEffect, reloaded.getLevel1Effect(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the card's stamp");
  }

  @Test
  void statusCardApplierRemoveDeletesOrphanedRow() {
    // A card no wrestler status applies is genuinely removable.
    var orphan = statusCardFixture("status_orphaned");
    orphan.setSeedFingerprint("fp-old");
    statusCardRepository.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "status_card", StatusCardSync.entityKeyFor("status_orphaned"), "status_orphaned", "fp");
    service(statusCardApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        statusCardRepository.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned status card");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void tournamentApplierKeepAsCustomClearsStamp() {
    var tournament = tournamentFixture("applier_custom");
    tournament.setSeedFingerprint("fp-old");
    tournamentRepository.save(tournament);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "tournament",
            TournamentSync.entityKeyFor("applier_custom"),
            "Fixture applier_custom",
            "name",
            "Fixture applier_custom",
            "Renamed By Seed",
            "fp-new");
    service(tournamentApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = tournamentRepository.findById(tournament.getId()).orElseThrow();
    assertEquals(
        "Fixture applier_custom", reloaded.getName(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the tournament's stamp");
  }

  @Test
  void tournamentApplierRemoveDeletesOrphanedRow() {
    // A tournament with no entries, rounds, or payoff show is genuinely removable.
    var orphan = tournamentFixture("applier_orphan");
    orphan.setSeedFingerprint("fp-old");
    tournamentRepository.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "tournament",
            TournamentSync.entityKeyFor("applier_orphan"),
            "Fixture applier_orphan",
            "fp");
    service(tournamentApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        tournamentRepository.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned tournament");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void tournamentApplierRemoveRefusedWhenLifecycleStateExists() {
    // A bracket round exists: the pre-dispatch hasDependents check must refuse the removal —
    // deleting the tournament would cascade-destroy the bracket (TournamentRound FK).
    var referenced = tournamentFixture("applier_rounded");
    com.github.javydreamercsw.management.domain.tournament.TournamentRound round =
        com.github.javydreamercsw.management.domain.tournament.TournamentRound.builder()
            .tournament(referenced)
            .roundNumber(1)
            .roundName("Quarterfinals")
            .status(
                com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus
                    .PENDING)
            .build();
    referenced.getRounds().add(round); // cascade ALL on the parent persists the round
    tournamentRepository.saveAndFlush(referenced);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "tournament",
            TournamentSync.entityKeyFor("applier_rounded"),
            "Fixture applier_rounded",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(tournamentApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a tournament with bracket state must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        tournamentRepository.findById(referenced.getId()).isPresent(),
        "the tournament with bracket state must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  @Test
  void campaignAbilityCardApplierKeepAsCustomClearsStamp() {
    var card = abilityCard("Custom Card", 1);
    String adminScript = card.getEffectScript();
    card.setSeedFingerprint("fp-old");
    campaignAbilityCardRepository.save(card);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "campaign_ability_card",
            CampaignAbilityCardSync.entityKeyFor("Custom Card|HEEL|1"),
            "Custom Card",
            "effectScript",
            adminScript,
            "seedScript()",
            "fp-new");
    service(campaignAbilityCardApplier)
        .apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = campaignAbilityCardRepository.findById(card.getId()).orElseThrow();
    assertEquals(
        adminScript, reloaded.getEffectScript(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the card's stamp");
  }

  @Test
  void campaignAbilityCardApplierRemoveDeletesOrphanedRow() {
    // A card no campaign state holds as active is genuinely removable.
    var orphan = abilityCard("Orphaned Card", 3);
    orphan.setSeedFingerprint("fp-old");
    campaignAbilityCardRepository.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "campaign_ability_card",
            CampaignAbilityCardSync.entityKeyFor("Orphaned Card|HEEL|3"),
            "Orphaned Card",
            "fp");
    service(campaignAbilityCardApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        campaignAbilityCardRepository.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned ability card");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void campaignAbilityCardApplierRemoveRefusedWhenCampaignStateHoldsCard() {
    // A campaign state holds the card as active: the pre-dispatch hasDependents check must
    // refuse the removal — deleting the card would orphan the campaign's active deck (FK).
    var referenced = abilityCard("Held Card", 2);
    Wrestler player = wrestlerRepository.save(Wrestler.builder().name("Campaign Player").build());
    com.github.javydreamercsw.management.domain.campaign.Campaign campaign =
        com.github.javydreamercsw.management.domain.campaign.Campaign.builder()
            .wrestler(player)
            .status(com.github.javydreamercsw.management.domain.campaign.CampaignStatus.ACTIVE)
            .build();
    campaign = campaignRepository.save(campaign);
    com.github.javydreamercsw.management.domain.campaign.CampaignState state =
        com.github.javydreamercsw.management.domain.campaign.CampaignState.builder()
            .campaign(campaign)
            .activeCards(
                new java.util.ArrayList<>(List.of(referenced))) // the FK the guard must protect
            .build();
    campaignStateRepository.save(state);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "campaign_ability_card",
            CampaignAbilityCardSync.entityKeyFor("Held Card|HEEL|2"),
            "Held Card",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(campaignAbilityCardApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a card held by a campaign state must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        campaignAbilityCardRepository.findById(referenced.getId()).isPresent(),
        "the held card must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  // ── Slice C: the 24-path verb matrix for faction/arena/location/team/commentator ──
  // Same per-domain shape as the fix-round block above: USE_NEW through the reminted entity key,
  // KEEP_MINE (stamp without changing the value), KEEP_AS_CUSTOM (clear the stamp), and REMOVE —
  // positive where the domain's real-FK guard permits a genuinely orphaned row, plus the refusal
  // pins for every FK the entity model holds.

  private com.github.javydreamercsw.management.domain.universe.Universe fixtureUniverse() {
    return universeRepositoryTest.findAll().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no default universe after reset"));
  }

  private com.github.javydreamercsw.management.domain.faction.Faction factionFixture(String name) {
    return factionRepository
        .findByName(name)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.faction.Faction f =
                  com.github.javydreamercsw.management.domain.faction.Faction.builder()
                      .name(name)
                      .build();
              return factionRepository.save(f);
            });
  }

  private com.github.javydreamercsw.management.domain.world.Location locationFixture(String name) {
    return locationRepositoryTest
        .findByName(name)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.world.Location l =
                  com.github.javydreamercsw.management.domain.world.Location.builder()
                      .name(name)
                      .build();
              return locationRepositoryTest.save(l);
            });
  }

  private com.github.javydreamercsw.management.domain.world.Arena arenaFixture(String name) {
    return arenaRepositoryTest
        .findByName(name)
        .orElseGet(
            () -> {
              com.github.javydreamercsw.management.domain.world.Arena a =
                  com.github.javydreamercsw.management.domain.world.Arena.builder()
                      .name(name)
                      .location(locationFixture(name + " Site"))
                      .capacity(100)
                      .alignmentBias(
                          com.github.javydreamercsw.management.domain.world.Arena.AlignmentBias
                              .NEUTRAL)
                      .build();
              return arenaRepositoryTest.save(a);
            });
  }

  private com.github.javydreamercsw.management.domain.team.Team teamFixture(String name) {
    return teamRepoTest
        .findByName(name)
        .orElseGet(
            () -> {
              Wrestler w1 =
                  wrestlerRepository.save(Wrestler.builder().name(name + " Member One").build());
              Wrestler w2 =
                  wrestlerRepository.save(Wrestler.builder().name(name + " Member Two").build());
              com.github.javydreamercsw.management.domain.team.Team t =
                  com.github.javydreamercsw.management.domain.team.Team.builder()
                      .name(name)
                      .build();
              t.setMembers(List.of(w1, w2));
              return teamRepoTest.save(t);
            });
  }

  private com.github.javydreamercsw.management.domain.commentator.Commentator commentatorFixture(
      String npcName) {
    return commentatorRepositoryTest.findAll().stream()
        .filter(c -> c.getNpc() != null && npcName.equals(c.getNpc().getName()))
        .findFirst()
        .orElseGet(
            () -> {
              Npc npc =
                  npcRepositoryTest
                      .findByName(npcName)
                      .orElseGet(
                          () -> {
                            Npc n = new Npc();
                            n.setName(npcName);
                            n.setDescription("commentator description");
                            n.setNpcType("Commentator");
                            return npcRepositoryTest.save(n);
                          });
              com.github.javydreamercsw.management.domain.commentator.Commentator c =
                  new com.github.javydreamercsw.management.domain.commentator.Commentator();
              c.setNpc(npc);
              c.setStyle("Play-by-play");
              return commentatorRepositoryTest.save(c);
            });
  }

  @Test
  void factionApplierAppliesUseNewThroughEntityKey() {
    var faction = factionFixture("Entity Key Faction");
    faction.setDescription("old description");
    factionRepository.save(faction);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "faction",
            FactionSync.entityKeyFor("Entity Key Faction"),
            "Drifted Display Faction", // display name drifts — resolution must ignore it
            "description",
            "old description",
            "fresh seed description",
            "fp-faction");
    service(factionApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = factionRepository.findById(faction.getId()).orElseThrow();
    assertEquals(
        "fresh seed description",
        reloaded.getDescription(),
        "USE_NEW must reach the row resolved through the reminted entity key");
    assertEquals("fp-faction", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void factionApplierKeepMineStampsWithoutChangingValue() {
    var faction = factionFixture("Keep Mine Faction");
    String adminDescription = "admin's stable lore";
    faction.setDescription(adminDescription);
    factionRepository.save(faction);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "faction",
            FactionSync.entityKeyFor("Keep Mine Faction"),
            "Keep Mine Faction",
            "description",
            adminDescription,
            "seed description",
            "fp-keep");
    service(factionApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = factionRepository.findById(faction.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void factionApplierKeepAsCustomClearsStamp() {
    var faction = factionFixture("Custom Faction");
    faction.setDescription("admin lore");
    faction.setSeedFingerprint("fp-old");
    factionRepository.save(faction);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "faction",
            FactionSync.entityKeyFor("Custom Faction"),
            "Custom Faction",
            "description",
            "admin lore",
            "seed description",
            "fp-new");
    service(factionApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = factionRepository.findById(faction.getId()).orElseThrow();
    assertEquals(
        "admin lore", reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the faction's stamp");
  }

  @Test
  void factionApplierRemoveDeletesOrphanedRow() {
    // A faction with no member states, teams, or rivalries is genuinely removable.
    var orphan = factionFixture("Orphaned Faction");
    orphan.setSeedFingerprint("fp-old");
    factionRepository.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "faction", FactionSync.entityKeyFor("Orphaned Faction"), "Orphaned Faction", "fp");
    service(factionApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        factionRepository.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned faction");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void factionApplierRemoveRefusedWhenRivalryExists() {
    // A rivalry references the faction on both sides: the pre-dispatch hasDependents check must
    // refuse the removal — deleting the faction would orphan the FactionRivalry FK.
    var referenced = factionFixture("Rivaled Faction");
    var other = factionFixture("Rival Opposition");
    com.github.javydreamercsw.management.domain.faction.FactionRivalry rivalry =
        new com.github.javydreamercsw.management.domain.faction.FactionRivalry();
    rivalry.setFaction1(referenced);
    rivalry.setFaction2(other);
    rivalry.setIsActive(true);
    rivalry.setHeat(5);
    rivalry.setStartedDate(Instant.now());
    factionRivalryRepository.save(rivalry);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "faction", FactionSync.entityKeyFor("Rivaled Faction"), "Rivaled Faction", "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(factionApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a faction with a rivalry must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        factionRepository.findById(referenced.getId()).isPresent(),
        "the rivaled faction must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  @Test
  void arenaApplierAppliesUseNewAndStamps() {
    var arena = arenaFixture("Applier Arena");
    arena.setDescription("old description");
    arenaRepositoryTest.save(arena);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "arena",
            ArenaSync.entityKeyFor("Applier Arena"),
            "Applier Arena",
            "description",
            "old description",
            "fresh arena description",
            "fp-arena");
    service(arenaApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = arenaRepositoryTest.findById(arena.getId()).orElseThrow();
    assertEquals(
        "fresh arena description",
        reloaded.getDescription(),
        "USE_NEW must reach the row resolved through the reminted entity key");
    assertEquals("fp-arena", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void arenaApplierKeepMineStampsWithoutChangingValue() {
    var arena = arenaFixture("Keep Mine Arena");
    String adminDescription = "admin's venue notes";
    arena.setDescription(adminDescription);
    arenaRepositoryTest.save(arena);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "arena",
            ArenaSync.entityKeyFor("Keep Mine Arena"),
            "Keep Mine Arena",
            "description",
            adminDescription,
            "seed description",
            "fp-keep");
    service(arenaApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = arenaRepositoryTest.findById(arena.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void arenaApplierKeepAsCustomClearsStamp() {
    var arena = arenaFixture("Custom Arena");
    arena.setDescription("admin notes");
    arena.setSeedFingerprint("fp-old");
    arenaRepositoryTest.save(arena);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "arena",
            ArenaSync.entityKeyFor("Custom Arena"),
            "Custom Arena",
            "description",
            "admin notes",
            "seed description",
            "fp-new");
    service(arenaApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = arenaRepositoryTest.findById(arena.getId()).orElseThrow();
    assertEquals("admin notes", reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the arena's stamp");
  }

  @Test
  void arenaApplierRemoveDeletesOrphanedRow() {
    // An arena with no booked shows is genuinely removable.
    var orphan = arenaFixture("Orphaned Arena");
    orphan.setSeedFingerprint("fp-old");
    arenaRepositoryTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("arena", ArenaSync.entityKeyFor("Orphaned Arena"), "Orphaned Arena", "fp");
    service(arenaApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        arenaRepositoryTest.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned arena");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void arenaApplierRemoveRefusedWhenShowBooked() {
    // A show is booked at the arena: the pre-dispatch hasDependents check must refuse — deleting
    // the arena would orphan the show's arena FK.
    var referenced = arenaFixture("Booked Arena");
    var showType = showTypeFixture("Arena Show Type");
    var show = new com.github.javydreamercsw.management.domain.show.Show();
    show.setName("Seed Applier Arena Show");
    show.setDescription("a show");
    show.setType(showType);
    show.setArena(referenced);
    show.setCreationDate(Instant.now());
    showRepository.save(show);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("arena", ArenaSync.entityKeyFor("Booked Arena"), "Booked Arena", "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service(arenaApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a booked arena must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        arenaRepositoryTest.findById(referenced.getId()).isPresent(),
        "the booked arena must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  @Test
  void locationApplierAppliesUseNewAndStamps() {
    var location = locationFixture("Applier Location");
    location.setDescription("old description");
    locationRepositoryTest.save(location);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "location",
            LocationSync.entityKeyFor("Applier Location"),
            "Applier Location",
            "description",
            "old description",
            "fresh location description",
            "fp-location");
    service(locationApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = locationRepositoryTest.findById(location.getId()).orElseThrow();
    assertEquals(
        "fresh location description",
        reloaded.getDescription(),
        "USE_NEW must reach the row resolved through the reminted entity key");
    assertEquals("fp-location", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void locationApplierKeepMineStampsWithoutChangingValue() {
    var location = locationFixture("Keep Mine Location");
    String adminDescription = "admin's city notes";
    location.setDescription(adminDescription);
    locationRepositoryTest.save(location);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "location",
            LocationSync.entityKeyFor("Keep Mine Location"),
            "Keep Mine Location",
            "description",
            adminDescription,
            "seed description",
            "fp-keep");
    service(locationApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = locationRepositoryTest.findById(location.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void locationApplierKeepAsCustomClearsStamp() {
    var location = locationFixture("Custom Location");
    location.setDescription("admin notes");
    location.setSeedFingerprint("fp-old");
    locationRepositoryTest.save(location);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "location",
            LocationSync.entityKeyFor("Custom Location"),
            "Custom Location",
            "description",
            "admin notes",
            "seed description",
            "fp-new");
    service(locationApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = locationRepositoryTest.findById(location.getId()).orElseThrow();
    assertEquals("admin notes", reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the location's stamp");
  }

  @Test
  void locationApplierRemoveDeletesOrphanedRow() {
    // A location with no arenas sited there is genuinely removable.
    var orphan = locationFixture("Orphaned Location");
    orphan.setSeedFingerprint("fp-old");
    locationRepositoryTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "location", LocationSync.entityKeyFor("Orphaned Location"), "Orphaned Location", "fp");
    service(locationApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        locationRepositoryTest.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned location");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void locationApplierRemoveRefusedWhenArenasExist() {
    // An arena is sited at the location: the pre-dispatch hasDependents check must refuse —
    // deleting the location would orphan the arena's location FK.
    var referenced = locationFixture("Arena Host Location");
    var arena = arenaFixture("Hosted Arena");
    arena.setLocation(referenced);
    arenaRepositoryTest.save(arena);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "location",
            LocationSync.entityKeyFor("Arena Host Location"),
            "Arena Host Location",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(locationApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a location with arenas must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        locationRepositoryTest.findById(referenced.getId()).isPresent(),
        "the referenced location must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  @Test
  void teamApplierAppliesUseNewThroughEntityKey() {
    var team = teamFixture("Entity Key Team");
    team.setDescription("old description");
    teamRepoTest.save(team);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "team",
            TeamSync.entityKeyFor("Entity Key Team"),
            "Drifted Display Team", // display name drifts — resolution must ignore it
            "description",
            "old description",
            "fresh team description",
            "fp-team");
    service(teamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = teamRepoTest.findById(team.getId()).orElseThrow();
    assertEquals(
        "fresh team description",
        reloaded.getDescription(),
        "USE_NEW must reach the row resolved through the reminted entity key");
    assertEquals("fp-team", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void teamApplierKeepMineStampsWithoutChangingValue() {
    var team = teamFixture("Keep Mine Team");
    String adminDescription = "admin's duo lore";
    team.setDescription(adminDescription);
    teamRepoTest.save(team);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "team",
            TeamSync.entityKeyFor("Keep Mine Team"),
            "Keep Mine Team",
            "description",
            adminDescription,
            "seed description",
            "fp-keep");
    service(teamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = teamRepoTest.findById(team.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void teamApplierKeepAsCustomClearsStamp() {
    var team = teamFixture("Custom Team");
    team.setDescription("admin lore");
    team.setSeedFingerprint("fp-old");
    teamRepoTest.save(team);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "team",
            TeamSync.entityKeyFor("Custom Team"),
            "Custom Team",
            "description",
            "admin lore",
            "seed description",
            "fp-new");
    service(teamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = teamRepoTest.findById(team.getId()).orElseThrow();
    assertEquals("admin lore", reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the team's stamp");
  }

  @Test
  void teamApplierRemoveDeletesOrphanedRow() {
    // A team with no entries or reigns is genuinely removable (its own member rows are not
    // dependents — they cascade with the team).
    var orphan = teamFixture("Orphaned Team");
    orphan.setSeedFingerprint("fp-old");
    teamRepoTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("team", TeamSync.entityKeyFor("Orphaned Team"), "Orphaned Team", "fp");
    service(teamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        teamRepoTest.findById(orphan.getId()).isPresent(), "REMOVE must delete the orphaned team");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void teamApplierRemoveRefusedWhenTrophyReignExists() {
    // A trophy reign names the team as winner: the pre-dispatch hasDependents check must refuse —
    // deleting the team would orphan the TrophyReign.winnerTeam FK.
    var referenced = teamFixture("Reigning Team");
    com.github.javydreamercsw.management.domain.tournament.TrophyReign reign =
        new com.github.javydreamercsw.management.domain.tournament.TrophyReign();
    reign.setTournament(tournamentFixture("applier_reign_tournament"));
    reign.setWinnerTeam(referenced);
    reign.setTrophyName("Applier Cup");
    reign.setYearWon(2034);
    reign.setWonOn(java.time.LocalDate.now());
    trophyReignRepository.save(reign);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval("team", TeamSync.entityKeyFor("Reigning Team"), "Reigning Team", "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service(teamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a team with a trophy reign must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        teamRepoTest.findById(referenced.getId()).isPresent(),
        "the reigning team must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  @Test
  void commentatorApplierAppliesUseNewAndStamps() {
    var commentator = commentatorFixture("Entity Key Commentator");
    commentator.setStyle("old style");
    commentatorRepositoryTest.save(commentator);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "commentator",
            CommentatorSync.entityKeyFor("Entity Key Commentator"),
            "Entity Key Commentator",
            "style",
            "old style",
            "Color / Fresh",
            "fp-commentator");
    service(commentatorApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = commentatorRepositoryTest.findById(commentator.getId()).orElseThrow();
    assertEquals(
        "Color / Fresh", reloaded.getStyle(), "USE_NEW must reach the row through the entity key");
    assertEquals("fp-commentator", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void commentatorApplierKeepMineStampsWithoutChangingValue() {
    var commentator = commentatorFixture("Keep Mine Commentator");
    String adminCatchphrase = "admin's catchphrase";
    commentator.setCatchphrase(adminCatchphrase);
    commentatorRepositoryTest.save(commentator);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "commentator",
            CommentatorSync.entityKeyFor("Keep Mine Commentator"),
            "Keep Mine Commentator",
            "catchphrase",
            adminCatchphrase,
            "seed catchphrase",
            "fp-keep");
    service(commentatorApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = commentatorRepositoryTest.findById(commentator.getId()).orElseThrow();
    assertEquals(
        adminCatchphrase, reloaded.getCatchphrase(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void commentatorApplierKeepAsCustomClearsStamp() {
    var commentator = commentatorFixture("Custom Commentator");
    commentator.setStyle("admin style");
    commentator.setSeedFingerprint("fp-old");
    commentatorRepositoryTest.save(commentator);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "commentator",
            CommentatorSync.entityKeyFor("Custom Commentator"),
            "Custom Commentator",
            "style",
            "admin style",
            "seed style",
            "fp-new");
    service(commentatorApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = commentatorRepositoryTest.findById(commentator.getId()).orElseThrow();
    assertEquals("admin style", reloaded.getStyle(), "KEEP_AS_CUSTOM must keep the value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the commentator's stamp");
  }

  @Test
  void commentatorApplierRemoveDeletesOrphanedRow() {
    // A commentator on no commentary team is genuinely removable.
    var orphan = commentatorFixture("Orphaned Commentator");
    orphan.setSeedFingerprint("fp-old");
    commentatorRepositoryTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "commentator",
            CommentatorSync.entityKeyFor("Orphaned Commentator"),
            "Orphaned Commentator",
            "fp");
    service(commentatorApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        commentatorRepositoryTest.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned commentator");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void commentatorApplierRemoveRefusedWhenOnATeam() {
    // A commentary team fields the commentator: the pre-dispatch hasDependents check must refuse —
    // deleting the commentator would orphan the commentary_team_members join row.
    var referenced = commentatorFixture("Teamed Commentator");
    com.github.javydreamercsw.management.domain.commentator.CommentaryTeam team =
        new com.github.javydreamercsw.management.domain.commentator.CommentaryTeam();
    team.setName("Applier Booth");
    team.setCommentators(
        new java.util.ArrayList<>(java.util.List.of(referenced))); // the FK the guard must protect
    commentaryTeamRepositoryTest.save(team);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "commentator",
            CommentatorSync.entityKeyFor("Teamed Commentator"),
            "Teamed Commentator",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(commentatorApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a teamed commentator must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        commentatorRepositoryTest.findById(referenced.getId()).isPresent(),
        "the teamed commentator must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }
}
