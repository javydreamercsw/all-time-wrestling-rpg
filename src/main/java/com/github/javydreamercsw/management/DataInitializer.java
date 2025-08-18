package com.github.javydreamercsw.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckCardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class DataInitializer {
  private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

  @Bean
  @Order(1)
  public ApplicationRunner syncSetsFromFile(CardSetService cardSetService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("sets.json");
      if (resource.exists()) {
        logger.info("Loading card sets from file: {}", resource.getPath());
        // Load card sets from JSON file
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var setsFromFile = mapper.readValue(is, new TypeReference<List<CardSet>>() {});
          // Map existing cards by name
          Map<String, CardSet> existing =
              cardSetService.findAll().stream().collect(Collectors.toMap(CardSet::getName, c -> c));
          for (CardSet c : setsFromFile) {
            CardSet existingSet = existing.get(c.getName());
            if (existingSet != null) {
              // Update fields
              cardSetService.save(existingSet);
              logger.info("Updated existing card set: {}", existingSet.getName());
            } else {
              cardSetService.save(c);
              logger.info("Saved new card set: {}", c.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(2)
  public ApplicationRunner syncCardsFromFile(
      CardService cardService, CardSetService cardSetService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("cards.json");
      if (resource.exists()) {
        logger.info("Loading cards from file: {}", resource.getPath());
        // Load cards from JSON file
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var cardsFromFile = mapper.readValue(is, new TypeReference<List<CardDTO>>() {});
          Map<String, Card> existing =
              cardService.findAll().stream()
                  .collect(
                      Collectors.toMap(
                          c -> c.getName() + "#" + c.getNumber(), // Unique key: name + number
                          c -> c));
          Map<String, CardSet> sets =
              cardSetService.findAll().stream().collect(Collectors.toMap(CardSet::getName, s -> s));
          for (CardDTO dto : cardsFromFile) {
            CardSet set = sets.get(dto.getSet());
            final String key = dto.getName() + "#" + dto.getNumber();
            Card card = existing.getOrDefault(key, new Card());
            card.setName(dto.getName());
            card.setDamage(dto.getDamage());
            card.setFinisher(dto.isFinisher());
            card.setSignature(dto.isSignature());
            card.setStamina(dto.getStamina());
            card.setMomentum(dto.getMomentum());
            card.setTarget(dto.getTarget());
            card.setNumber(dto.getNumber());
            card.setSet(set);
            card.setType(dto.getType());
            card.setTaunt(dto.isTaunt());
            card.setPin(dto.isPin());
            card.setRecover(dto.isRecover());
            cardService.save(card);
            if (existing.containsKey(dto.getName())) {
              logger.info("Updated existing card: {}", card.getName());
            } else {
              logger.info("Saved new card: {}", card.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(3)
  public ApplicationRunner syncWrestlersFromFile(WrestlerService wrestlerService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("wrestlers.json");
      if (resource.exists()) {
        logger.info("Loading wrestlers from file: {}", resource.getPath());
        // Load wrestlers from JSON file
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var wrestlersFromFile = mapper.readValue(is, new TypeReference<List<Wrestler>>() {});
          // Map existing wrestlers by name
          Map<String, Wrestler> existing =
              wrestlerService.findAll().stream()
                  .collect(Collectors.toMap(Wrestler::getName, w -> w));
          for (Wrestler w : wrestlersFromFile) {
            Wrestler existingWrestler = existing.get(w.getName());
            if (existingWrestler != null) {
              // Update fields
              existingWrestler.setDeckSize(w.getDeckSize());
              existingWrestler.setStartingHealth(w.getStartingHealth());
              existingWrestler.setLowHealth(w.getLowHealth());
              existingWrestler.setStartingStamina(w.getStartingStamina());
              existingWrestler.setLowStamina(w.getLowStamina());
              wrestlerService.save(existingWrestler);
              logger.info("Updated existing wrestler: {}", existingWrestler.getName());
            } else {
              wrestlerService.save(w);
              logger.info("Saved new wrestler: {}", w.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(4)
  public ApplicationRunner syncDecksFromFile(
      CardService cardService,
      WrestlerService wrestlerService,
      DeckService deckService,
      DeckCardService deckCardService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("decks.json");
      if (resource.exists()) {
        logger.info("Loading decks from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var decksFromFile = mapper.readValue(is, new TypeReference<List<DeckDTO>>() {});
          Map<String, Wrestler> wrestlers =
              wrestlerService.findAll().stream()
                  .collect(Collectors.toMap(Wrestler::getName, w -> w));
          List<Card> allCards = cardService.findAll();
          for (DeckDTO deckDTO : decksFromFile) {
            Wrestler wrestler = wrestlers.get(deckDTO.getWrestler());
            if (wrestler == null) {
              continue;
            }
            // Try to find an existing deck for this wrestler
            List<Deck> byWrestler = deckService.findByWrestler(wrestler);
            Deck deck;
            if (byWrestler.isEmpty()) {
              deck = deckService.createDeck(wrestler);
            } else {
              continue; // Skip if a deck already exists for this wrestler
            }
            deck.setWrestler(wrestler);
            deck.getCards().forEach(deckCardService::delete);
            for (DeckCardDTO cardDTO : deckDTO.getCards()) {
              Card card =
                  allCards.stream()
                      .filter(
                          c ->
                              c.getNumber() == cardDTO.getNumber()
                                  && c.getSet().getName().equals(cardDTO.getSet()))
                      .findFirst()
                      .orElse(null);
              if (card == null) {
                logger.warn(
                    "Card not found: {} in set {} from deck {}",
                    cardDTO.getNumber(),
                    cardDTO.getSet(),
                    wrestler.getName());
                continue;
              }
              deck.addCard(card, cardDTO.getAmount());
            }
            deckService.save(deck);
            logger.info("Saved deck for wrestler: {}", wrestler.getName());
          }
        }
      }
    };
  }

  @Bean
  @Order(5)
  public ApplicationRunner syncShowTypesFromFile(ShowTypeService showTypeService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("show_types.json");
      if (resource.exists()) {
        logger.info("Loading show types from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var showTypesFromFile = mapper.readValue(is, new TypeReference<List<ShowType>>() {});
          Map<String, ShowType> existing =
              showTypeService.findAll().stream()
                  .collect(Collectors.toMap(ShowType::getName, s -> s));
          for (ShowType st : showTypesFromFile) {
            ShowType existingType = existing.get(st.getName());
            if (existingType == null) {
              showTypeService.save(st);
              logger.info("Saved new show type: {}", st.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(6)
  public ApplicationRunner syncShowsFromFile(
      ShowService showService, ShowTypeService showTypeService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("shows.json");
      if (resource.exists()) {
        logger.info("Loading shows from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var showsFromFile = mapper.readValue(is, new TypeReference<List<ShowDTO>>() {});
          Map<String, ShowType> showTypes =
              showTypeService.findAll().stream()
                  .collect(Collectors.toMap(ShowType::getName, s -> s));
          for (ShowDTO dto : showsFromFile) {
            ShowType type = showTypes.get(dto.getShowType());
            if (type == null) continue;
            Show show = showService.findByName(dto.getName()).orElseGet(Show::new);
            show.setName(dto.getName());
            show.setDescription(dto.getDescription());
            show.setType(type);
            showService.save(show);
            logger.info(
                "{} show: {} of type {}",
                show.getId() == null ? "Saved new" : "Updated existing",
                show.getName(),
                type.getName());
          }
        }
      }
    };
  }

  @Data
  public static class CardDTO {
    private String name;
    private String type;
    private int damage;
    private boolean finisher = false;
    private boolean signature = false;
    private boolean pin = false;
    private boolean taunt = false;
    private boolean recover = false;
    private int stamina;
    private int momentum;
    private int target;
    private int number;
    private String set;
  }

  // DTOs for deserialization
  @Data
  public static class DeckDTO {
    private String wrestler;
    private List<DeckCardDTO> cards;
  }

  @Data
  public static class DeckCardDTO {
    private int number;
    private String set;
    private int amount;
  }

  public static class ShowDTO {
    private String name;
    private String showType;
    private String description;

    // getters and setters
    public String getName() {
      return name;
    }

    public void setName(String title) {
      this.name = title;
    }

    public String getShowType() {
      return showType;
    }

    public void setShowType(String type) {
      this.showType = type;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
