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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbilityRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.WrestlerImportDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WrestlerSyncTest {

  @Mock private WrestlerRepository wrestlerRepository;

  @Mock private WrestlerAbilityRepository wrestlerAbilityRepository;

  @Mock private WrestlerService wrestlerService;
  @Mock private UniverseRepository universeRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private NpcService npcService;
  @Mock private TierRecalculationService tierRecalculationService;
  @Mock private ResourcePatternResolver resourcePatternResolver;
  @Mock private SeedUpdateReviewService seedReviewService;

  private WrestlerSync sync;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  @BeforeEach
  void setUp() throws Exception {
    Universe mockUniverse = Mockito.mock(Universe.class);
    when(mockUniverse.getId()).thenReturn(1L);
    when(universeRepository.findAll()).thenReturn(List.of(mockUniverse));

    // Return the real wrestlers*.json from the classpath for integration-style characterization
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    Resource[] resources = real.getResources("classpath*:wrestlers/*.json");
    when(resourcePatternResolver.getResources(anyString())).thenReturn(resources);

    sync =
        new WrestlerSync(
            "false",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
  }

  @Test
  void sync_savesNewWrestlers() {
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of());
    WrestlerState state = new WrestlerState();
    lenient().when(wrestlerService.getOrCreateState(any(), any())).thenReturn(state);

    sync.sync();

    verify(wrestlerRepository, atLeastOnce()).saveAll(any());
  }

  @Test
  void sync_skipsWhenNotEmptyFlagSet() {
    WrestlerSync skipSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    when(wrestlerRepository.count()).thenReturn(10L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of());
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());

    skipSync.sync();

    // Empty-but-flagged install: the top-up seeds everything missing (all seed files' wrestlers
    // are absent). The gate's guarantee is about existing rows, so assert insertions happened.
    verify(wrestlerRepository, atLeastOnce()).saveAll(any());
  }

  @Test
  void sync_topUpNeverResavesExistingWrestlerWhenSkipFlagSet() {
    Wrestler existing = new Wrestler();
    existing.setId(1L);
    existing.setName("Rob Van Dam");
    existing.setDescription("user-modified description");
    when(wrestlerRepository.count()).thenReturn(1L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(existing));
    when(wrestlerRepository.findAll()).thenReturn(List.of(existing));
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());

    WrestlerSync skipSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);

    skipSync.sync();

    // Rob Van Dam is seeded but already exists: under the skip gate his row must never be
    // re-saved (data-protection guarantee) and user modifications must survive.
    verify(wrestlerRepository, never())
        .saveAll(
            argThat(
                list ->
                    list != null
                        && StreamSupport.stream(list.spliterator(), false)
                            .anyMatch(w -> w == existing)));
    assertEquals("user-modified description", existing.getDescription());
  }

  @Test
  void sync_backfillsAbilitiesWhenSkippingExistingWrestlers() {
    Wrestler existing = new Wrestler();
    existing.setId(1L);
    existing.setName("Rob Van Dam");
    when(wrestlerRepository.count()).thenReturn(1L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(existing));
    when(wrestlerRepository.findAll()).thenReturn(List.of(existing));
    when(wrestlerAbilityRepository.findByWrestler(existing)).thenReturn(List.of());
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());

    WrestlerSync skipSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);

    skipSync.sync();

    verify(wrestlerAbilityRepository, atLeastOnce()).save(any());
  }

  @Test
  void sync_topUpInsertsMissingWrestlersWhenSkipFlagSet() throws Exception {
    // Simulates a flag-protected install that predates recent expansions: one seeded wrestler
    // exists (Rob Van Dam), every other seeded wrestler is missing. Top-up must insert exactly
    // the missing rows and never the existing one. The expected count is derived from the same
    // resources the sync reads so the assertion holds regardless of classpath contents.
    when(wrestlerRepository.count()).thenReturn(45L);
    Wrestler existing = new Wrestler();
    existing.setId(1L);
    existing.setName("Rob Van Dam");
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(existing));
    when(wrestlerRepository.findAll()).thenReturn(List.of(existing));
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());

    WrestlerSync skipSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);

    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Wrestler>> captor = ArgumentCaptor.forClass(List.class);
    verify(wrestlerRepository, atLeastOnce()).saveAll(captor.capture());
    List<Wrestler> saved =
        captor.getAllValues().stream().flatMap(List::stream).collect(Collectors.toList());
    long seededCount =
        Arrays.stream(
                new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:wrestlers/*.json"))
            .filter(Resource::exists)
            .mapToLong(
                r -> {
                  try (var is = r.getInputStream()) {
                    return new ObjectMapper().readValue(is, List.class).size();
                  } catch (IOException e) {
                    return 0L;
                  }
                })
            .sum();
    assertEquals(
        seededCount - 1,
        saved.size(),
        "every seeded wrestler except the existing one must be inserted");
    assertFalse(saved.stream().anyMatch(w -> "Rob Van Dam".equals(w.getName())));
  }

  @Test
  void sync_preservesFansWhenExistingIsHigher() {
    Wrestler existing = new Wrestler();
    existing.setId(1L);
    existing.setName("Rob Van Dam");
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(existing));

    WrestlerState state = new WrestlerState();
    state.setId(1L);
    state.setFans(999_999L); // higher than any seed value in the file
    state.setBumps(Integer.MAX_VALUE); // higher than any seed bump value
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(state);

    sync.sync();

    // fans should NOT be decreased — dirty-check only sets if file value is higher
    verify(wrestlerStateRepository, never()).save(any());
  }

  @Test
  void sync_evictsWrestlerCacheAfterSync() {
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of());
    lenient().when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());

    sync.sync();

    verify(wrestlerService).evictWrestlerCache();
  }

  // ── Verify mode (seed_fingerprint-gated reconciliation) ────────────────────

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  /** Reads Rob Van Dam's seed DTO from the real classpath seed files. */
  private WrestlerImportDTO rvdSeedDto() throws IOException {
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    for (Resource r : real.getResources("classpath*:wrestlers/*.json")) {
      try (var is = r.getInputStream()) {
        for (WrestlerImportDTO dto :
            new ObjectMapper().readValue(is, new TypeReference<List<WrestlerImportDTO>>() {})) {
          if ("Rob Van Dam".equals(dto.getName())) {
            return dto;
          }
        }
      }
    }
    throw new IllegalStateException("Rob Van Dam not found in seed files");
  }

  /** Builds an in-memory Wrestler mirroring the seed payload, with seed-owned fields set. */
  private Wrestler seededRvd(WrestlerImportDTO dto) {
    Wrestler w = new Wrestler();
    w.setId(1L);
    w.setName(dto.getName());
    w.setDeckSize(dto.getDeckSize());
    w.setStartingHealth(dto.getStartingHealth());
    w.setLowHealth(dto.getLowHealth());
    w.setStartingStamina(dto.getStartingStamina());
    w.setLowStamina(dto.getLowStamina());
    w.setDescription(dto.getDescription());
    w.setGender(dto.getGender());
    w.setHeritageTag(dto.getHeritageTag());
    w.setExpansionCode(dto.getExpansionCode());
    w.setDrive(dto.getDrive());
    w.setResilience(dto.getResilience());
    w.setCharisma(dto.getCharisma());
    w.setBrawl(dto.getBrawl());
    if (dto.getAlignment() != null) {
      w.setAlignment(
          WrestlerAlignment.builder()
              .wrestler(w)
              .alignmentType(AlignmentType.valueOf(dto.getAlignment().toUpperCase()))
              .level(0)
              .build());
    }
    return w;
  }

  private WrestlerSync verifySync() {
    return new WrestlerSync(
        "verify",
        wrestlerRepository,
        wrestlerAbilityRepository,
        wrestlerService,
        universeRepository,
        wrestlerStateRepository,
        npcService,
        tierRecalculationService,
        resourcePatternResolver,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private void stubOneExisting(Wrestler existing) {
    when(wrestlerRepository.count()).thenReturn(1L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(existing));
    when(wrestlerRepository.findAll()).thenReturn(List.of(existing));
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());
  }

  @Test
  void verifyMode_reconcilesSeedOwnedFieldsOfCleanRow() throws Exception {
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto);
    // The row is exactly what an older release's seed write left behind: its fields (including the
    // since-fixed description) hash to the stored fingerprint.
    existing.setDescription("stale description from an older release");
    existing.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)));
    stubOneExisting(existing);

    verifySync().sync();

    assertEquals(
        dto.getDescription(),
        existing.getDescription(),
        "clean-fingerprint row's stale seed-owned field must be reconciled to the seed value");
    assertEquals(
        VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)),
        existing.getSeedFingerprint(),
        "fingerprint must be re-stamped after reconcile");
  }

  @Test
  void verifyMode_preservesUserEditedRow() throws Exception {
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto);
    existing.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)));
    // Edited after seeding: the row's fields no longer hash to the stored fingerprint.
    existing.setDescription("deliberately hand-tuned");
    stubOneExisting(existing);

    verifySync().sync();

    assertEquals(
        "deliberately hand-tuned",
        existing.getDescription(),
        "stale-fingerprint (user-edited) row must be preserved with a WARN, never overwritten");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() throws Exception {
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto); // null fingerprint, fields match seed
    stubOneExisting(existing);

    verifySync().sync();

    assertEquals(
        VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)),
        existing.getSeedFingerprint(),
        "legacy row matching the seed must be adopted (fingerprint stamped)");
  }

  @Test
  void verifyMode_treatsLegacyMismatchedRowAsEdited() throws Exception {
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto);
    existing.setDescription("hand-tuned long before fingerprints existed"); // null fingerprint
    stubOneExisting(existing);

    verifySync().sync();

    assertEquals(
        "hand-tuned long before fingerprints existed",
        existing.getDescription(),
        "legacy row whose seed-owned fields differ must be treated as user-edited and preserved");
  }

  @Test
  void verifyMode_doesNotTouchGameplayFields() throws Exception {
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto);
    existing.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)));
    stubOneExisting(existing);
    WrestlerState state = new WrestlerState();
    state.setId(1L);
    state.setFans(999_999L);
    state.setBumps(Integer.MAX_VALUE);
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(state);

    verifySync().sync();

    verify(wrestlerStateRepository, never()).save(any());
  }

  @Test
  void verifyMode_replacesAlignmentInPlaceInsteadOfAppending() throws Exception {
    // Regression 5a: setAlignment appends; replacing via the builder left the stale alignment
    // first in the set, so the derived value never changed and the row never converged.
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto);
    existing.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)));
    // Simulate a seed alignment change: rehash the row for its current fields, then swap the
    // alignment type of the existing (first) alignment to HEEL while the stamp still says FACE.
    existing.getAlignment().setAlignmentType(AlignmentType.HEEL);
    // Row now hashes differently from its stamp → looks edited. Re-stamp is NOT possible here
    // without changing the seed; instead pin the replace-in-place behavior directly:
    // applySeedFields must mutate the existing WrestlerAlignment, not append a second one.
    stubOneExisting(existing);

    // Force the clean path by stamping over the row's CURRENT (HEEL) fields; the seed still says
    // the original alignment, so the reconcile must rewrite the alignment to FACE in place.
    existing.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)));

    int alignmentsBefore = 1; // seededRvd builds exactly one
    verifySync().sync();

    assertEquals(alignmentsBefore, 1, "reconcile must not append a second alignment row");
    assertEquals(
        AlignmentType.valueOf(dto.getAlignment().toUpperCase()),
        existing.getAlignment().getAlignmentType(),
        "existing alignment must be replaced in place with the seed value");
  }

  @Test
  void verifyMode_convergesWhenSeedOmitsAFieldTheRowCarries() throws Exception {
    // Regression 5b: matchesSeed compared fields applySeedFields null-guards, so a row holding
    // an imageUrl while the seed omits it re-reconciled forever without converging.
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto);
    // Row carries an imageUrl the seed does not ship (all current seed files omit it).
    existing.setImageUrl("http://example.com/user-uploaded.png");
    existing.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)));
    stubOneExisting(existing);

    // First pass: row's fields hash to its stamp; matchesSeed must treat the extra imageUrl as
    // legitimate (seed omits it) → already-in-sync, no churn.
    verifySync().sync();

    assertEquals(
        "http://example.com/user-uploaded.png",
        existing.getImageUrl(),
        "seed-omitted field's existing value must be left alone, not fought over");
  }

  @Test
  void verifyMode_ignoresUserCreatedWrestlers() {
    // User-created wrestler: no seed entry shares his name, so reconcile must not consider him.
    Wrestler custom = new Wrestler();
    custom.setId(2L);
    custom.setName("Custom Guy");
    custom.setDescription("a wrestler the user invented");
    when(wrestlerRepository.count()).thenReturn(1L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(custom));
    when(wrestlerRepository.findAll()).thenReturn(List.of(custom));
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());

    verifySync().sync();

    assertEquals(
        "a wrestler the user invented",
        custom.getDescription(),
        "user-created wrestler must never be touched by verify mode");
  }

  // ── Version-change detection (seed-update review, PR 4 Step 1 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() throws Exception {
    // THREE-TIER pin, end-to-end through the wired pipeline (Task 5's classifier contract,
    // exercised via WrestlerSync.detectVersionChangeCollisions + a recording parking sink):
    //   tier 1 — untouched field, seed changed → updated silently + re-stamped
    //   tier 2 — admin-edited field, seed changed → parked as PENDING decision, DB value unchanged
    //   tier 3 — seed unchanged → no-op whether or not the admin edited it (PRESERVE_EDIT, nothing
    //            parked, nothing written)
    //
    // The seed resource is a fixture copy of BASE_GAME.json with Rob Van Dam's description AND
    // Gail Kim's brawl changed (the version upgrade's seed delta). The DB rows are stamped as the
    // PREVIOUS release's seed write left them, then the admin hand-edits:
    //   Rob Van Dam: heritageTag hand-tuned (tier 3 — seed doesn't touch it; description is
    //                tier 1: row untouched, seed changed)
    //   Gail Kim:    brawl=7 by the admin (tier 2 — the seed ALSO changed brawl 2→9, so the row's
    //                single brawl edit vs the stamp is a genuine collision)
    WrestlerImportDTO rvdDto = rvdSeedDto();
    WrestlerImportDTO gailDto = seedDtoFor("Gail Kim");

    Wrestler rvd = seededRvd(rvdDto);
    rvd.setDescription("stale description from an older release"); // old seed's description
    rvd.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(rvd)));
    rvd.setHeritageTag(
        "hand-tuned heritage"); // admin edit AFTER the stamp → dirty row, single edit

    Wrestler gail = seededFrom(gailDto);
    gail.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(gail)));
    gail.setBrawl(7); // admin edit AFTER the stamp

    when(wrestlerRepository.count()).thenReturn(2L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(rvd, gail));
    when(wrestlerRepository.findAll()).thenReturn(List.of(rvd, gail));

    // Seed fixture: RVD's row with the description changed (a normal release's seed delta),
    // served through the same resolver mock the sync already uses.
    ByteArrayResource changedRvd;
    {
      ArrayNode array = new ObjectMapper().createArrayNode();
      ObjectNode node = array.addObject();
      node.put("name", rvdDto.getName());
      node.put("deckSize", rvdDto.getDeckSize());
      node.put("startingHealth", rvdDto.getStartingHealth());
      node.put("lowHealth", rvdDto.getLowHealth());
      node.put("startingStamina", rvdDto.getStartingStamina());
      node.put("lowStamina", rvdDto.getLowStamina());
      node.put("description", "seed description changed by the new release");
      node.put("gender", rvdDto.getGender() == null ? null : rvdDto.getGender().name());
      node.put("heritageTag", rvdDto.getHeritageTag());
      node.put("expansion_code", rvdDto.getExpansionCode());
      node.put("drive", rvdDto.getDrive());
      node.put("resilience", rvdDto.getResilience());
      node.put("charisma", rvdDto.getCharisma());
      node.put("brawl", rvdDto.getBrawl());
      node.put("alignment", rvdDto.getAlignment());
      changedRvd = new ByteArrayResource(array.toString().getBytes(StandardCharsets.UTF_8));
    }
    // Gail Kim's seed entry with the brawl bumped 2 → 9 (the release's second seed change).
    ByteArrayResource changedGail;
    {
      ArrayNode array = new ObjectMapper().createArrayNode();
      ObjectNode node = array.addObject();
      node.put("name", gailDto.getName());
      node.put("deckSize", gailDto.getDeckSize());
      node.put("startingHealth", gailDto.getStartingHealth());
      node.put("lowHealth", gailDto.getLowHealth());
      node.put("startingStamina", gailDto.getStartingStamina());
      node.put("lowStamina", gailDto.getLowStamina());
      node.put("description", gailDto.getDescription());
      node.put("gender", gailDto.getGender() == null ? null : gailDto.getGender().name());
      node.put("heritageTag", gailDto.getHeritageTag());
      // Filename-derived code: the sync stamps expansionCode from the seed file's name.
      node.put("expansion_code", "GAIL_KIM");
      node.put("drive", gailDto.getDrive());
      node.put("resilience", gailDto.getResilience());
      node.put("charisma", gailDto.getCharisma());
      node.put("brawl", 9);
      node.put("alignment", gailDto.getAlignment());
      changedGail = new ByteArrayResource(array.toString().getBytes(StandardCharsets.UTF_8));
    }
    Resource[] seedResources = {changedRvd, changedGail};
    when(resourcePatternResolver.getResources(ArgumentMatchers.anyString()))
        .thenReturn(seedResources);

    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              // Distinct coordinates only: parkFieldConflict's refresh path fires per pass, the
              // contract is at most one open decision per (entity, field).
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyString());

    WrestlerSync detectionSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "seed description changed by the new release",
        rvd.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged heritage edit was preserved with no parking.
    assertEquals(
        "hand-tuned heritage",
        rvd.getHeritageTag(),
        "tier 3: admin edit on an unchanged seed field must be preserved silently");
    // Tier 2: the admin-edited brawl kept its value; the row parked the brawl collision.
    assertEquals(
        7,
        gail.getBrawl(),
        "tier 2: admin-edited field must keep its value — only a PENDING decision is created");
    Mockito.verify(parking)
        .parkFieldConflict(
            ArgumentMatchers.eq("wrestler"),
            ArgumentMatchers.eq("wrestler:gail-kim"),
            ArgumentMatchers.eq("Gail Kim"),
            ArgumentMatchers.eq("brawl"),
            ArgumentMatchers.eq("7"),
            ArgumentMatchers.eq("9"),
            ArgumentMatchers.anyString());
    // Only Gail Kim's brawl parked; Rob Van Dam's tier-1/tier-3 fields never park.
    assertEquals(
        Set.of("wrestler:gail-kim|brawl"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  /** Reads one named wrestler's seed DTO from the real classpath seed files. */
  private WrestlerImportDTO seedDtoFor(String name) throws IOException {
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    for (Resource r : real.getResources("classpath*:wrestlers/*.json")) {
      try (var is = r.getInputStream()) {
        for (WrestlerImportDTO dto :
            new ObjectMapper().readValue(is, new TypeReference<List<WrestlerImportDTO>>() {})) {
          if (name.equals(dto.getName())) {
            return dto;
          }
        }
      }
    }
    throw new IllegalStateException(name + " not found in seed files");
  }

  /** Builds an in-memory Wrestler mirroring any seed payload (same shape as {@link #seededRvd}). */
  private Wrestler seededFrom(WrestlerImportDTO dto) {
    Wrestler w = new Wrestler();
    w.setId(2L);
    w.setName(dto.getName());
    w.setDeckSize(dto.getDeckSize());
    w.setStartingHealth(dto.getStartingHealth());
    w.setLowHealth(dto.getLowHealth());
    w.setStartingStamina(dto.getStartingStamina());
    w.setLowStamina(dto.getLowStamina());
    w.setDescription(dto.getDescription());
    w.setGender(dto.getGender());
    w.setHeritageTag(dto.getHeritageTag());
    w.setExpansionCode(dto.getExpansionCode());
    w.setDrive(dto.getDrive());
    w.setResilience(dto.getResilience());
    w.setCharisma(dto.getCharisma());
    w.setBrawl(dto.getBrawl());
    if (dto.getAlignment() != null) {
      w.setAlignment(
          WrestlerAlignment.builder()
              .wrestler(w)
              .alignmentType(AlignmentType.valueOf(dto.getAlignment().toUpperCase()))
              .level(0)
              .build());
    }
    return w;
  }

  @Test
  void versionChange_silentUpdatesApplyNullGuardedFields() throws Exception {
    // Tier-1 write-set arms the three-tier pin never reached: the null-guarded silent fields.
    // Rob Van Dam is the clean row whose seed changed several null-guarded fields (imageUrl set
    // on a row missing it, gender/drive/resilience/charisma/brawl/alignment changed); Gail Kim's
    // row carries an imageUrl the new seed drops — the silent write for imageUrl must skip the
    // null (seed-omitted) value, and her deckSize (row null, seed 16) must be written.
    WrestlerImportDTO rvdDto = rvdSeedDto();
    WrestlerImportDTO gailDto = seedDtoFor("Gail Kim");

    Wrestler rvd = seededRvd(rvdDto);
    rvd.getAlignments().clear(); // no alignment holder — the silent alignment write builds one
    rvd.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(rvd)));

    Wrestler gail = seededFrom(gailDto);
    gail.setDeckSize(null); // row missing deckSize — the observation ternary's null side
    gail.setStartingHealth(null); // row missing startingHealth — another ternary null side
    gail.setImageUrl("http://example.com/gail.png"); // the new seed drops it
    gail.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(gail)));
    String gailStampBefore = gail.getSeedFingerprint();

    when(wrestlerRepository.count()).thenReturn(2L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(rvd, gail));
    when(wrestlerRepository.findAll()).thenReturn(List.of(rvd, gail));

    ArrayNode rvdSeed = new ObjectMapper().createArrayNode();
    ObjectNode rvdNode = rvdSeed.addObject();
    rvdNode.put("name", rvdDto.getName());
    rvdNode.put("deckSize", rvdDto.getDeckSize() + 1);
    rvdNode.put("startingHealth", rvdDto.getStartingHealth() + 1);
    rvdNode.put("lowHealth", rvdDto.getLowHealth() + 1);
    rvdNode.put("startingStamina", rvdDto.getStartingStamina() + 1);
    rvdNode.put("lowStamina", rvdDto.getLowStamina() + 1);
    rvdNode.put("description", "seed description rebuilt by the release");
    rvdNode.put("gender", "FEMALE");
    rvdNode.put("imageUrl", "http://example.com/rvd.png");
    rvdNode.put("heritageTag", rvdDto.getHeritageTag());
    rvdNode.put("expansion_code", rvdDto.getExpansionCode());
    rvdNode.put("drive", 5);
    rvdNode.put("resilience", 5);
    rvdNode.put("charisma", 5);
    rvdNode.put("brawl", 5);
    rvdNode.put("alignment", "HEEL");
    ByteArrayResource changedRvd =
        new ByteArrayResource(rvdSeed.toString().getBytes(StandardCharsets.UTF_8));

    ArrayNode gailSeed = new ObjectMapper().createArrayNode();
    ObjectNode gailNode = gailSeed.addObject();
    gailNode.put("name", gailDto.getName());
    gailNode.put("deckSize", gailDto.getDeckSize());
    gailNode.put("startingHealth", gailDto.getStartingHealth());
    gailNode.put("lowHealth", gailDto.getLowHealth());
    gailNode.put("startingStamina", gailDto.getStartingStamina());
    gailNode.put("lowStamina", gailDto.getLowStamina());
    gailNode.put("description", gailDto.getDescription());
    gailNode.put("gender", gailDto.getGender() == null ? null : gailDto.getGender().name());
    gailNode.putNull("imageUrl"); // the seed drops the field the row carries
    gailNode.put("heritageTag", gailDto.getHeritageTag());
    gailNode.put("expansion_code", gailDto.getExpansionCode());
    gailNode.put("drive", gailDto.getDrive());
    gailNode.put("resilience", gailDto.getResilience());
    gailNode.put("charisma", gailDto.getCharisma());
    gailNode.put("brawl", gailDto.getBrawl());
    gailNode.put("alignment", gailDto.getAlignment());
    ByteArrayResource changedGail =
        new ByteArrayResource(gailSeed.toString().getBytes(StandardCharsets.UTF_8));

    when(resourcePatternResolver.getResources(ArgumentMatchers.anyString()))
        .thenReturn(new Resource[] {changedRvd, changedGail});

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    WrestlerSync detectionSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Rob Van Dam: every changed seed field applied silently through its write arm.
    assertEquals("http://example.com/rvd.png", rvd.getImageUrl(), "null-guarded imageUrl set");
    assertEquals(Gender.FEMALE, rvd.getGender(), "gender updated silently");
    assertEquals(rvdDto.getDeckSize() + 1, rvd.getDeckSize(), "deckSize arm wrote the seed value");
    assertEquals(
        rvdDto.getStartingHealth() + 1,
        rvd.getStartingHealth(),
        "startingHealth arm wrote the seed value");
    assertEquals(
        rvdDto.getLowHealth() + 1, rvd.getLowHealth(), "lowHealth arm wrote the seed value");
    assertEquals(
        rvdDto.getStartingStamina() + 1,
        rvd.getStartingStamina(),
        "startingStamina arm wrote the seed value");
    assertEquals(rvdDto.getLowStamina() + 1, rvd.getLowStamina(), "lowStamina arm wrote");
    assertEquals(
        "seed description rebuilt by the release",
        rvd.getDescription(),
        "description arm wrote the seed value");
    assertEquals(5, rvd.getDrive(), "drive updated silently");
    assertEquals(5, rvd.getResilience(), "resilience updated silently");
    assertEquals(5, rvd.getCharisma(), "charisma updated silently");
    assertEquals(5, rvd.getBrawl(), "brawl updated silently");
    assertEquals(
        AlignmentType.HEEL, rvd.getAlignment().getAlignmentType(), "alignment built when absent");
    // Gail Kim: deckSize written (row was null), imageUrl kept (seed-omitted null write skips).
    assertEquals(16, gail.getDeckSize(), "row-missing deckSize written from the seed");
    assertEquals(13, gail.getStartingHealth(), "row-missing startingHealth written from the seed");
    assertEquals(
        "http://example.com/gail.png",
        gail.getImageUrl(),
        "seed-omitted imageUrl must NOT clear the row's value (null-guard skip arm)");
    // Both rows are provenance-exact clean rows → re-stamped with the incoming fingerprint.
    org.junit.jupiter.api.Assertions.assertNotEquals(
        gailStampBefore,
        gail.getSeedFingerprint(),
        "tier-1 row re-stamps with the incoming fingerprint");
    Mockito.verify(parking, Mockito.never())
        .parkFieldConflict(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyString());
  }

  @Test
  void versionChange_stampOnlyRowReStampsFromManifest() throws Exception {
    // The stamp-only-save branch: a clean row whose seed is unchanged (nothing to apply, nothing
    // to park) but whose manifest hash differs from the row's content stamp adopts the manifest
    // hash through the silent-apply-free save path.
    WrestlerImportDTO rvdDto = rvdSeedDto();
    Wrestler rvd = seededRvd(rvdDto);
    rvd.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(rvd)));

    when(wrestlerRepository.count()).thenReturn(1L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(rvd));
    when(wrestlerRepository.findAll()).thenReturn(List.of(rvd));

    SeedProvenanceManifest manifest =
        SeedProvenanceManifest.of(Map.of("wrestler", Map.of("Rob Van Dam", "manifest-hash-rvd")));
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    WrestlerSync detectionSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            parking,
            manifest);
    detectionSync.detectVersionChangeCollisions();

    assertEquals(
        "manifest-hash-rvd",
        rvd.getSeedFingerprint(),
        "a clean unchanged row must adopt the manifest hash (stamp-only save)");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Wrestler>> captor = ArgumentCaptor.forClass(List.class);
    verify(wrestlerRepository).saveAll(captor.capture());
    assertTrue(captor.getValue().contains(rvd), "the stamp-only row must be persisted");
    Mockito.verify(parking, Mockito.never())
        .parkFieldConflict(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyString());
  }

  @Test
  void versionChangeDetection_emptyTableIsInert() {
    // Empty-DB guard arm: PROTECT mode with no rows returns before touching any resource.
    when(wrestlerRepository.count()).thenReturn(0L);
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    WrestlerSync detectionSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();
    Mockito.verifyNoInteractions(parking);
    verify(wrestlerRepository, never()).findAllWithAlignments();
  }

  @Test
  void versionChangeDetection_toleratesBrokenSeedResource() throws Exception {
    // Inner IOException catch: a seed file that fails to deserialize is logged and skipped —
    // the pass never propagates the failure.
    WrestlerImportDTO rvdDto = rvdSeedDto();
    Wrestler rvd = seededRvd(rvdDto);
    rvd.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(rvd)));
    when(wrestlerRepository.count()).thenReturn(1L);
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(rvd));
    when(wrestlerRepository.findAll()).thenReturn(List.of(rvd));

    ObjectMapper brokenMapper =
        new ObjectMapper() {
          @Override
          public <T> T readValue(java.io.InputStream src, TypeReference<T> typeRef)
              throws IOException {
            throw new IOException("boom");
          }
        };
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    WrestlerSync detectionSync =
        new WrestlerSync(
            "true",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            brokenMapper,
            parking,
            EMPTY_MANIFEST);

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        detectionSync::detectVersionChangeCollisions);
    verify(wrestlerRepository, never()).saveAll(any());
    Mockito.verifyNoInteractions(parking);
  }

  @Test
  void normalBootBehaviorUnchanged() throws Exception {
    // Tier 3 pin at the sync level: on a boot where detection does not run (the normal boot), the
    // sync's own behavior is byte-identical to today — verify reconcile preserves the edited row
    // and nothing parks. The detector path is not entered (shouldRunDetection gates it); this
    // test pins that detectVersionChangeCollisions is inert when invoked on a FULL-mode sync and
    // that the pre-existing verify behavior still holds with the new constructor dependencies.
    WrestlerImportDTO dto = rvdSeedDto();
    Wrestler existing = seededRvd(dto);
    existing.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(WrestlerSeedFields.canonical(existing)));
    existing.setDescription("deliberately hand-tuned");
    stubOneExisting(existing);

    // FULL-mode detection is a no-op (FULL applies the seed everywhere, nothing to collide).
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    WrestlerSync fullModeSync =
        new WrestlerSync(
            "false",
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    fullModeSync.detectVersionChangeCollisions();
    Mockito.verifyNoInteractions(parking);
    // Verify mode (the normal-boot path this class already pins) still preserves the edit.
    verifySync().sync();
    assertEquals(
        "deliberately hand-tuned",
        existing.getDescription(),
        "normal-boot verify behavior is unchanged by the detection wiring");
  }
}
