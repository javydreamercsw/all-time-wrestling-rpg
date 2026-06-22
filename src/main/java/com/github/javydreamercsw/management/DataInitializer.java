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
import com.github.javydreamercsw.base.Initializable;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.util.LogSanitizer;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.ArenaImportDTO;
import com.github.javydreamercsw.management.dto.CampaignAbilityCardDTO;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.dto.DeckCardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.dto.FactionImportDTO;
import com.github.javydreamercsw.management.dto.LocationImportDTO;
import com.github.javydreamercsw.management.dto.NpcDTO;
import com.github.javydreamercsw.management.dto.OutcomeMatrixEntryImportDTO;
import com.github.javydreamercsw.management.dto.OutcomeMatrixImportDTO;
import com.github.javydreamercsw.management.dto.RelationshipImportDTO;
import com.github.javydreamercsw.management.dto.RingsideActionDTO;
import com.github.javydreamercsw.management.dto.RingsideActionTypeDTO;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.dto.StatusCardDTO;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.dto.TitleDTO;
import com.github.javydreamercsw.management.dto.WrestlerImportDTO;
import com.github.javydreamercsw.management.dto.commentator.CommentaryTeamImportDTO;
import com.github.javydreamercsw.management.dto.commentator.CommentatorImportDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.commentator.CommentaryService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements Initializable {

  private final boolean enabled;
  private final boolean skipIfNotEmpty;
  private final ShowTemplateService showTemplateService;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final com.github.javydreamercsw.management.domain.universe.UniverseRepository
      universeRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final ShowTypeService showTypeService;
  private final SegmentRuleService segmentRuleService;
  private final SegmentTypeService segmentTypeService;
  private final CardSetService cardSetService;
  private final CardSetRepository cardSetRepository;
  private final CardService cardService;
  private final TitleService titleService;
  private final DeckService deckService;
  private final GameSettingService gameSettingService;
  private final com.github.javydreamercsw.management.domain.GameSettingRepository
      gameSettingRepository;
  private final NpcService npcService;
  private final FactionService factionService;
  private final TeamService teamService;
  private final TeamRepository teamRepository;
  private final TierRecalculationService tierRecalculationService;
  private final CampaignAbilityCardService campaignAbilityCardService;
  private final CommentaryService commentaryService;
  private final CampaignUpgradeService campaignUpgradeService;
  private final StatusCardService statusCardService;
  private final LocationRepository locationRepository;
  private final ArenaRepository arenaRepository;
  private final WrestlerRelationshipService relationshipService;
  private final Environment env;
  private final AchievementRepository achievementRepository;
  private final RingsideActionDataService ringsideActionDataService;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;
  private final OutcomeMatrixService outcomeMatrixService;

  @Autowired
  public DataInitializer(
      @Value("${data.initializer.enabled:true}") final boolean enabled,
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final ShowTemplateService showTemplateService,
      final WrestlerRepository wrestlerRepository,
      final WrestlerService wrestlerService,
      final com.github.javydreamercsw.management.domain.universe.UniverseRepository
          universeRepository,
      final WrestlerStateRepository wrestlerStateRepository,
      final ShowTypeService showTypeService,
      final SegmentRuleService segmentRuleService,
      final SegmentTypeService segmentTypeService,
      final CardSetService cardSetService,
      final CardSetRepository cardSetRepository,
      final CardService cardService,
      final TitleService titleService,
      final DeckService deckService,
      final GameSettingService gameSettingService,
      final com.github.javydreamercsw.management.domain.GameSettingRepository gameSettingRepository,
      final NpcService npcService,
      final FactionService factionService,
      final TeamService teamService,
      final TeamRepository teamRepository,
      final TierRecalculationService tierRecalculationService,
      final CampaignAbilityCardService campaignAbilityCardService,
      final CommentaryService commentaryService,
      final CampaignUpgradeService campaignUpgradeService,
      final StatusCardService statusCardService,
      final Environment env,
      final AchievementRepository achievementRepository,
      final RingsideActionDataService ringsideActionDataService,
      final ResourcePatternResolver resourcePatternResolver,
      final LocationRepository locationRepository,
      final ArenaRepository arenaRepository,
      final com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService
          relationshipService,
      final ObjectMapper objectMapper,
      final OutcomeMatrixService outcomeMatrixService) {
    this.enabled = enabled;
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.showTemplateService = showTemplateService;
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerService = wrestlerService;
    this.universeRepository = universeRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.showTypeService = showTypeService;
    this.segmentRuleService = segmentRuleService;
    this.segmentTypeService = segmentTypeService;
    this.cardSetService = cardSetService;
    this.cardSetRepository = cardSetRepository;
    this.cardService = cardService;
    this.titleService = titleService;
    this.deckService = deckService;
    this.gameSettingService = gameSettingService;
    this.gameSettingRepository = gameSettingRepository;
    this.npcService = npcService;
    this.factionService = factionService;
    this.teamService = teamService;
    this.teamRepository = teamRepository;
    this.tierRecalculationService = tierRecalculationService;
    this.campaignAbilityCardService = campaignAbilityCardService;
    this.commentaryService = commentaryService;
    this.campaignUpgradeService = campaignUpgradeService;
    this.statusCardService = statusCardService;
    this.env = env;
    this.achievementRepository = achievementRepository;
    this.ringsideActionDataService = ringsideActionDataService;
    this.resourcePatternResolver = resourcePatternResolver;
    this.locationRepository = locationRepository;
    this.arenaRepository = arenaRepository;
    this.relationshipService = relationshipService;
    this.objectMapper = objectMapper;
    this.outcomeMatrixService = outcomeMatrixService;
  }

  public void init() {
    log.debug("DataInitializer.init() called. enabled={}", enabled);
    if (enabled) {
      GeneralSecurityUtils.runAsAdmin(this::performInit);
    }
  }

  private void performInit() {
    syncAiSettingsFromEnvironment();
    initializeGameDate();
    loadSegmentRulesFromFile();
    syncShowTypesFromFile();
    loadSegmentTypesFromFile();
    loadShowTemplatesFromFile();
    syncSetsFromFile();
    syncCardsFromFile();
    syncNpcsFromFile();
    syncLocationsFromFile();
    syncArenasFromFile();
    syncWrestlersFromFile();
    syncRelationshipsFromFile();
    syncChampionshipsFromFile();
    syncDecksFromFile();
    syncFactionsFromFile();
    syncTeamsFromFile();
    syncCampaignAbilityCardsFromFile();
    syncStatusCardsFromFile();
    campaignUpgradeService.loadUpgrades();
    syncCommentatorsFromFile();
    syncCommentaryTeamsFromFile();
    loadAchievements();
    syncRingsideActions();
    syncOutcomeMatricesFromFiles();
    log.debug("Data initialization complete.");
  }

  private void syncRingsideActions() {
    syncRingsideActionTypesFromFile();
    syncRingsideActionsFromFile();
  }

  private void syncRingsideActionTypesFromFile() {
    if (skipIfNotEmpty && ringsideActionDataService.countTypes() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_action_types.json");
    if (resource.exists()) {
      log.debug("Loading ringside action types from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<RingsideActionTypeDTO> dtos =
            mapper.readValue(is, new TypeReference<List<RingsideActionTypeDTO>>() {});
        for (RingsideActionTypeDTO dto : dtos) {
          ringsideActionDataService.createOrUpdateType(
              dto.getName(),
              dto.isIncreasesAwareness(),
              dto.isCanCauseDq(),
              dto.getBaseRiskMultiplier());
          log.debug("Loaded ringside action type: {}", dto.getName());
        }
        log.debug("Ringside action type loading completed - {} types loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside action types from file", e);
      }
    }
  }

  private void syncRingsideActionsFromFile() {
    if (skipIfNotEmpty && ringsideActionDataService.countActions() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (resource.exists()) {
      log.debug("Loading ringside actions from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<RingsideActionDTO> dtos = mapper.readValue(is, new TypeReference<>() {});
        for (RingsideActionDTO dto : dtos) {
          ringsideActionDataService.createOrUpdateAction(
              dto.getName(),
              dto.getType(),
              dto.getDescription(),
              dto.getImpact(),
              dto.getRisk(),
              dto.getAlignment(),
              dto.getSet() != null ? dto.getSet() : "BASE_GAME");
          log.debug("Loaded ringside action: {}", dto.getName());
        }
        log.debug("Ringside action loading completed - {} actions loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside actions from file", e);
      }
    }
  }

  private void loadAchievements() {
    if (skipIfNotEmpty && achievementRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("achievements.json");
    if (resource.exists()) {
      log.debug("Loading achievements from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<Achievement> achievementsFromFile = mapper.readValue(is, new TypeReference<>() {});
        List<Achievement> toSave = new ArrayList<>();
        for (Achievement a : achievementsFromFile) {
          Optional<Achievement> existingOpt = achievementRepository.findByKey(a.getKey());
          if (existingOpt.isPresent()) {
            Achievement existing = existingOpt.get();
            existing.setName(a.getName());
            existing.setDescription(a.getDescription());
            existing.setXpValue(a.getXpValue());
            existing.setCategory(a.getCategory());
            toSave.add(existing);
          } else {
            toSave.add(a);
          }
        }
        achievementRepository.saveAll(toSave);
        log.debug(
            "Achievement loading completed - {} achievements processed",
            achievementsFromFile.size());
      } catch (IOException e) {
        log.error("Error loading achievements from file", e);
      }
    } else {
      log.warn("Achievements file not found: {}", resource.getPath());
    }
  }

  private void syncCommentatorsFromFile() {
    if (skipIfNotEmpty && commentaryService.countCommentators() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("commentators.json");
    if (resource.exists()) {
      log.debug("Loading commentators from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<CommentatorImportDTO> dtos = mapper.readValue(is, new TypeReference<>() {});
        for (CommentatorImportDTO cDto : dtos) {
          commentaryService.createOrUpdateCommentator(
              cDto.getNpcName(),
              cDto.getGender(),
              cDto.getAlignment(),
              cDto.getDescription(),
              cDto.getStyle(),
              cDto.getCatchphrase(),
              cDto.getPersonaDescription(),
              cDto.getSet() != null ? cDto.getSet() : "BASE_GAME");
          log.debug("Loaded commentator: {}", cDto.getNpcName());
        }
        log.debug("Commentator loading completed - {} commentators loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading commentators from file", e);
      }
    } else {
      log.warn("Commentators file not found: {}", resource.getPath());
    }
  }

  private void syncCommentaryTeamsFromFile() {
    if (skipIfNotEmpty && commentaryService.countTeams() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("commentary_teams.json");
    if (resource.exists()) {
      log.debug("Loading commentary teams from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<CommentaryTeamImportDTO> dtos = mapper.readValue(is, new TypeReference<>() {});
        for (CommentaryTeamImportDTO teamDto : dtos) {
          commentaryService.createOrUpdateTeam(teamDto.getTeamName(), teamDto.getMemberNames());
          log.debug("Loaded commentary team: {}", teamDto.getTeamName());
        }
        log.debug("Commentary team loading completed - {} teams loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading commentary teams from file", e);
      }
    } else {
      log.warn("Commentary teams file not found: {}", resource.getPath());
    }
  }

  private Long getGlobalUniverseId() {
    return universeRepository.findAll().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No universe found"))
        .getId();
  }

  private void syncAiSettingsFromEnvironment() {
    log.debug(
        "Syncing AI settings from environment variables/system properties/Spring environment...");

    // Pre-load all existing settings once to avoid one DB round-trip per key
    Map<String, com.github.javydreamercsw.management.domain.GameSetting> existingSettings =
        gameSettingRepository.findAllGlobal().stream()
            .collect(
                Collectors.toMap(
                    com.github.javydreamercsw.management.domain.GameSetting::getSettingKey,
                    s -> s,
                    (a, b) -> a));
    boolean forceOverride =
        Boolean.parseBoolean(env.getProperty("data.initializer.aiSettings.forceOverride", "false"));
    List<com.github.javydreamercsw.management.domain.GameSetting> toSave = new ArrayList<>();

    syncSetting("AI_TIMEOUT", "300", existingSettings, forceOverride, toSave);
    syncSetting("AI_PROVIDER_AUTO", "true", existingSettings, forceOverride, toSave);

    // OpenAI
    syncSetting("AI_OPENAI_ENABLED", "false", existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_OPENAI_API_URL",
        "https://api.openai.com/v1/chat/completions",
        existingSettings,
        forceOverride,
        toSave);
    syncSetting("AI_OPENAI_API_KEY", null, existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_OPENAI_DEFAULT_MODEL", "gpt-3.5-turbo", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_PREMIUM_MODEL", "gpt-4", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_IMAGE_MODEL", "dall-e-3", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_MAX_TOKENS", "4000", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_TEMPERATURE", "0.7", existingSettings, forceOverride, toSave);

    // Claude
    syncSetting("AI_CLAUDE_ENABLED", "false", existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_CLAUDE_API_URL",
        "https://api.anthropic.com/v1/messages/",
        existingSettings,
        forceOverride,
        toSave);
    syncSetting("AI_CLAUDE_API_KEY", null, existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_CLAUDE_MODEL_NAME", "claude-3-haiku-20240307", existingSettings, forceOverride, toSave);

    // Gemini
    syncSetting("AI_GEMINI_ENABLED", "false", existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_GEMINI_API_URL",
        "https://generativelanguage.googleapis.com/v1beta/models/",
        existingSettings,
        forceOverride,
        toSave);
    syncSetting("AI_GEMINI_API_KEY", null, existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_GEMINI_MODEL_NAME",
        "gemini-3.1-flash-lite-preview",
        existingSettings,
        forceOverride,
        toSave);

    if (!toSave.isEmpty()) {
      gameSettingRepository.saveAll(toSave);
      log.debug("AI settings synchronization saved {} settings.", toSave.size());
    }
    log.debug("AI settings synchronization complete.");
  }

  private void syncSetting(
      @NonNull final String key,
      final String defaultValue,
      final Map<String, com.github.javydreamercsw.management.domain.GameSetting> existingSettings,
      final boolean forceOverride,
      final List<com.github.javydreamercsw.management.domain.GameSetting> toSave) {
    String envValue = env.getProperty(key);

    // Prefer NOT overwriting DB values unless explicitly forced.
    if (envValue != null) {
      if (forceOverride) {
        log.debug(
            "AI setting sync: overriding '{}' from environment (forceOverride=true): {}",
            key,
            maskIfSecret(key, envValue));
        gameSettingService.save(key, envValue);
      } else if (existingSettings.containsKey(key)) {
        log.debug(
            "AI setting sync: skipping '{}' from environment because DB already has a value: {}",
            key,
            maskIfSecret(key, envValue));
      } else {
        log.debug(
            "AI setting sync: saving missing '{}' from environment: {}",
            key,
            maskIfSecret(key, envValue));
        saveIfMissing(key, envValue, existingSettings, toSave);
      }
      return;
    }

    // No env value: seed defaults only if missing.
    if (defaultValue != null) {
      if (existingSettings.containsKey(key)) {
        log.debug("AI setting sync: '{}' already present in DB; default not applied.", key);
      } else {
        log.debug(
            "AI setting sync: seeding missing '{}' with default: {}",
            key,
            maskIfSecret(key, defaultValue));
        saveIfMissing(key, defaultValue, existingSettings, toSave);
      }
    } else {
      log.debug("AI setting sync: '{}' has no env value and no default; leaving as-is.", key);
    }
  }

  private String maskIfSecret(final String key, final String value) {
    if (key != null && key.toUpperCase().contains("KEY")) {
      return "********";
    }
    return value;
  }

  private void saveIfMissing(
      @NonNull final String key,
      @NonNull final String value,
      final Map<String, com.github.javydreamercsw.management.domain.GameSetting> existingSettings,
      final List<com.github.javydreamercsw.management.domain.GameSetting> toSave) {
    if (!existingSettings.containsKey(key)) {
      com.github.javydreamercsw.management.domain.GameSetting setting =
          new com.github.javydreamercsw.management.domain.GameSetting();
      setting.setSettingKey(key);
      setting.setValue(value);
      existingSettings.put(key, setting); // prevent duplicate adds within this run
      toSave.add(setting);
      log.debug("Initialized missing setting: {} = {}", key, maskIfSecret(key, value));
    }
  }

  private void syncCampaignAbilityCardsFromFile() {
    if (skipIfNotEmpty && campaignAbilityCardService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (resource.exists()) {
      log.debug("Loading campaign ability cards from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<CampaignAbilityCardDTO> cardsFromFile = mapper.readValue(is, new TypeReference<>() {});
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
        log.debug(
            "Campaign ability card loading completed - {} cards loaded", cardsFromFile.size());
      } catch (IOException e) {
        log.error("Error loading campaign ability cards from file", e);
      }
    } else {
      log.warn("Campaign ability cards file not found: {}", resource.getPath());
    }
  }

  private void syncStatusCardsFromFile() {
    ClassPathResource resource = new ClassPathResource("status_cards.json");
    if (resource.exists()) {
      log.debug("Loading status cards from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<StatusCardDTO> cardsFromFile = mapper.readValue(is, new TypeReference<>() {});
        // Skip loading if the count already matches — avoids N×M DB round-trips in tests
        // where DataInitializer.init() runs on every @BeforeEach reset.
        if (statusCardService.findAll().size() == cardsFromFile.size()) {
          log.debug("Status cards already loaded ({} cards), skipping sync.", cardsFromFile.size());
          return;
        }
        for (StatusCardDTO dto : cardsFromFile) {
          statusCardService.createOrUpdateCard(
              dto.getKey(),
              dto.getLevel1Name(),
              dto.getLevel2Name(),
              dto.getDescription(),
              dto.isPositive(),
              dto.getLevel1Effect(),
              dto.getLevel2Effect(),
              dto.getFlipUpCondition(),
              dto.getFlipDownCondition(),
              dto.getDiscardCondition());
          log.debug("Loaded status card: {}", dto.getKey());
        }
        log.debug("Status card loading completed - {} cards loaded", cardsFromFile.size());
      } catch (IOException e) {
        log.error("Error loading status cards from file", e);
      }
    } else {
      log.warn("Status cards file not found: {}", resource.getPath());
    }
  }

  private void loadSegmentRulesFromFile() {
    if (skipIfNotEmpty && segmentRuleService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("segment_rules.json");
    if (resource.exists()) {
      log.debug("Loading segment rules from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<SegmentRuleDTO> segmentRulesFromFile = mapper.readValue(is, new TypeReference<>() {});

        for (SegmentRuleDTO dto : segmentRulesFromFile) {
          segmentRuleService.createOrUpdateRule(
              dto.getName(),
              dto.getDescription(),
              dto.isRequiresHighHeat(),
              dto.isNoDq(),
              dto.getBumpAddition(),
              dto.getSet() != null ? dto.getSet() : "BASE_GAME");
          log.debug(
              "Loaded segment rule: {} (High Heat: {}, No DQ: {}, Bump Addition: {})",
              dto.getName(),
              dto.isRequiresHighHeat(),
              dto.isNoDq(),
              dto.getBumpAddition());
        }
        log.debug("Segment rule loading completed - {} rules loaded", segmentRulesFromFile.size());
      } catch (IOException e) {
        log.error("Error loading segment rules from file", e);
      }
    } else {
      log.warn("Segment rules file not found: {}", resource.getPath());
    }
  }

  private void syncShowTypesFromFile() {
    if (skipIfNotEmpty && showTypeService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("show_types.json");
    if (resource.exists()) {
      log.debug("Loading show types from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<ShowType> showTypesFromFile = mapper.readValue(is, new TypeReference<>() {});
        for (ShowType st : showTypesFromFile) {
          showTypeService.createOrUpdateShowType(
              st.getName(), st.getDescription(), st.getExpectedMatches(), st.getExpectedPromos());
          log.debug(
              "Loaded show type: {} (Expected Matches: {}, Expected Promos: {})",
              st.getName(),
              st.getExpectedMatches(),
              st.getExpectedPromos());
        }
        log.debug("Show type loading completed - {} types loaded", showTypesFromFile.size());
      } catch (IOException e) {
        log.error("Error loading show types from file", e);
      }
    } else {
      log.warn("Show types file not found: {}", resource.getPath());
    }
  }

  private void loadSegmentTypesFromFile() {
    if (skipIfNotEmpty && segmentTypeService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("segment_types.json");
    if (resource.exists()) {
      log.debug("Loading segment types from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<SegmentTypeDTO> segmentTypesFromFile = mapper.readValue(is, new TypeReference<>() {});

        for (SegmentTypeDTO dto : segmentTypesFromFile) {
          // Only create if it's new
          Optional<SegmentType> existingType = segmentTypeService.findByName(dto.getName());
          if (existingType.isEmpty()) {
            SegmentType segmentType =
                segmentTypeService.createOrUpdateSegmentType(
                    dto.getName(),
                    dto.getDescription(),
                    dto.getSet() != null ? dto.getSet() : "BASE_GAME");
            log.debug(
                "Loaded segment type: {} (Players: {})",
                segmentType.getName(),
                dto.isUnlimited() ? "Unlimited" : dto.getPlayerAmount());
          } else {
            log.debug("Segment type {} already exists, skipping creation.", dto.getName());
          }
        }

        log.debug("Segment type loading completed");
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
      log.debug(
          "Show templates table already contains {} templates - skipping file import",
          existingTemplatesCount);
      return;
    }

    ClassPathResource resource = new ClassPathResource("show_templates.json");
    if (resource.exists()) {
      log.debug(
          "Show templates table is empty - loading templates from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<ShowTemplateDTO> templatesFromFile = mapper.readValue(is, new TypeReference<>() {});

        for (ShowTemplateDTO dto : templatesFromFile) {
          ShowTemplate template =
              showTemplateService.createOrUpdateTemplate(
                  dto.getName(),
                  dto.getDescription(),
                  dto.getShowTypeName(),
                  null,
                  dto.getCommentaryTeamName(),
                  dto.getExpectedMatches(),
                  dto.getExpectedPromos(),
                  dto.getDurationDays(),
                  dto.getRecurrenceType() != null
                      ? RecurrenceType.valueOf(dto.getRecurrenceType())
                      : null,
                  dto.getDayOfWeek() != null ? DayOfWeek.valueOf(dto.getDayOfWeek()) : null,
                  dto.getDayOfMonth(),
                  dto.getWeekOfMonth(),
                  dto.getMonth() != null ? Month.valueOf(dto.getMonth()) : null,
                  dto.getGenderConstraint() != null
                      ? Gender.valueOf(dto.getGenderConstraint())
                      : null);
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
    if (skipIfNotEmpty && cardSetService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("sets.json");
    if (resource.exists()) {
      log.debug("Loading card sets from file: {}", resource.getPath());
      // Load card sets from JSON file
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<CardSet> setsFromFile = mapper.readValue(is, new TypeReference<>() {});
        List<CardSet> toSave = new ArrayList<>();
        for (CardSet c : setsFromFile) {
          Optional<CardSet> existingSetOpt = cardSetService.findBySetCode(c.getCode());
          if (existingSetOpt.isPresent()) {
            CardSet existingSet = existingSetOpt.get();
            // Update fields
            existingSet.setName(c.getName());
            toSave.add(existingSet);
          } else {
            toSave.add(c);
          }
        }
        cardSetService.saveAll(toSave);
        log.debug("Card sets loading completed - {} sets processed", setsFromFile.size());
      } catch (IOException e) {
        log.error("Error loading card sets from file", e);
      }
    }
  }

  private void syncCardsFromFile() {
    if (skipIfNotEmpty && cardService.count() > 0) {
      return;
    }
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:cards/*.json");
      Map<String, CardSet> setCache = new HashMap<>();
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

      for (Resource resource : resources) {
        if (resource.exists()) {
          log.debug("Loading cards from file: {}", resource.getFilename());
          // Load cards from JSON file
          ObjectMapper mapper = objectMapper;
          try (var is = resource.getInputStream()) {
            List<CardDTO> cardsFromFile = mapper.readValue(is, new TypeReference<>() {});
            List<Card> toSave = new ArrayList<>();
            for (CardDTO dto : cardsFromFile) {
              CardSet set = setCache.get(dto.getSet());
              if (set == null) {
                Optional<CardSet> setOpt = cardSetService.findBySetCode(dto.getSet());
                if (setOpt.isEmpty()) {
                  log.warn(
                      "CardSet with code {} not found for card {}. Skipping card.",
                      dto.getSet(),
                      dto.getName());
                  continue;
                }
                set = setOpt.get();
                setCache.put(dto.getSet(), set);
              }

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
              toSave.add(card);
            }
            cardService.saveAll(toSave);
            log.debug("Saved/Updated {} cards from {}", toSave.size(), resource.getFilename());
          } catch (IOException e) {
            log.error("Error loading cards from file: {}", resource.getFilename(), e);
          }
        }
      }
    } catch (IOException e) {
      log.error("Error resolving card resources", e);
    }
  }

  protected void syncWrestlersFromFile() {
    if (skipIfNotEmpty && wrestlerRepository.count() > 0) {
      return;
    }
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:wrestlers*.json");
      // Load universe once
      Universe universe =
          universeRepository.findAll().stream()
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("No universe found"));
      Long leagueId = universe.getId();

      // Pre-load all wrestlers once to avoid N per-wrestler DB queries across all files.
      // findAllWithAlignments uses LEFT JOIN FETCH so that getAlignment() (which iterates the
      // lazy alignments collection) works safely on the detached entities.
      Map<String, Wrestler> wrestlersByName =
          wrestlerRepository.findAllWithAlignments().stream()
              .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
      // Lazy NPC cache for manager lookups — populated on first use per manager name
      Map<String, Npc> npcByName = new HashMap<>();

      for (Resource resource : resources) {
        if (resource.exists()) {
          log.debug("Loading wrestlers from file: {}", resource.getFilename());
          // Load wrestlers from JSON file
          ObjectMapper mapper = objectMapper;
          try (var is = resource.getInputStream()) {
            List<WrestlerImportDTO> wrestlersFromFile =
                mapper.readValue(is, new TypeReference<>() {});

            // toSaveBulk = only wrestlers whose fields actually changed (or are new).
            // allWrestlers = every wrestler from the file — used for state processing.
            Map<String, WrestlerImportDTO> dtoMap = new HashMap<>();
            List<Wrestler> toSaveBulk = new ArrayList<>();
            List<Wrestler> allWrestlers = new ArrayList<>();

            for (WrestlerImportDTO w : wrestlersFromFile) {
              Wrestler existingWrestler = wrestlersByName.get(w.getName());

              Wrestler wrestlerToSave;
              if (existingWrestler != null) {
                // Dirty-check each field — only save if something actually changed.
                boolean changed = false;
                if (!Objects.equals(existingWrestler.getDeckSize(), w.getDeckSize())) {
                  existingWrestler.setDeckSize(w.getDeckSize());
                  changed = true;
                }
                if (!Objects.equals(existingWrestler.getStartingHealth(), w.getStartingHealth())) {
                  existingWrestler.setStartingHealth(w.getStartingHealth());
                  changed = true;
                }
                if (!Objects.equals(existingWrestler.getLowHealth(), w.getLowHealth())) {
                  existingWrestler.setLowHealth(w.getLowHealth());
                  changed = true;
                }
                if (!Objects.equals(
                    existingWrestler.getStartingStamina(), w.getStartingStamina())) {
                  existingWrestler.setStartingStamina(w.getStartingStamina());
                  changed = true;
                }
                if (!Objects.equals(existingWrestler.getLowStamina(), w.getLowStamina())) {
                  existingWrestler.setLowStamina(w.getLowStamina());
                  changed = true;
                }
                if (!Objects.equals(existingWrestler.getDescription(), w.getDescription())) {
                  existingWrestler.setDescription(w.getDescription());
                  changed = true;
                }
                if (!Objects.equals(existingWrestler.getGender(), w.getGender())) {
                  existingWrestler.setGender(w.getGender());
                  changed = true;
                }
                if (w.getImageUrl() != null
                    && !Objects.equals(existingWrestler.getImageUrl(), w.getImageUrl())) {
                  existingWrestler.setImageUrl(w.getImageUrl());
                  changed = true;
                }
                if (w.getHeritageTag() != null
                    && !Objects.equals(existingWrestler.getHeritageTag(), w.getHeritageTag())) {
                  existingWrestler.setHeritageTag(w.getHeritageTag());
                  changed = true;
                }
                if (w.getSet() != null
                    && !Objects.equals(existingWrestler.getExpansionCode(), w.getSet())) {
                  existingWrestler.setExpansionCode(w.getSet());
                  changed = true;
                }
                if (w.getDrive() != null
                    && !Objects.equals(existingWrestler.getDrive(), w.getDrive())) {
                  existingWrestler.setDrive(w.getDrive());
                  changed = true;
                }
                if (w.getResilience() != null
                    && !Objects.equals(existingWrestler.getResilience(), w.getResilience())) {
                  existingWrestler.setResilience(w.getResilience());
                  changed = true;
                }
                if (w.getCharisma() != null
                    && !Objects.equals(existingWrestler.getCharisma(), w.getCharisma())) {
                  existingWrestler.setCharisma(w.getCharisma());
                  changed = true;
                }
                if (w.getBrawl() != null
                    && !Objects.equals(existingWrestler.getBrawl(), w.getBrawl())) {
                  existingWrestler.setBrawl(w.getBrawl());
                  changed = true;
                }

                if (w.getAlignment() != null) {
                  if (existingWrestler.getAlignment() == null
                      || existingWrestler.getAlignment().getAlignmentType() == null) {
                    try {
                      AlignmentType at = AlignmentType.valueOf(w.getAlignment().toUpperCase());
                      existingWrestler.setAlignment(
                          WrestlerAlignment.builder()
                              .wrestler(existingWrestler)
                              .universe(universe)
                              .alignmentType(at)
                              .level(0)
                              .build());
                      log.debug(
                          "Initialized alignment for existing wrestler {}: {}",
                          existingWrestler.getName(),
                          at);
                      changed = true;
                    } catch (IllegalArgumentException e) {
                      log.warn(
                          "Invalid alignment '{}' for wrestler '{}'",
                          LogSanitizer.sanitize(w.getAlignment()),
                          LogSanitizer.sanitize(w.getName()));
                    }
                  }
                }

                if (existingWrestler.getActive() == null) {
                  existingWrestler.setActive(true);
                  changed = true;
                }
                if (existingWrestler.getIsPlayer() == null) {
                  existingWrestler.setIsPlayer(false);
                  changed = true;
                }

                wrestlerToSave = existingWrestler;
                if (changed) {
                  toSaveBulk.add(wrestlerToSave);
                  log.debug(
                      "Wrestler changed, will save: {}",
                      LogSanitizer.sanitize(existingWrestler.getName()));
                } else {
                  log.debug(
                      "Wrestler unchanged, skipping save: {}",
                      LogSanitizer.sanitize(existingWrestler.getName()));
                }
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
                newWrestler.setActive(true);
                newWrestler.setIsPlayer(false);
                newWrestler.setImageUrl(w.getImageUrl());
                newWrestler.setHeritageTag(w.getHeritageTag());
                if (w.getSet() != null) {
                  newWrestler.setExpansionCode(w.getSet());
                }
                // Manager is set on WrestlerState in the post-save loop below
                if (w.getDrive() != null) {
                  newWrestler.setDrive(w.getDrive());
                }
                if (w.getResilience() != null) {
                  newWrestler.setResilience(w.getResilience());
                }
                if (w.getCharisma() != null) {
                  newWrestler.setCharisma(w.getCharisma());
                }
                if (w.getBrawl() != null) {
                  newWrestler.setBrawl(w.getBrawl());
                }

                if (w.getAlignment() != null) {
                  try {
                    AlignmentType at = AlignmentType.valueOf(w.getAlignment().toUpperCase());
                    newWrestler.setAlignment(
                        WrestlerAlignment.builder()
                            .wrestler(newWrestler)
                            .alignmentType(at)
                            .level(0)
                            .build());
                  } catch (IllegalArgumentException e) {
                    log.warn(
                        "Invalid alignment '{}' for wrestler '{}'",
                        LogSanitizer.sanitize(w.getAlignment()),
                        LogSanitizer.sanitize(w.getName()));
                  }
                }
                wrestlerToSave = newWrestler;
              }
              // New wrestlers always go to both lists; existing ones already handled above.
              if (existingWrestler == null) {
                toSaveBulk.add(wrestlerToSave);
              }
              allWrestlers.add(wrestlerToSave);
              dtoMap.put(wrestlerToSave.getName(), w);
            }

            // Save only changed/new wrestlers
            if (!toSaveBulk.isEmpty()) {
              log.debug(
                  "Saving {}/{} wrestlers with changes", toSaveBulk.size(), allWrestlers.size());
              wrestlerRepository.saveAll(toSaveBulk);
              wrestlerRepository.flush();
            } else {
              log.debug("All {} wrestlers unchanged — skipping saveAll", allWrestlers.size());
            }

            // Keep caches current for any subsequent wrestler files
            toSaveBulk.forEach(saved -> wrestlersByName.put(saved.getName(), saved));

            // Process states for ALL wrestlers, but only save if something actually changed.
            for (Wrestler wrestler : allWrestlers) {
              WrestlerImportDTO w = dtoMap.get(wrestler.getName());
              WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), leagueId);
              boolean stateChanged = state.getId() == null; // newly created state always needs save

              if (w.getFans() != null && w.getFans() > state.getFans()) {
                state.setFans(w.getFans());
                stateChanged = true;
              }
              if (w.getBumps() != null && w.getBumps() > state.getBumps()) {
                state.setBumps(w.getBumps());
                stateChanged = true;
              }
              if (stateChanged) {
                state.setTier(WrestlerTier.fromFanCount(state.getFans()));
                tierRecalculationService.recalculateTier(state);
              }

              if (w.getManager() != null) {
                if (!npcByName.containsKey(w.getManager())) {
                  npcByName.put(w.getManager(), npcService.findByName(w.getManager()));
                }
                Npc manager = npcByName.get(w.getManager());
                if (manager != null && !Objects.equals(state.getManager(), manager)) {
                  state.setManager(manager);
                  stateChanged = true;
                }
              }
              if (stateChanged) {
                wrestlerStateRepository.save(state);
              }
            }
          } catch (IOException e) {
            log.error("Error loading wrestlers from file", e);
          }
        }
      }
    } catch (IOException e) {
      log.error("Error resolving wrestler resources", e);
    }
  }

  private void syncChampionshipsFromFile() {
    if (skipIfNotEmpty && titleService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("championships.json");
    if (resource.exists()) {
      log.debug("Loading championships from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<TitleDTO> championshipsFromFile = mapper.readValue(is, new TypeReference<>() {});
        Long universeId = getGlobalUniverseId();
        // Pre-load wrestlers once to avoid per-champion-name DB queries
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        List<Title> toSave = new ArrayList<>();
        for (TitleDTO dto : championshipsFromFile) {
          Optional<Title> existingTitle = titleService.findByName(dto.getName());
          Title title;
          if (existingTitle.isEmpty()) {
            title =
                titleService.createTitle(
                    dto.getName(),
                    dto.getDescription(),
                    dto.getTier(),
                    dto.getChampionshipType(),
                    dto.getGender(),
                    universeId);
            log.debug(
                "Created new title: {} with type: {}", dto.getName(), dto.getChampionshipType());
          } else {
            title = existingTitle.get();
            log.debug("Title {} already exists, updating.", dto.getName());
          }
          title.setChampionshipType(dto.getChampionshipType());
          title.setEffectScript(dto.getEffectScript());
          title.setGender(dto.getGender());
          title.setImageUrl(dto.getImageUrl());
          title.setExpansionCode(dto.getSet() != null ? dto.getSet() : "BASE_GAME");
          if (dto.getIncludeInRankings() != null) {
            title.setIncludeInRankings(dto.getIncludeInRankings());
          }
          toSave.add(title);
        }
        titleService.saveAll(toSave);

        // Process awards separately as they involve complex logic
        for (TitleDTO dto : championshipsFromFile) {
          if (dto.getCurrentChampionName() != null
              && !dto.getCurrentChampionName().trim().isEmpty()) {
            Title title = titleService.findByName(dto.getName()).orElse(null);
            if (title == null) {
              continue;
            }

            String[] championNames = dto.getCurrentChampionName().split(",");
            List<Wrestler> champions = new ArrayList<>();
            for (String name : championNames) {
              Optional<Wrestler> championOpt =
                  Optional.ofNullable(wrestlersByName.get(name.trim()));
              championOpt.ifPresent(champions::add);
            }

            if (!champions.isEmpty()) {
              boolean matchesCurrent =
                  title.getCurrentChampions().size() == champions.size()
                      && new HashSet<>(title.getCurrentChampions()).containsAll(champions);

              if (!matchesCurrent) {
                titleService.awardTitleTo(title, champions);
                log.debug(
                    "Awarded title {} to champions {}",
                    title.getName(),
                    dto.getCurrentChampionName());
              }
            }
          } else {
            // If no champion is specified in DTO leave it as it is currently.
            log.debug("Leaving title {} as is in the database.", dto.getName());
          }
        }
      } catch (IOException e) {
        log.error("Error loading championships from file", e);
      }
    }
  }

  private void syncDecksFromFile() {
    if (skipIfNotEmpty && deckService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("decks.json");
    if (resource.exists()) {
      log.debug("Loading decks from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<DeckDTO> decksFromFile = mapper.readValue(is, new TypeReference<>() {});
        Map<String, Wrestler> wrestlers =
            wrestlerRepository.findAll().stream()
                .collect(
                    Collectors.toMap(
                        Wrestler::getName, w -> w, (existing1, existing2) -> existing1));
        // Seed cache using the repository directly (REQUIRED propagation) so all CardSet
        // instances stay managed in the current persistence context and Hibernate sees
        // only one instance per ID during cascade merge.
        Map<Long, CardSet> setCache = new HashMap<>();
        cardSetRepository.findAll().forEach(cs -> setCache.put(cs.getId(), cs));

        List<Deck> decksToSave = new ArrayList<>();
        for (DeckDTO deckDTO : decksFromFile) {
          Wrestler wrestler = wrestlers.get(deckDTO.getWrestler());
          if (wrestler == null) {
            continue;
          }

          List<Deck> byWrestler = deckService.findByWrestlerWithCards(wrestler);
          Deck deck =
              byWrestler.isEmpty() ? deckService.createDeck(wrestler) : byWrestler.getFirst();

          // Normalize existing DeckCard.set references to the canonical managed instances
          deck.getCards()
              .forEach(
                  dc -> {
                    if (dc.getSet() != null && dc.getSet().getId() != null) {
                      dc.setSet(setCache.getOrDefault(dc.getSet().getId(), dc.getSet()));
                    }
                    if (dc.getCard() != null
                        && dc.getCard().getSet() != null
                        && dc.getCard().getSet().getId() != null) {
                      dc.getCard()
                          .setSet(
                              setCache.getOrDefault(
                                  dc.getCard().getSet().getId(), dc.getCard().getSet()));
                    }
                  });

          // Keep track of cards to be removed
          Set<DeckCard> cardsToRemove = new HashSet<>(deck.getCards());
          Map<String, Integer> cardKeyToAmount = new HashMap<>();
          Map<String, Card> cardKeyToCard = new HashMap<>();

          for (DeckCardDTO cardDTO : deckDTO.getCards()) {
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

            CardSet canonicalSet =
                setCache.computeIfAbsent(card.getSet().getId(), id -> card.getSet());
            card.setSet(canonicalSet);

            String key = card.getSet().getName() + "-" + card.getId();
            cardKeyToAmount.merge(key, cardDTO.getAmount(), Integer::sum);
            cardKeyToCard.putIfAbsent(key, card);
          }

          boolean changed = false;
          for (var entry : cardKeyToAmount.entrySet()) {
            Card card = cardKeyToCard.get(entry.getKey());
            int amount = entry.getValue();

            Optional<DeckCard> existingDeckCardOpt =
                deck.getCards().stream()
                    .filter(dc -> dc.getCard().equals(card) && dc.getSet().equals(card.getSet()))
                    .findFirst();

            if (existingDeckCardOpt.isPresent()) {
              DeckCard existingDeckCard = existingDeckCardOpt.get();
              if (existingDeckCard.getAmount() != amount) {
                existingDeckCard.setAmount(amount);
                changed = true;
              }
              cardsToRemove.remove(existingDeckCard);
            } else {
              DeckCard newDeckCard = new DeckCard();
              newDeckCard.setCard(card);
              newDeckCard.setSet(card.getSet());
              newDeckCard.setAmount(amount);
              newDeckCard.setDeck(deck);
              deck.getCards().add(newDeckCard);
              changed = true;
            }
          }

          if (!cardsToRemove.isEmpty()) {
            deck.getCards().removeAll(cardsToRemove);
            changed = true;
          }

          if (changed) {
            decksToSave.add(deck);
          }
        }
        deckService.saveAll(decksToSave);
        log.debug(
            "Deck loading completed - {} decks processed, {} updated",
            decksFromFile.size(),
            decksToSave.size());
      } catch (IOException e) {
        log.error("Error loading decks from file", e);
      }
    }
  }

  private void initializeGameDate() {
    if (gameSettingRepository.findGlobal(GameSettingService.CURRENT_GAME_DATE_KEY).isEmpty()) {
      log.debug("In-game date not set. Initializing to current date.");
      com.github.javydreamercsw.management.domain.GameSetting setting =
          new com.github.javydreamercsw.management.domain.GameSetting();
      setting.setSettingKey(GameSettingService.CURRENT_GAME_DATE_KEY);
      setting.setValue(LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
      gameSettingRepository.save(setting);
    }
  }

  void syncNpcsFromFile() {
    if (skipIfNotEmpty && npcService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("npcs.json");
    if (resource.exists()) {
      log.debug("Loading npcs from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<NpcDTO> dtos = mapper.readValue(is, new TypeReference<>() {});
        // Pre-load existing NPCs once to avoid per-NPC DB queries
        Map<String, Npc> existingNpcs =
            npcService.findAll().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        List<Npc> toSave = new ArrayList<>();
        for (NpcDTO dto : dtos) {
          Npc npc = existingNpcs.get(dto.getName());
          if (npc == null) {
            // New NPC — always save
            npc = new Npc();
            npc.setName(dto.getName());
            npc.setDescription(dto.getDescription());
            npc.setNpcType(dto.getType());
            if (dto.getSet() != null) {
              npc.setExpansionCode(dto.getSet());
            }
            if (dto.getAwareness() != null) {
              npcService.setAwareness(npc, dto.getAwareness());
            }
            if (dto.getAlignment() != null) {
              try {
                AlignmentType at = AlignmentType.valueOf(dto.getAlignment().toUpperCase());
                npc.setAlignment(at);
              } catch (IllegalArgumentException e) {
                log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
              }
            }
            toSave.add(npc);
          } else {
            // Existing NPC — dirty-check before saving
            boolean changed = false;
            if (!Objects.equals(npc.getDescription(), dto.getDescription())) {
              npc.setDescription(dto.getDescription());
              changed = true;
            }
            if (!Objects.equals(npc.getNpcType(), dto.getType())) {
              npc.setNpcType(dto.getType());
              changed = true;
            }
            if (dto.getSet() != null && !Objects.equals(npc.getExpansionCode(), dto.getSet())) {
              npc.setExpansionCode(dto.getSet());
              changed = true;
            }
            if (dto.getAlignment() != null) {
              try {
                AlignmentType at = AlignmentType.valueOf(dto.getAlignment().toUpperCase());
                if (!Objects.equals(npc.getAlignment(), at)) {
                  npc.setAlignment(at);
                  changed = true;
                }
              } catch (IllegalArgumentException e) {
                log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
              }
            }
            // Awareness is a separate side-effectful call; skip if unchanged would require
            // an extra query — leave as unconditional for now (it's a cheap update).
            if (dto.getAwareness() != null) {
              npcService.setAwareness(npc, dto.getAwareness());
            }
            if (changed) {
              toSave.add(npc);
            }
          }
        }
        if (!toSave.isEmpty()) {
          npcService.saveAll(toSave);
        }
        log.debug("Npc loading completed - {} npcs processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading npcs from file", e);
      }
    } else {
      log.warn("Npcs file not found: {}", resource.getPath());
    }
  }

  private void syncFactionsFromFile() {
    if (skipIfNotEmpty && factionService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("factions.json");
    if (resource.exists()) {
      log.debug("Loading factions from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<FactionImportDTO> dtos = mapper.readValue(is, new TypeReference<>() {});
        Long universeId = getGlobalUniverseId();
        // Pre-load wrestlers and NPCs once to avoid per-leader / per-member / per-manager queries
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        Map<String, Npc> npcByName =
            npcService.findAll().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        for (FactionImportDTO dto : dtos) {
          Optional<Wrestler> leaderOpt = Optional.ofNullable(wrestlersByName.get(dto.getLeader()));
          if (leaderOpt.isPresent()) {
            Optional<Faction> factionOpt = factionService.getFactionByName(dto.getName());
            if (factionOpt.isEmpty()) {
              factionOpt =
                  factionService.createFaction(
                      dto.getName(), dto.getDescription(), leaderOpt.get().getId(), universeId);
            }
            if (factionOpt.isPresent()) {
              Faction faction = factionOpt.get();
              for (String memberName : dto.getMembers()) {
                Optional<Wrestler> memberOpt = Optional.ofNullable(wrestlersByName.get(memberName));
                memberOpt.ifPresent(
                    wrestler ->
                        factionService.addMemberToFaction(faction.getId(), wrestler.getId()));
              }
              if (dto.getManager() != null) {
                Npc manager = npcByName.get(dto.getManager());
                if (manager != null) {
                  faction.setManager(manager);
                  factionService.save(faction);
                }
              }
            }
          }
          log.debug("Loaded faction: {}", dto.getName());
        }
        log.debug("Faction loading completed - {} factions processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading factions from file", e);
      }
    } else {
      log.warn("Factions file not found: {}", resource.getPath());
    }
  }

  private void syncTeamsFromFile() {
    if (skipIfNotEmpty && teamService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("teams.json");
    if (resource.exists()) {
      log.debug("Loading teams from file: {}", resource.getPath());
      ObjectMapper mapper = objectMapper;
      try (var is = resource.getInputStream()) {
        List<TeamImportDTO> dtos = mapper.readValue(is, new TypeReference<>() {});
        // Pre-load wrestlers and NPCs once to avoid per-member / per-manager queries
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        Map<String, Npc> npcByName =
            npcService.findAll().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        for (TeamImportDTO dto : dtos) {
          Optional<Wrestler> wrestler1Opt =
              Optional.ofNullable(wrestlersByName.get(dto.getWrestler1()));
          Optional<Wrestler> wrestler2Opt =
              Optional.ofNullable(wrestlersByName.get(dto.getWrestler2()));
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
                Npc manager = npcByName.get(dto.getManager());
                if (manager != null) {
                  team.setManager(manager);
                  teamRepository.save(team);
                }
              }
            }
          }
          log.debug("Loaded team: {}", dto.getName());
        }
        log.debug("Team loading completed - {} teams processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading teams from file", e);
      }
    } else {
      log.warn("Teams file not found: {}", resource.getPath());
    }
  }

  private void syncLocationsFromFile() {
    if (skipIfNotEmpty && locationRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("locations.json");
    if (resource.exists()) {
      log.debug("Loading locations from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<LocationImportDTO> locationsFromFile =
            objectMapper.readValue(is, new TypeReference<>() {});
        if (locationsFromFile == null) {
          return;
        }

        List<Location> toSave = new ArrayList<>();
        log.debug("Found {} locations in JSON file", locationsFromFile.size());
        for (LocationImportDTO dto : locationsFromFile) {
          Optional<Location> existingLocation = locationRepository.findByName(dto.getName());
          if (existingLocation.isEmpty()) {
            toSave.add(
                Location.builder()
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .imageUrl(dto.getImageUrl())
                    .culturalTags(dto.getCulturalTags())
                    .build());
          } else {
            Location existing = existingLocation.get();
            boolean changed = false;
            if (!dto.getDescription().equals(existing.getDescription())) {
              existing.setDescription(dto.getDescription());
              changed = true;
            }
            if (dto.getImageUrl() != null && !dto.getImageUrl().equals(existing.getImageUrl())) {
              existing.setImageUrl(dto.getImageUrl());
              changed = true;
            }
            if (dto.getCulturalTags() != null
                && !dto.getCulturalTags().equals(existing.getCulturalTags())) {
              existing.setCulturalTags(dto.getCulturalTags());
              changed = true;
            }
            if (changed) {
              toSave.add(existing);
            }
          }
        }
        locationRepository.saveAll(toSave);
        locationRepository.flush();
        log.debug("Location loading completed - {} locations processed", locationsFromFile.size());

      } catch (IOException e) {
        log.error("Error loading locations from file", e);
      }
    } else {
      log.warn("Locations file not found: {}", resource.getPath());
    }
  }

  private void syncArenasFromFile() {
    if (skipIfNotEmpty && arenaRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("arenas.json");
    if (resource.exists()) {
      log.debug("Loading arenas from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<ArenaImportDTO> arenasFromFile = objectMapper.readValue(is, new TypeReference<>() {});
        if (arenasFromFile == null) {
          return;
        }

        Map<String, Arena> existingByName =
            arenaRepository.findAllWithLocation().stream()
                .collect(Collectors.toMap(Arena::getName, a -> a));

        List<Arena> toSave = new ArrayList<>();
        log.debug("Found {} arenas in JSON file", arenasFromFile.size());
        for (ArenaImportDTO dto : arenasFromFile) {
          Optional<Arena> existingArena = Optional.ofNullable(existingByName.get(dto.getName()));
          if (existingArena.isEmpty()) {
            Optional<Location> location = locationRepository.findByName(dto.getLocation());
            if (location.isPresent()) {
              toSave.add(
                  Arena.builder()
                      .name(dto.getName())
                      .description(dto.getDescription())
                      .location(location.get())
                      .capacity(dto.getCapacity())
                      .alignmentBias(dto.getAlignmentBias())
                      .imageUrl(dto.getImageUrl())
                      .environmentalTraits(dto.getEnvironmentalTraits())
                      .build());
            }
          } else {
            Arena existing = existingArena.get();
            boolean changed = false;
            if (!Objects.equals(dto.getDescription(), existing.getDescription())) {
              existing.setDescription(dto.getDescription());
              changed = true;
            }
            if (dto.getCapacity() != existing.getCapacity()) {
              existing.setCapacity(dto.getCapacity());
              changed = true;
            }
            if (dto.getAlignmentBias() != existing.getAlignmentBias()) {
              existing.setAlignmentBias(dto.getAlignmentBias());
              changed = true;
            }
            if (!Objects.equals(dto.getImageUrl(), existing.getImageUrl())) {
              existing.setImageUrl(dto.getImageUrl());
              changed = true;
            }
            if (!Objects.equals(dto.getEnvironmentalTraits(), existing.getEnvironmentalTraits())) {
              existing.setEnvironmentalTraits(dto.getEnvironmentalTraits());
              changed = true;
            }
            Optional<Location> locationOpt = locationRepository.findByName(dto.getLocation());
            if (locationOpt.isPresent() && !locationOpt.get().equals(existing.getLocation())) {
              existing.setLocation(locationOpt.get());
              changed = true;
            }
            if (changed) {
              toSave.add(existing);
            }
          }
        }
        arenaRepository.saveAll(toSave);
        log.debug("Arena loading completed - {} arenas processed", arenasFromFile.size());
      } catch (IOException e) {
        log.error("Error loading arenas from file", e);
      }
    } else {
      log.warn("Arenas file not found: {}", resource.getPath());
    }
  }

  private void syncRelationshipsFromFile() {
    if (skipIfNotEmpty && relationshipService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("relationships.json");
    if (resource.exists()) {
      log.debug("Loading relationships from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RelationshipImportDTO> dtos =
            objectMapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<>() {});

        // Pre-load wrestlers once to avoid 2 queries per relationship
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));

        for (RelationshipImportDTO dto : dtos) {
          Optional<Wrestler> w1 = Optional.ofNullable(wrestlersByName.get(dto.getWrestler1()));
          Optional<Wrestler> w2 = Optional.ofNullable(wrestlersByName.get(dto.getWrestler2()));

          if (w1.isPresent() && w2.isPresent()) {
            relationshipService.createOrUpdateRelationship(
                w1.get().getId(),
                w2.get().getId(),
                dto.getType(),
                dto.getLevel(),
                dto.getIsStoryline(),
                dto.getNotes());
          }
        }
        log.debug("Relationship loading completed - {} relationships processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading relationships from file", e);
      }
    } else {
      log.warn("Relationships file not found: {}", resource.getPath());
    }
  }

  void syncOutcomeMatricesFromFiles() {
    if (skipIfNotEmpty && outcomeMatrixService.count() > 0) {
      return;
    }
    try {
      Resource[] resources =
          resourcePatternResolver.getResources("classpath*:outcome_matrices/*.json");
      // First pass: create or update all matrices (without redirect FKs)
      for (Resource res : resources) {
        if (!res.exists()) {
          continue;
        }
        log.debug("Loading outcome matrix from file: {}", res.getFilename());
        try (var is = res.getInputStream()) {
          OutcomeMatrixImportDTO dto =
              objectMapper.readValue(is, new TypeReference<OutcomeMatrixImportDTO>() {});
          OutcomeMatrix matrix =
              outcomeMatrixService.getByName(dto.getName()).orElseGet(OutcomeMatrix::new);
          matrix.setName(dto.getName());
          matrix.setDescription(dto.getDescription());
          try {
            matrix.setCategory(OutcomeMatrixCategory.valueOf(dto.getCategory()));
          } catch (IllegalArgumentException e) {
            log.warn(
                "Unknown OutcomeMatrixCategory '{}' in file {}",
                dto.getCategory(),
                res.getFilename());
            continue;
          }
          matrix = outcomeMatrixService.createMatrix(matrix);

          if (dto.getEntries() != null) {
            Map<Integer, OutcomeMatrixEntry> existingByRoll =
                outcomeMatrixService.getEntries(matrix.getId()).stream()
                    .collect(Collectors.toMap(OutcomeMatrixEntry::getDiceRoll, e -> e));
            for (OutcomeMatrixEntryImportDTO entryDto : dto.getEntries()) {
              OutcomeMatrixEntry entry =
                  existingByRoll.getOrDefault(entryDto.getDiceRoll(), new OutcomeMatrixEntry());
              entry.setDiceRoll(entryDto.getDiceRoll());
              entry.setTemplateText(entryDto.getTemplateText());
              entry.setHeatDelta(entryDto.getHeatDelta());
              entry.setFanDelta(entryDto.getFanDelta());
              entry.setTvGradeDelta(entryDto.getTvGradeDelta());
              entry.setGrudgeGradeDelta(entryDto.getGrudgeGradeDelta());
              entry.setInjuryCaused(entryDto.isInjuryCaused());
              // Redirect resolved in second pass
              if (entry.getId() == null) {
                outcomeMatrixService.addEntry(matrix.getId(), entry);
              } else {
                outcomeMatrixService.updateEntry(entry);
              }
            }
          }
        } catch (IOException e) {
          log.error("Error loading outcome matrix from file {}", res.getFilename(), e);
        }
      }

      // Second pass: wire redirect FKs by name
      for (Resource res : resources) {
        if (!res.exists()) {
          continue;
        }
        try (var is = res.getInputStream()) {
          OutcomeMatrixImportDTO dto =
              objectMapper.readValue(is, new TypeReference<OutcomeMatrixImportDTO>() {});
          if (dto.getEntries() == null) {
            continue;
          }
          OutcomeMatrix matrix = outcomeMatrixService.getByName(dto.getName()).orElse(null);
          if (matrix == null) {
            continue;
          }
          for (OutcomeMatrixEntryImportDTO entryDto : dto.getEntries()) {
            if (entryDto.getRedirectToMatrix() == null) {
              continue;
            }
            outcomeMatrixService.getEntries(matrix.getId()).stream()
                .filter(e -> e.getDiceRoll() == entryDto.getDiceRoll())
                .findFirst()
                .ifPresent(
                    entry -> {
                      outcomeMatrixService
                          .getByName(entryDto.getRedirectToMatrix())
                          .ifPresentOrElse(
                              target -> {
                                entry.setRedirectToMatrix(target);
                                outcomeMatrixService.updateEntry(entry);
                              },
                              () ->
                                  log.warn(
                                      "Redirect matrix '{}' not found for entry roll={} in '{}'",
                                      entryDto.getRedirectToMatrix(),
                                      entryDto.getDiceRoll(),
                                      dto.getName()));
                    });
          }
        } catch (IOException e) {
          log.error("Error wiring redirects for file {}", res.getFilename(), e);
        }
      }
      log.debug("Outcome matrix loading complete.");
    } catch (IOException e) {
      log.error("Error resolving outcome_matrices resources", e);
    }
  }
}
