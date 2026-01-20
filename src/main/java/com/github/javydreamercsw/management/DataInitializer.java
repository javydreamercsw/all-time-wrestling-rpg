/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.CampaignAbilityCardDTO;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.dto.DeckCardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.dto.FactionImportDTO;
import com.github.javydreamercsw.management.dto.NpcDTO;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.dto.TitleDTO;
import com.github.javydreamercsw.management.dto.WrestlerImportDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class DataInitializer implements com.github.javydreamercsw.base.Initializable {

  private final boolean enabled;
  private final ShowTemplateService showTemplateService;
  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final ShowTypeService showTypeService;
  private final SegmentRuleService segmentRuleService;
  private final SegmentTypeService segmentTypeService;
  private final CardSetService cardSetService;
  private final CardService cardService;
  private final TitleService titleService;
  private final DeckService deckService;
  private final GameSettingService gameSettingService;
  private final NpcService npcService;
  private final FactionService factionService;
  private final TeamService teamService;
  private final TeamRepository teamRepository;
  private final CampaignAbilityCardService campaignAbilityCardService;

  @Autowired
  public DataInitializer(
      @Value("${data.initializer.enabled:true}") boolean enabled,
      ShowTemplateService showTemplateService,
      @Lazy WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      ShowTypeService showTypeService,
      SegmentRuleService segmentRuleService,
      SegmentTypeService segmentTypeService,
      CardSetService cardSetService,
      CardService cardService,
      TitleService titleService,
      DeckService deckService,
      GameSettingService gameSettingService,
      NpcService npcService,
      FactionService factionService,
      TeamService teamService,
      TeamRepository teamRepository,
      CampaignAbilityCardService campaignAbilityCardService) {
    this.enabled = enabled;
    this.showTemplateService = showTemplateService;
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.showTypeService = showTypeService;
    this.segmentRuleService = segmentRuleService;
    this.segmentTypeService = segmentTypeService;
    this.cardSetService = cardSetService;
    this.cardService = cardService;
    this.titleService = titleService;
    this.deckService = deckService;
    this.gameSettingService = gameSettingService;
    this.npcService = npcService;
    this.factionService = factionService;
    this.teamService = teamService;
    this.teamRepository = teamRepository;
    this.campaignAbilityCardService = campaignAbilityCardService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void init() {
    if (enabled) {
      initializeGameDate();
      loadSegmentRulesFromFile();
      syncShowTypesFromFile();
      loadSegmentTypesFromFile();
      loadShowTemplatesFromFile();
      syncSetsFromFile();
      syncCardsFromFile();
      syncWrestlersFromFile();
      syncChampionshipsFromFile();
      syncDecksFromFile();
      syncNpcsFromFile();
      syncFactionsFromFile();
      syncTeamsFromFile();
      syncCampaignAbilityCardsFromFile();
    }
  }

  private void syncCampaignAbilityCardsFromFile() {
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (resource.exists()) {
      log.info("Loading campaign ability cards from file: {}", resource.getPath());
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var cardsFromFile =
            mapper.readValue(is, new TypeReference<List<CampaignAbilityCardDTO>>() {});
        for (CampaignAbilityCardDTO dto : cardsFromFile) {
          campaignAbilityCardService.createOrUpdateCard(
              dto.getName(),
              dto.getDescription(),
              dto.getAlignmentType(),
              dto.getLevel(),
              dto.isOneTimeUse(),
              dto.getTiming(),
              dto.getEffectScript(),
              dto.getSecondaryEffectScript(),
              dto.isSecondaryOneTimeUse(),
              dto.getSecondaryTiming());
          log.debug("Loaded campaign ability card: {}", dto.getName());
        }
        log.info("Campaign ability card loading completed - {} cards loaded", cardsFromFile.size());
      } catch (IOException e) {
        log.error("Error loading campaign ability cards from file", e);
      }
    } else {
      log.warn("Campaign ability cards file not found: {}", resource.getPath());
    }
  }

  private void loadSegmentRulesFromFile() {
    ClassPathResource resource = new ClassPathResource("segment_rules.json");
    if (resource.exists()) {
      log.debug("Loading segment rules from file: {}", resource.getPath());
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var segmentRulesFromFile =
            mapper.readValue(is, new TypeReference<List<SegmentRuleDTO>>() {});

        for (SegmentRuleDTO dto : segmentRulesFromFile) {
          segmentRuleService.createOrUpdateRule(
              dto.getName(), dto.getDescription(), dto.isRequiresHighHeat(), dto.getBumpAddition());
          log.info(
              "Loaded segment rule: {} (High Heat: {}, Bump Addition: {})",
              dto.getName(),
              dto.isRequiresHighHeat(),
              dto.getBumpAddition());
        }
        log.info("Segment rule loading completed - {} rules loaded", segmentRulesFromFile.size());
      } catch (IOException e) {
        log.error("Error loading segment rules from file", e);
      }
    } else {
      log.warn("Segment rules file not found: {}", resource.getPath());
    }
  }

  private void syncShowTypesFromFile() {
    ClassPathResource resource = new ClassPathResource("show_types.json");
    if (resource.exists()) {
      log.info("Loading show types from file: {}", resource.getPath());
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var showTypesFromFile = mapper.readValue(is, new TypeReference<List<ShowType>>() {});
        for (ShowType st : showTypesFromFile) {
          showTypeService.createOrUpdateShowType(
              st.getName(), st.getDescription(), st.getExpectedMatches(), st.getExpectedPromos());
          log.debug(
              "Loaded show type: {} (Expected Matches: {}, Expected Promos: {})",
              st.getName(),
              st.getExpectedMatches(),
              st.getExpectedPromos());
        }
        log.info("Show type loading completed - {} types loaded", showTypesFromFile.size());
      } catch (IOException e) {
        log.error("Error loading show types from file", e);
      }
    } else {
      log.warn("Show types file not found: {}", resource.getPath());
    }
  }

  private void loadSegmentTypesFromFile() {
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
                segmentTypeService.createOrUpdateSegmentType(dto.getName(), dto.getDescription());
            log.debug(
                "Loaded segment type: {} (Players: {})",
                segmentType.getName(),
                dto.isUnlimited() ? "Unlimited" : dto.getPlayerAmount());
          } else {
            log.debug("Segment type {} already exists, skipping creation.", dto.getName());
          }
        }

        log.info("Segment type loading completed");
      } catch (IOException e) {
        log.error("Error loading segment types from file", e);
      }
    } else {
      log.warn("Segment types file not found: {}", resource.getPath());
    }
  }

  private void loadShowTemplatesFromFile() {
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
        var templatesFromFile = mapper.readValue(is, new TypeReference<List<ShowTemplateDTO>>() {});

        for (ShowTemplateDTO dto : templatesFromFile) {
          var template =
              showTemplateService.createOrUpdateTemplate(
                  dto.getName(), dto.getDescription(), dto.getShowTypeName(), dto.getNotionUrl());
          if (template != null) {
            log.debug(
                "Loaded show template: {} (Type: {})", template.getName(), dto.getShowTypeName());
          } else {
            log.warn(
                "Failed to load show template: {} - show type not found: {}",
                dto.getName(),
                dto.getShowTypeName());
          }
        }
      } catch (IOException e) {
        log.error("Error loading show templates from file", e);
      }
    } else {
      log.warn("Show templates file not found: {}", resource.getPath());
    }
  }

  private void syncSetsFromFile() {
    ClassPathResource resource = new ClassPathResource("sets.json");
    if (resource.exists()) {
      log.info("Loading card sets from file: {}", resource.getPath());
      // Load card sets from JSON file
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var setsFromFile = mapper.readValue(is, new TypeReference<List<CardSet>>() {});
        for (CardSet c : setsFromFile) {
          Optional<CardSet> existingSetOpt = cardSetService.findBySetCode(c.getCode());
          if (existingSetOpt.isPresent()) {
            CardSet existingSet = existingSetOpt.get();
            // Update fields
            existingSet.setName(c.getName());
            cardSetService.save(existingSet);
            log.debug("Updated existing card set: {}", existingSet.getName());
          } else {
            cardSetService.save(c);
            log.debug("Saved new card set: {}", c.getName());
          }
        }
      } catch (IOException e) {
        log.error("Error loading card sets from file", e);
      }
    }
  }

  private void syncCardsFromFile() {
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
                            c.getSet().getCode()
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
            log.debug("Updated existing card: {}", card.getName());
          } else {
            log.debug("Saved new card: {}", card.getName());
          }
        }
      } catch (IOException e) {
        log.error("Error loading cards from file", e);
      }
    }
  }

  protected void syncWrestlersFromFile() {
    ClassPathResource resource = new ClassPathResource("wrestlers.json");
    if (resource.exists()) {
      log.info("Loading wrestlers from file: {}", resource.getPath());
      // Load wrestlers from JSON file
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var wrestlersFromFile =
            mapper.readValue(is, new TypeReference<List<WrestlerImportDTO>>() {});
        // Map existing wrestlers by name (handle duplicates by keeping the first one)
        Map<String, Wrestler> existing =
            wrestlerRepository.findAll().stream()
                .collect(
                    Collectors.toMap(
                        Wrestler::getName, w -> w, (existing1, existing2) -> existing1));
        for (WrestlerImportDTO w : wrestlersFromFile) {
          // Smart duplicate handling - prefer external ID, fallback to name
          Wrestler existingWrestler = null;
          if (w.getExternalId() != null && !w.getExternalId().trim().isEmpty()) {
            existingWrestler = wrestlerRepository.findByExternalId(w.getExternalId()).orElse(null);
          }
          if (existingWrestler == null) {
            existingWrestler = existing.get(w.getName());
          }

          if (existingWrestler != null) {
            // Update fields
            existingWrestler.setDeckSize(w.getDeckSize());
            existingWrestler.setStartingHealth(w.getStartingHealth());
            existingWrestler.setLowHealth(w.getLowHealth());
            existingWrestler.setStartingStamina(w.getStartingStamina());
            existingWrestler.setLowStamina(w.getLowStamina());
            existingWrestler.setDescription(w.getDescription());
            existingWrestler.setGender(w.getGender());

            if (w.getFans() != null) {
              if (w.getFans() > existingWrestler.getFans()) {
                existingWrestler.setFans(w.getFans());
              }
            }

            if (w.getBumps() != null) {
              if (w.getBumps() > existingWrestler.getBumps()) {
                existingWrestler.setBumps(w.getBumps());
              }
            }

            if (w.getImageUrl() != null) {
              existingWrestler.setImageUrl(w.getImageUrl());
            }

            if (w.getExternalId() != null) {
              existingWrestler.setExternalId(w.getExternalId());
            }

            if (w.getManager() != null) {
              Npc manager = npcService.findByName(w.getManager());
              if (manager != null) {
                existingWrestler.setManager(manager);
              }
            }

            if (w.getDrive() != null) existingWrestler.setDrive(w.getDrive());
            if (w.getResilience() != null) existingWrestler.setResilience(w.getResilience());
            if (w.getCharisma() != null) existingWrestler.setCharisma(w.getCharisma());
            if (w.getBrawl() != null) existingWrestler.setBrawl(w.getBrawl());

            wrestlerRepository.save(existingWrestler);
            log.debug("Updated existing wrestler: {}", existingWrestler.getName());
          } else {
            Wrestler newWrestler = new Wrestler();
            newWrestler.setName(w.getName());
            newWrestler.setDeckSize(w.getDeckSize());
            newWrestler.setStartingHealth(w.getStartingHealth());
            newWrestler.setLowHealth(w.getLowHealth());
            newWrestler.setStartingStamina(w.getStartingStamina());
            newWrestler.setLowStamina(w.getLowStamina());
            newWrestler.setDescription(w.getDescription());
            newWrestler.setGender(w.getGender());
            newWrestler.setFans(w.getFans());
            newWrestler.setBumps(w.getBumps());
            newWrestler.setImageUrl(w.getImageUrl());
            newWrestler.setTier(WrestlerTier.ROOKIE);
            if (w.getExternalId() != null) {
              newWrestler.setExternalId(w.getExternalId());
            }
            if (w.getManager() != null) {
              Npc manager = npcService.findByName(w.getManager());
              if (manager != null) {
                newWrestler.setManager(manager);
              }
            }
            if (w.getDrive() != null) newWrestler.setDrive(w.getDrive());
            if (w.getResilience() != null) newWrestler.setResilience(w.getResilience());
            if (w.getCharisma() != null) newWrestler.setCharisma(w.getCharisma());
            if (w.getBrawl() != null) newWrestler.setBrawl(w.getBrawl());

            wrestlerRepository.save(newWrestler);
            log.debug("Saved new wrestler: {}", newWrestler.getName());
          }
        }
      } catch (IOException e) {
        log.error("Error loading wrestlers from file", e);
      }
    }
  }

  private void syncChampionshipsFromFile() {
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
            title =
                titleService.createTitle(
                    dto.getName(), dto.getDescription(), dto.getTier(), dto.getChampionshipType());
            log.debug(
                "Created new title: {} with type: {}", dto.getName(), dto.getChampionshipType());
          } else {
            title = existingTitle.get();
            log.debug("Title {} already exists, skipping creation.", dto.getName());
          }
          title.setChampionshipType(dto.getChampionshipType());

          // Award title if currentChampionName is provided
          if (dto.getCurrentChampionName() != null
              && !dto.getCurrentChampionName().trim().isEmpty()) {
            Optional<Wrestler> championOpt =
                wrestlerRepository.findByName(dto.getCurrentChampionName());
            if (championOpt.isPresent()) {
              // Check if the title is already held by this champion
              if (title.getCurrentChampions().isEmpty()
                  || !title.getCurrentChampions().contains(championOpt.get())) {
                titleService.awardTitleTo(title, List.of(championOpt.get()));
                log.debug(
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
      } catch (IOException e) {
        log.error("Error loading championships from file", e);
      }
    }
  }

  private void syncDecksFromFile() {
    ClassPathResource resource = new ClassPathResource("decks.json");
    if (resource.exists()) {
      log.info("Loading decks from file: {}", resource.getPath());
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var decksFromFile = mapper.readValue(is, new TypeReference<List<DeckDTO>>() {});
        Map<String, Wrestler> wrestlers =
            wrestlerRepository.findAll().stream()
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
          Map<String, Integer> cardKeyToAmount = new HashMap<>();
          Map<String, Card> cardKeyToCard = new HashMap<>();
          Map<Long, CardSet> setCache = new HashMap<>(); // Cache for CardSet instances

          for (DeckCardDTO cardDTO : deckDTO.getCards()) {
            log.debug(
                "Looking for: {} in set {} from deck {}",
                cardDTO.getNumber(),
                cardDTO.getSet(),
                wrestler.getName());
            Card card =
                cardService.findByNumberAndSet(cardDTO.getNumber(), cardDTO.getSet()).orElse(null);
            if (card == null) {
              log.warn(
                  "Card not found: {} in set {} from deck {}",
                  cardDTO.getNumber(),
                  cardDTO.getSet(),
                  wrestler.getName());
              continue;
            }

            // Ensure we use a single instance of CardSet from our cache
            CardSet canonicalSet =
                setCache.computeIfAbsent(
                    card.getSet().getId(),
                    id -> {
                      log.debug("Caching CardSet: {}", card.getSet().getName());
                      return card.getSet();
                    });
            card.setSet(canonicalSet); // Replace with the cached instance

            String key = card.getSet().getName() + "-" + card.getId();
            cardKeyToAmount.merge(key, cardDTO.getAmount(), Integer::sum);
            cardKeyToCard.putIfAbsent(key, card);
          }

          for (var entry : cardKeyToAmount.entrySet()) {
            Card card = cardKeyToCard.get(entry.getKey());
            int amount = entry.getValue();

            Optional<DeckCard> existingDeckCardOpt =
                deck.getCards().stream()
                    .filter(dc -> dc.getCard().equals(card) && dc.getSet().equals(card.getSet()))
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
          log.debug("Saved deck for wrestler: {}", wrestler.getName());
        }
      } catch (IOException e) {
        log.error("Error loading decks from file", e);
      }
    }
  }

  private void initializeGameDate() {
    if (gameSettingService.findById(GameSettingService.CURRENT_GAME_DATE_KEY).isEmpty()) {
      log.info("In-game date not set. Initializing to current date.");
      gameSettingService.saveCurrentGameDate(LocalDate.now());
    }
  }

  private void syncNpcsFromFile() {
    ClassPathResource resource = new ClassPathResource("npcs.json");
    if (resource.exists()) {
      log.info("Loading npcs from file: {}", resource.getPath());
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var dtos = mapper.readValue(is, new TypeReference<List<NpcDTO>>() {});
        for (NpcDTO dto : dtos) {
          Npc npc = npcService.findByName(dto.getName());
          if (npc == null) {
            npc = new Npc();
            npc.setName(dto.getName());
          }
          npc.setDescription(dto.getDescription());
          npc.setNpcType(dto.getType());
          npcService.save(npc);
          log.debug("Loaded npc: {}", dto.getName());
        }
        log.info("Npc loading completed - {} npcs loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading npcs from file", e);
      }
    } else {
      log.warn("Npcs file not found: {}", resource.getPath());
    }
  }

  private void syncFactionsFromFile() {
    ClassPathResource resource = new ClassPathResource("factions.json");
    if (resource.exists()) {
      log.info("Loading factions from file: {}", resource.getPath());
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var dtos = mapper.readValue(is, new TypeReference<List<FactionImportDTO>>() {});
        for (FactionImportDTO dto : dtos) {
          Optional<Wrestler> leaderOpt = wrestlerRepository.findByName(dto.getLeader());
          if (leaderOpt.isPresent()) {
            Optional<Faction> factionOpt = factionService.getFactionByName(dto.getName());
            if (factionOpt.isEmpty()) {
              factionOpt =
                  factionService.createFaction(
                      dto.getName(), dto.getDescription(), leaderOpt.get().getId());
            }
            if (factionOpt.isPresent()) {
              Faction faction = factionOpt.get();
              for (String memberName : dto.getMembers()) {
                Optional<Wrestler> memberOpt = wrestlerRepository.findByName(memberName);
                if (memberOpt.isPresent()) {
                  factionService.addMemberToFaction(faction.getId(), memberOpt.get().getId());
                }
              }
              if (dto.getManager() != null) {
                Npc manager = npcService.findByName(dto.getManager());
                if (manager != null) {
                  faction.setManager(manager);
                  factionService.save(faction);
                }
              }
            }
          }
          log.debug("Loaded faction: {}", dto.getName());
        }
        log.info("Faction loading completed - {} factions loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading factions from file", e);
      }
    } else {
      log.warn("Factions file not found: {}", resource.getPath());
    }
  }

  private void syncTeamsFromFile() {
    ClassPathResource resource = new ClassPathResource("teams.json");
    if (resource.exists()) {
      log.info("Loading teams from file: {}", resource.getPath());
      ObjectMapper mapper = new ObjectMapper();
      try (var is = resource.getInputStream()) {
        var dtos = mapper.readValue(is, new TypeReference<List<TeamImportDTO>>() {});
        for (TeamImportDTO dto : dtos) {
          Optional<Wrestler> wrestler1Opt = wrestlerRepository.findByName(dto.getWrestler1());
          Optional<Wrestler> wrestler2Opt = wrestlerRepository.findByName(dto.getWrestler2());
          if (wrestler1Opt.isPresent() && wrestler2Opt.isPresent()) {
            Optional<Team> teamOpt = teamService.getTeamByName(dto.getName());
            if (teamOpt.isEmpty()) {
              teamOpt =
                  teamService.createTeam(
                      dto.getName(),
                      dto.getDescription(),
                      wrestler1Opt.get().getId(),
                      wrestler2Opt.get().getId(),
                      null,
                      null);
            }
            if (teamOpt.isPresent()) {
              Team team = teamOpt.get();
              if (dto.getManager() != null) {
                Npc manager = npcService.findByName(dto.getManager());
                if (manager != null) {
                  team.setManager(manager);
                  teamRepository.save(team);
                }
              }
            }
          }
          log.debug("Loaded team: {}", dto.getName());
        }
        log.info("Team loading completed - {} teams loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading teams from file", e);
      }
    } else {
      log.warn("Teams file not found: {}", resource.getPath());
    }
  }
}
