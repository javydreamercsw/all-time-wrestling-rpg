package com.github.javydreamercsw.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.MatchRuleDTO;
import com.github.javydreamercsw.management.dto.MatchTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckCardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.match.MatchRuleService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Pageable;

@Configuration
@Profile("!test")
public class DataInitializer {
  private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

  @Bean
  @Order(-1)
  public ApplicationRunner loadMatchRulesFromFile(MatchRuleService matchRuleService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("match_rules.json");
      if (resource.exists()) {
        logger.info("Loading match rules from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var matchRulesFromFile = mapper.readValue(is, new TypeReference<List<MatchRuleDTO>>() {});

          for (MatchRuleDTO dto : matchRulesFromFile) {
            matchRuleService.createOrUpdateRule(
                dto.getName(), dto.getDescription(), dto.isRequiresHighHeat());
            logger.info(
                "Loaded match rule: {} (High Heat: {})", dto.getName(), dto.isRequiresHighHeat());
          }

          logger.info("Match rule loading completed - {} rules loaded", matchRulesFromFile.size());
        } catch (Exception e) {
          logger.error("Error loading match rules from file", e);
        }
      } else {
        logger.warn("Match rules file not found: {}", resource.getPath());
      }
    };
  }

  @Bean
  @Order(1)
  public ApplicationRunner loadShowTemplatesFromFile(ShowTemplateService showTemplateService) {
    return args -> {
      // Only load show templates from file if the table is empty
      long existingTemplatesCount = showTemplateService.count();
      if (existingTemplatesCount > 0) {
        logger.info(
            "Show templates table already contains {} templates - skipping file import",
            existingTemplatesCount);
        return;
      }

      ClassPathResource resource = new ClassPathResource("show_templates.json");
      if (resource.exists()) {
        logger.info(
            "Show templates table is empty - loading templates from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var templatesFromFile =
              mapper.readValue(is, new TypeReference<List<ShowTemplateDTO>>() {});

          for (ShowTemplateDTO dto : templatesFromFile) {
            var template =
                showTemplateService.createOrUpdateTemplate(
                    dto.getName(), dto.getDescription(), dto.getShowTypeName(), dto.getNotionUrl());
            if (template != null) {
              logger.info(
                  "Loaded show template: {} (Type: {})", template.getName(), dto.getShowTypeName());
            } else {
              logger.warn(
                  "Failed to load show template: {} - show type not found: {}",
                  dto.getName(),
                  dto.getShowTypeName());
            }
          }

          logger.info(
              "Show template loading completed - {} templates processed", templatesFromFile.size());
        } catch (Exception e) {
          logger.error("Error loading show templates from file", e);
        }
      } else {
        logger.warn("Show templates file not found: {}", resource.getPath());
      }
    };
  }

  @Bean
  @Order(0)
  public ApplicationRunner loadMatchTypesFromFile(MatchTypeService matchTypeService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("match_types.json");
      if (resource.exists()) {
        logger.info("Loading match types from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var matchTypesFromFile = mapper.readValue(is, new TypeReference<List<MatchTypeDTO>>() {});

          for (MatchTypeDTO dto : matchTypesFromFile) {
            MatchType matchType =
                matchTypeService.createOrUpdateMatchType(dto.getName(), dto.getDescription());
            logger.info(
                "Loaded match type: {} (Players: {})",
                matchType.getName(),
                dto.isUnlimited() ? "Unlimited" : dto.getPlayerAmount());
          }

          logger.info("Match type loading completed");
        } catch (Exception e) {
          logger.error("Error loading match types from file", e);
        }
      } else {
        logger.warn("Match types file not found: {}", resource.getPath());
      }
    };
  }

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
          // Map existing wrestlers by name (handle duplicates by keeping the first one)
          Map<String, Wrestler> existing =
              wrestlerService.findAll().stream()
                  .collect(
                      Collectors.toMap(
                          Wrestler::getName, w -> w, (existing1, existing2) -> existing1));
          for (Wrestler w : wrestlersFromFile) {
            // Smart duplicate handling - prefer external ID, fallback to name
            Wrestler existingWrestler = null;
            if (w.getExternalId() != null && !w.getExternalId().trim().isEmpty()) {
              existingWrestler = wrestlerService.findByExternalId(w.getExternalId()).orElse(null);
            }
            if (existingWrestler == null) {
              existingWrestler = existing.get(w.getName());
            }

            if (existingWrestler != null) {
              // Update card game fields
              existingWrestler.setDeckSize(w.getDeckSize());
              existingWrestler.setStartingHealth(w.getStartingHealth());
              existingWrestler.setLowHealth(w.getLowHealth());
              existingWrestler.setStartingStamina(w.getStartingStamina());
              existingWrestler.setLowStamina(w.getLowStamina());

              // Update ATW RPG fields if they exist in the JSON
              if (w.getFans() != null) {
                existingWrestler.setFans(w.getFans());
              }
              if (w.getIsPlayer() != null) {
                existingWrestler.setIsPlayer(w.getIsPlayer());
              }
              if (w.getBumps() != null) {
                existingWrestler.setBumps(w.getBumps());
              }
              if (w.getFaction() != null) {
                existingWrestler.setFaction(w.getFaction());
              }
              if (w.getDescription() != null) {
                existingWrestler.setDescription(w.getDescription());
              }
              if (w.getWrestlingStyle() != null) {
                existingWrestler.setWrestlingStyle(w.getWrestlingStyle());
              }
              if (w.getExternalId() != null) {
                existingWrestler.setExternalId(w.getExternalId());
              }

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
                  .collect(
                      Collectors.toMap(
                          Wrestler::getName, w -> w, (existing1, existing2) -> existing1));
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

  @Bean
  @Order(6)
  public ApplicationRunner syncShowsFromFile(
      ShowService showService,
      ShowTypeService showTypeService,
      SeasonService seasonService,
      ShowTemplateService showTemplateService) {
    return args -> {
      // Only load shows from file if the shows table is empty
      long existingShowsCount = showService.count();
      if (existingShowsCount > 0) {
        logger.info(
            "Shows table already contains {} shows - skipping file import", existingShowsCount);
        return;
      }

      ClassPathResource resource = new ClassPathResource("shows.json");
      if (resource.exists()) {
        logger.info("Shows table is empty - loading shows from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try (var is = resource.getInputStream()) {
          var showsFromFile = mapper.readValue(is, new TypeReference<List<ShowDTO>>() {});

          // Cache lookups for performance
          Map<String, ShowType> showTypes =
              showTypeService.findAll().stream()
                  .collect(Collectors.toMap(ShowType::getName, s -> s));
          Map<String, Season> seasons =
              seasonService.getAllSeasons(Pageable.unpaged()).stream()
                  .collect(Collectors.toMap(Season::getName, s -> s));
          Map<String, ShowTemplate> templates =
              showTemplateService.findAll().stream()
                  .collect(Collectors.toMap(ShowTemplate::getName, t -> t));

          for (ShowDTO dto : showsFromFile) {
            ShowType type = showTypes.get(dto.getShowType());
            if (type == null) {
              logger.warn("Show type not found: {} for show: {}", dto.getShowType(), dto.getName());
              continue;
            }

            // Smart duplicate handling - prefer external ID, fallback to name
            Show show = null;
            if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
              show = showService.findByExternalId(dto.getExternalId()).orElse(null);
            }
            if (show == null) {
              show = showService.findByName(dto.getName()).orElseGet(Show::new);
            }

            show.setName(dto.getName());
            show.setDescription(dto.getDescription());
            show.setType(type);
            show.setExternalId(dto.getExternalId()); // Set external ID if provided

            // Set show date if provided
            if (dto.getShowDate() != null && !dto.getShowDate().trim().isEmpty()) {
              try {
                show.setShowDate(LocalDate.parse(dto.getShowDate()));
              } catch (Exception e) {
                logger.warn(
                    "Invalid date format for show {}: {}", dto.getName(), dto.getShowDate());
              }
            }

            // Set season if provided
            if (dto.getSeasonName() != null && !dto.getSeasonName().trim().isEmpty()) {
              Season season = seasons.get(dto.getSeasonName());
              if (season != null) {
                show.setSeason(season);
              } else {
                logger.warn(
                    "Season not found: {} for show: {}", dto.getSeasonName(), dto.getName());
              }
            }

            // Set template if provided
            if (dto.getTemplateName() != null && !dto.getTemplateName().trim().isEmpty()) {
              ShowTemplate template = templates.get(dto.getTemplateName());
              if (template != null) {
                show.setTemplate(template);
              } else {
                logger.warn(
                    "Template not found: {} for show: {}", dto.getTemplateName(), dto.getName());
              }
            }

            showService.save(show);
            logger.info(
                "{} show: {} (Date: {}, Season: {}, Template: {})",
                show.getId() == null ? "Saved new" : "Updated existing",
                show.getName(),
                show.getShowDate(),
                show.getSeason() != null ? show.getSeason().getName() : "None",
                show.getTemplate() != null ? show.getTemplate().getName() : "None");
          }

          logger.info("Show loading completed - {} shows processed", showsFromFile.size());
        } catch (Exception e) {
          logger.error("Error loading shows from file", e);
        }
      } else {
        logger.warn("Shows file not found: {}", resource.getPath());
      }
    };
  }

  @Data
  public static class DeckCardDTO {
    private int number;
    private String set;
    private int amount;
  }

  @Data
  public static class ShowDTO {
    private String name;
    private String showType;
    private String description;
    private String showDate; // ISO date format (YYYY-MM-DD)
    private String seasonName; // Reference to season by name
    private String templateName; // Reference to show template by name
    private String externalId; // External system ID (e.g., Notion page ID)
  }
}
