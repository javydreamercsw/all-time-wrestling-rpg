package com.github.javydreamercsw.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckCardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.config.name=application-test")
class DataInitializerTest {

  @Autowired private DataInitializer dataInitializer;
  @Autowired private CardService cardService;
  @Autowired private CardSetService cardSetService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private DeckService deckService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private ShowService showService;
  @Autowired private TitleService titleService;
  @Autowired private SegmentRuleService segmentRuleService;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private DeckCardService deckCardService;

  @BeforeEach
  void setUp() throws Exception {
    dataInitializer.loadSegmentRulesFromFile(segmentRuleService);
    dataInitializer.syncShowTypesFromFile(showTypeService);
    dataInitializer.loadSegmentTypesFromFile(segmentTypeService);
    dataInitializer.loadShowTemplatesFromFile(showTemplateService);
    dataInitializer.syncSetsFromFile(cardSetService);
    dataInitializer.syncCardsFromFile(cardService, cardSetService);
    dataInitializer.syncWrestlersFromFile(wrestlerService);
    dataInitializer.syncChampionshipsFromFile(titleService);
    dataInitializer.syncDecksFromFile(cardService, wrestlerService, deckService, deckCardService);
  }

  @Test
  @Transactional
  void testDataLoadedFromFile() {
    List<Card> cards = cardService.findAll();
    List<CardSet> sets = cardSetService.findAll();
    List<Wrestler> wrestlers = wrestlerService.findAll();
    List<Deck> decks = deckService.findAll();
    List<ShowType> showTypes = showTypeService.findAll();
    List<Show> shows = showService.findAll();
    List<Title> titles = titleService.findAll();
    List<SegmentRule> segmentRules = segmentRuleService.findAll();
    List<ShowTemplate> showTemplates = showTemplateService.findAll();
    List<SegmentType> segmentTypes = segmentTypeService.findAll();

    // Check that cards and sets are loaded
    assertThat(cards).isNotEmpty();
    assertThat(sets).isNotEmpty();
    assertThat(wrestlers).isNotEmpty();
    assertThat(decks).isNotEmpty();
    assertThat(showTypes).isNotEmpty();
    assertThat(titles).isNotEmpty();
    assertThat(segmentRules).isNotEmpty();
    assertThat(showTemplates).isNotEmpty();
    assertThat(segmentTypes).isNotEmpty();
    // Note: shows.json is empty, so we don't expect shows to be loaded from file
    // Shows are typically created through the booking service or Notion sync
    assertThat(shows).isEmpty(); // Changed expectation since shows.json is empty

    // Check that a card has the correct set mapped
    Card card =
        cards.stream()
            .filter(c -> "Springboard Thrust Kick".equals(c.getName()))
            .findFirst()
            .orElseThrow();
    assertThat(card.getSet()).isNotNull();
    assertThat(card.getSet().getName()).isEqualTo("RVD");
  }
}
