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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javydreamercsw.Application;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Test-classpath entry point that generates the committed seed-fingerprint manifest ({@code
 * seed-fingerprints.json}): boots the real application context on an empty in-memory H2 database
 * (test profile, no web server), runs the production seed sync pipeline exactly as {@code
 * DataInitializer} does (every {@link DataSyncContributor} in {@code @Order} sequence, FULL mode),
 * then re-hashes each domain's rows through the sync classes' own package-private fingerprint
 * methods and writes the manifest.
 *
 * <p>Exit codes: {@code 0} = manifest written; {@code 1} = precondition or classpath-guard failure;
 * {@code 2} = sync/generation exception.
 *
 * <p>The manifest drives the manifest ↔ stored ↔ recomputed three-way match in verify-mode syncs
 * (see {@link SeedFastPathChecker}). Each recorded value is the fingerprint RECOMPUTED from the
 * freshly seeded row via the owning sync's own method — the same value the fast path recomputes at
 * verify time, so the manifest can never drift from production reconciliation.
 *
 * <p>Classpath pollution guard: the generator runs on the test classpath, so before syncing it
 * resolves every domain's seed resources with the same patterns the syncs use and fails unless all
 * of them resolve outside {@code test-classes} — a stray test fixture would otherwise be seeded
 * into the manifest and later poison verify-mode reconciliation.
 */
@Slf4j
public final class GenerateSeedFingerprintManifest {

  /**
   * Names of the rows Flyway migration {@code V112__Migrate_Campaign_Promo_Segment_Types.sql}
   * MERGEs into {@code segment_rule} on every fresh install — schema bootstrap, not user data. The
   * empty-DB precondition tolerates exactly these rows; any other pre-existing segment_rule row
   * aborts generation.
   */
  static final Set<String> FLYWAY_BOOTSTRAP_SEGMENT_RULES =
      Set.of("Faction Beatdown", "GM Office Confrontation", "Performance Review");

  /**
   * Seed resource patterns per manifest domain, mirroring the resolution style of the owning sync
   * (plain names resolve as {@link ClassPathResource}, {@code classpath*:} patterns through a
   * {@link PathMatchingResourcePatternResolver}).
   */
  private static final Map<String, List<String>> SEED_RESOURCE_PATTERNS =
      Map.ofEntries(
          Map.entry(
              "achievement",
              List.of("achievements.json", "classpath*:challenges/**/*achievements*.json")),
          Map.entry("campaign_ability_card", List.of("campaign_ability_cards.json")),
          Map.entry("card", List.of("classpath*:cards/*.json")),
          Map.entry("card_set", List.of("sets.json")),
          Map.entry("championship", List.of("championships.json")),
          Map.entry("deck", List.of("decks.json")),
          Map.entry("npc", List.of("npcs.json")),
          Map.entry(
              "ringside_action", List.of("ringside_action_types.json", "ringside_actions.json")),
          Map.entry("segment_rule", List.of("segment_rules.json")),
          Map.entry("segment_type", List.of("segment_types.json")),
          Map.entry(
              "wrestler",
              List.of(
                  "classpath*:wrestlers/*.json", "classpath*:wrestler_abilities_universal.json")));

  private GenerateSeedFingerprintManifest() {}

  /**
   * Standalone entry point: {@code args[0]} is the output file path. Boots the application on the
   * test profile (no web server) and writes the manifest. Intended to be invoked by the Maven
   * generation profile on the test classpath.
   *
   * @param args {@code args[0]} = output file path
   */
  public static void main(String[] args) {
    if (args.length != 1 || args[0].isBlank()) {
      System.err.println("Usage: GenerateSeedFingerprintManifest <output-file>");
      System.exit(1);
    }
    System.exit(run(Path.of(args[0])));
  }

