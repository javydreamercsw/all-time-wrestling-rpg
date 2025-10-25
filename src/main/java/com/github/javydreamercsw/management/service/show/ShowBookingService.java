package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
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
 * Service for automated show booking in the ATW RPG system. Handles intelligent segment creation
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
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRepository segmentRepository;
  private final WrestlerRepository wrestlerRepository;
  private final SeasonService seasonService;
  private final RivalryService rivalryService;
  private final NPCSegmentResolutionService npcSegmentResolutionService;
  private final PromoBookingService promoBookingService;
  private final SegmentRuleService segmentRuleService;
  private final Clock clock;
  private final Random random;

  /**
   * Book a wrestling show with template and date support.
   *
   * @param showName Name of the show
   * @param showDescription Description of the show
   * @param showTypeName Type of show (e.g., "Weekly Show", "PPV")
   * @param segmentCount Number of segments to book (3-8 for regular shows, 5-10 for PPVs)
   * @param templateName Name of the show template to use (optional)
   * @param showDate Date when the show takes place (optional)
   * @return The booked show with all segments
   */
  @Transactional
  public Optional<Show> bookShow(
      @NonNull String showName,
      @NonNull String showDescription,
      @NonNull String showTypeName,
      int segmentCount,
      String templateName,
      LocalDate showDate) {
    return bookShowInternal(
        showName, showDescription, showTypeName, segmentCount, templateName, showDate);
  }

  /**
   * Book a complete show with automatically generated segments based on storylines and rivalries.
   *
   * @param showName Name of the show
   * @param showDescription Description of the show
   * @param showTypeName Type of show (e.g., "Weekly Show", "PPV")
   * @param segmentCount Number of segments to book (3-8 for regular shows, 5-10 for PPVs)
   * @return The booked show with all segments
   */
  @Transactional
  public Optional<Show> bookShow(
      @NonNull String showName,
      @NonNull String showDescription,
      @NonNull String showTypeName,
      int segmentCount) {
    return bookShowInternal(showName, showDescription, showTypeName, segmentCount, null, null);
  }

  /** Internal method to book a show with all parameters. */
  private Optional<Show> bookShowInternal(
      @NonNull String showName,
      @NonNull String showDescription,
      @NonNull String showTypeName,
      int segmentCount,
      String templateName,
      LocalDate showDate) {
    try {
      // Validate inputs
      if (segmentCount < 3 || segmentCount > 10) {
        log.warn("Invalid segment count: {}. Must be between 3 and 10", segmentCount);
        return Optional.empty();
      }

      log.info(
          "Booking show '{}' with description '{}' of type '{}' with {} segments.",
          showName,
          showDescription,
          showTypeName,
          segmentCount);

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

      // Generate segments and promos for the show
      List<Segment> segments = generateSegmentsForShow(savedShow, segmentCount);
      List<Segment> promos = generatePromosForShow(savedShow, segmentCount);

      log.info(
          "Successfully booked show '{}' with {} segments and {} promos",
          showName,
          segments.size(),
          promos.size());
      return Optional.of(savedShow);

    } catch (Exception e) {
      log.error("Error booking show '{}': {}", showName, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Book a PPV show with enhanced segment quality and storyline focus.
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

      // Generate 6-8 segments for PPV (higher quality, storyline-focused)
      int segmentCount = 6 + random.nextInt(3); // 6-8 segments
      List<Segment> segments = generatePPVSegments(savedShow, segmentCount);
      List<Segment> promos = generatePromosForPPV(savedShow, segmentCount);

      log.info(
          "Successfully booked PPV '{}' with {} segments and {} promos",
          ppvName,
          segments.size(),
          promos.size());
      return Optional.of(savedShow);

    } catch (Exception e) {
      log.error("Error booking PPV '{}': {}", ppvName, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /** Generate segments for a regular show based on storylines and wrestler availability. */
  private List<Segment> generateSegmentsForShow(@NonNull Show show, int segmentCount) {
    List<Segment> segments = new ArrayList<>();
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());
    log.info("Found {} available wrestlers for show booking.", availableWrestlers.size());

    if (availableWrestlers.size() < 4) {
      log.warn("Not enough wrestlers available for show booking");
      return segments;
    }

    // Shuffle wrestlers for variety
    Collections.shuffle(availableWrestlers, random);

    // 1. Book rivalry segments first (high priority)
    segments.addAll(bookRivalrySegments(show, availableWrestlers, Math.min(2, segmentCount / 2)));

    // 2. Book random segments to fill remaining slots
    int remainingSegments = segmentCount - segments.size();
    segments.addAll(bookRandomSegments(show, availableWrestlers, remainingSegments));

    return segments;
  }

  /** Generate enhanced segments for PPV shows with focus on major storylines. */
  private List<Segment> generatePPVSegments(Show show, int segmentCount) {
    List<Segment> segments = new ArrayList<>();
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());

    if (availableWrestlers.size() < 6) {
      log.warn("Not enough wrestlers available for PPV booking");
      return segments;
    }

    Collections.shuffle(availableWrestlers, random);

    // 1. Book high-heat rivalry segments (60% of PPV segments)
    int rivalrySegments = Math.max(3, (segmentCount * 6) / 10);
    segments.addAll(bookHighHeatRivalrySegments(show, availableWrestlers, rivalrySegments));

    // 2. Book multi-person segments for variety (20% of PPV segments)
    int multiPersonSegments = Math.max(1, segmentCount / 5);
    segments.addAll(bookMultiPersonSegments(show, availableWrestlers, multiPersonSegments));

    // 3. Fill remaining slots with quality singles segments
    int remainingSegments = segmentCount - segments.size();
    segments.addAll(bookQualitySegments(show, availableWrestlers, remainingSegments));

    return segments;
  }

  /** Book segments based on active rivalries. */
  private List<Segment> bookRivalrySegments(
      Show show, List<Wrestler> availableWrestlers, int maxSegments) {
    List<Segment> segments = new ArrayList<>();
    List<Rivalry> activeRivalries = rivalryService.getActiveRivalries();

    int bookedSegments = 0;
    for (Rivalry rivalry : activeRivalries) {
      if (bookedSegments >= maxSegments) break;

      Wrestler wrestler1 = rivalry.getWrestler1();
      Wrestler wrestler2 = rivalry.getWrestler2();

      // Check if both wrestlers are available
      if (availableWrestlers.contains(wrestler1) && availableWrestlers.contains(wrestler2)) {
        Optional<Segment> segment =
            bookSinglesSegment(show, wrestler1, wrestler2, "Rivalry Segment");
        if (segment.isPresent()) {
          segments.add(segment.get());
          availableWrestlers.remove(wrestler1);
          availableWrestlers.remove(wrestler2);
          bookedSegments++;

          log.info(
              "Booked rivalry segment: {} vs {} (Heat: {})",
              wrestler1.getName(),
              wrestler2.getName(),
              rivalry.getHeat());
        }
      }
    }

    return segments;
  }

  /** Book high-heat rivalry segments for PPVs. */
  private List<Segment> bookHighHeatRivalrySegments(
      Show show, List<Wrestler> availableWrestlers, int maxSegments) {
    List<Segment> segments = new ArrayList<>();
    List<Rivalry> highHeatRivalries =
        rivalryService.getActiveRivalries().stream()
            .filter(rivalry -> rivalry.getHeat() >= 15) // High heat rivalries
            .toList();

    int bookedSegments = 0;
    for (Rivalry rivalry : highHeatRivalries) {
      if (bookedSegments >= maxSegments) break;

      Wrestler wrestler1 = rivalry.getWrestler1();
      Wrestler wrestler2 = rivalry.getWrestler2();

      if (availableWrestlers.contains(wrestler1) && availableWrestlers.contains(wrestler2)) {
        // Use special stipulations for high-heat segments
        String stipulation = getHighHeatStipulation(rivalry.getHeat());
        Optional<Segment> segment = bookSinglesSegment(show, wrestler1, wrestler2, stipulation);
        if (segment.isPresent()) {
          segments.add(segment.get());
          availableWrestlers.remove(wrestler1);
          availableWrestlers.remove(wrestler2);
          bookedSegments++;

          log.info(
              "Booked high-heat PPV segment: {} vs {} ({}, Heat: {})",
              wrestler1.getName(),
              wrestler2.getName(),
              stipulation,
              rivalry.getHeat());
        }
      }
    }

    return segments;
  }

  /** Book multi-person segments for variety. */
  private List<Segment> bookMultiPersonSegments(
      Show show, List<Wrestler> availableWrestlers, int maxSegments) {
    List<Segment> segments = new ArrayList<>();

    for (int i = 0; i < maxSegments && availableWrestlers.size() >= 3; i++) {
      // Create 3-4 person segments
      int participantCount = 3 + random.nextInt(2); // 3 or 4 wrestlers
      if (availableWrestlers.size() < participantCount) {
        participantCount = availableWrestlers.size();
      }

      List<Wrestler> participants = new ArrayList<>();
      for (int j = 0; j < participantCount; j++) {
        participants.add(availableWrestlers.remove(0));
      }

      Optional<Segment> segment = bookMultiPersonSegment(show, participants);
      if (segment.isPresent()) {
        segments.add(segment.get());
        log.info("Booked multi-person segment with {} wrestlers", participantCount);
      }
    }

    return segments;
  }

  /** Book random segments to fill show slots. */
  private List<Segment> bookRandomSegments(
      Show show, List<Wrestler> availableWrestlers, int maxSegments) {
    List<Segment> segments = new ArrayList<>();

    for (int i = 0; i < maxSegments && availableWrestlers.size() >= 2; i++) {
      Wrestler wrestler1 = availableWrestlers.remove(0);
      Wrestler wrestler2 = availableWrestlers.remove(0);

      Optional<Segment> segment = bookSinglesSegment(show, wrestler1, wrestler2, "Standard Match");
      if (segment.isPresent()) {
        segments.add(segment.get());
        log.debug("Booked random segment: {} vs {}", wrestler1.getName(), wrestler2.getName());
      }
    }

    return segments;
  }

  /** Book quality segments for PPV fill-in slots. */
  private List<Segment> bookQualitySegments(
      Show show, List<Wrestler> availableWrestlers, int maxSegments) {
    // For now, same as random segments but could be enhanced with tier matching
    return bookRandomSegments(show, availableWrestlers, maxSegments);
  }

  /** Generate promo segments for a regular show. */
  private List<Segment> generatePromosForShow(Show show, int segmentCount) {
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());
    Collections.shuffle(availableWrestlers, random);

    // Regular shows: 1-2 promos (20-30% of segment count)
    int promoCount = Math.max(1, segmentCount / 4); // 1-2 promos typically

    return promoBookingService.bookPromosForShow(show, availableWrestlers, promoCount);
  }

  /** Generate enhanced promo segments for PPV shows. */
  private List<Segment> generatePromosForPPV(Show show, int segmentCount) {
    List<Wrestler> availableWrestlers = new ArrayList<>(wrestlerRepository.findAll());
    Collections.shuffle(availableWrestlers, random);

    // PPVs: 2-4 promos (more storyline development)
    int promoCount = Math.max(2, segmentCount / 3); // 2-4 promos typically

    return promoBookingService.bookPromosForShow(show, availableWrestlers, promoCount);
  }

  // ==================== HELPER METHODS ====================

  /** Book a singles segment between two wrestlers. */
  private Optional<Segment> bookSinglesSegment(
      Show show, Wrestler wrestler1, Wrestler wrestler2, String stipulation) {
    try {
      // Get one-on-one segment type from database
      Optional<SegmentType> segmentTypeOpt = segmentTypeRepository.findByName("One on One");
      if (segmentTypeOpt.isEmpty()) {
        log.warn("One on One segment type not found in database");
        return Optional.empty();
      }

      // Create teams
      SegmentTeam team1 = new SegmentTeam(wrestler1);
      SegmentTeam team2 = new SegmentTeam(wrestler2);

      // Resolve the segment
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              team1, team2, segmentTypeOpt.get(), show, stipulation);

      return Optional.of(result);

    } catch (Exception e) {
      log.error("Error booking singles segment: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /** Book a multi-person segment. */
  private Optional<Segment> bookMultiPersonSegment(Show show, List<Wrestler> participants) {
    try {
      if (participants.size() < 3) {
        return Optional.empty();
      }

      // Get appropriate segment type from database
      String segmentTypeName =
          participants.size() == 3
              ? "Free-for-All"
              : "Free-for-All"; // Use Free-for-All for multi-person
      Optional<SegmentType> segmentTypeOpt = segmentTypeRepository.findByName(segmentTypeName);
      if (segmentTypeOpt.isEmpty()) {
        // Fallback to One on One if specific type not found
        segmentTypeOpt = segmentTypeRepository.findByName("One on One");
        if (segmentTypeOpt.isEmpty()) {
          log.warn("No suitable segment type found for multi-person segment");
          return Optional.empty();
        }
      }

      // Create teams (each wrestler is their own team)
      List<SegmentTeam> teams = participants.stream().map(SegmentTeam::new).toList();

      // Resolve the segment
      Segment result =
          npcSegmentResolutionService.resolveMultiTeamSegment(
              teams, segmentTypeOpt.get(), show, segmentTypeName + " Segment");

      return Optional.of(result);

    } catch (Exception e) {
      log.error("Error booking multi-person segment: {}", e.getMessage(), e);
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
                  "Steel Cage Segment",
                  "Hell in a Cell",
                  "Last Man Standing",
                  "I Quit Segment",
                  "Hardcore Segment",
                  "Buried Alive Segment",
                  "Inferno Segment")));
    } else if (heat >= 20) {
      // High heat - get intense stipulations from database
      availableStipulations.addAll(
          getStipulationsByNames(
              List.of(
                  "No Disqualification",
                  "Falls Count Anywhere",
                  "Steel Cage Segment",
                  "Submission Segment",
                  "Iron Man Segment",
                  "Ladder Segment",
                  "Tables Segment")));
    } else {
      // Medium heat - get standard enhanced segments from database
      availableStipulations.addAll(
          getStipulationsByNames(
              List.of(
                  "No Count Out", "No Disqualification", "Submission Segment", "Chairs Segment")));
    }

    // If no specific stipulations found, fall back to high heat rules from database
    if (availableStipulations.isEmpty()) {
      List<SegmentRule> highHeatRules = segmentRuleService.getHighHeatRules();
      if (!highHeatRules.isEmpty()) {
        SegmentRule selectedRule = highHeatRules.get(random.nextInt(highHeatRules.size()));
        return selectedRule.getName();
      }
      // Ultimate fallback
      return "Standard Match";
    }

    return availableStipulations.get(random.nextInt(availableStipulations.size()));
  }

  /** Get rule names from database, filtering out non-existent ones. */
  private List<String> getStipulationsByNames(List<String> requestedNames) {
    return requestedNames.stream().filter(name -> segmentRuleService.existsByName(name)).toList();
  }

  /** Get all segments for a specific show. */
  public List<Segment> getSegmentsForShow(Long showId) {
    return showRepository.findById(showId).map(segmentRepository::findByShow).orElse(List.of());
  }

  /** Get show statistics. */
  public ShowStatistics getShowStatistics(Long showId) {
    Optional<Show> showOpt = showRepository.findById(showId);
    if (showOpt.isEmpty()) {
      return new ShowStatistics(0, 0, 0, 0, 0);
    }

    List<Segment> allSegments = getSegmentsForShow(showId);

    // Separate segments from promos
    List<Segment> segments =
        allSegments.stream()
            .filter(segment -> !promoBookingService.isPromoSegment(segment))
            .toList();

    List<Segment> promos =
        allSegments.stream().filter(promoBookingService::isPromoSegment).toList();

    int totalSegments = segments.size();
    int totalPromos = promos.size();
    int totalWrestlers =
        (int)
            allSegments.stream()
                .flatMap(segment -> segment.getWrestlers().stream())
                .distinct()
                .count();

    int rivalrySegments =
        (int)
            segments.stream()
                .filter(segment -> segment.getWrestlers().size() == 2)
                .filter(this::isRivalrySegment)
                .count();

    int multiPersonSegments =
        (int) segments.stream().filter(segment -> segment.getWrestlers().size() > 2).count();

    return new ShowStatistics(
        totalSegments, totalWrestlers, rivalrySegments, multiPersonSegments, totalPromos);
  }

  /** Check if a segment involves wrestlers with an active rivalry. */
  private boolean isRivalrySegment(Segment segment) {
    List<Wrestler> wrestlers = segment.getWrestlers();
    if (wrestlers.size() != 2) return false;

    return rivalryService
        .getRivalryBetweenWrestlers(wrestlers.get(0).getId(), wrestlers.get(1).getId())
        .isPresent();
  }

  /** Record class for show statistics. */
  public record ShowStatistics(
      int totalSegments,
      int totalWrestlers,
      int rivalrySegments,
      int multiPersonSegments,
      int totalPromos) {}
}
