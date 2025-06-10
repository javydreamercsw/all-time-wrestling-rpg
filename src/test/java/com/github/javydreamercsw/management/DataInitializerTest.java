package com.github.javydreamercsw.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.config.name=application-test")
@Transactional
class DataInitializerTest {

  @Autowired private CardService cardService;

  @Autowired private CardSetService cardSetService;

  @Autowired private WrestlerService wrestlerService;

  @Autowired private DeckService deckService;

  @Autowired private ShowTypeService showTypeService;

  @Autowired private ShowService showService;

  @Test
  void testDataLoadedFromFile() {
    List<Card> cards = cardService.findAll();
    List<CardSet> sets = cardSetService.findAll();
    List<Wrestler> wrestlers = wrestlerService.findAll();
    List<Deck> decks = deckService.findAll();
    List<ShowType> showTypes = showTypeService.findAll();
    List<Show> shows = showService.findAll();

    // Check that cards and sets are loaded
    assertThat(cards).isNotEmpty();
    assertThat(sets).isNotEmpty();
    assertThat(wrestlers).isNotEmpty();
    assertThat(decks).isNotEmpty();
    assertThat(showTypes).isNotEmpty();
    assertThat(shows).isNotEmpty();

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
