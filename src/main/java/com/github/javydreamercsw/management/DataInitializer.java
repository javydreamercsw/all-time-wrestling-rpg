package com.github.javydreamercsw.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.dto.DeckCardDTO;
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
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class DataInitializer {

  public DataInitializer() {
    log.info("DataInitializer initialized.");
  }

  @Value("${data.initializer.enabled:true}")
  private boolean enabled;

  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private DeckCardRepository deckCardRepository;

  @Bean
  @Order(-1)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner loadSegmentRulesFromFile(
      @NonNull SegmentRuleService segmentRuleService) {
    return args -> {
      if (enabled) {
        ClassPathResource resource = new ClassPathResource("segment_rules.json");
        if (resource.exists()) {
          log.info("Loading segment rules from file: {}", resource.getPath());
          ObjectMapper mapper = new ObjectMapper();
          try (var is = resource.getInputStream()) {
            var segmentRulesFromFile =
                mapper.readValue(is, new TypeReference<List<SegmentRuleDTO>>() {});

            for (SegmentRuleDTO dto : segmentRulesFromFile) {
              // Only create if it's new
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
            log.info(
                "Segment rule loading completed - {} rules loaded", segmentRulesFromFile.size());
          } catch (java.io.IOException e) {
            log.error("Error loading segment rules from file", e);
          }
        } else {
          log.warn("Segment rules file not found: {}", resource.getPath());
        }
      }
    };
  }

  @Bean
  @Order(0)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner syncShowTypesFromFile(@NonNull ShowTypeService showTypeService) {
    return args -> {
      if (enabled) {
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
            showTypeRepository.flush();
          } catch (java.io.IOException e) {
            log.error("Error loading show types from file", e);
          }
        }
      }
    };
  }

  @Bean
  @Order(1)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner loadSegmentTypesFromFile(
      @NonNull SegmentTypeService segmentTypeService) {
    return args -> {
      if (enabled) {
        ClassPathResource resource = new ClassPathResource("segment_types.json");
        if (resource.exists()) {
          log.info("Loading segment types from file: {}", resource.getPath());
          ObjectMapper mapper = new ObjectMapper();
          try (var is = resource.getInputStream()) {
            var segmentTypesFromFile =
                mapper.readValue(is, new TypeReference<List<SegmentTypeDTO>>() {});

            for (SegmentTypeDTO dto : segmentTypesFromFile) {
              // Only create if it's new
              Optional<SegmentType> existingType = segmentTypeService.findByName(dto.getName());
              if (existingType.isEmpty()) {
                SegmentType segmentType =
                    segmentTypeService.createOrUpdateSegmentType(
                        dto.getName(), dto.getDescription());
                log.info(
                    "Loaded segment type: {} (Players: {})",
                    segmentType.getName(),
                    dto.isUnlimited() ? "Unlimited" : dto.getPlayerAmount());
              } else {
                log.debug("Segment type {} already exists, skipping creation.", dto.getName());
              }
            }

            log.info("Segment type loading completed");
          } catch (java.io.IOException e) {
            log.error("Error loading segment types from file", e);
          }
        } else {
          log.warn("Segment types file not found: {}", resource.getPath());
        }
      }
    };
  }

  @Bean
  @Order(2)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner loadShowTemplatesFromFile(
      @NonNull ShowTemplateService showTemplateService) {
    return args -> {
      if (enabled) {
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
              "Show templates table is empty - loading templates from file: {}",
              resource.getPath());
          ObjectMapper mapper = new ObjectMapper();
          try (var is = resource.getInputStream()) {
            var templatesFromFile =
                mapper.readValue(is, new TypeReference<List<ShowTemplateDTO>>() {});

            for (ShowTemplateDTO dto : templatesFromFile) {
              var template =
                  showTemplateService.createOrUpdateTemplate(
                      dto.getName(),
                      dto.getDescription(),
                      dto.getShowTypeName(),
                      dto.getNotionUrl());
              if (template != null) {
                log.info(
                    "Loaded show template: {} (Type: {})",
                    template.getName(),
                    dto.getShowTypeName());
              } else {
                log.warn(
                    "Failed to load show template: {} - show type not found: {}",
                    dto.getName(),
                    dto.getShowTypeName());
              }
            }
            showTemplateRepository.flush();
            log.info(
                "Show template loading completed - {} templates processed",
                templatesFromFile.size());
          } catch (java.io.IOException e) {
            log.error("Error loading show templates from file", e);
          }
        } else {
          log.warn("Show templates file not found: {}", resource.getPath());
        }
      }
    };
  }

  @Bean
  @Order(3)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner syncSetsFromFile(@NonNull CardSetService cardSetService) {
    return args -> {
      if (enabled) {
        ClassPathResource resource = new ClassPathResource("sets.json");
        if (resource.exists()) {
          log.info("Loading card sets from file: {}", resource.getPath());
          // Load card sets from JSON file
          ObjectMapper mapper = new ObjectMapper();
          try (var is = resource.getInputStream()) {
            var setsFromFile = mapper.readValue(is, new TypeReference<List<CardSet>>() {});
            for (CardSet c : setsFromFile) {
              Optional<CardSet> existingSetOpt = cardSetService.findBySetCode(c.getSetCode());
              if (existingSetOpt.isPresent()) {
                CardSet existingSet = existingSetOpt.get();
                // Update fields
                existingSet.setName(c.getName());
                cardSetService.save(existingSet);
                log.info("Updated existing card set: {}", existingSet.getName());
              } else {
                cardSetService.save(c);
                log.info("Saved new card set: {}", c.getName());
              }
            }
          } catch (java.io.IOException e) {
            log.error("Error loading card sets from file", e);
          }
        }
      }
    };
  }

  @Bean
  @Order(4)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner syncCardsFromFile(
      @NonNull CardService cardService, @NonNull CardSetService cardSetService) {
    return args -> {
      if (enabled) {
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
                            c ->
                                c.getSet().getSetCode()
                                    + "#"
                                    + c.getNumber(), // Unique key: set code + number
                            c -> c,
                            (existingCard, duplicateCard) -> existingCard));

            for (CardDTO dto : cardsFromFile) {
              Optional<CardSet> setOpt = cardSetService.findBySetCode(dto.getSet());
              if (setOpt.isEmpty()) {
                log.warn(
                    "CardSet with code {} not found for card {}. Skipping card.",
                    dto.getSet(),
                    dto.getName());
                continue;
              }
              CardSet set = setOpt.get();
              final String key = dto.getSet() + "#" + dto.getNumber();
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
          } catch (java.io.IOException e) {
            log.error("Error loading cards from file", e);
          }
        }
      }
    };
  }

  @Bean
  @Order(5)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner syncWrestlersFromFile(@NonNull WrestlerService wrestlerService) {
    return args -> {
      if (enabled) {
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
            log.info("Total wrestlers in database after sync: {}", wrestlerService.count());
          } catch (java.io.IOException e) {
            log.error("Error loading wrestlers from file", e);
          }
        }
      }
    };
  }

  @Bean
  @Order(6)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner syncChampionshipsFromFile(@NonNull TitleService titleService) {
    return args -> {
      if (enabled) {
        ClassPathResource resource = new ClassPathResource("championships.json");
        if (resource.exists()) {
          log.info("Loading championships from file: {}", resource.getPath());
          ObjectMapper mapper = new ObjectMapper();
          try (var is = resource.getInputStream()) {
            var championshipsFromFile =
                mapper.readValue(is, new TypeReference<List<TitleDTO>>() {});
            for (TitleDTO dto : championshipsFromFile) {
              Optional<Title> existingTitle = titleService.findByName(dto.getName());
              Title title;
              if (existingTitle.isEmpty()) {
                title =
                    titleService.createTitle(dto.getName(), dto.getDescription(), dto.getTier());
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
                    titleService.awardTitleTo(title, List.of(championOpt.get()));
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
              } else {
                // If no champion is specified in DTO leave it as it is currently.
                log.info("Leaving title {} as is in the database.", title.getName());
              }
            }
          } catch (java.io.IOException e) {
            log.error("Error loading championships from file", e);
          }
        }
      }
    };
  }

  @Bean
  @Order(7)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ApplicationRunner syncDecksFromFile(
      @NonNull CardService cardService,
      @NonNull WrestlerService wrestlerService,
      @NonNull DeckService deckService,
      @NonNull DeckCardService deckCardService) {
    return args -> {
      if (enabled) {
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
              if (!byWrestler.isEmpty()) {
                deck = byWrestler.get(0); // Get the existing deck
              } else {
                deck = deckService.createDeck(wrestler);
              }

              // Keep track of cards to be removed
              Set<DeckCard> cardsToRemove = new HashSet<>(deck.getCards());

              // Aggregate cards from DTO
              Map<String, Integer> cardKeyToAmount = new java.util.HashMap<>();
              Map<String, Card> cardKeyToCard = new java.util.HashMap<>();
              for (DeckCardDTO cardDTO : deckDTO.getCards()) {
                log.debug(
                    "Looking for: {} in set {} from deck {}",
                    cardDTO.getNumber(),
                    cardDTO.getSet(),
                    wrestler.getName());
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
                String key = card.getSet().getName() + "-" + card.getId();
                cardKeyToAmount.merge(key, cardDTO.getAmount(), Integer::sum);
                cardKeyToCard.putIfAbsent(key, card);
              }

              for (var entry : cardKeyToAmount.entrySet()) {
                Card card = cardKeyToCard.get(entry.getKey());
                int amount = entry.getValue();

                Optional<DeckCard> existingDeckCardOpt =
                    deck.getCards().stream()
                        .filter(
                            dc -> dc.getCard().equals(card) && dc.getSet().equals(card.getSet()))
                        .findFirst();

                if (existingDeckCardOpt.isPresent()) {
                  DeckCard existingDeckCard = existingDeckCardOpt.get();
                  existingDeckCard.setAmount(amount);
                  cardsToRemove.remove(existingDeckCard); // Don't remove this card
                  log.debug(
                      "Updated existing deck card: {} {}-{} set (Amount: {})",
                      card.getName(),
                      card.getSet().getName(),
                      card.getId(),
                      amount);
                } else {
                  log.debug(
                      "Adding new deck card: {} {}-{} set (Amount: {})",
                      card.getName(),
                      card.getSet().getName(),
                      card.getId(),
                      amount);
                  DeckCard newDeckCard = new DeckCard();
                  newDeckCard.setCard(card);
                  newDeckCard.setSet(card.getSet());
                  newDeckCard.setAmount(amount);
                  newDeckCard.setDeck(deck); // Set the back-reference
                  deck.getCards().add(newDeckCard); // Add to the collection
                }
              }

              // Remove cards that are no longer in the DTO
              deck.getCards().removeAll(cardsToRemove);

              deckService.save(deck); // Save the deck, which will cascade to the new cards.
              log.info("Saved deck for wrestler: {}", wrestler.getName());
            }
          } catch (java.io.IOException e) {
            log.error("Error loading decks from file", e);
          }
        }
      }
    };
  }
}
