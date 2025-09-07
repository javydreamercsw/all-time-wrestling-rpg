package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.show.match.rule.MatchRule;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.match.MatchRuleService;
import com.github.javydreamercsw.management.service.match.MatchTeam;
import com.github.javydreamercsw.management.service.match.NPCMatchResolutionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for automated show booking in the ATW RPG system. Handles intelligent match creation
 * based on storylines, rivalries, and wrestler availability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShowBookingService {

  private final ShowRepository showRepository;
  private final ShowTypeRepository showTypeRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final MatchTypeRepository matchTypeRepository;
  private final MatchRepository matchRepository;
  private final WrestlerRepository wrestlerRepository;
  private final SeasonService seasonService;
  private final RivalryService rivalryService;
  private final NPCMatchResolutionService npcMatchResolutionService;
  private final PromoBookingService promoBookingService;
  private final MatchRuleService matchRuleService;
  private final Clock clock;
  private final Random random = new Random();

  /**
   * Book a wrestling show with template and date support.
   *
   * @param showName Name of the show
   * @param showDescription Description of the show
   * @param showTypeName Type of show (e.g., "Weekly Show", "PPV")
   * @param matchCount Number of matches to book (3-8 for regular shows, 5-10 for PPVs)
   * @param templateName Name of the show template to use (optional)
   * @param showDate Date when the show takes place (optional)
   * @return The booked show with all matches
   */
  @Transactional
  public Optional<Show> bookShow(
      @NonNull String showName,
      @NonNull String showDescription,
      @NonNull String showTypeName,
      int matchCount,
      String templateName,
      LocalDate showDate) {
    return bookShowInternal(
        showName, showDescription, showTypeName, matchCount, templateName, showDate);
  }

  /**
   * Book a complete show with automatically generated matches based on storylines and rivalries.
   *
   * @param showName Name of the show
   * @param showDescription Description of the show
   * @param showTypeName Type of show (e.g., "Weekly Show", "PPV")
   * @param matchCount Number of matches to book (3-8 for regular shows, 5-10 for PPVs)
   * @return The booked show with all matches
   */
  @Transactional
  public Optional<Show> bookShow(
      @NonNull String showName,
      @NonNull String showDescription,
      @NonNull String showTypeName,
      int matchCount) {
    return bookShowInternal(showName, showDescription, showTypeName, matchCount, null, null);
  }

  /** Internal method to book a show with all parameters. */
  private Optional<Show> bookShowInternal(
      @NonNull String showName,
      @NonNull String showDescription,
      @NonNull String showTypeName,
      int matchCount,
      String templateName,
      LocalDate showDate) {
    try {
      // Validate inputs
      if (matchCount < 3 || matchCount > 10) {
        log.warn("Invalid match count: {}. Must be between 3 and 10", matchCount);
        return Optional.empty();
      }

      // Get show type
      Optional<ShowType> showTypeOpt = showTypeRepository.findByName(showTypeName);
      if (showTypeOpt.isEmpty()) {
        log.warn("Show type not found: {}", showTypeName);
        return Optional.empty();
      }

      // Get show template if specified
      ShowTemplate template = null;
      if (templateName != null && !templateName.trim().isEmpty()) {
        Optional<ShowTemplate> templateOpt = showTemplateRepository.findByName(templateName);
        if (templateOpt.isPresent()) {
          template = templateOpt.get();
          log.info("Using show template: {}", templateName);
        } else {
          log.warn("Show template not found: {}", templateName);
        }
      }

      // Create the show
      Show show = new Show();
      show.setName(showName);
      show.setDescription(showDescription);
      show.setType(showTypeOpt.get());
      show.setTemplate(template);
      show.setShowDate(showDate);

      Show savedShow = showRepository.save(show);

      // Add show to active season
      seasonService.addShowToActiveSeason(savedShow);

      // Generate matches and promos for the show
      List<Match> matches = generateMatchesForShow(savedShow, matchCount);
      List<Match> promos = generatePromosForShow(savedShow, matchCount);

      log.info(
          "Successfully booked show '{}' with {} matches and {} promos",
          showName,
          matches.size(),
          promos.size());
      return Optional.of(savedShow);

    } catch (Exception e) {
      log.error("Error booking show '{}': {}", showName, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Book a PPV show with enhanced match quality and storyline focus.
   *
   * @param ppvName Name of the PPV
   * @param ppvDescription Description of the PPV
   * @return The booked PPV show
   */
  @Transactional
  public Optional<Show> bookPPV(@NonNull String ppvName, @NonNull String ppvDescription) {
    try {
      // Get PPV show type
      Optional<ShowType> ppvTypeOpt = showTypeRepository.findByName("PPV");
      if (ppvTypeOpt.isEmpty()) {
        // Create PPV show type if it doesn't exist
        ShowType ppvType = new ShowType();
        ppvType.setName("PPV");
        ppvType.setDescription("Pay-Per-View special event");
        ppvType = showTypeRepository.save(ppvType);
        ppvTypeOpt = Optional.of(ppvType);
      }

      // Create the PPV show
      Show ppvShow = new Show();
      ppvShow.setName(ppvName);
      ppvShow.setDescription(ppvDescription);
      ppvShow.setType(ppvTypeOpt.get());

      Show savedShow = showRepository.save(ppvShow);

      // Add show to active season
      seasonService.addShowToActiveSeason(savedShow);

      // Generate 6-8 matches for PPV (higher quality, storyline-focused)
      int matchCount = 6 + random.nextInt(3); // 6-8 matches
      List<Match> matches = generatePPVMatches(savedShow, matchCount);
      List<Match> promos = generatePromosForPPV(savedShow, matchCount);

      log.info(
          "Successfully booked PPV '{}' with {} matches and {} promos",
          ppvName,
          matches.size(),
          promos.size());
      return Optional.of(savedShow);

    } catch (Exception e) {
      log.error("Error booking PPV '{}': {}", ppvName, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /** Generate matches for a regular show based on storylines and wrestler availability. */
  private List<Match> generateMatchesForShow(@NonNull Show show, int matchCount) {
    List<Match> matches = new ArrayList<>();
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());

    if (availableWrestlers.size() < 4) {
      log.warn("Not enough wrestlers available for show booking");
      return matches;
    }

    // Shuffle wrestlers for variety
    Collections.shuffle(availableWrestlers, random);

    // 1. Book rivalry matches first (high priority)
    matches.addAll(bookRivalryMatches(show, availableWrestlers, Math.min(2, matchCount / 2)));

    // 2. Book random matches to fill remaining slots
    int remainingMatches = matchCount - matches.size();
    matches.addAll(bookRandomMatches(show, availableWrestlers, remainingMatches));

    return matches;
  }

  /** Generate enhanced matches for PPV shows with focus on major storylines. */
  private List<Match> generatePPVMatches(Show show, int matchCount) {
    List<Match> matches = new ArrayList<>();
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());

    if (availableWrestlers.size() < 6) {
      log.warn("Not enough wrestlers available for PPV booking");
      return matches;
    }

    Collections.shuffle(availableWrestlers, random);

    // 1. Book high-heat rivalry matches (60% of PPV matches)
    int rivalryMatches = Math.max(3, (matchCount * 6) / 10);
    matches.addAll(bookHighHeatRivalryMatches(show, availableWrestlers, rivalryMatches));

    // 2. Book multi-person matches for variety (20% of PPV matches)
    int multiPersonMatches = Math.max(1, matchCount / 5);
    matches.addAll(bookMultiPersonMatches(show, availableWrestlers, multiPersonMatches));

    // 3. Fill remaining slots with quality singles matches
    int remainingMatches = matchCount - matches.size();
    matches.addAll(bookQualityMatches(show, availableWrestlers, remainingMatches));

    return matches;
  }

  /** Book matches based on active rivalries. */
  private List<Match> bookRivalryMatches(
      Show show, List<Wrestler> availableWrestlers, int maxMatches) {
    List<Match> matches = new ArrayList<>();
    List<Rivalry> activeRivalries = rivalryService.getActiveRivalries();

    int bookedMatches = 0;
    for (Rivalry rivalry : activeRivalries) {
      if (bookedMatches >= maxMatches) break;

      Wrestler wrestler1 = rivalry.getWrestler1();
      Wrestler wrestler2 = rivalry.getWrestler2();

      // Check if both wrestlers are available
      if (availableWrestlers.contains(wrestler1) && availableWrestlers.contains(wrestler2)) {
        Optional<Match> match = bookSinglesMatch(show, wrestler1, wrestler2, "Rivalry Match");
        if (match.isPresent()) {
          matches.add(match.get());
          availableWrestlers.remove(wrestler1);
          availableWrestlers.remove(wrestler2);
          bookedMatches++;

          log.info(
              "Booked rivalry match: {} vs {} (Heat: {})",
              wrestler1.getName(),
              wrestler2.getName(),
              rivalry.getHeat());
        }
      }
    }

    return matches;
  }

  /** Book high-heat rivalry matches for PPVs. */
  private List<Match> bookHighHeatRivalryMatches(
      Show show, List<Wrestler> availableWrestlers, int maxMatches) {
    List<Match> matches = new ArrayList<>();
    List<Rivalry> highHeatRivalries =
        rivalryService.getActiveRivalries().stream()
            .filter(rivalry -> rivalry.getHeat() >= 15) // High heat rivalries
            .toList();

    int bookedMatches = 0;
    for (Rivalry rivalry : highHeatRivalries) {
      if (bookedMatches >= maxMatches) break;

      Wrestler wrestler1 = rivalry.getWrestler1();
      Wrestler wrestler2 = rivalry.getWrestler2();

      if (availableWrestlers.contains(wrestler1) && availableWrestlers.contains(wrestler2)) {
        // Use special stipulations for high-heat matches
        String stipulation = getHighHeatStipulation(rivalry.getHeat());
        Optional<Match> match = bookSinglesMatch(show, wrestler1, wrestler2, stipulation);
        if (match.isPresent()) {
          matches.add(match.get());
          availableWrestlers.remove(wrestler1);
          availableWrestlers.remove(wrestler2);
          bookedMatches++;

          log.info(
              "Booked high-heat PPV match: {} vs {} ({}, Heat: {})",
              wrestler1.getName(),
              wrestler2.getName(),
              stipulation,
              rivalry.getHeat());
        }
      }
    }

    return matches;
  }

  /** Book multi-person matches for variety. */
  private List<Match> bookMultiPersonMatches(
      Show show, List<Wrestler> availableWrestlers, int maxMatches) {
    List<Match> matches = new ArrayList<>();

    for (int i = 0; i < maxMatches && availableWrestlers.size() >= 3; i++) {
      // Create 3-4 person matches
      int participantCount = 3 + random.nextInt(2); // 3 or 4 wrestlers
      if (availableWrestlers.size() < participantCount) {
        participantCount = availableWrestlers.size();
      }

      List<Wrestler> participants = new ArrayList<>();
      for (int j = 0; j < participantCount; j++) {
        participants.add(availableWrestlers.remove(0));
      }

      Optional<Match> match = bookMultiPersonMatch(show, participants);
      if (match.isPresent()) {
        matches.add(match.get());
        log.info("Booked multi-person match with {} wrestlers", participantCount);
      }
    }

    return matches;
  }

  /** Book random matches to fill show slots. */
  private List<Match> bookRandomMatches(
      Show show, List<Wrestler> availableWrestlers, int maxMatches) {
    List<Match> matches = new ArrayList<>();

    for (int i = 0; i < maxMatches && availableWrestlers.size() >= 2; i++) {
      Wrestler wrestler1 = availableWrestlers.remove(0);
      Wrestler wrestler2 = availableWrestlers.remove(0);

      Optional<Match> match = bookSinglesMatch(show, wrestler1, wrestler2, "Standard Match");
      if (match.isPresent()) {
        matches.add(match.get());
        log.debug("Booked random match: {} vs {}", wrestler1.getName(), wrestler2.getName());
      }
    }

    return matches;
  }

  /** Book quality matches for PPV fill-in slots. */
  private List<Match> bookQualityMatches(
      Show show, List<Wrestler> availableWrestlers, int maxMatches) {
    // For now, same as random matches but could be enhanced with tier matching
    return bookRandomMatches(show, availableWrestlers, maxMatches);
  }

  /** Generate promo segments for a regular show. */
  private List<Match> generatePromosForShow(Show show, int matchCount) {
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());
    Collections.shuffle(availableWrestlers, random);

    // Regular shows: 1-2 promos (20-30% of match count)
    int promoCount = Math.max(1, matchCount / 4); // 1-2 promos typically

    return promoBookingService.bookPromosForShow(show, availableWrestlers, promoCount);
  }

  /** Generate enhanced promo segments for PPV shows. */
  private List<Match> generatePromosForPPV(Show show, int matchCount) {
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());
    Collections.shuffle(availableWrestlers, random);

    // PPVs: 2-4 promos (more storyline development)
    int promoCount = Math.max(2, matchCount / 3); // 2-4 promos typically

    return promoBookingService.bookPromosForShow(show, availableWrestlers, promoCount);
  }

  // ==================== HELPER METHODS ====================

  /** Book a singles match between two wrestlers. */
  private Optional<Match> bookSinglesMatch(
      Show show, Wrestler wrestler1, Wrestler wrestler2, String stipulation) {
    try {
      // Get one-on-one match type from database
      Optional<MatchType> matchTypeOpt = matchTypeRepository.findByName("One on One");
      if (matchTypeOpt.isEmpty()) {
        log.warn("One on One match type not found in database");
        return Optional.empty();
      }

      // Create teams
      MatchTeam team1 = new MatchTeam(wrestler1);
      MatchTeam team2 = new MatchTeam(wrestler2);

      // Resolve the match
      Match result =
          npcMatchResolutionService.resolveTeamMatch(
              team1, team2, matchTypeOpt.get(), show, stipulation);

      return Optional.of(result);

    } catch (Exception e) {
      log.error("Error booking singles match: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /** Book a multi-person match. */
  private Optional<Match> bookMultiPersonMatch(Show show, List<Wrestler> participants) {
    try {
      if (participants.size() < 3) {
        return Optional.empty();
      }

      // Get appropriate match type from database
      String matchTypeName =
          participants.size() == 3
              ? "Free-for-All"
              : "Free-for-All"; // Use Free-for-All for multi-person
      Optional<MatchType> matchTypeOpt = matchTypeRepository.findByName(matchTypeName);
      if (matchTypeOpt.isEmpty()) {
        // Fallback to One on One if specific type not found
        matchTypeOpt = matchTypeRepository.findByName("One on One");
        if (matchTypeOpt.isEmpty()) {
          log.warn("No suitable match type found for multi-person match");
          return Optional.empty();
        }
      }

      // Create teams (each wrestler is their own team)
      List<MatchTeam> teams = participants.stream().map(MatchTeam::new).toList();

      // Resolve the match
      Match result =
          npcMatchResolutionService.resolveMultiTeamMatch(
              teams, matchTypeOpt.get(), show, matchTypeName + " Match");

      return Optional.of(result);

    } catch (Exception e) {
      log.error("Error booking multi-person match: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /** Get appropriate rule for high-heat rivalries using database-driven rule selection. */
  private String getHighHeatStipulation(int heat) {
    List<String> availableStipulations = new ArrayList<>();

    if (heat >= 30) {
      // Extreme heat - get the most dangerous stipulations from database
      availableStipulations.addAll(
          getStipulationsByNames(
              List.of(
                  "Steel Cage Match",
                  "Hell in a Cell",
                  "Last Man Standing",
                  "I Quit Match",
                  "Hardcore Match",
                  "Buried Alive Match",
                  "Inferno Match")));
    } else if (heat >= 20) {
      // High heat - get intense stipulations from database
      availableStipulations.addAll(
          getStipulationsByNames(
              List.of(
                  "No Disqualification",
                  "Falls Count Anywhere",
                  "Steel Cage Match",
                  "Submission Match",
                  "Iron Man Match",
                  "Ladder Match",
                  "Tables Match")));
    } else {
      // Medium heat - get standard enhanced matches from database
      availableStipulations.addAll(
          getStipulationsByNames(
              List.of("No Count Out", "No Disqualification", "Submission Match", "Chairs Match")));
    }

    // If no specific stipulations found, fall back to high heat rules from database
    if (availableStipulations.isEmpty()) {
      List<MatchRule> highHeatRules = matchRuleService.getHighHeatRules();
      if (!highHeatRules.isEmpty()) {
        MatchRule selectedRule = highHeatRules.get(random.nextInt(highHeatRules.size()));
        return selectedRule.getName();
      }
      // Ultimate fallback
      return "Standard Match";
    }

    return availableStipulations.get(random.nextInt(availableStipulations.size()));
  }

  /** Get rule names from database, filtering out non-existent ones. */
  private List<String> getStipulationsByNames(List<String> requestedNames) {
    return requestedNames.stream().filter(name -> matchRuleService.existsByName(name)).toList();
  }

  /** Get all matches for a specific show. */
  public List<Match> getMatchesForShow(Long showId) {
    return showRepository.findById(showId).map(matchRepository::findByShow).orElse(List.of());
  }

  /** Get show statistics. */
  public ShowStatistics getShowStatistics(Long showId) {
    Optional<Show> showOpt = showRepository.findById(showId);
    if (showOpt.isEmpty()) {
      return new ShowStatistics(0, 0, 0.0, 0, 0, 0);
    }

    List<Match> allSegments = getMatchesForShow(showId);

    // Separate matches from promos
    List<Match> matches =
        allSegments.stream()
            .filter(segment -> !promoBookingService.isPromoSegment(segment))
            .toList();

    List<Match> promos =
        allSegments.stream().filter(promoBookingService::isPromoSegment).toList();

    int totalMatches = matches.size();
    int totalPromos = promos.size();
    int totalWrestlers =
        (int)
            allSegments.stream()
                .flatMap(segment -> segment.getWrestlers().stream())
                .distinct()
                .count();

    double averageRating =
        allSegments.stream().mapToInt(Match::getMatchRating).average().orElse(0.0);

    int rivalryMatches =
        (int)
            matches.stream()
                .filter(match -> match.getWrestlers().size() == 2)
                .filter(this::isRivalryMatch)
                .count();

    int multiPersonMatches =
        (int) matches.stream().filter(match -> match.getWrestlers().size() > 2).count();

    return new ShowStatistics(
        totalMatches,
        totalWrestlers,
        averageRating,
        rivalryMatches,
        multiPersonMatches,
        totalPromos);
  }

  /** Check if a match involves wrestlers with an active rivalry. */
  private boolean isRivalryMatch(Match match) {
    List<Wrestler> wrestlers = match.getWrestlers();
    if (wrestlers.size() != 2) return false;

    return rivalryService
        .getRivalryBetweenWrestlers(wrestlers.get(0).getId(), wrestlers.get(1).getId())
        .isPresent();
  }

  /** Record class for show statistics. */
  public record ShowStatistics(
      int totalMatches,
      int totalWrestlers,
      double averageRating,
      int rivalryMatches,
      int multiPersonMatches,
      int totalPromos) {}
}