package com.github.javydreamercsw.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

@SpringBootTest
class DataInitializerTest {

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
  @Autowired private DataInitializer dataInitializer;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private DeckCardRepository deckCardRepository;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    // Clear repositories before each test to ensure a clean state
    clearRepositories();
    // Explicitly run the data initializer to load data from files
    dataInitializer.loadSegmentRulesFromFile(segmentRuleService).run(null);
    dataInitializer.syncShowTypesFromFile(showTypeService).run(null);
    dataInitializer.loadSegmentTypesFromFile(segmentTypeService).run(null);
    dataInitializer.loadShowTemplatesFromFile(showTemplateService).run(null);
    dataInitializer.syncSetsFromFile(cardSetService).run(null);
    dataInitializer.syncCardsFromFile(cardService, cardSetService).run(null);
    dataInitializer.syncWrestlersFromFile(wrestlerService).run(null);
    dataInitializer.syncChampionshipsFromFile(titleService).run(null);
    dataInitializer
        .syncDecksFromFile(cardService, wrestlerService, deckService, deckCardService)
        .run(null);
  }

  private void clearRepositories() {
    deckCardRepository.deleteAll();
    deckRepository.deleteAll();
    showRepository.deleteAll();
    titleRepository.deleteAll();
    wrestlerRepository.deleteAll();
    cardRepository.deleteAll();
    cardSetRepository.deleteAll();
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
    segmentRuleRepository.deleteAll();
    segmentTypeRepository.deleteAll();
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
    assertThat(card.getSet().getSetCode()).isEqualTo("RVD");
  }

  @Test
  void testDeckImportIsIdempotentAndNoDuplicates() throws Exception {
    // For each deck, ensure no duplicate DeckCard entries for the same (deck, card, set)
    List<Deck> decks = deckService.findAll();
    for (Deck deck : decks) {
      List<DeckCard> deckCards = new java.util.ArrayList<>();
      deckCardService.findAll().forEach(deckCards::add);
      List<DeckCard> filteredDeckCards =
          deckCards.stream()
              .filter(
                  dc -> {
                    Assertions.assertNotNull(dc.getDeck().getId());
                    return dc.getDeck().getId().equals(deck.getId());
                  })
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
