package com.github.javydreamercsw.management;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ManagementIntegrationTest extends AbstractIntegrationTest {
  @Autowired protected DeckCardRepository deckCardRepository;
  @Autowired protected DramaEventRepository dramaEventRepository;
  @Autowired protected FactionRivalryRepository factionRivalryRepository;
  @Autowired protected MultiWrestlerFeudRepository multiWrestlerFeudRepository;
  @Autowired protected RivalryRepository rivalryRepository;
  @Autowired protected InjuryRepository injuryRepository;
  @Autowired protected InjuryTypeRepository injuryTypeRepository;
  @Autowired protected SegmentRepository segmentRepository;
  @Autowired protected SegmentRuleRepository segmentRuleRepository;
  @Autowired protected NpcRepository npcRepository;
  @Autowired protected SeasonRepository seasonRepository;
  @Autowired protected SegmentRuleService segmentRuleService;
  @Autowired protected ShowTypeService showTypeService;
  @Autowired protected ShowTypeRepository showTypeRepository;
  @Autowired protected ShowTemplateRepository showTemplateRepository;
  @Autowired protected ShowRepository showRepository;
  @Autowired protected SegmentTypeService segmentTypeService;
  @Autowired protected ShowTemplateService showTemplateService;
  @Autowired protected CardSetService cardSetService;
  @Autowired protected CardService cardService;
  @Autowired protected WrestlerService wrestlerService;
  @Autowired protected NotionSyncService notionSyncService;
  @Autowired protected FactionRepository factionRepository;
  @Autowired protected TitleReignRepository titleReignRepository;
  @Autowired protected TitleRepository titleRepository;
  @Autowired protected WrestlerRepository wrestlerRepository;
  @Autowired protected TeamService teamService;
  @Autowired protected TeamRepository teamRepository;
  @Autowired protected ShowService showService;
  @Autowired protected DeckRepository deckRepository;
  @Autowired protected CardRepository cardRepository;
  @Autowired protected CardSetRepository cardSetRepository;
  @Autowired protected SegmentTypeRepository segmentTypeRepository;
  @Autowired protected DramaEventService dramaEventService;
  private static Routes routes;

  @BeforeAll
  public static void discoverRoutes() {
    // Auto-discover your application's routes
    routes = new Routes().autoDiscoverViews("com.github.javydreamercsw");
  }

  @BeforeEach
  public void setupKaribu() {
    MockitoAnnotations.openMocks(this);
    MockVaadin.setup(routes); // Set up Karibu with your discovered routes
  }

  @AfterEach
  public void tearDown() {
    MockVaadin.tearDown();
  }

  protected void clearAllRepositories() {
    deckCardRepository.deleteAllInBatch();
    deckRepository.deleteAll();
    deckRepository.deleteAll();
    dramaEventRepository.deleteAll();
    factionRivalryRepository.deleteAll();
    multiWrestlerFeudRepository.deleteAll();
    rivalryRepository.deleteAll();
    injuryRepository.deleteAll();
    injuryTypeRepository.deleteAll();
    segmentRepository.deleteAll();
    segmentRuleRepository.deleteAll();
    teamRepository.deleteAll();
    titleReignRepository.deleteAll();
    titleRepository.deleteAll();
    factionRepository.deleteAll();
    npcRepository.deleteAll();
    wrestlerRepository.deleteAll();
    cardRepository.deleteAll();
    cardSetRepository.deleteAll();
    seasonRepository.deleteAll();
    showRepository.deleteAll();
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
    segmentTypeRepository.deleteAll();
  }

  protected Wrestler createTestWrestler(@NonNull String name) {
    return TestUtils.createWrestler(wrestlerRepository, name);
  }
}
