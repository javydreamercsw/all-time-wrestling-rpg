package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
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
import com.github.javydreamercsw.management.domain.storyline.StorylineBranchRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckCardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
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
  @Autowired protected StorylineBranchRepository storylineBranchRepository;
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
  @Autowired protected SegmentTypeRepository segmentTypeRepository;
  @Autowired protected DramaEventService dramaEventService;
  @Autowired protected DataInitializer dataInitializer;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  protected void clearAllRepositories() {
    deckCardRepository.deleteAll();
    deckRepository.deleteAll();
    dramaEventRepository.deleteAll();
    factionRivalryRepository.deleteAll();
    multiWrestlerFeudRepository.deleteAll();
    rivalryRepository.deleteAll();
    injuryRepository.deleteAll();
    injuryTypeRepository.deleteAll();
    segmentRepository.deleteAll();
    segmentRuleRepository.deleteAll();
    storylineBranchRepository.deleteAll();
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

  protected com.github.javydreamercsw.management.domain.wrestler.Wrestler createTestWrestler(
      @lombok.NonNull String name) {
    com.github.javydreamercsw.management.domain.wrestler.Wrestler wrestler =
        new com.github.javydreamercsw.management.domain.wrestler.Wrestler();
    wrestler.setName(name);
    wrestler.setFans(10_000L);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(0);
    wrestler.setStartingStamina(0);
    wrestler.setLowStamina(0);
    wrestler.setDeckSize(15);
    wrestler.setIsPlayer(false);
    wrestler.setGender(com.github.javydreamercsw.management.domain.wrestler.Gender.MALE);
    return wrestler;
  }
}