  /**
   * Boots the application on the test profile with no HTTP server and generates the manifest.
   * Mirrors the surefire boot: {@code spring.profiles.active=test} is applied as a system property
   * (only when not already set) so it overrides the {@code h2} default in {@code
   * application.properties} exactly as the surefire argLine does.
   *
   * <p>The web environment matches {@code @SpringBootTest} with the default {@code MOCK} mode —
   * servlet web-application type with a {@link GenericWebApplicationContext} backed by a {@link
   * MockServletContext}, no server. Vaadin's {@code SpringBootAutoConfiguration} requires a {@code
   * WebApplicationContext} bean injection, which a plain non-web context cannot satisfy; this is
   * the lightest environment that boots the production context unmodified.
   *
   * @param outputFile manifest destination
   * @return the process exit code (see class javadoc)
   */
  static int run(Path outputFile) {
    if (System.getProperty("spring.profiles.active") == null) {
      System.setProperty("spring.profiles.active", "test");
    }
    MockServletContext servletContext = new MockServletContext();
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.SERVLET)
            .contextFactory(
                ApplicationContextFactory.ofContextClass(GenericWebApplicationContext.class))
            .initializers(
                new ServletContextApplicationContextInitializer(servletContext, true),
                new ExcludeTestOnlyBeansInitializer())
            .run()) {
      return generate(context, outputFile);
    } catch (RuntimeException e) {
      log.error("Seed fingerprint manifest generation failed while booting the context", e);
      return 2;
    }
  }

  /**
   * Removes test-only seed-writing beans from the bean definitions before any bean is instantiated.
   * The test classpath carries {@code TestUserInitializer} (a test-profile {@code
   * CommandLineRunner} that inserts "Owned Wrestler" and "Other Wrestler" at boot), which would
   * both violate the empty-DB precondition and pollute the wrestler manifest with non-seed rows.
   * The production context has no such bean, so excluding it does not change what the sync pipeline
   * sees.
   */
  private static final class ExcludeTestOnlyBeansInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      context.addBeanFactoryPostProcessor(
          factory -> {
            if (!(factory instanceof BeanDefinitionRegistry registry)) {
              return;
            }
            Stream.of("testUserInitializer")
                .filter(name -> registry.containsBeanDefinition(name))
                .forEach(
                    beanName -> {
                      registry.removeBeanDefinition(beanName);
                      log.info(
                          "Seed manifest generation: removed test-only bean '{}' from the"
                              + " context.",
                          beanName);
                    });
          });
    }
  }

  /**
   * Generates the manifest against an already-booted context: classpath guard, empty-DB
   * precondition, FULL-mode sync, per-domain recompute, manifest write.
   *
   * @param context the booted application context (test profile, empty in-memory database)
   * @param outputFile manifest destination
   * @return the process exit code (see class javadoc)
   */
  static int generate(ConfigurableApplicationContext context, Path outputFile) {
    Map<String, List<Resource>> domainResources;
    try {
      domainResources = resolveSeedResources();
    } catch (IOException e) {
      log.error("Seed fingerprint generation aborted: could not resolve seed resources", e);
      return 1;
    }
    String violation = findTestClasspathViolation(domainResources);
    if (violation != null) {
      log.error("Seed fingerprint generation aborted: {}", violation);
      System.err.println("Seed fingerprint generation aborted: " + violation);
      return 1;
    }
    ObjectMapper mapper = context.getBean(ObjectMapper.class);
    try {
      return GeneralSecurityUtils.runAsAdmin(
          () -> syncAndWrite(context, domainResources, outputFile, mapper));
    } catch (RuntimeException e) {
      log.error("Seed fingerprint manifest generation failed", e);
      return 2;
    }
  }

  private static int syncAndWrite(
      ConfigurableApplicationContext context,
      Map<String, List<Resource>> domainResources,
      Path outputFile,
      ObjectMapper mapper) {
    String populatedDomain = findPopulatedDomain(context);
    if (populatedDomain != null) {
      log.error(
          "Seed fingerprint generation aborted: domain '{}' is not empty — the manifest must be "
              + "generated from an empty database so every recorded fingerprint reflects a fresh "
              + "FULL-mode seed write.",
          populatedDomain);
      return 1;
    }
    log.info("Running the FULL-mode seed sync across all contributors...");
    context
        .getBeanProvider(DataSyncContributor.class)
        .orderedStream()
        .forEach(DataSyncContributor::sync);
    log.info("Seed sync complete; recomputing per-domain fingerprints...");
    Map<String, Map<String, String>> domains = extractFingerprints(context);
    SeedFingerprintManifest manifest = SeedFingerprintManifest.of(domains);
    try {
      writeManifest(manifest, outputFile, mapper);
    } catch (IOException e) {
      log.error("Failed to write seed fingerprint manifest to {}", outputFile, e);
      return 2;
    }
    domains.forEach(
        (domain, entries) ->
            log.info("Seed manifest: domain '{}' -> {} entr(ies)", domain, entries.size()));
    log.info("Seed fingerprint manifest written to {}", outputFile.toAbsolutePath());
    return 0;
  }

  /**
   * Resolves every domain's seed resources with the same patterns the owning sync uses.
   *
   * @return domain key → resolved (existing) resources
   * @throws IOException when a {@code classpath*:} pattern cannot be resolved at all
   */
  static Map<String, List<Resource>> resolveSeedResources() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Map<String, List<Resource>> resolved = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : SEED_RESOURCE_PATTERNS.entrySet()) {
      List<Resource> resources = new ArrayList<>();
      for (String pattern : entry.getValue()) {
        if (pattern.startsWith("classpath*:")) {
          for (Resource resource : resolver.getResources(pattern)) {
            if (resource.exists()) {
              resources.add(resource);
            }
          }
        } else {
          ClassPathResource resource = new ClassPathResource(pattern);
          if (resource.exists()) {
            resources.add(resource);
          }
        }
      }
      resolved.put(entry.getKey(), resources);
    }
    return resolved;
  }

  /**
   * Pure guard over resolved resources: reports the first resource whose URL carries the test
   * classpath marker, or {@code null} when every resource resolves outside the test classpath.
   *
   * @param domainResources domain key → resolved seed resources
   * @return a violation message naming the domain and the offending {@code test-classes} URL, or
   *     {@code null} when clean
   */
  static String findTestClasspathViolation(Map<String, List<Resource>> domainResources) {
    for (Map.Entry<String, List<Resource>> domainEntry : domainResources.entrySet()) {
      for (Resource resource : domainEntry.getValue()) {
        if (!resource.exists()) {
          continue;
        }
        try {
          String url = resource.getURL().toString();
          if (url.contains("test-classes")) {
            return "seed resource for domain '%s' resolves into the test classpath: %s -> %s"
                .formatted(domainEntry.getKey(), resource.getDescription(), url);
          }
        } catch (IOException e) {
          return "could not resolve URL of seed resource for domain '%s': %s"
              .formatted(domainEntry.getKey(), resource.getDescription());
        }
      }
    }
    return null;
  }

  /**
   * @return the first manifest domain whose table is not empty, or {@code null} when the database
   *     is clean — the manifest must be generated from an empty database so every fingerprint
   *     reflects a fresh FULL-mode seed write. Segment rule rows created by the {@code V112} Flyway
   *     migration are schema bootstrap (the FULL path re-syncs and re-stamps their seed content)
   *     and are tolerated; any other pre-existing row aborts.
   */
  private static String findPopulatedDomain(ApplicationContext context) {
    if (context.getBean(AchievementRepository.class).count() > 0) {
      return "achievement";
    }
    if (context.getBean(CampaignAbilityCardService.class).count() > 0) {
      return "campaign_ability_card";
    }
    if (context.getBean(CardService.class).count() > 0) {
      return "card";
    }
    if (context.getBean(CardSetService.class).count() > 0) {
      return "card_set";
    }
    if (context.getBean(TitleService.class).count() > 0) {
      return "championship";
    }
    if (context.getBean(DeckService.class).count() > 0) {
      return "deck";
    }
    if (context.getBean(NpcService.class).count() > 0) {
      return "npc";
    }
    RingsideActionDataService ringsideActionDataService =
        context.getBean(RingsideActionDataService.class);
    if (ringsideActionDataService.countActions() > 0
        || ringsideActionDataService.countTypes() > 0) {
      return "ringside_action";
    }
    List<SegmentRule> preExistingRules =
        context.getBean(SegmentRuleService.class).findAllForAdmin();
    boolean onlyBootstrapRules =
        preExistingRules.stream()
            .allMatch(r -> FLYWAY_BOOTSTRAP_SEGMENT_RULES.contains(r.getName()));
    if (!onlyBootstrapRules) {
      return "segment_rule";
    }
    if (context.getBean(SegmentTypeService.class).count() > 0) {
      return "segment_type";
    }
    if (context.getBean(WrestlerRepository.class).count() > 0) {
      return "wrestler";
    }
    return null;
  }

  /**
   * Re-hashes every freshly seeded row through the owning sync's fingerprint method, keyed
   * identically to the sync's {@code fastPathApplies()} map. Runs on the same beans the syncs
   * themselves run on, so the manifest can never drift from what verify mode recomputes.
   *
   * <p>Rows whose stored fingerprint is blank are skipped: they were written by a DIFFERENT sync
   * than the domain's own (e.g. {@code CommentatorSync} creates NPC rows that {@code NpcSync} never
   * stamps), and the fast path's stored-side check ({@code stored.equals(manifestValue)}) could
   * never hold for them — including them would permanently disable the fast path.
   *
   * @param context the application context
   * @return domain key → (seed key → recomputed fingerprint), domains and entries sorted
   */
  private static Map<String, Map<String, String>> extractFingerprints(ApplicationContext context) {
    // SeedSyncSupport.fingerprint() is mode-independent (a pure SHA-256 over the given values), so
    // any raw flag value yields the production fingerprint function.
    SeedSyncSupport syncSupport = new SeedSyncSupport("false");
    Map<String, Map<String, String>> domains = new TreeMap<>();

    AchievementSync achievementSync = context.getBean(AchievementSync.class);
    Map<String, String> achievement = new TreeMap<>();
    for (Achievement a : context.getBean(AchievementRepository.class).findAll()) {
      if (SeedSyncSupport.isBlankFingerprint(a.getSeedFingerprint())) {
        continue;
      }
      achievement.put(a.getKey(), achievementSync.achievementFingerprint(a));
    }
    domains.put("achievement", achievement);

    CampaignAbilityCardSync abilityCardSync = context.getBean(CampaignAbilityCardSync.class);
    Map<String, String> abilityCards = new TreeMap<>();
    for (CampaignAbilityCard c : context.getBean(CampaignAbilityCardService.class).findAll()) {
      if (SeedSyncSupport.isBlankFingerprint(c.getSeedFingerprint())) {
        continue;
      }
      abilityCards.put(
          c.getName() + "|" + c.getAlignmentType() + "|" + c.getLevel(),
          abilityCardSync.cardFingerprint(c));
    }
    domains.put("campaign_ability_card", abilityCards);

    CardSync cardSync = context.getBean(CardSync.class);
    Map<String, String> cards = new TreeMap<>();
    for (Card c : context.getBean(CardService.class).findAll()) {
      if (SeedSyncSupport.isBlankFingerprint(c.getSeedFingerprint())) {
        continue;
      }
      cards.put(c.getSet().getCode() + "#" + c.getNumber(), cardSync.cardFingerprint(c));
    }
    domains.put("card", cards);

    Map<String, String> cardSets = new TreeMap<>();
    for (CardSet c : context.getBean(CardSetService.class).findAll()) {
      if (SeedSyncSupport.isBlankFingerprint(c.getSeedFingerprint())) {
        continue;
      }
      cardSets.put(c.getCode(), syncSupport.fingerprint(c.getName()));
    }
    domains.put("card_set", cardSets);

    ChampionshipSync championshipSync = context.getBean(ChampionshipSync.class);
    Map<String, String> championships = new TreeMap<>();
    for (Title t : context.getBean(TitleService.class).findAllUnfiltered()) {
      if (SeedSyncSupport.isBlankFingerprint(t.getSeedFingerprint())) {
        continue;
      }
      championships.put(t.getName(), championshipSync.titleFingerprint(t));
    }
    domains.put("championship", championships);

    DeckSync deckSync = context.getBean(DeckSync.class);
    Map<String, String> decks = new TreeMap<>();
    for (Deck deck : context.getBean(DeckService.class).findAllWithCards()) {
      if (deck.getWrestler() == null) {
        continue; // mirrors DeckSync.fastPathApplies(): wrestler-less decks are not seed state
      }
      if (SeedSyncSupport.isBlankFingerprint(deck.getSeedFingerprint())) {
        continue;
      }
      decks.put(deck.getWrestler().getName(), deckSync.compositionFingerprint(deck));
    }
    domains.put("deck", decks);

    NpcSync npcSync = context.getBean(NpcSync.class);
    Map<String, String> npcs = new TreeMap<>();
    for (Npc n : context.getBean(NpcService.class).findAllUnfiltered()) {
      if (SeedSyncSupport.isBlankFingerprint(n.getSeedFingerprint())) {
        continue; // e.g. commentator-created NPCs — NpcSync never stamps those rows
      }
      npcs.put(n.getName(), npcSync.npcFingerprint(n, null));
    }
    domains.put("npc", npcs);

    RingsideActionSync ringsideSync = context.getBean(RingsideActionSync.class);
    Map<String, String> ringsideActions = new TreeMap<>();
    for (RingsideAction a :
        context.getBean(RingsideActionDataService.class).findAllActionsUnfiltered()) {
      if (SeedSyncSupport.isBlankFingerprint(a.getSeedFingerprint())) {
        continue;
      }
      ringsideActions.put(a.getName(), ringsideSync.actionFingerprint(a));
    }
    domains.put("ringside_action", ringsideActions);

    SegmentRuleSync ruleSync = context.getBean(SegmentRuleSync.class);
    Map<String, String> segmentRules = new TreeMap<>();
    for (SegmentRule sr : context.getBean(SegmentRuleService.class).findAllForAdmin()) {
      if (SeedSyncSupport.isBlankFingerprint(sr.getSeedFingerprint())) {
        continue;
      }
      segmentRules.put(sr.getName(), ruleSync.ruleFingerprint(sr));
    }
    domains.put("segment_rule", segmentRules);

    SegmentTypeSync typeSync = context.getBean(SegmentTypeSync.class);
    Map<String, String> segmentTypes = new TreeMap<>();
    for (SegmentType st : context.getBean(SegmentTypeService.class).findAll()) {
      if (SeedSyncSupport.isBlankFingerprint(st.getSeedFingerprint())) {
        continue;
      }
      segmentTypes.put(st.getName(), typeSync.segmentTypeFingerprint(st));
    }
    domains.put("segment_type", segmentTypes);

    WrestlerSync wrestlerSync = context.getBean(WrestlerSync.class);
    Map<String, String> wrestlers = new TreeMap<>();
    for (Wrestler w : context.getBean(WrestlerRepository.class).findAllWithAlignments()) {
      if (SeedSyncSupport.isBlankFingerprint(w.getSeedFingerprint())) {
        continue; // e.g. player-owned rows created by test bootstrapping
      }
      wrestlers.put(w.getName(), wrestlerSync.wrestlerFingerprint(w));
    }
    domains.put("wrestler", wrestlers);

    return domains;
  }

  /**
   * Writes the manifest JSON with the production mapper configuration plus {@code INDENT_OUTPUT}.
   * Document shape matches {@link SeedFingerprintManifest}'s parser ({@code generatedAt} first,
   * then {@code domains}); domain and entry keys are sorted so regenerating after an unrelated seed
   * change produces a minimal diff.
   *
   * @param manifest the built manifest
   * @param outputFile destination
   * @param mapper the production {@link ObjectMapper} (see {@code JacksonConfig})
   * @throws IOException on write failure
   */
  private static void writeManifest(
      SeedFingerprintManifest manifest, Path outputFile, ObjectMapper mapper) throws IOException {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("generatedAt", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    document.put("domains", manifest.domains());
    ObjectMapper writer = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    Path absolute = outputFile.toAbsolutePath();
    if (absolute.getParent() != null) {
      Files.createDirectories(absolute.getParent());
    }
    writer.writeValue(absolute.toFile(), document);
  }
}
