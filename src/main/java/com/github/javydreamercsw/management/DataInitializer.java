package com.github.javydreamercsw.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckCardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
public class DataInitializer {

  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerService wrestlerService;

  @Bean
  @Order(-1)
  public ApplicationRunner loadSegmentRulesFromFile(
      @NonNull SegmentRuleService segmentRuleService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("segment_rules.json");
      if (resource.exists()) {
        log.info("Loading segment rules from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var segmentRulesFromFile =
              mapper.readValue(is, new TypeReference<List<SegmentRuleDTO>>() {});

          for (SegmentRuleDTO dto : segmentRulesFromFile) {
            // Only create if it doesn't exist
            Optional<SegmentRule> existingRule = segmentRuleService.findByName(dto.getName());
            if (existingRule.isEmpty()) {
              segmentRuleService.createOrUpdateRule(
                  dto.getName(), dto.getDescription(), dto.isRequiresHighHeat());
              log.info(
                  "Loaded segment rule: {} (High Heat: {})",
                  dto.getName(),
                  dto.isRequiresHighHeat());
            } else {
              log.debug("Segment rule {} already exists, skipping creation.", dto.getName());
            }
          }
          log.info("Segment rule loading completed - {} rules loaded", segmentRulesFromFile.size());
        } catch (Exception e) {
          log.error("Error loading segment rules from file", e);
        }
      } else {
        log.warn("Segment rules file not found: {}", resource.getPath());
      }
    };
  }

  @Bean
  @Order(0)
  public ApplicationRunner syncShowTypesFromFile(@NonNull ShowTypeService showTypeService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("show_types.json");
      if (resource.exists()) {
        log.info("Loading show types from file: {}", resource.getPath());
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
              log.info("Saved new show type: {}", st.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(1)
  public ApplicationRunner loadSegmentTypesFromFile(
      @NonNull SegmentTypeService segmentTypeService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("segment_types.json");
      if (resource.exists()) {
        log.info("Loading segment types from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var segmentTypesFromFile =
              mapper.readValue(is, new TypeReference<List<SegmentTypeDTO>>() {});

          for (SegmentTypeDTO dto : segmentTypesFromFile) {
            // Only create if it doesn't exist
            Optional<SegmentType> existingType = segmentTypeService.findByName(dto.getName());
            if (existingType.isEmpty()) {
              SegmentType segmentType =
                  segmentTypeService.createOrUpdateSegmentType(dto.getName(), dto.getDescription());
              log.info(
                  "Loaded segment type: {} (Players: {})",
                  segmentType.getName(),
                  dto.isUnlimited() ? "Unlimited" : dto.getPlayerAmount());
            } else {
              log.debug("Segment type {} already exists, skipping creation.", dto.getName());
            }
          }

          log.info("Segment type loading completed");
        } catch (Exception e) {
          log.error("Error loading segment types from file", e);
        }
      } else {
        log.warn("Segment types file not found: {}", resource.getPath());
      }
    };
  }

  @Bean
  @Order(2)
  public ApplicationRunner loadShowTemplatesFromFile(
      @NonNull ShowTemplateService showTemplateService) {
    return args -> {
      // Only load show templates from file if the table is empty
      long existingTemplatesCount = showTemplateService.count();
      if (existingTemplatesCount > 0) {
        log.info(
            "Show templates table already contains {} templates - skipping file import",
            existingTemplatesCount);
        return;
      }

      ClassPathResource resource = new ClassPathResource("show_templates.json");
      if (resource.exists()) {
        log.info(
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
              log.info(
                  "Loaded show template: {} (Type: {})", template.getName(), dto.getShowTypeName());
            } else {
              log.warn(
                  "Failed to load show template: {} - show type not found: {}",
                  dto.getName(),
                  dto.getShowTypeName());
            }
          }
          showTemplateRepository.flush();
          log.info(
              "Show template loading completed - {} templates processed", templatesFromFile.size());
        } catch (Exception e) {
          log.error("Error loading show templates from file", e);
        }
      } else {
        log.warn("Show templates file not found: {}", resource.getPath());
      }
    };
  }

  @Bean
  @Order(3)
  public ApplicationRunner syncSetsFromFile(@NonNull CardSetService cardSetService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("sets.json");
      if (resource.exists()) {
        log.info("Loading card sets from file: {}", resource.getPath());
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
              log.info("Updated existing card set: {}", existingSet.getName());
            } else {
              cardSetService.save(c);
              log.info("Saved new card set: {}", c.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(4)
  public ApplicationRunner syncCardsFromFile(
      @NonNull CardService cardService, @NonNull CardSetService cardSetService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("cards.json");
      if (resource.exists()) {
        log.info("Loading cards from file: {}", resource.getPath());
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
              log.info("Updated existing card: {}", card.getName());
            } else {
              log.info("Saved new card: {}", card.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(5)
  public ApplicationRunner syncWrestlersFromFile(@NonNull WrestlerService wrestlerService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("wrestlers.json");
      if (resource.exists()) {
        log.info("Loading wrestlers from file: {}", resource.getPath());
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

              if (w.getExternalId() != null) {
                existingWrestler.setExternalId(w.getExternalId());
              }

              wrestlerService.save(existingWrestler);
              log.info("Updated existing wrestler: {}", existingWrestler.getName());
            } else {
              wrestlerService.save(w);
              log.info("Saved new wrestler: {}", w.getName());
            }
          }
        }
      }
    };
  }

  @Bean
  @Order(6)
  public ApplicationRunner syncChampionshipsFromFile(@NonNull TitleService titleService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("championships.json");
      if (resource.exists()) {
        log.info("Loading championships from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var championshipsFromFile = mapper.readValue(is, new TypeReference<List<TitleDTO>>() {});
          for (TitleDTO dto : championshipsFromFile) {
            Optional<Title> existingTitle = titleService.findByName(dto.getName());
            Title title;
            if (existingTitle.isEmpty()) {
              title = titleService.createTitle(dto.getName(), dto.getDescription(), dto.getTier());
              log.info("Created new title: {}", dto.getName());
            } else {
              title = existingTitle.get();
              log.debug("Title {} already exists, skipping creation.", dto.getName());
            }

            // Award title if currentChampionName is provided
            if (dto.getCurrentChampionName() != null
                && !dto.getCurrentChampionName().trim().isEmpty()) {
              Optional<Wrestler> championOpt =
                  wrestlerService.findByName(dto.getCurrentChampionName());
              if (championOpt.isPresent()) {
                // Check if the title is already held by this champion
                if (title.getCurrentChampions().isEmpty()
                    || !title.getCurrentChampions().contains(championOpt.get())) {
                  titleService.awardTitle(title.getId(), championOpt.get().getId());
                  log.info(
                      "Awarded title {} to champion {}",
                      title.getName(),
                      dto.getCurrentChampionName());
                } else {
                  log.debug(
                      "Title {} already held by champion {}",
                      title.getName(),
                      dto.getCurrentChampionName());
                }
              } else {
                log.warn(
                    "Champion '{}' not found for title '{}'. Title will remain vacant.",
                    dto.getCurrentChampionName(),
                    dto.getName());
              }
            } else if (!title.isVacant()) {
              // If no champion is specified in DTO but title is not vacant, vacate it
              titleService.vacateTitle(title.getId());
              log.info("Vacated title {} as no champion was specified in DTO.", title.getName());
            }
          }
        } catch (Exception e) {
          log.error("Error loading championships from file", e);
        }
      }
    };
  }

  @Bean
  @Order(7)
  public ApplicationRunner syncDecksFromFile(
      @NonNull CardService cardService,
      @NonNull WrestlerService wrestlerService,
      @NonNull DeckService deckService,
      @NonNull DeckCardService deckCardService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("decks.json");
      if (resource.exists()) {
        log.info("Loading decks from file: {}", resource.getPath());
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var decksFromFile = mapper.readValue(is, new TypeReference<List<DeckDTO>>() {});
          Map<String, Wrestler> wrestlers =
              wrestlerService.findAll().stream()
                  .collect(
                      Collectors.toMap(
                          Wrestler::getName, w -> w, (existing1, existing2) -> existing1));
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
                  cardService
                      .findByNumberAndSet(cardDTO.getNumber(), cardDTO.getSet())
                      .orElse(null);
              if (card == null) {
                log.warn(
                    "Card not found: {} in set {} from deck {}",
                    cardDTO.getNumber(),
                    cardDTO.getSet(),
                    wrestler.getName());
                continue;
              }
              deck.addCard(card, cardDTO.getAmount());
            }
            deckService.save(deck);
            log.info("Saved deck for wrestler: {}", wrestler.getName());
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

  @Data
  public static class TitleDTO {
    private String name;
    private String description;
    private WrestlerTier tier;
    private Gender gender;
    private String currentChampionName;
  }
}
