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

import com.github.javydreamercsw.base.domain.account.*;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.campaign.*;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntryRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixRepository;
import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionRepository;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.*;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.seedreview.SeedUpdateDecisionService;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import com.github.javydreamercsw.management.sync.review.SeedFieldDecision;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  @Autowired private CommentaryTeamSeedApplier commentaryTeamApplier;
  @Autowired private CampaignUpgradeSeedApplier campaignUpgradeApplier;
  @Autowired private OutcomeMatrixSeedApplier outcomeMatrixApplier;
  @Autowired private RelationshipSeedApplier relationshipApplier;
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

  @Autowired private SegmentTypeRepository segmentTypeRepository;

  @Autowired private ShowTypeRepository showTypeRepository;

  @Autowired private ShowTemplateRepository showTemplateRepository;

  @Autowired private ShowRepository showRepository;

  @Autowired private StatusCardRepository statusCardRepository;

  @Autowired private WrestlerStatusRepository wrestlerStatusRepository;

  @Autowired private TournamentRepository tournamentRepository;

  @Autowired private CampaignAbilityCardRepository campaignAbilityCardRepository;

  @Autowired private WrestlerStateRepository wrestlerStateRepository;

  @Autowired private CampaignRepository campaignRepository;

  @Autowired private CampaignStateRepository campaignStateRepository;

  @Autowired private FactionRepository factionRepository;

  @Autowired private FactionRivalryRepository factionRivalryRepository;

  @Autowired private ArenaRepository arenaRepositoryTest;

  @Autowired private LocationRepository locationRepositoryTest;

  @Autowired private TeamRepository teamRepoTest;

  @Autowired private CommentatorRepository commentatorRepositoryTest;

  @Autowired private CommentaryTeamRepository commentaryTeamRepositoryTest;

  @Autowired private UniverseRepository universeRepositoryTest;

  @Autowired private NpcRepository npcRepositoryTest;

  @Autowired private OutcomeMatrixRepository outcomeMatrixRepositoryTest;

  @Autowired private OutcomeMatrixEntryRepository outcomeMatrixEntryRepositoryTest;

  @Autowired private WrestlerRelationshipRepository wrestlerRelationshipRepositoryTest;

  @Autowired private CampaignUpgradeRepository campaignUpgradeRepositoryTest;

  @Autowired private TrophyReignRepository trophyReignRepository;

  @Autowired private TournamentEntryRepository tournamentEntryRepository;

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
  void wrestlerApplierUseNewCoversGenderReferenceArms() {
    // The typed enum/reference arms the lossy-name pin never reached: gender (enum valueOf),
    // imageUrl, and expansionCode — one batch, one row.
    Wrestler jessie = wrestler("Jessie McKay");
    wrestlerRepository.save(jessie);
    flushAndClear();

    SeedUpdateDecision gender =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "gender",
            "MALE",
            "FEMALE",
            "fp-g");
    SeedUpdateDecision image =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "imageUrl",
            null,
            "http://example.com/jessie.png",
            "fp-i");
    SeedUpdateDecision expansion =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "expansionCode",
            "EXTREME",
            "RUMBLE",
            "fp-e");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(gender.getId(), SeedFieldDecision.USE_NEW);
    batch.put(image.getId(), SeedFieldDecision.USE_NEW);
    batch.put(expansion.getId(), SeedFieldDecision.USE_NEW);
    service(wrestlerApplier).apply(batch);

    flushAndClear();
    Wrestler reloaded = wrestlerRepository.findById(jessie.getId()).orElseThrow();
    assertEquals(Gender.FEMALE, reloaded.getGender(), "gender arm wrote the parsed enum");
    assertEquals("http://example.com/jessie.png", reloaded.getImageUrl(), "imageUrl arm wrote");
    assertEquals("RUMBLE", reloaded.getExpansionCode(), "expansionCode arm wrote");
  }

  @Test
  void wrestlerApplierNotFoundWarnsOnEveryVerb() {
    // Every verb's not-found lambda: decisions whose display name matches no row log a warning
    // and leave the queue untouched (no row is created, no decision fails).
    SeedUpdateDecision keep =
        park("wrestler", "wrestler:ghost-row", "Ghost Row", "description", "old", "new", "fp-k");
    SeedUpdateDecision useNew =
        park("wrestler", "wrestler:ghost-row", "Ghost Row", "description", "old", "new", "fp-u");
    SeedUpdateDecision custom =
        park("wrestler", "wrestler:ghost-row", "Ghost Row", "description", "old", "new", "fp-c");
    SeedUpdateDecision remove = parkRemoval("wrestler", "wrestler:ghost-row", "Ghost Row", "fp-r");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(keep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(useNew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(custom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(remove.getId(), SeedFieldDecision.REMOVE);
    service(wrestlerApplier).apply(batch);

    flushAndClear();
    assertTrue(
        wrestlerRepository.findByName("Ghost Row").isEmpty(),
        "a decision for a missing row must not create it");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(keep.getId()).orElseThrow().getStatus(),
        "the keep decision still terminates (nothing to keep is a warn, not a failure)");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_NEW,
        decisionRepository.findById(useNew.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP_AS_CUSTOM,
        decisionRepository.findById(custom.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(remove.getId()).orElseThrow().getStatus());
  }

  @Test
  void wrestlerApplierUseNewBuildsAlignmentAndToleratesInvalidEnum() {
    // The alignment writer's two remaining arms: the builder path (row holds no alignment yet —
    // setAlignment appends the first holder) and the IllegalArgumentException catch (an invalid
    // enum string is warned and skipped, the apply still stamps).
    Wrestler bare = wrestler("OMZ");
    bare.getAlignments().clear();
    wrestlerRepository.save(bare);
    flushAndClear();
    assertTrue(
        wrestlerRepository.findById(bare.getId()).orElseThrow().getAlignment() == null,
        "fixture precondition: the row holds no alignment");

    SeedUpdateDecision build =
        park("wrestler", "wrestler:omz", "OMZ", "alignment", "FACE", "HEEL", "fp-build");
    service(wrestlerApplier).apply(Map.of(build.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    assertEquals(
        AlignmentType.HEEL,
        wrestlerRepository.findById(bare.getId()).orElseThrow().getAlignment().getAlignmentType(),
        "USE_NEW must build the alignment holder when the row has none");

    Wrestler holder = wrestler("Jessie McKay"); // seeds at HEEL
    flushAndClear();
    SeedUpdateDecision invalid =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "alignment",
            "HEEL",
            "NOT_A_TYPE",
            "fp-bad");
    service(wrestlerApplier).apply(Map.of(invalid.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    Wrestler stillThere = wrestlerRepository.findById(holder.getId()).orElseThrow();
    assertEquals(
        AlignmentType.HEEL,
        stillThere.getAlignment().getAlignmentType(),
        "an invalid enum value must be warned and skipped, not written");
    assertEquals("fp-bad", stillThere.getSeedFingerprint(), "the apply still stamps the row");
  }

  @Test
  void wrestlerApplierUnknownFieldFailsLoud() {
    // The default arm: an unknown field name fails loud with IllegalStateException.
    Wrestler jessie = wrestler("Jessie McKay");
    flushAndClear();
    SeedUpdateDecision unknown =
        park("wrestler", "wrestler:jessie-mckay", "Jessie McKay", "fans", "1", "2", "fp-u");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(wrestlerApplier).apply(Map.of(unknown.getId(), SeedFieldDecision.USE_NEW)));
    assertTrue(
        exception.getMessage().contains("not a seed-owned field"),
        "an unknown wrestler field must fail loud");
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

  @Test
  void wrestlerApplierUseNewRewritesAlignmentAndStamps() {
    // "Jessie McKay" seeds at HEEL; the admin changed the row to FACE, so the seed's HEEL parked
    // as a field conflict. USE_NEW must rewrite the row's alignment holder in place (the derived
    // getAlignment() must reflect the seed change, not leave the stale holder first in the set)
    // and stamp the decision's new fingerprint.
    Wrestler jessie = wrestler("Jessie McKay");
    flushAndClear();
    assertEquals(
        AlignmentType.HEEL,
        wrestlerRepository.findById(jessie.getId()).orElseThrow().getAlignment().getAlignmentType(),
        "fixture precondition: the seeded row holds the seed's HEEL alignment");

    SeedUpdateDecision decision =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "alignment",
            AlignmentType.FACE.name(),
            AlignmentType.HEEL.name(),
            "fp-align");
    service(wrestlerApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    Wrestler reloaded = wrestlerRepository.findById(jessie.getId()).orElseThrow();
    assertEquals(
        AlignmentType.HEEL,
        reloaded.getAlignment().getAlignmentType(),
        "USE_NEW must rewrite the row's alignment to the decision's incoming seed value");
    assertEquals(
        "fp-align", reloaded.getSeedFingerprint(), "USE_NEW must stamp the detection fingerprint");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_NEW,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void wrestlerApplierUseNewCoversNullValueArms() {
    // The null-value arms of the typed writer on the columns that are nullable: a decision whose
    // newValue is null clears heritageTag and passes a null description straight through (the
    // drive/deckSize/startingHealth null arms are unreachable at the persistence layer — those
    // columns are NOT NULL).
    Wrestler jessie = wrestler("Jessie McKay");
    jessie.setHeritageTag("old tag");
    jessie.setDescription("old description");
    wrestlerRepository.save(jessie);
    flushAndClear();

    SeedUpdateDecision heritage =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "heritageTag",
            "old tag",
            null,
            "fp-ht");
    SeedUpdateDecision description =
        park(
            "wrestler",
            "wrestler:jessie-mckay",
            "Jessie McKay",
            "description",
            "old description",
            null,
            "fp-d");

    service(wrestlerApplier)
        .apply(
            Map.of(
                heritage.getId(), SeedFieldDecision.USE_NEW,
                description.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    Wrestler reloaded = wrestlerRepository.findById(jessie.getId()).orElseThrow();
    assertNull(reloaded.getHeritageTag(), "null heritageTag arm clears the value");
    assertNull(reloaded.getDescription(), "null description arm clears the value");
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

  @Test
  void segmentRuleApplierUseNewGuideHashWarnsStampsAndAppliesWithoutRewriting() {
    // The guide payload travels as a hash: the decision's newValue carries the SHA-256 of the
    // incoming guide JSON, not the guide itself, so USE_NEW cannot rewrite the guide content.
    // The applier must warn, write nothing, and still stamp + mark APPLIED_NEW (same behavior as
    // SegmentTypeSeedApplier) instead of throwing and rolling back the whole apply batch.
    SegmentRule noDq = segmentRule("No DQ");
    SegmentRulePlayGuide guideBefore = noDq.getGuide();
    String hashBefore = noDq.getGuideHash();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "segment_rule",
            "segment_rule:no-dq",
            "No DQ",
            "guideHash",
            hashBefore,
            "incoming-guide-hash-value",
            "fp-guide");
    service(segmentRuleApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    SegmentRule reloaded = segmentRuleRepository.findById(noDq.getId()).orElseThrow();
    assertEquals(
        guideBefore,
        reloaded.getGuide(),
        "USE_NEW on guideHash must leave the guide content unchanged (the hash is not the guide)");
    assertEquals(
        "fp-guide",
        reloaded.getSeedFingerprint(),
        "USE_NEW on guideHash must still stamp the detection fingerprint");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_NEW,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the guideHash decision must resolve as APPLIED_NEW, not roll back");
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
    Map<String, Integer> composition = new HashMap<>();
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
    WrestlerState state =
        WrestlerState.builder().wrestler(managed).universe(defaultUniverse).build();
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

  @Test
  void npcApplierUseNewCoversEveryFieldArm() {
    // One decision per uncovered applyFieldValue arm: npcType, expansionCode, the null-alignment
    // arm (seed omits alignment → NEUTRAL), and the awareness write through the service. A second
    // row covers the null-awareness arm (seed omits it → 0) — both rows sit in one all-or-nothing
    // batch, matching the review UI's single submit.
    Npc target = npc("Charles");
    target.setAlignment(AlignmentType.FACE);
    target.getAttributes().put(NpcService.ATTRIBUTE_AWARENESS, 50);
    npcRepositoryTest.save(target);
    Npc cleared = npc("Lena Voss");
    cleared.getAttributes().put(NpcService.ATTRIBUTE_AWARENESS, 50);
    npcRepositoryTest.save(cleared);
    flushAndClear();

    SeedUpdateDecision type =
        park("npc", "npc:charles", "Charles", "npcType", "Owner", "Referee", "fp-t");
    SeedUpdateDecision expansion =
        park("npc", "npc:charles", "Charles", "expansionCode", "BASE_GAME", "RUMBLE", "fp-e");
    SeedUpdateDecision alignment =
        park("npc", "npc:charles", "Charles", "alignment", "FACE", null, "fp-a");
    SeedUpdateDecision awareness =
        park("npc", "npc:charles", "Charles", "awareness", "50", "85", "fp-w");
    SeedUpdateDecision clearedAwareness =
        park("npc", "npc:lena-voss", "Lena Voss", "awareness", "50", null, "fp-w0");

    service(npcApplier)
        .apply(
            Map.of(
                type.getId(), SeedFieldDecision.USE_NEW,
                expansion.getId(), SeedFieldDecision.USE_NEW,
                alignment.getId(), SeedFieldDecision.USE_NEW,
                awareness.getId(), SeedFieldDecision.USE_NEW,
                clearedAwareness.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    Npc reloaded = npcRepositoryTest.findById(target.getId()).orElseThrow();
    assertEquals("Referee", reloaded.getNpcType(), "npcType arm wrote the decision value");
    assertEquals(
        "RUMBLE", reloaded.getExpansionCode(), "expansionCode arm wrote the decision value");
    assertEquals(
        AlignmentType.NEUTRAL, reloaded.getAlignment(), "null alignment arm falls back to NEUTRAL");
    assertEquals(
        85,
        ((Number) reloaded.getAttributes().get(NpcService.ATTRIBUTE_AWARENESS)).intValue(),
        "awareness arm wrote through the service");
    Npc clearedRow = npcRepositoryTest.findById(cleared.getId()).orElseThrow();
    assertEquals(
        0,
        ((Number) clearedRow.getAttributes().get(NpcService.ATTRIBUTE_AWARENESS)).intValue(),
        "null-awareness arm falls back to 0");
  }

  @Test
  void npcApplierVerbsOnOrphanedAndMissingRows() {
    // The remaining verb paths: KEEP_MINE stamps without changing the value; KEEP_AS_CUSTOM
    // clears the stamp; REMOVE on a genuinely unreferenced row deletes it; and one missing-row
    // decision per verb covers the warn lambdas.
    Npc orphan = npc("Ghost Manager");
    orphan.setSeedFingerprint("fp-old");
    npcRepositoryTest.save(orphan);
    Npc doomed = npc("Doomed Manager");
    npcRepositoryTest.save(doomed);
    flushAndClear();

    SeedUpdateDecision keep =
        park("npc", "npc:charles", "Charles", "description", "old", "seed", "fp-k");
    SeedUpdateDecision custom =
        park("npc", "npc:ghost-manager", "Ghost Manager", "description", "old", "seed", "fp-c");
    SeedUpdateDecision remove = parkRemoval("npc", "npc:doomed-manager", "Doomed Manager", "fp-r");
    SeedUpdateDecision ghostKeep =
        park("npc", "npc:missing-row", "Missing Row", "description", "old", "seed", "fp-gk");
    SeedUpdateDecision ghostNew =
        park("npc", "npc:missing-row", "Missing Row", "description", "old", "seed", "fp-gn");
    SeedUpdateDecision ghostCustom =
        park("npc", "npc:missing-row", "Missing Row", "description", "old", "seed", "fp-gc");
    SeedUpdateDecision ghostRemove = parkRemoval("npc", "npc:missing-row", "Missing Row", "fp-gr");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(keep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(custom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(remove.getId(), SeedFieldDecision.REMOVE);
    batch.put(ghostKeep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(ghostNew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(ghostCustom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(ghostRemove.getId(), SeedFieldDecision.REMOVE);
    service(npcApplier).apply(batch);

    flushAndClear();
    Npc reloaded = npcRepositoryTest.findById(orphan.getId()).orElseThrow();
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM clears the orphan's stamp");
    assertFalse(
        npcRepositoryTest.findById(doomed.getId()).isPresent(),
        "REMOVE deletes the unreferenced row");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(keep.getId()).orElseThrow().getStatus());
    assertTrue(
        npcRepositoryTest.findByName("Missing Row").isEmpty(),
        "missing-row decisions never create the row");
  }

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
    Account account = createTestAccount("seed-achiever", RoleName.PLAYER);
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

  @Test
  void achievementApplierUseNewCoversEveryFieldArm() {
    // One decision per uncovered applyFieldValue arm: name, xpValue, the category enum arm, and
    // unlockCondition, plus the null-category fail-safe pin on the second row. xpValue's null arm
    // is unreachable at the persistence layer (NOT NULL column), so the value arm is pinned
    // instead.
    Achievement first = achievement("FIRST_WRESTLER", "Rookie Manager");
    Achievement blank = achievement("CHALLENGE_FIRST_HARD", "Glutton for Punishment");
    flushAndClear();

    SeedUpdateDecision name =
        park(
            "achievement",
            "achievement:first_wrestler",
            "FIRST_WRESTLER",
            "name",
            "Rookie Manager",
            "Renamed By Seed",
            "fp-n");
    SeedUpdateDecision xp =
        park(
            "achievement",
            "achievement:first_wrestler",
            "FIRST_WRESTLER",
            "xpValue",
            "50",
            "75",
            "fp-x");
    SeedUpdateDecision category =
        park(
            "achievement",
            "achievement:first_wrestler",
            "FIRST_WRESTLER",
            "category",
            "COLLECTION",
            "CHALLENGE",
            "fp-c");
    SeedUpdateDecision unlock =
        park(
            "achievement",
            "achievement:first_wrestler",
            "FIRST_WRESTLER",
            "unlockCondition",
            "!wrestlers.isEmpty()",
            "own three wrestlers",
            "fp-u");
    SeedUpdateDecision secondCategory =
        park(
            "achievement",
            "achievement:challenge_first_hard",
            "CHALLENGE_FIRST_HARD",
            "category",
            "CHALLENGE",
            "FANS",
            "fp-c0");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(name.getId(), SeedFieldDecision.USE_NEW);
    batch.put(xp.getId(), SeedFieldDecision.USE_NEW);
    batch.put(category.getId(), SeedFieldDecision.USE_NEW);
    batch.put(unlock.getId(), SeedFieldDecision.USE_NEW);
    batch.put(secondCategory.getId(), SeedFieldDecision.USE_NEW);

    service(achievementApplier).apply(batch);

    flushAndClear();
    Achievement reloaded = achievementRepository.findById(first.getId()).orElseThrow();
    assertEquals("Renamed By Seed", reloaded.getName(), "name arm wrote the decision value");
    assertEquals(75, reloaded.getXpValue(), "xpValue arm wrote the parsed int");
    assertEquals(
        AchievementCategory.CHALLENGE, reloaded.getCategory(), "category arm wrote the enum");
    assertEquals(
        "own three wrestlers",
        reloaded.getUnlockCondition(),
        "unlockCondition arm wrote the value");
    Achievement blankRow = achievementRepository.findById(blank.getId()).orElseThrow();
    assertEquals(
        AchievementCategory.FANS,
        blankRow.getCategory(),
        "the second row's category arm wrote its own decision's enum");
  }

  @Test
  void achievementApplierVerbsOnOrphanedAndMissingRows() {
    // The remaining verb paths: KEEP_MINE on an orphaned row stamps it; KEEP_AS_CUSTOM clears
    // the stamp; REMOVE deletes the unearned row; and one missing-row decision per verb covers
    // the warn lambdas.
    Achievement orphan = achievement("ORPHAN_ACHIEVEMENT", "Orphan Achievement");
    orphan.setSeedFingerprint("fp-old");
    achievementRepository.save(orphan);
    Achievement doomed = achievement("DOOMED_ACHIEVEMENT", "Doomed Achievement");
    achievementRepository.save(doomed);
    flushAndClear();

    SeedUpdateDecision keep =
        park(
            "achievement",
            "achievement:first_wrestler",
            "FIRST_WRESTLER",
            "description",
            "old",
            "seed",
            "fp-k");
    SeedUpdateDecision custom =
        park(
            "achievement",
            "achievement:orphan_achievement",
            "ORPHAN_ACHIEVEMENT",
            "description",
            "old",
            "seed",
            "fp-c");
    SeedUpdateDecision remove =
        parkRemoval("achievement", "achievement:doomed_achievement", "DOOMED_ACHIEVEMENT", "fp-r");
    SeedUpdateDecision ghostKeep =
        park(
            "achievement",
            "achievement:missing_key",
            "MISSING_KEY",
            "description",
            "old",
            "seed",
            "fp-gk");
    SeedUpdateDecision ghostNew =
        park(
            "achievement",
            "achievement:missing_key",
            "MISSING_KEY",
            "description",
            "old",
            "seed",
            "fp-gn");
    SeedUpdateDecision ghostCustom =
        park(
            "achievement",
            "achievement:missing_key",
            "MISSING_KEY",
            "description",
            "old",
            "seed",
            "fp-gc");
    SeedUpdateDecision ghostRemove =
        parkRemoval("achievement", "achievement:missing_key", "MISSING_KEY", "fp-gr");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(keep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(custom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(remove.getId(), SeedFieldDecision.REMOVE);
    batch.put(ghostKeep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(ghostNew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(ghostCustom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(ghostRemove.getId(), SeedFieldDecision.REMOVE);
    service(achievementApplier).apply(batch);

    flushAndClear();
    Achievement reloaded = achievementRepository.findById(orphan.getId()).orElseThrow();
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM clears the orphan's stamp");
    assertFalse(
        achievementRepository.findById(doomed.getId()).isPresent(),
        "REMOVE deletes the unearned achievement");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(keep.getId()).orElseThrow().getStatus());
    assertTrue(
        achievementRepository.findByKey("MISSING_KEY").isEmpty(),
        "missing-row decisions never create the row");
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

  private SegmentType segmentTypeFixture(String name, String description) {
    return segmentTypeRepository
        .findByName(name)
        .orElseGet(
            () -> {
              SegmentType st = new SegmentType();
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
    ShowTemplate template = showTemplateFixture();
    ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
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

  private ShowType showTypeFixture(String name) {
    return showTypeRepository
        .findByName(name)
        .orElseGet(
            () -> {
              ShowType st = new ShowType();
              st.setName(name);
              st.setDescription("original");
              st.setCategory(ShowCategory.OTHER);
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
    var show = new Show();
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

  private ShowTemplate showTemplateFixture() {
    ShowTemplate t = new ShowTemplate();
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
    var show = new Show();
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

  @Test
  void showTemplateApplierUseNewCoversEveryFieldArm() {
    // One decision per uncovered applyFieldValue arm, all riding one batch on the fixture
    // template: showTypeName (resolved through the repository), commentaryTeamName (found),
    // expectedMatches (null → cleared), expectedPromos, recurrenceType, dayOfWeek, dayOfMonth,
    // weekOfMonth, month, genderConstraint, and requiredExpansions (replaced). The unknown-name
    // arms are exercised in the second template: showTypeName and commentaryTeamName lookups
    // that resolve to nothing keep the current references (warn, not write).
    var template = showTemplateFixture();
    template.setCommentaryTeam(null);
    template.setRecurrenceType(RecurrenceType.NONE);
    template.setDayOfWeek(null);
    template.setDayOfMonth(null);
    template.setWeekOfMonth(null);
    template.setMonth(null);
    template.setGenderConstraint(null);
    template.setExpectedPromos(1);
    showTemplateRepository.save(template);
    var drift = new ShowTemplate();
    drift.setName("Ghost Reference Template");
    drift.setShowType(showTypeFixture("Weekly"));
    drift.setExpectedPromos(2);
    showTemplateRepository.save(drift);
    var team = commentaryTeamFixture("Applier Crew", List.of("Solo Member"));
    flushAndClear();

    String key = ShowTemplateSync.entityKeyFor("Applier Fixture Template");
    String driftKey = ShowTemplateSync.entityKeyFor("Ghost Reference Template");
    SeedUpdateDecision showType =
        park(
            "show_template",
            key,
            "Applier Fixture Template",
            "showTypeName",
            "Weekly",
            "Weekly",
            "fp-st");
    SeedUpdateDecision crew =
        park(
            "show_template",
            key,
            "Applier Fixture Template",
            "commentaryTeamName",
            null,
            "Applier Crew",
            "fp-ct");
    SeedUpdateDecision expectedMatches =
        park(
            "show_template",
            key,
            "Applier Fixture Template",
            "expectedMatches",
            "3",
            null,
            "fp-em");
    SeedUpdateDecision expectedPromos =
        park("show_template", key, "Applier Fixture Template", "expectedPromos", "1", "4", "fp-ep");
    SeedUpdateDecision recurrence =
        park(
            "show_template",
            key,
            "Applier Fixture Template",
            "recurrenceType",
            "NONE",
            "WEEKLY",
            "fp-rt");
    SeedUpdateDecision dayOfWeek =
        park(
            "show_template",
            key,
            "Applier Fixture Template",
            "dayOfWeek",
            null,
            "MONDAY",
            "fp-dow");
    SeedUpdateDecision dayOfMonth =
        park("show_template", key, "Applier Fixture Template", "dayOfMonth", null, "7", "fp-dom");
    SeedUpdateDecision weekOfMonth =
        park("show_template", key, "Applier Fixture Template", "weekOfMonth", null, "2", "fp-wom");
    SeedUpdateDecision month =
        park("show_template", key, "Applier Fixture Template", "month", null, "JANUARY", "fp-m");
    SeedUpdateDecision gender =
        park(
            "show_template",
            key,
            "Applier Fixture Template",
            "genderConstraint",
            null,
            "MALE",
            "fp-g");
    SeedUpdateDecision expansions =
        park(
            "show_template",
            key,
            "Applier Fixture Template",
            "requiredExpansions",
            "",
            "RUMBLE\nEXTREME",
            "fp-re");
    SeedUpdateDecision ghostShowType =
        park(
            "show_template",
            driftKey,
            "Ghost Reference Template",
            "showTypeName",
            "Weekly",
            "Ghost Show Type",
            "fp-gst");
    SeedUpdateDecision ghostCrew =
        park(
            "show_template",
            driftKey,
            "Ghost Reference Template",
            "commentaryTeamName",
            null,
            "Ghost Crew",
            "fp-gct");

    SeedUpdateDecision durationFallback =
        park(
            "show_template",
            driftKey,
            "Ghost Reference Template",
            "durationDays",
            "2",
            null,
            "fp-df");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(showType.getId(), SeedFieldDecision.USE_NEW);
    batch.put(crew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(expectedMatches.getId(), SeedFieldDecision.USE_NEW);
    batch.put(expectedPromos.getId(), SeedFieldDecision.USE_NEW);
    batch.put(recurrence.getId(), SeedFieldDecision.USE_NEW);
    batch.put(dayOfWeek.getId(), SeedFieldDecision.USE_NEW);
    batch.put(dayOfMonth.getId(), SeedFieldDecision.USE_NEW);
    batch.put(weekOfMonth.getId(), SeedFieldDecision.USE_NEW);
    batch.put(month.getId(), SeedFieldDecision.USE_NEW);
    batch.put(gender.getId(), SeedFieldDecision.USE_NEW);
    batch.put(expansions.getId(), SeedFieldDecision.USE_NEW);
    batch.put(ghostShowType.getId(), SeedFieldDecision.USE_NEW);
    batch.put(ghostCrew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(durationFallback.getId(), SeedFieldDecision.USE_NEW);

    service(showTemplateApplier).apply(batch);

    flushAndClear();
    var reloaded = showTemplateRepository.findById(template.getId()).orElseThrow();
    assertEquals("Weekly", reloaded.getShowType().getName(), "showTypeName arm resolved the type");
    assertEquals(
        "Applier Crew",
        reloaded.getCommentaryTeam().getName(),
        "commentaryTeamName arm resolved the team");
    assertNull(reloaded.getExpectedMatches(), "null expectedMatches arm clears the value");
    assertEquals(4, reloaded.getExpectedPromos(), "expectedPromos arm wrote the value");
    assertEquals(RecurrenceType.WEEKLY, reloaded.getRecurrenceType(), "recurrenceType arm wrote");
    assertEquals(DayOfWeek.MONDAY, reloaded.getDayOfWeek(), "dayOfWeek arm wrote the value");
    assertEquals(7, reloaded.getDayOfMonth(), "dayOfMonth arm wrote the value");
    assertEquals(2, reloaded.getWeekOfMonth(), "weekOfMonth arm wrote the value");
    assertEquals(Month.JANUARY, reloaded.getMonth(), "month arm wrote the enum");
    assertEquals(
        Gender.MALE, reloaded.getGenderConstraint(), "genderConstraint arm wrote the enum");
    assertEquals(
        Set.of("RUMBLE", "EXTREME"),
        reloaded.getRequiredExpansions(),
        "requiredExpansions arm replaced the list");
    var drifted = showTemplateRepository.findById(drift.getId()).orElseThrow();
    assertEquals(
        "Weekly",
        drifted.getShowType().getName(),
        "unknown showTypeName keeps the current reference (warn arm)");
    assertNull(drifted.getCommentaryTeam(), "unknown commentaryTeamName keeps the reference unset");
    assertEquals(
        1, drifted.getDurationDays(), "null durationDays arm falls back to the default of 1");
  }

  @Test
  void showTemplateApplierNotFoundWarnsOnEveryVerb() {
    // Every verb's not-found lambda plus the fail-safe remove re-check: decisions whose key
    // matches no template log a warning; a removal that arrives with shows already checked via
    // hasDependents still re-checks harmlessly through the delete-time guard.
    SeedUpdateDecision keep =
        park(
            "show_template",
            ShowTemplateSync.entityKeyFor("Ghost Template"),
            "Ghost Template",
            "description",
            "o",
            "n",
            "fp-k");
    SeedUpdateDecision useNew =
        park(
            "show_template",
            ShowTemplateSync.entityKeyFor("Ghost Template"),
            "Ghost Template",
            "description",
            "o",
            "n",
            "fp-u");
    SeedUpdateDecision custom =
        park(
            "show_template",
            ShowTemplateSync.entityKeyFor("Ghost Template"),
            "Ghost Template",
            "description",
            "o",
            "n",
            "fp-c");
    SeedUpdateDecision remove =
        parkRemoval(
            "show_template",
            ShowTemplateSync.entityKeyFor("Ghost Template"),
            "Ghost Template",
            "fp-r");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(keep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(useNew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(custom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(remove.getId(), SeedFieldDecision.REMOVE);
    service(showTemplateApplier).apply(batch);

    flushAndClear();
    assertTrue(
        showTemplateRepository.findByName("Ghost Template").isEmpty(),
        "decisions for a missing template never create the row");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(keep.getId()).orElseThrow().getStatus());
  }

  // ── StatusCardSeedApplier ───────────────────────────────────────────────────

  private StatusCard statusCardFixture(String key) {
    return statusCardRepository
        .findByKey(key)
        .orElseGet(
            () -> {
              StatusCard card = new StatusCard();
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
    WrestlerStatus status = WrestlerStatus.builder().wrestler(holder).statusCard(card).build();
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

  private Tournament tournamentFixture(String code) {
    return tournamentRepository
        .findByCodeWithRules(code)
        .orElseGet(
            () -> {
              Tournament t = new Tournament();
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

  private CampaignAbilityCard abilityCard(String name, int level) {
    return campaignAbilityCardRepository
        .findByNameAndAlignmentTypeAndLevel(name, AlignmentType.HEEL, level)
        .orElseGet(
            () -> {
              CampaignAbilityCard c = new CampaignAbilityCard();
              c.setName(name);
              c.setAlignmentType(AlignmentType.HEEL);
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
    TournamentRound round =
        TournamentRound.builder()
            .tournament(referenced)
            .roundNumber(1)
            .roundName("Quarterfinals")
            .status(TournamentRoundStatus.PENDING)
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
    Campaign campaign = Campaign.builder().wrestler(player).status(CampaignStatus.ACTIVE).build();
    campaign = campaignRepository.save(campaign);
    CampaignState state =
        CampaignState.builder()
            .campaign(campaign)
            .activeCards(new ArrayList<>(List.of(referenced))) // the FK the guard must protect
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

  private Faction factionFixture(String name) {
    return factionRepository
        .findByName(name)
        .orElseGet(
            () -> {
              Faction f = Faction.builder().name(name).build();
              return factionRepository.save(f);
            });
  }

  private Location locationFixture(String name) {
    return locationRepositoryTest
        .findByName(name)
        .orElseGet(
            () -> {
              Location l = Location.builder().name(name).build();
              return locationRepositoryTest.save(l);
            });
  }

  private Arena arenaFixture(String name) {
    return arenaRepositoryTest
        .findByName(name)
        .orElseGet(
            () -> {
              Arena a =
                  Arena.builder()
                      .name(name)
                      .location(locationFixture(name + " Site"))
                      .capacity(100)
                      .alignmentBias(Arena.AlignmentBias.NEUTRAL)
                      .build();
              return arenaRepositoryTest.save(a);
            });
  }

  private Team teamFixture(String name) {
    return teamRepoTest
        .findByName(name)
        .orElseGet(
            () -> {
              Wrestler w1 =
                  wrestlerRepository.save(Wrestler.builder().name(name + " Member One").build());
              Wrestler w2 =
                  wrestlerRepository.save(Wrestler.builder().name(name + " Member Two").build());
              Team t = Team.builder().name(name).build();
              t.setMembers(List.of(w1, w2));
              return teamRepoTest.save(t);
            });
  }

  private Commentator commentatorFixture(String npcName) {
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
              Commentator c = new Commentator();
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
    FactionRivalry rivalry = new FactionRivalry();
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
    var show = new Show();
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
  void arenaApplierUseNewCoversEveryFieldArm() {
    // One decision per uncovered applyFieldValue arm on the fixture arena: the resolvable
    // location arm, the unresolvable-location warn arm (reference kept), the null-capacity arm
    // (falls back to 0), the alignmentBias enum arm, the imageUrl arm, and the
    // environmentalTraits serialization arm.
    var arena = arenaFixture("Trait Test Arena");
    arena.setCapacity(50);
    arena.setImageUrl("http://example.com/old.png");
    arena.setEnvironmentalTraits(new HashSet<>(Set.of("old-trait")));
    arenaRepositoryTest.save(arena);
    var ghost = arenaFixture("Ghost Reference Arena");
    arenaRepositoryTest.save(ghost);
    flushAndClear();

    String key = ArenaSync.entityKeyFor("Trait Test Arena");
    String ghostKey = ArenaSync.entityKeyFor("Ghost Reference Arena");
    SeedUpdateDecision location =
        park(
            "arena",
            key,
            "Trait Test Arena",
            "location",
            "Trait Test Arena Site",
            "Chicago",
            "fp-l");
    SeedUpdateDecision capacity =
        park("arena", key, "Trait Test Arena", "capacity", "50", null, "fp-c");
    SeedUpdateDecision bias =
        park(
            "arena", key, "Trait Test Arena", "alignmentBias", "NEUTRAL", "HEEL_FAVORABLE", "fp-b");
    SeedUpdateDecision image =
        park(
            "arena",
            key,
            "Trait Test Arena",
            "imageUrl",
            "http://example.com/old.png",
            "http://example.com/new.png",
            "fp-i");
    SeedUpdateDecision traits =
        park(
            "arena",
            key,
            "Trait Test Arena",
            "environmentalTraits",
            "old-trait",
            "new-trait-a\nnew-trait-b",
            "fp-t");
    SeedUpdateDecision ghostLocation =
        park(
            "arena",
            ghostKey,
            "Ghost Reference Arena",
            "location",
            "Ghost Site",
            "Ghost City",
            "fp-gl");

    service(arenaApplier)
        .apply(
            Map.of(
                location.getId(), SeedFieldDecision.USE_NEW,
                capacity.getId(), SeedFieldDecision.USE_NEW,
                bias.getId(), SeedFieldDecision.USE_NEW,
                image.getId(), SeedFieldDecision.USE_NEW,
                traits.getId(), SeedFieldDecision.USE_NEW,
                ghostLocation.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = arenaRepositoryTest.findById(arena.getId()).orElseThrow();
    assertEquals("Chicago", reloaded.getLocation().getName(), "location arm resolved by name");
    assertEquals(0, reloaded.getCapacity(), "null capacity arm falls back to 0");
    assertEquals(
        Arena.AlignmentBias.HEEL_FAVORABLE, reloaded.getAlignmentBias(), "bias arm wrote the enum");
    assertEquals("http://example.com/new.png", reloaded.getImageUrl(), "imageUrl arm wrote");
    assertEquals(
        Set.of("new-trait-a", "new-trait-b"),
        reloaded.getEnvironmentalTraits(),
        "traits arm split the newline-joined serialization");
    var ghostRow = arenaRepositoryTest.findById(ghost.getId()).orElseThrow();
    assertEquals(
        "Ghost Reference Arena Site",
        ghostRow.getLocation().getName(),
        "unknown location keeps the current reference (warn arm)");
  }

  @Test
  void arenaApplierNotFoundWarnsOnEveryVerb() {
    // Every verb's not-found lambda: decisions whose entity key matches no arena log a warning
    // and leave the queue untouched.
    SeedUpdateDecision keep =
        park(
            "arena",
            ArenaSync.entityKeyFor("Ghost Arena"),
            "Ghost Arena",
            "description",
            "o",
            "n",
            "fp-k");
    SeedUpdateDecision useNew =
        park(
            "arena",
            ArenaSync.entityKeyFor("Ghost Arena"),
            "Ghost Arena",
            "description",
            "o",
            "n",
            "fp-u");
    SeedUpdateDecision custom =
        park(
            "arena",
            ArenaSync.entityKeyFor("Ghost Arena"),
            "Ghost Arena",
            "description",
            "o",
            "n",
            "fp-c");
    SeedUpdateDecision remove =
        parkRemoval("arena", ArenaSync.entityKeyFor("Ghost Arena"), "Ghost Arena", "fp-r");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(keep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(useNew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(custom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(remove.getId(), SeedFieldDecision.REMOVE);
    service(arenaApplier).apply(batch);

    flushAndClear();
    assertTrue(
        arenaRepositoryTest.findByName("Ghost Arena").isEmpty(),
        "decisions for a missing arena never create the row");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(keep.getId()).orElseThrow().getStatus());
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
    TrophyReign reign = new TrophyReign();
    reign.setTournament(tournamentFixture("applier_reign_tournament"));
    reign.setWinnerTeam(referenced);
    reign.setTrophyName("Applier Cup");
    reign.setYearWon(2034);
    reign.setWonOn(LocalDate.now());
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
    CommentaryTeam team = new CommentaryTeam();
    team.setName("Applier Booth");
    team.setCommentators(new ArrayList<>(List.of(referenced))); // the FK the guard must protect
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

  // ── Slice D: the 24-path verb matrix for commentary-team/campaign-upgrade/outcome-matrix/
  // relationship ──. Same per-domain shape as the Slice C block: USE_NEW through the reminted
  // entity key, KEEP_MINE (stamp without changing the value), KEEP_AS_CUSTOM (clear the stamp),
  // and REMOVE — positive where the domain's real-FK guard permits a genuinely orphaned row, plus
  // the refusal pins for every FK the entity model holds.

  private CommentaryTeam commentaryTeamFixture(String name, List<String> memberNpcNames) {
    return commentaryTeamRepositoryTest.findAll().stream()
        .filter(t -> name.equals(t.getName()))
        .findFirst()
        .orElseGet(
            () -> {
              CommentaryTeam t = new CommentaryTeam();
              t.setName(name);
              for (String npcName : memberNpcNames) {
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
                Commentator c = new Commentator();
                c.setNpc(npc);
                c.setStyle("Play-by-play");
                t.getCommentators().add(commentatorRepositoryTest.save(c));
              }
              return commentaryTeamRepositoryTest.save(t);
            });
  }

  @Test
  void commentaryTeamApplierAppliesUseNewThroughEntityKey() {
    // The membership apply path: USE_NEW resolves the decision's newline-joined member list back
    // to commentator rows through the service (the drifted display name never takes part) — the
    // new members must exist as commentator rows for the service to field them.
    var team = commentaryTeamFixture("Applier Crew", List.of("Old Member"));
    commentatorFixture("New Member One");
    commentatorFixture("New Member Two");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "commentary_team",
            CommentaryTeamSync.entityKeyFor("Applier Crew"),
            "Drifted Display Crew", // display name drifts — resolution must ignore it
            "members",
            "Old Member",
            "New Member One\nNew Member Two",
            "fp-team");
    service(commentaryTeamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = commentaryTeamRepositoryTest.findById(team.getId()).orElseThrow();
    assertEquals(
        List.of("New Member One", "New Member Two"),
        reloaded.getCommentators().stream().map(c -> c.getNpc().getName()).sorted().toList(),
        "USE_NEW must rebuild the membership from the decision's captured names");
    assertEquals("fp-team", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void commentaryTeamApplierKeepMineStampsWithoutChangingMembership() {
    var team = commentaryTeamFixture("Keep Mine Crew", List.of("Admin Member"));
    List<String> before =
        team.getCommentators().stream().map(c -> c.getNpc().getName()).sorted().toList();
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "commentary_team",
            CommentaryTeamSync.entityKeyFor("Keep Mine Crew"),
            "Keep Mine Crew",
            "members",
            "Admin Member",
            "seed member",
            "fp-keep");
    service(commentaryTeamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = commentaryTeamRepositoryTest.findById(team.getId()).orElseThrow();
    assertEquals(
        before,
        reloaded.getCommentators().stream().map(c -> c.getNpc().getName()).sorted().toList(),
        "KEEP_MINE must keep the admin's membership");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void commentaryTeamApplierKeepAsCustomClearsStamp() {
    var team = commentaryTeamFixture("Custom Crew", List.of("Custom Member"));
    team.setSeedFingerprint("fp-old");
    commentaryTeamRepositoryTest.save(team);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "commentary_team",
            CommentaryTeamSync.entityKeyFor("Custom Crew"),
            "Custom Crew",
            "members",
            "Custom Member",
            "seed member",
            "fp-new");
    service(commentaryTeamApplier)
        .apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = commentaryTeamRepositoryTest.findById(team.getId()).orElseThrow();
    assertEquals(
        List.of("Custom Member"),
        reloaded.getCommentators().stream().map(c -> c.getNpc().getName()).sorted().toList(),
        "KEEP_AS_CUSTOM must keep the admin's membership");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the team's stamp");
  }

  @Test
  void commentaryTeamApplierRemoveDeletesOrphanedRow() {
    // A team no show or template fields is genuinely removable.
    var orphan = commentaryTeamFixture("Orphaned Crew", List.of("Orphan Member"));
    orphan.setSeedFingerprint("fp-old");
    commentaryTeamRepositoryTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "commentary_team",
            CommentaryTeamSync.entityKeyFor("Orphaned Crew"),
            "Orphaned Crew",
            "fp");
    service(commentaryTeamApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        commentaryTeamRepositoryTest.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned commentary team");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void commentaryTeamApplierRemoveRefusedWhenShowBooked() {
    // A show fields the team: the pre-dispatch hasDependents check must refuse — deleting the
    // team would orphan the show's commentary_team FK.
    var referenced = commentaryTeamFixture("Booked Crew", List.of("Booked Member"));
    var showType = showTypeFixture("Crew Show Type");
    var show = new Show();
    show.setName("Crew Seed Applier Show");
    show.setDescription("a show");
    show.setType(showType);
    show.setCommentaryTeam(referenced);
    show.setCreationDate(Instant.now());
    showRepository.save(show);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "commentary_team", CommentaryTeamSync.entityKeyFor("Booked Crew"), "Booked Crew", "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(commentaryTeamApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a booked commentary team must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        commentaryTeamRepositoryTest.findById(referenced.getId()).isPresent(),
        "the booked commentary team must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  private CampaignUpgrade upgradeFixture(String name, String description, String type) {
    return campaignUpgradeRepositoryTest.findAll().stream()
        .filter(u -> name.equals(u.getName()))
        .findFirst()
        .orElseGet(
            () ->
                campaignUpgradeRepositoryTest.save(
                    CampaignUpgrade.builder()
                        .name(name)
                        .description(description)
                        .type(type)
                        .build()));
  }

  @Test
  void campaignUpgradeApplierAppliesUseNewThroughEntityKey() {
    var upgrade = upgradeFixture("Applier Upgrade", "old description", "STAMINA");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "campaign_upgrade",
            CampaignUpgradeSync.entityKeyFor("Applier Upgrade"),
            "Drifted Display Upgrade", // display name drifts — resolution must ignore it
            "description",
            "old description",
            "fresh seed description",
            "fp-upgrade");
    service(campaignUpgradeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = campaignUpgradeRepositoryTest.findById(upgrade.getId()).orElseThrow();
    assertEquals(
        "fresh seed description",
        reloaded.getDescription(),
        "USE_NEW must reach the row resolved through the reminted entity key");
    assertEquals("fp-upgrade", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void campaignUpgradeApplierKeepMineStampsWithoutChangingValue() {
    var upgrade = upgradeFixture("Keep Mine Upgrade", "admin's description", "HEALTH");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "campaign_upgrade",
            CampaignUpgradeSync.entityKeyFor("Keep Mine Upgrade"),
            "Keep Mine Upgrade",
            "description",
            "admin's description",
            "seed description",
            "fp-keep");
    service(campaignUpgradeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = campaignUpgradeRepositoryTest.findById(upgrade.getId()).orElseThrow();
    assertEquals(
        "admin's description", reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void campaignUpgradeApplierKeepAsCustomClearsStamp() {
    var upgrade = upgradeFixture("Custom Upgrade", "admin lore", "DEFENSE");
    upgrade.setSeedFingerprint("fp-old");
    campaignUpgradeRepositoryTest.save(upgrade);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "campaign_upgrade",
            CampaignUpgradeSync.entityKeyFor("Custom Upgrade"),
            "Custom Upgrade",
            "description",
            "admin lore",
            "seed description",
            "fp-new");
    service(campaignUpgradeApplier)
        .apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = campaignUpgradeRepositoryTest.findById(upgrade.getId()).orElseThrow();
    assertEquals(
        "admin lore", reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the upgrade's stamp");
  }

  @Test
  void campaignUpgradeApplierRemoveDeletesOrphanedRow() {
    // An upgrade no campaign state owns is genuinely removable.
    var orphan = upgradeFixture("Orphaned Upgrade", "unused", "DAMAGE");
    orphan.setSeedFingerprint("fp-old");
    campaignUpgradeRepositoryTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "campaign_upgrade",
            CampaignUpgradeSync.entityKeyFor("Orphaned Upgrade"),
            "Orphaned Upgrade",
            "fp");
    service(campaignUpgradeApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        campaignUpgradeRepositoryTest.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned campaign upgrade");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void campaignUpgradeApplierRemoveRefusedWhenCampaignStateOwnsIt() {
    // A campaign state holds the upgrade as owned: the pre-dispatch hasDependents check must
    // refuse — deleting the upgrade would orphan the campaign_state_upgrades join row.
    var referenced = upgradeFixture("Owned Upgrade", "used", "STAMINA");
    Wrestler player = wrestlerRepository.save(Wrestler.builder().name("Upgrade Player").build());
    Campaign campaign = Campaign.builder().wrestler(player).status(CampaignStatus.ACTIVE).build();
    campaign = campaignRepository.save(campaign);
    CampaignState state =
        CampaignState.builder()
            .campaign(campaign)
            .upgrades(new ArrayList<>(List.of(referenced))) // the FK the guard protects
            .build();
    campaignStateRepository.save(state);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "campaign_upgrade",
            CampaignUpgradeSync.entityKeyFor("Owned Upgrade"),
            "Owned Upgrade",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(campaignUpgradeApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of an owned upgrade must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        campaignUpgradeRepositoryTest.findById(referenced.getId()).isPresent(),
        "the owned upgrade must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  private OutcomeMatrix matrixFixture(String name) {
    return outcomeMatrixRepositoryTest
        .findByNameIgnoreCase(name)
        .orElseGet(
            () -> {
              OutcomeMatrix m = new OutcomeMatrix();
              m.setName(name);
              m.setDescription("original");
              m.setCategory(OutcomeMatrixCategory.MATCH_FLOW);
              return outcomeMatrixRepositoryTest.save(m);
            });
  }

  private OutcomeMatrixEntry matrixEntryFixture(OutcomeMatrix matrix, int roll, String text) {
    return outcomeMatrixEntryRepositoryTest
        .findByMatrixAndDiceRoll(matrix, roll)
        .orElseGet(
            () -> {
              OutcomeMatrixEntry e = new OutcomeMatrixEntry();
              e.setMatrix(matrix);
              e.setDiceRoll(roll);
              e.setTemplateText(text);
              return outcomeMatrixEntryRepositoryTest.save(e);
            });
  }

  @Test
  void outcomeMatrixApplierAppliesUseNewThroughEntityKey() {
    // Both row families resolve through their reminted keys: the matrix header by its name slug,
    // the entry by the owner slug plus the roll.
    var matrix = matrixFixture("Applier Matrix");
    matrix.setDescription("old description");
    outcomeMatrixRepositoryTest.save(matrix);
    var entry = matrixEntryFixture(matrix, 42, "old template");
    flushAndClear();

    SeedUpdateDecision matrixDecision =
        park(
            "outcome_matrix",
            OutcomeMatrixSync.matrixEntityKeyFor("Applier Matrix"),
            "Drifted Display Matrix", // display name drifts — resolution must ignore it
            "description",
            "old description",
            "fresh matrix description",
            "fp-matrix");
    SeedUpdateDecision entryDecision =
        park(
            "outcome_matrix",
            OutcomeMatrixSync.entryEntityKeyFor("Applier Matrix", 42),
            "Applier Matrix (roll 42)",
            "templateText",
            "old template",
            "fresh entry template",
            "fp-entry");

    service(outcomeMatrixApplier)
        .apply(
            Map.of(
                matrixDecision.getId(),
                SeedFieldDecision.USE_NEW,
                entryDecision.getId(),
                SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloadedMatrix = outcomeMatrixRepositoryTest.findById(matrix.getId()).orElseThrow();
    assertEquals(
        "fresh matrix description",
        reloadedMatrix.getDescription(),
        "USE_NEW must reach the matrix header through its reminted key");
    assertEquals("fp-matrix", reloadedMatrix.getSeedFingerprint(), "USE_NEW must stamp the row");
    var reloadedEntry = outcomeMatrixEntryRepositoryTest.findById(entry.getId()).orElseThrow();
    assertEquals(
        "fresh entry template",
        reloadedEntry.getTemplateText(),
        "USE_NEW must reach the entry row through its owner-slug+roll key");
    assertEquals("fp-entry", reloadedEntry.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void outcomeMatrixApplierKeepMineStampsWithoutChangingValue() {
    var matrix = matrixFixture("Keep Mine Matrix");
    String adminDescription = "admin's chart notes";
    matrix.setDescription(adminDescription);
    outcomeMatrixRepositoryTest.save(matrix);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "outcome_matrix",
            OutcomeMatrixSync.matrixEntityKeyFor("Keep Mine Matrix"),
            "Keep Mine Matrix",
            "description",
            adminDescription,
            "seed description",
            "fp-keep");
    service(outcomeMatrixApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = outcomeMatrixRepositoryTest.findById(matrix.getId()).orElseThrow();
    assertEquals(
        adminDescription, reloaded.getDescription(), "KEEP_MINE must keep the admin's value");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void outcomeMatrixApplierKeepAsCustomClearsStamp() {
    var matrix = matrixFixture("Custom Matrix");
    matrix.setDescription("admin notes");
    matrix.setSeedFingerprint("fp-old");
    outcomeMatrixRepositoryTest.save(matrix);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "outcome_matrix",
            OutcomeMatrixSync.matrixEntityKeyFor("Custom Matrix"),
            "Custom Matrix",
            "description",
            "admin notes",
            "seed description",
            "fp-new");
    service(outcomeMatrixApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = outcomeMatrixRepositoryTest.findById(matrix.getId()).orElseThrow();
    assertEquals(
        "admin notes", reloaded.getDescription(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the matrix's stamp");
  }

  @Test
  void outcomeMatrixApplierRemoveDeletesOrphanedEntry() {
    // An entry row is leaf content (no FK points at an entry) — genuinely removable.
    var matrix = matrixFixture("Removal Matrix");
    var orphan = matrixEntryFixture(matrix, 7, "unused template");
    orphan.setSeedFingerprint("fp-old");
    outcomeMatrixEntryRepositoryTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "outcome_matrix",
            OutcomeMatrixSync.entryEntityKeyFor("Removal Matrix", 7),
            "Removal Matrix (roll 7)",
            "fp");
    service(outcomeMatrixApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        outcomeMatrixEntryRepositoryTest.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the orphaned entry row");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void outcomeMatrixApplierRemoveDeletesUnredirectedMatrix() {
    // A matrix no entry redirects to is genuinely removable (its own entries are its content —
    // the cascade owns them, not a dependent).
    var orphan = matrixFixture("Orphaned Matrix");
    orphan.setSeedFingerprint("fp-old");
    outcomeMatrixRepositoryTest.save(orphan);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "outcome_matrix",
            OutcomeMatrixSync.matrixEntityKeyFor("Orphaned Matrix"),
            "Orphaned Matrix",
            "fp");
    service(outcomeMatrixApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        outcomeMatrixRepositoryTest.findById(orphan.getId()).isPresent(),
        "REMOVE must delete the unredirected matrix");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }

  @Test
  void outcomeMatrixApplierRemoveRefusedWhenRedirectTargetsIt() {
    // Another entry redirects to the matrix: the pre-dispatch hasDependents check must refuse —
    // deleting the target would orphan the redirect FK.
    var target = matrixFixture("Redirect Target Matrix");
    var source = matrixFixture("Redirect Source Matrix");
    var entry = matrixEntryFixture(source, 3, "redirect entry");
    entry.setRedirectToMatrix(target);
    outcomeMatrixEntryRepositoryTest.save(entry);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "outcome_matrix",
            OutcomeMatrixSync.matrixEntityKeyFor("Redirect Target Matrix"),
            "Redirect Target Matrix",
            "fp");
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                service(outcomeMatrixApplier)
                    .apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE)));
    assertTrue(
        exception.getMessage().contains("references it"),
        "removal of a redirect-target matrix must be refused by the pre-dispatch check");
    flushAndClear();
    assertTrue(
        outcomeMatrixRepositoryTest.findById(target.getId()).isPresent(),
        "the redirect target must survive the refused removal");
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus(),
        "the refused removal decision must stay pending");
  }

  @Test
  void outcomeMatrixApplierUseNewCoversCategoryAndEntryDeltaArms() {
    // One decision per uncovered arm, riding one batch: the matrix's category enum arm, the
    // entry's heatDelta/fanDelta/tvGradeDelta/grudgeGradeDelta arms (including null values that
    // clear the mechanical deltas), the injuryCaused boolean arm, and both redirect arms — a
    // resolvable target sets the reference and a blank value clears it (the redirect-to-nowhere
    // resolution arm).
    var matrix = matrixFixture("Delta Target Matrix");
    matrix.setCategory(OutcomeMatrixCategory.MATCH_FLOW);
    outcomeMatrixRepositoryTest.save(matrix);
    var target = matrixFixture("Redirect Resolution Matrix");
    var entry = matrixEntryFixture(matrix, 11, "old template");
    entry.setHeatDelta(1);
    entry.setFanDelta(1L);
    entry.setTvGradeDelta(1);
    entry.setGrudgeGradeDelta(1);
    entry.setInjuryCaused(false);
    entry.setRedirectToMatrix(target);
    outcomeMatrixEntryRepositoryTest.save(entry);
    var cleared = matrixEntryFixture(matrix, 12, "cleared template");
    cleared.setHeatDelta(null);
    outcomeMatrixEntryRepositoryTest.save(cleared);
    flushAndClear();

    String matrixKey = OutcomeMatrixSync.matrixEntityKeyFor("Delta Target Matrix");
    String entryKey = OutcomeMatrixSync.entryEntityKeyFor("Delta Target Matrix", 11);
    String clearedKey = OutcomeMatrixSync.entryEntityKeyFor("Delta Target Matrix", 12);
    SeedUpdateDecision category =
        park(
            "outcome_matrix",
            matrixKey,
            "Delta Target Matrix",
            "category",
            "MATCH_FLOW",
            "PROMO",
            "fp-cat");
    SeedUpdateDecision heat =
        park(
            "outcome_matrix",
            entryKey,
            "Delta Target Matrix (roll 11)",
            "heatDelta",
            "1",
            "9",
            "fp-h");
    SeedUpdateDecision fan =
        park(
            "outcome_matrix",
            entryKey,
            "Delta Target Matrix (roll 11)",
            "fanDelta",
            "1",
            null,
            "fp-f");
    SeedUpdateDecision tv =
        park(
            "outcome_matrix",
            entryKey,
            "Delta Target Matrix (roll 11)",
            "tvGradeDelta",
            "1",
            null,
            "fp-tv");
    SeedUpdateDecision grudge =
        park(
            "outcome_matrix",
            entryKey,
            "Delta Target Matrix (roll 11)",
            "grudgeGradeDelta",
            "1",
            null,
            "fp-g");
    SeedUpdateDecision injury =
        park(
            "outcome_matrix",
            entryKey,
            "Delta Target Matrix (roll 11)",
            "injuryCaused",
            "false",
            "true",
            "fp-i");
    SeedUpdateDecision redirect =
        park(
            "outcome_matrix",
            entryKey,
            "Delta Target Matrix (roll 11)",
            "redirectToMatrix",
            "Redirect Resolution Matrix",
            "Delta Target Matrix",
            "fp-r");
    SeedUpdateDecision redirectCleared =
        park(
            "outcome_matrix",
            clearedKey,
            "Delta Target Matrix (roll 12)",
            "redirectToMatrix",
            null,
            "   ",
            "fp-r0");

    service(outcomeMatrixApplier)
        .apply(
            Map.of(
                category.getId(), SeedFieldDecision.USE_NEW,
                heat.getId(), SeedFieldDecision.USE_NEW,
                fan.getId(), SeedFieldDecision.USE_NEW,
                tv.getId(), SeedFieldDecision.USE_NEW,
                grudge.getId(), SeedFieldDecision.USE_NEW,
                injury.getId(), SeedFieldDecision.USE_NEW,
                redirect.getId(), SeedFieldDecision.USE_NEW,
                redirectCleared.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloadedMatrix = outcomeMatrixRepositoryTest.findById(matrix.getId()).orElseThrow();
    assertEquals(
        OutcomeMatrixCategory.PROMO, reloadedMatrix.getCategory(), "category arm wrote the enum");
    var reloaded = outcomeMatrixEntryRepositoryTest.findById(entry.getId()).orElseThrow();
    assertEquals(9, reloaded.getHeatDelta(), "heatDelta arm wrote the value");
    assertNull(reloaded.getFanDelta(), "null fanDelta arm clears the value");
    assertNull(reloaded.getTvGradeDelta(), "null tvGradeDelta arm clears the value");
    assertNull(reloaded.getGrudgeGradeDelta(), "null grudgeGradeDelta arm clears the value");
    assertTrue(reloaded.isInjuryCaused(), "injuryCaused arm wrote the boolean");
    assertEquals(
        matrix,
        reloaded.getRedirectToMatrix(),
        "redirect arm resolved the target matrix by seed name");
    var clearedRow = outcomeMatrixEntryRepositoryTest.findById(cleared.getId()).orElseThrow();
    assertNull(
        clearedRow.getRedirectToMatrix(), "blank redirect value clears the reference (null arm)");
  }

  @Test
  void outcomeMatrixApplierEntryNotFoundAndUnresolvableRedirectWarn() {
    // The entry-family warn arms: USE_NEW/KEEP/KEEP_AS_CUSTOM/REMOVE on an entry key that
    // matches no row (the not-found lambdas), plus a redirect decision whose target name
    // resolves to nothing (the redirect-not-changed warn arm — the reference is kept).
    var matrix = matrixFixture("Warn Target Matrix");
    var entry = matrixEntryFixture(matrix, 21, "keep me");
    entry.setRedirectToMatrix(matrix);
    outcomeMatrixEntryRepositoryTest.save(entry);
    flushAndClear();

    String entryKey = OutcomeMatrixSync.entryEntityKeyFor("Warn Target Matrix", 21);
    String ghostKey = OutcomeMatrixSync.entryEntityKeyFor("Warn Target Matrix", 99);
    SeedUpdateDecision redirectGhost =
        park(
            "outcome_matrix",
            entryKey,
            "Warn Target Matrix (roll 21)",
            "redirectToMatrix",
            "Warn Target Matrix",
            "Ghost Matrix Nowhere",
            "fp-rg");
    SeedUpdateDecision ghostNew =
        park(
            "outcome_matrix",
            ghostKey,
            "Warn Target Matrix (roll 99)",
            "templateText",
            "a",
            "b",
            "fp-gn");
    SeedUpdateDecision ghostKeep =
        park(
            "outcome_matrix",
            ghostKey,
            "Warn Target Matrix (roll 99)",
            "templateText",
            "a",
            "b",
            "fp-gk");
    SeedUpdateDecision ghostCustom =
        park(
            "outcome_matrix",
            ghostKey,
            "Warn Target Matrix (roll 99)",
            "templateText",
            "a",
            "b",
            "fp-gc");
    SeedUpdateDecision ghostRemove =
        parkRemoval("outcome_matrix", ghostKey, "Warn Target Matrix (roll 99)", "fp-gr");

    Map<Long, SeedFieldDecision> batch = new HashMap<>();
    batch.put(redirectGhost.getId(), SeedFieldDecision.USE_NEW);
    batch.put(ghostNew.getId(), SeedFieldDecision.USE_NEW);
    batch.put(ghostKeep.getId(), SeedFieldDecision.KEEP_MINE);
    batch.put(ghostCustom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM);
    batch.put(ghostRemove.getId(), SeedFieldDecision.REMOVE);
    service(outcomeMatrixApplier).apply(batch);

    flushAndClear();
    var reloaded = outcomeMatrixEntryRepositoryTest.findById(entry.getId()).orElseThrow();
    assertEquals(
        matrix,
        reloaded.getRedirectToMatrix(),
        "an unresolvable redirect target keeps the current reference (warn arm)");
    assertTrue(
        outcomeMatrixEntryRepositoryTest.findById(entry.getId()).isPresent(),
        "ghost-key decisions never touch the real entry");
  }

  @Test
  void outcomeMatrixApplierMatrixFieldValueDefaultsFailLoud() {
    // The matrix writer's default arm: an unknown matrix-header field fails loud.
    var matrix = matrixFixture("Loud Matrix");
    flushAndClear();
    SeedUpdateDecision unknown =
        park(
            "outcome_matrix",
            OutcomeMatrixSync.matrixEntityKeyFor("Loud Matrix"),
            "Loud Matrix",
            "entries",
            "x",
            "y",
            "fp-u");
    assertThrows(
        IllegalStateException.class,
        () ->
            service(outcomeMatrixApplier)
                .apply(Map.of(unknown.getId(), SeedFieldDecision.USE_NEW)));
  }

  @Test
  void outcomeMatrixApplierApplyKeepOnEntryRow() {
    // KEEP_MINE on an entry row: the second branch of the keep resolver (matrix miss → entry).
    var matrix = matrixFixture("Entry Keep Matrix");
    var entry = matrixEntryFixture(matrix, 5, "admin's template");
    flushAndClear();
    SeedUpdateDecision keep =
        park(
            "outcome_matrix",
            OutcomeMatrixSync.entryEntityKeyFor("Entry Keep Matrix", 5),
            "Entry Keep Matrix (roll 5)",
            "templateText",
            "admin's template",
            "seed template",
            "fp-k");
    service(outcomeMatrixApplier).apply(Map.of(keep.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = outcomeMatrixEntryRepositoryTest.findById(entry.getId()).orElseThrow();
    assertEquals("admin's template", reloaded.getTemplateText(), "KEEP_MINE keeps the value");
    assertEquals("fp-k", reloaded.getSeedFingerprint(), "KEEP_MINE stamps the entry row");
  }

  @Test
  void outcomeMatrixApplierKeepAsCustomOnEntryRow() {
    // KEEP_AS_CUSTOM on an entry row: the second branch of the custom resolver.
    var matrix = matrixFixture("Entry Custom Matrix");
    var entry = matrixEntryFixture(matrix, 6, "admin's template");
    entry.setSeedFingerprint("fp-old");
    outcomeMatrixEntryRepositoryTest.save(entry);
    flushAndClear();
    SeedUpdateDecision custom =
        park(
            "outcome_matrix",
            OutcomeMatrixSync.entryEntityKeyFor("Entry Custom Matrix", 6),
            "Entry Custom Matrix (roll 6)",
            "templateText",
            "admin's template",
            "seed template",
            "fp-c");
    service(outcomeMatrixApplier).apply(Map.of(custom.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = outcomeMatrixEntryRepositoryTest.findById(entry.getId()).orElseThrow();
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM clears the entry's stamp");
  }

  private WrestlerRelationship relationshipFixture(
      Wrestler w1, Wrestler w2, RelationshipType type, int level, String notes) {
    return wrestlerRelationshipRepositoryTest.findAllWithWrestlers().stream()
        .filter(
            r ->
                r.getType() == type
                    && ((r.getWrestler1().equals(w1) && r.getWrestler2().equals(w2))
                        || (r.getWrestler1().equals(w2) && r.getWrestler2().equals(w1))))
        .findFirst()
        .orElseGet(
            () -> {
              WrestlerRelationship r =
                  WrestlerRelationship.builder()
                      .wrestler1(w1)
                      .wrestler2(w2)
                      .type(type)
                      .level(level)
                      .isStoryline(false)
                      .build();
              r.setNotes(notes);
              return wrestlerRelationshipRepositoryTest.save(r);
            });
  }

  @Test
  void relationshipApplierAppliesUseNewThroughEntityKey() {
    Wrestler w1 = wrestlerRepository.save(Wrestler.builder().name("Applier Partner").build());
    Wrestler w2 = wrestlerRepository.save(Wrestler.builder().name("Applier Sweetheart").build());
    var relationship = relationshipFixture(w1, w2, RelationshipType.SPOUSE, 50, "old note");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "wrestler_relationship",
            RelationshipSync.entityKeyFor(
                "Applier Partner", "Applier Sweetheart", RelationshipType.SPOUSE),
            "Drifted Display <SPOUSE> Pair", // display name drifts — resolution must ignore it
            "notes",
            "old note",
            "fresh seed note",
            "fp-rel");
    service(relationshipApplier).apply(Map.of(decision.getId(), SeedFieldDecision.USE_NEW));

    flushAndClear();
    var reloaded = wrestlerRelationshipRepositoryTest.findById(relationship.getId()).orElseThrow();
    assertEquals(
        "fresh seed note",
        reloaded.getNotes(),
        "USE_NEW must reach the row resolved through the reminted entity key");
    assertEquals("fp-rel", reloaded.getSeedFingerprint(), "USE_NEW must stamp the row");
  }

  @Test
  void relationshipApplierKeepMineStampsWithoutChangingValue() {
    Wrestler w1 = wrestlerRepository.save(Wrestler.builder().name("Keep Partner").build());
    Wrestler w2 = wrestlerRepository.save(Wrestler.builder().name("Keep Sweetheart").build());
    var relationship = relationshipFixture(w1, w2, RelationshipType.BEST_FRIEND, 77, "admin note");
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "wrestler_relationship",
            RelationshipSync.entityKeyFor(
                "Keep Partner", "Keep Sweetheart", RelationshipType.BEST_FRIEND),
            "Keep Partner <BEST_FRIEND> Keep Sweetheart",
            "level",
            "77",
            "50",
            "fp-keep");
    service(relationshipApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_MINE));

    flushAndClear();
    var reloaded = wrestlerRelationshipRepositoryTest.findById(relationship.getId()).orElseThrow();
    assertEquals(77, reloaded.getLevel(), "KEEP_MINE must keep the admin's level");
    assertEquals("fp-keep", reloaded.getSeedFingerprint(), "KEEP_MINE must stamp the row");
  }

  @Test
  void relationshipApplierKeepAsCustomClearsStamp() {
    Wrestler w1 = wrestlerRepository.save(Wrestler.builder().name("Custom Partner").build());
    Wrestler w2 = wrestlerRepository.save(Wrestler.builder().name("Custom Sweetheart").build());
    var relationship = relationshipFixture(w1, w2, RelationshipType.MENTOR, 40, "admin note");
    relationship.setSeedFingerprint("fp-old");
    wrestlerRelationshipRepositoryTest.save(relationship);
    flushAndClear();

    SeedUpdateDecision decision =
        park(
            "wrestler_relationship",
            RelationshipSync.entityKeyFor(
                "Custom Partner", "Custom Sweetheart", RelationshipType.MENTOR),
            "Custom Partner <MENTOR> Custom Sweetheart",
            "notes",
            "admin note",
            "seed note",
            "fp-new");
    service(relationshipApplier).apply(Map.of(decision.getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    flushAndClear();
    var reloaded = wrestlerRelationshipRepositoryTest.findById(relationship.getId()).orElseThrow();
    assertEquals("admin note", reloaded.getNotes(), "KEEP_AS_CUSTOM must keep the admin's value");
    assertNull(reloaded.getSeedFingerprint(), "KEEP_AS_CUSTOM must clear the relationship's stamp");
  }

  @Test
  void relationshipApplierRemoveDeletesTheRow() {
    // A relationship is its own gameplay edge — no FK points at it (verified against the entity
    // model: Wrestler's inverse sides map the same rows, they do not reference them
    // independently). The removal deletes the edge.
    Wrestler w1 = wrestlerRepository.save(Wrestler.builder().name("Doomed Partner").build());
    Wrestler w2 = wrestlerRepository.save(Wrestler.builder().name("Doomed Sweetheart").build());
    var doomed = relationshipFixture(w1, w2, RelationshipType.ROMANCE, 60, "unused note");
    doomed.setSeedFingerprint("fp-old");
    wrestlerRelationshipRepositoryTest.save(doomed);
    flushAndClear();

    SeedUpdateDecision decision =
        parkRemoval(
            "wrestler_relationship",
            RelationshipSync.entityKeyFor(
                "Doomed Partner", "Doomed Sweetheart", RelationshipType.ROMANCE),
            "Doomed Partner <ROMANCE> Doomed Sweetheart",
            "fp");
    service(relationshipApplier).apply(Map.of(decision.getId(), SeedFieldDecision.REMOVE));

    flushAndClear();
    assertFalse(
        wrestlerRelationshipRepositoryTest.findById(doomed.getId()).isPresent(),
        "REMOVE must delete the relationship edge");
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(decision.getId()).orElseThrow().getStatus());
  }
}
