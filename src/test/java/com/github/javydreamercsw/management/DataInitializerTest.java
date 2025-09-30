package com.github.javydreamercsw.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.dto.TitleDTO;
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
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@EnabledIf("isNotionTokenAvailable")
class DataInitializerTest extends AbstractIntegrationTest {

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
  void setUp() {
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
  void validateSegmentRulesJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("segment_rules.json");
    List<SegmentRuleDTO> segmentRules =
        mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueNames = new HashSet<>();
    for (SegmentRuleDTO segmentRule : segmentRules) {
      assertThat(uniqueNames.add(segmentRule.getName()))
          .withFailMessage("Duplicate segment rule name found: %s", segmentRule.getName())
          .isTrue();
    }
  }

  @Test
  void validateShowTypesJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("show_types.json");
    List<ShowType> showTypes =
        mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueNames = new HashSet<>();
    for (ShowType showType : showTypes) {
      assertThat(uniqueNames.add(showType.getName()))
          .withFailMessage("Duplicate show type name found: %s", showType.getName())
          .isTrue();
    }
  }

  @Test
  void validateSegmentTypesJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("segment_types.json");
    List<SegmentTypeDTO> segmentTypes =
        mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueNames = new HashSet<>();
    for (SegmentTypeDTO segmentType : segmentTypes) {
      assertThat(uniqueNames.add(segmentType.getName()))
          .withFailMessage("Duplicate segment type name found: %s", segmentType.getName())
          .isTrue();
    }
  }

  @Test
  void validateShowTemplatesJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("show_templates.json");
    List<ShowTemplateDTO> showTemplates =
        mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueNames = new HashSet<>();
    for (ShowTemplateDTO showTemplate : showTemplates) {
      assertThat(uniqueNames.add(showTemplate.getName()))
          .withFailMessage("Duplicate show template name found: %s", showTemplate.getName())
          .isTrue();
    }
  }

  @Test
  void validateSetsJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("sets.json");
    List<CardSet> sets = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueNames = new HashSet<>();
    for (CardSet set : sets) {
      assertThat(uniqueNames.add(set.getName()))
          .withFailMessage("Duplicate set name found: %s", set.getName())
          .isTrue();
    }
  }

  @Test
  void validateCardsJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("cards.json");
    List<CardDTO> cards = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueCards = new HashSet<>();
    for (CardDTO card : cards) {
      String cardIdentifier = card.getSet() + "-" + card.getNumber();
      assertThat(uniqueCards.add(cardIdentifier))
          .withFailMessage(
              "Duplicate card found: set %s, number %d", card.getSet(), card.getNumber())
          .isTrue();
    }
  }

  @Test
  void validateWrestlersJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("wrestlers.json");
    List<Wrestler> wrestlers =
        mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueNames = new HashSet<>();
    Set<String> uniqueExternalIds = new HashSet<>();
    for (Wrestler wrestler : wrestlers) {
      assertThat(uniqueNames.add(wrestler.getName()))
          .withFailMessage("Duplicate wrestler name found: %s", wrestler.getName())
          .isTrue();
      if (wrestler.getExternalId() != null && !wrestler.getExternalId().trim().isEmpty()) {
        assertThat(uniqueExternalIds.add(wrestler.getExternalId()))
            .withFailMessage("Duplicate wrestler external ID found: %s", wrestler.getExternalId())
            .isTrue();
      }
    }
  }

  @Test
  void validateChampionshipsJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("championships.json");
    List<TitleDTO> championships =
        mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    Set<String> uniqueNames = new HashSet<>();
    for (TitleDTO championship : championships) {
      assertThat(uniqueNames.add(championship.getName()))
          .withFailMessage("Duplicate championship name found: %s", championship.getName())
          .isTrue();
    }
  }

  @Test
  void validateDecksJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("decks.json");
    List<DeckDTO> decks = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});

    for (DeckDTO deck : decks) {
      Set<String> uniqueCards = new HashSet<>();
      for (var card : deck.getCards()) {
        String cardIdentifier = card.getSet() + "-" + card.getNumber();
        assertThat(uniqueCards.add(cardIdentifier))
            .withFailMessage(
                "Duplicate card found in deck for %s: set %s, number %d",
                deck.getWrestler(), card.getSet(), card.getNumber())
            .isTrue();
      }
    }
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

  @Test
  @Transactional
  void testDeckImportIsIdempotentAndNoDuplicates() throws Exception {
    // Initial import
    dataInitializer
        .syncDecksFromFile(cardService, wrestlerService, deckService, deckCardService)
        .run(null);
    // Import again (simulate repeated import)
    dataInitializer
        .syncDecksFromFile(cardService, wrestlerService, deckService, deckCardService)
        .run(null);

    // For each deck, ensure no duplicate DeckCard entries for the same (deck, card, set)
    List<Deck> decks = deckService.findAll();
    for (Deck deck : decks) {
      List<DeckCard> deckCards =
          deckCardService.findAll().stream()
              .filter(dc -> dc.getDeck().getId().equals(deck.getId()))
              .toList();
      // Check for duplicates by (cardId, setId)
      java.util.Set<String> uniqueKeys = new java.util.HashSet<>();
      for (DeckCard dc : deckCards) {
        String key = dc.getCard().getId() + "-" + dc.getSet().getId();
        boolean added = uniqueKeys.add(key);
        assertThat(added)
            .withFailMessage(
                "Duplicate DeckCard for deck %s, card %s, set %s",
                deck.getId(), dc.getCard().getId(), dc.getSet().getId())
            .isTrue();
      }
    }
  }
}
