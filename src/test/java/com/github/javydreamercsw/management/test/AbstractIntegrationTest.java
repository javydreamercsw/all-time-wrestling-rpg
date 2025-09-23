package com.github.javydreamercsw.management.test;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckCardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("isNotionTokenAvailable")
public abstract class AbstractIntegrationTest {

  @Autowired protected SegmentRuleService segmentRuleService;
  @Autowired protected ShowTypeService showTypeService;
  @Autowired protected SegmentTypeService segmentTypeService;
  @Autowired protected ShowTemplateService showTemplateService;
  @Autowired protected CardSetService cardSetService;
  @Autowired protected CardService cardService;
  @Autowired protected WrestlerService wrestlerService;
  @Autowired protected TitleService titleService;
  @Autowired protected DeckService deckService;
  @Autowired protected DeckCardService deckCardService;
  @Autowired protected NotionSyncService notionSyncService;
  @Autowired protected FactionRepository factionRepository;
  @Autowired protected TitleReignRepository titleReignRepository;
  @Autowired protected TitleRepository titleRepository;
  @Autowired protected WrestlerRepository wrestlerRepository;
  @Autowired protected WrestlerSyncService wrestlerSyncService;
  @Autowired protected TeamService teamService;
  @Autowired protected TeamRepository teamRepository;
  @Autowired protected ShowService showService;
  @Autowired protected DeckRepository deckRepository;
  @Autowired protected CardRepository cardRepository;
  @Autowired protected CardSetRepository cardSetRepository;

  @Autowired protected DataInitializer dataInitializer;

  @BeforeEach
  @Transactional(rollbackFor = Exception.class)
  public void setUp() throws Exception {
    dataInitializer
        .loadSegmentRulesFromFile(segmentRuleService)
        .run(new DefaultApplicationArguments());
    dataInitializer.syncShowTypesFromFile(showTypeService).run(new DefaultApplicationArguments());
    dataInitializer
        .loadSegmentTypesFromFile(segmentTypeService)
        .run(new DefaultApplicationArguments());
    dataInitializer
        .loadShowTemplatesFromFile(showTemplateService)
        .run(new DefaultApplicationArguments());
    dataInitializer.syncSetsFromFile(cardSetService).run(new DefaultApplicationArguments());
    dataInitializer
        .syncCardsFromFile(cardService, cardSetService)
        .run(new DefaultApplicationArguments());
    dataInitializer.syncWrestlersFromFile(wrestlerService).run(new DefaultApplicationArguments());
    dataInitializer.syncChampionshipsFromFile(titleService).run(new DefaultApplicationArguments());
    dataInitializer
        .syncDecksFromFile(cardService, wrestlerService, deckService, deckCardService)
        .run(new DefaultApplicationArguments());
  }

  protected Wrestler createTestWrestler(String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    wrestler.setFans(10_000L);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(0);
    wrestler.setStartingStamina(0);
    wrestler.setLowStamina(0);
    wrestler.setDeckSize(15);
    wrestler.setIsPlayer(false);
    return wrestler;
  }

  protected com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext
      createCustomSegmentContext() {
    com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext context =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext();

    // Match Type
    com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext matchType =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext();
    matchType.setSegmentType("Hell in a Cell");
    matchType.setStipulation("King of the Ring 1998");
    matchType.setRules(java.util.Arrays.asList("No Disqualification", "Falls Count Anywhere"));
    context.setSegmentType(matchType);

    // Venue
    com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext venue =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext();
    venue.setName("Civic Arena");
    venue.setLocation("Pittsburgh, Pennsylvania");
    venue.setType("Indoor Arena");
    venue.setCapacity(17_000);
    venue.setDescription("Historic venue for legendary matches");
    venue.setAtmosphere("Intense and foreboding");
    venue.setSignificance("Site of the most famous Hell in a Cell segment");
    context.setVenue(venue);

    // Wrestlers
    com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext undertaker =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext();
    undertaker.setName("The Undertaker");
    undertaker.setDescription("The Deadman - Phenom of WWE");

    com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext mankind =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext();
    mankind.setName("Mankind");
    mankind.setDescription("Hardcore legend Mick Foley");

    context.setWrestlers(java.util.Arrays.asList(undertaker, mankind));

    // Context
    context.setAudience("Shocked and horrified crowd of 17,000");
    context.setDeterminedOutcome(
        "The Undertaker wins after Mankind is thrown off the Hell in a Cell");

    return context;
  }
}
