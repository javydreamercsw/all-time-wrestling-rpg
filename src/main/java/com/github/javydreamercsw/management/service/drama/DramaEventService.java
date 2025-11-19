package com.github.javydreamercsw.management.service.drama;

import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing drama events in the ATW RPG system. Handles creation, processing, and impact
 * resolution of random drama events that affect storylines and wrestler development.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DramaEventService {

  private final DramaEventRepository dramaEventRepository;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final RivalryService rivalryService;
  private final InjuryService injuryService;
  private final Clock clock;
  private final Random random;

  /**
   * Create a new drama event.
   *
   * @param primaryWrestlerId ID of the primary wrestler involved
   * @param secondaryWrestlerId ID of the secondary wrestler (optional)
   * @param eventType Type of drama event
   * @param severity Severity level of the event
   * @param title Event title
   * @param description Event description
   * @return Created drama event
   */
  @Transactional
  public Optional<DramaEvent> createDramaEvent(
      Long primaryWrestlerId,
      Long secondaryWrestlerId,
      DramaEventType eventType,
      DramaEventSeverity severity,
      String title,
      String description) {

    Optional<Wrestler> primaryWrestlerOpt = wrestlerRepository.findById(primaryWrestlerId);
    if (primaryWrestlerOpt.isEmpty()) {
      log.warn("Primary wrestler not found with ID: {}", primaryWrestlerId);
      return Optional.empty();
    }

    Wrestler primaryWrestler = primaryWrestlerOpt.get();
    Wrestler secondaryWrestler = null;

    if (secondaryWrestlerId != null) {
      Optional<Wrestler> secondaryWrestlerOpt = wrestlerRepository.findById(secondaryWrestlerId);
      if (secondaryWrestlerOpt.isEmpty()) {
        log.warn("Secondary wrestler not found with ID: {}", secondaryWrestlerId);
        return Optional.empty();
      }
      secondaryWrestler = secondaryWrestlerOpt.get();
    }

    DramaEvent event = new DramaEvent();
    event.setPrimaryWrestler(primaryWrestler);
    event.setSecondaryWrestler(secondaryWrestler);
    event.setEventType(eventType);
    event.setSeverity(severity);
    event.setTitle(title);
    event.setDescription(description);
    event.setEventDate(clock.instant());

    // Calculate potential impacts based on event type and severity
    calculateEventImpacts(event);

    DramaEvent savedEvent = dramaEventRepository.save(event);
    log.info("Created drama event: {} ({})", savedEvent.getTitle(), savedEvent.getSeverity());

    return Optional.of(savedEvent);
  }

  /**
   * Generate a random drama event for a wrestler.
   *
   * @param wrestlerId ID of the wrestler to generate event for
   * @return Generated drama event, or empty if wrestler not found
   */
  @Transactional
  public Optional<DramaEvent> generateRandomDramaEvent(Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    if (wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    Wrestler wrestler = wrestlerOpt.get();

    // Generate random event type and severity
    DramaEventType eventType = getRandomEventType();
    DramaEventSeverity severity = getRandomSeverity();

    // Determine if this should be a multi-wrestler event
    Wrestler secondaryWrestler = null;
    if (eventType.isMultiWrestlerType() && random.nextDouble() < 0.7) {
      secondaryWrestler = getRandomOpponent(wrestler);
    }

    // Generate title and description
    DramaEventTemplate template =
        generateEventTemplate(eventType, severity, wrestler, secondaryWrestler);

    DramaEvent event = new DramaEvent();
    event.setPrimaryWrestler(wrestler);
    event.setSecondaryWrestler(secondaryWrestler);
    event.setEventType(eventType);
    event.setSeverity(severity);
    event.setTitle(template.title());
    event.setDescription(template.description());
    event.setEventDate(clock.instant());

    calculateEventImpacts(event);

    DramaEvent savedEvent = dramaEventRepository.save(event);
    log.info("Generated random drama event: {} for {}", savedEvent.getTitle(), wrestler.getName());

    return Optional.of(savedEvent);
  }

  /**
   * Process all unprocessed drama events and apply their impacts.
   *
   * @return Number of events processed
   */
  @Transactional
  public int processUnprocessedEvents() {
    List<DramaEvent> unprocessedEvents =
        dramaEventRepository.findByIsProcessedFalseOrderByEventDateAsc();

    int processedCount = 0;
    for (DramaEvent event : unprocessedEvents) {
      try {
        processEvent(event);
        processedCount++;
      } catch (Exception e) {
        log.error("Failed to process drama event {}: {}", event.getId(), e.getMessage(), e);
      }
    }

    log.info("Processed {} drama events", processedCount);
    return processedCount;
  }

  /**
   * Process a specific drama event and apply its impacts.
   *
   * @param event The event to process
   */
  @Transactional
  public void processEvent(@NonNull DramaEvent event) {
    if (event.getIsProcessed()) {
      log.warn("Event {} is already processed", event.getId());
      return;
    }

    StringBuilder processingNotes = new StringBuilder();

    // Apply fan impact
    if (event.getFanImpact() != null && event.getFanImpact() != 0) {
      applyFanImpact(event, processingNotes);
    }

    // Apply heat impact
    if (event.getHeatImpact() != null
        && event.getHeatImpact() != 0
        && event.getSecondaryWrestler() != null) {
      applyHeatImpact(event, processingNotes);
    }

    // Handle injury
    if (event.getInjuryCaused()) {
      applyInjuryImpact(event, processingNotes);
    }

    // Handle rivalry creation
    if (event.getRivalryCreated() && event.getSecondaryWrestler() != null) {
      applyRivalryCreation(event, processingNotes);
    }

    // Handle rivalry ending
    if (event.getRivalryEnded() && event.getSecondaryWrestler() != null) {
      applyRivalryEnding(event, processingNotes);
    }

    event.markAsProcessed(processingNotes.toString());
    dramaEventRepository.save(event);

    log.info("Processed drama event: {} - {}", event.getTitle(), processingNotes.toString());
  }

  /**
   * Get all drama events for a wrestler.
   *
   * @param wrestlerId ID of the wrestler
   * @return List of drama events involving the wrestler
   */
  public List<DramaEvent> getEventsForWrestler(Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    if (wrestlerOpt.isEmpty()) {
      return List.of();
    }
    return dramaEventRepository.findByWrestler(wrestlerOpt.get());
  }

  /**
   * Get drama events for a wrestler with pagination.
   *
   * @param wrestlerId ID of the wrestler
   * @param pageable Pagination information
   * @return Page of drama events
   */
  public Page<DramaEvent> getEventsForWrestler(Long wrestlerId, Pageable pageable) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    if (wrestlerOpt.isEmpty()) {
      return Page.empty();
    }
    return dramaEventRepository.findByWrestler(wrestlerOpt.get(), pageable);
  }

  /**
   * Get recent drama events (within last 30 days).
   *
   * @return List of recent drama events
   */
  public List<DramaEvent> getRecentEvents() {
    Instant thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS);
    return dramaEventRepository.findRecentEvents(thirtyDaysAgo);
  }

  /**
   * Get drama events between two wrestlers.
   *
   * @param wrestler1Id ID of first wrestler
   * @param wrestler2Id ID of second wrestler
   * @return List of drama events between the wrestlers
   */
  public List<DramaEvent> getEventsBetweenWrestlers(Long wrestler1Id, Long wrestler2Id) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return List.of();
    }

    return dramaEventRepository.findBetweenWrestlers(wrestler1Opt.get(), wrestler2Opt.get());
  }

  // ==================== PRIVATE HELPER METHODS ====================

  /** Calculate potential impacts for a drama event based on type and severity. */
  private void calculateEventImpacts(DramaEvent event) {
    DramaEventType type = event.getEventType();
    DramaEventSeverity severity = event.getSeverity();

    // Calculate fan impact
    if (type.affectsFans()) {
      DramaEventSeverity.FanImpactRange fanRange = severity.getFanImpactRange();
      event.setFanImpact(fanRange.getRandomValue(random));
    }

    // Calculate heat impact (only for multi-wrestler events)
    if (event.getSecondaryWrestler() != null && type.createsHeat()) {
      DramaEventSeverity.HeatImpactRange heatRange = severity.getHeatImpactRange();
      event.setHeatImpact(heatRange.getRandomValue(random));
    }

    // Determine if injury is caused
    if (type.canCauseInjury() && severity.canCauseInjury()) {
      event.setInjuryCaused(random.nextDouble() < 0.3); // 30% chance
    }

    // Determine if rivalry is created
    if (event.getSecondaryWrestler() != null && severity.canCreateRivalry()) {
      event.setRivalryCreated(random.nextDouble() < 0.4); // 40% chance
    }

    // Determine if rivalry is ended (only for positive major events)
    if (event.getSecondaryWrestler() != null && severity == DramaEventSeverity.POSITIVE) {
      event.setRivalryEnded(random.nextDouble() < 0.2); // 20% chance
    }
  }

  /** Apply fan impact to the primary wrestler. */
  private void applyFanImpact(DramaEvent event, StringBuilder notes) {
    Wrestler wrestler = event.getPrimaryWrestler();
    Long fanChange = event.getFanImpact();

    Long oldFans = wrestler.getFans();
    Long newFans = Math.max(0, oldFans + fanChange);
    wrestler.setFans(newFans);
    wrestlerRepository.save(wrestler);

    notes.append(
        String.format(
            "Fan impact: %s %d fans (%d → %d); ",
            fanChange > 0 ? "+" : "", fanChange, oldFans, newFans));
  }

  /** Apply heat impact between wrestlers. */
  private void applyHeatImpact(@NonNull DramaEvent event, @NonNull StringBuilder notes) {
    rivalryService.addHeatBetweenWrestlers(
        event.getPrimaryWrestler().getId(),
        event.getSecondaryWrestler().getId(),
        event.getHeatImpact(),
        "Drama Event: " + event.getTitle());

    notes.append(
        String.format(
            "Heat impact: +%d heat between %s and %s; ",
            event.getHeatImpact(),
            event.getPrimaryWrestler().getName(),
            event.getSecondaryWrestler().getName()));
  }

  /** Apply injury impact to the primary wrestler. */
  private void applyInjuryImpact(@NonNull DramaEvent event, @NonNull StringBuilder notes) {
    // Add bumps that might lead to injury
    Wrestler wrestler = event.getPrimaryWrestler();
    boolean injuryOccurred = wrestler.addBump();
    wrestlerRepository.save(wrestler);

    // If injury occurred (3 bumps reached), create injury
    if (injuryOccurred) {
      injuryService.createInjuryFromBumps(wrestler.getId());
      notes.append("Injury created from bumps; ");
    } else {
      notes.append("Bump added; ");
    }
  }

  /** Create a new rivalry between wrestlers. */
  private void applyRivalryCreation(@NonNull DramaEvent event, @NonNull StringBuilder notes) {
    rivalryService.createRivalry(
        event.getPrimaryWrestler().getId(),
        event.getSecondaryWrestler().getId(),
        "Created by drama event: " + event.getTitle());

    notes.append(
        String.format(
            "Rivalry created between %s and %s; ",
            event.getPrimaryWrestler().getName(), event.getSecondaryWrestler().getName()));
  }

  /** End existing rivalry between wrestlers. */
  private void applyRivalryEnding(@NonNull DramaEvent event, @NonNull StringBuilder notes) {
    // This would require a method in RivalryService to end rivalries
    // For now, just add negative heat to cool down the rivalry
    rivalryService.addHeatBetweenWrestlers(
        event.getPrimaryWrestler().getId(),
        event.getSecondaryWrestler().getId(),
        -5,
        "Rivalry cooled by drama event: " + event.getTitle());

    notes.append("Rivalry heat reduced; ");
  }

  /** Get a random event type. */
  private DramaEventType getRandomEventType() {
    DramaEventType[] types = DramaEventType.values();
    return types[random.nextInt(types.length)];
  }

  /** Get a random severity level. */
  private DramaEventSeverity getRandomSeverity() {
    // Weight the probabilities: Neutral most common, Major least common
    double roll = random.nextDouble();
    if (roll < 0.4) return DramaEventSeverity.NEUTRAL;
    if (roll < 0.7) return DramaEventSeverity.NEGATIVE;
    if (roll < 0.9) return DramaEventSeverity.POSITIVE;
    return DramaEventSeverity.MAJOR;
  }

  /** Get a random opponent for multi-wrestler events. */
  private Wrestler getRandomOpponent(@NonNull Wrestler wrestler) {
    List<Wrestler> allWrestlers = wrestlerRepository.findAll();
    allWrestlers.removeIf(w -> w.getId().equals(wrestler.getId()));

    if (allWrestlers.isEmpty()) {
      return null;
    }

    return allWrestlers.get(random.nextInt(allWrestlers.size()));
  }

  /** Generate event template with title and description. */
  private DramaEventTemplate generateEventTemplate(
      @NonNull DramaEventType type,
      @NonNull DramaEventSeverity severity,
      @NonNull Wrestler primary,
      Wrestler secondary) {

    String primaryName = primary.getName();
    String secondaryName = secondary != null ? secondary.getName() : ""; // Handle null secondary

    return switch (type) {
      case BACKSTAGE_INCIDENT -> generateBackstageIncident(severity, primaryName, secondaryName);
      case SOCIAL_MEDIA_DRAMA -> generateSocialMediaDrama(severity, primaryName, secondaryName);
      case INJURY_INCIDENT -> generateInjuryIncident(severity, primaryName);
      case FAN_INTERACTION -> generateFanInteraction(severity, primaryName);
      case CONTRACT_DISPUTE -> generateContractDispute(severity, primaryName);
      case BETRAYAL -> generateBetrayal(severity, primaryName, secondaryName);
      case ALLIANCE_FORMED -> generateAllianceFormed(severity, primaryName, secondaryName);
      case SURPRISE_RETURN -> generateSurpriseReturn(severity, primaryName);
      case RETIREMENT_TEASE -> generateRetirementTease(severity, primaryName);
      case CHAMPIONSHIP_CHALLENGE ->
          generateChampionshipChallenge(severity, primaryName, secondaryName);
      case PERSONAL_ISSUE -> generatePersonalIssue(severity, primaryName);
      case MEDIA_CONTROVERSY -> generateMediaControversy(severity, primaryName);
    };
  }

  // ==================== EVENT TEMPLATE GENERATORS ====================

  private DramaEventTemplate generateBackstageIncident(
      DramaEventSeverity severity, String primary, String secondary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Helps Resolve Backstage Dispute",
              primary
                  + " stepped in to help resolve a heated argument between crew members, earning"
                  + " respect from everyone backstage.");
      case NEUTRAL ->
          new DramaEventTemplate(
              "Minor Backstage Disagreement Involving " + primary,
              primary
                  + " had a brief disagreement with "
                  + secondary
                  + " over segment planning, but it was quickly resolved.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " and " + secondary + " Backstage Altercation",
              "Tensions boiled over backstage as "
                  + primary
                  + " and "
                  + secondary
                  + " got into a heated confrontation that had to be broken up by security.");
      case MAJOR ->
          new DramaEventTemplate(
              "BREAKING: Major Backstage Brawl Involving " + primary,
              "A massive backstage brawl erupted involving "
                  + primary
                  + " and "
                  + secondary
                  + ", requiring multiple security personnel and potentially causing injuries.");
    };
  }

  private DramaEventTemplate generateSocialMediaDrama(
      @NonNull DramaEventSeverity severity, @NonNull String primary, @NonNull String secondary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Viral Social Media Moment",
              primary
                  + "'s heartfelt social media post about their wrestling journey has gone viral,"
                  + " earning thousands of new fans.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Social Media Exchange",
              primary
                  + " and "
                  + secondary
                  + " had a brief back-and-forth on social media that caught fans' attention.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Social Media Controversy",
              primary
                  + " posted controversial comments that sparked backlash from fans and fellow"
                  + " wrestlers, including "
                  + secondary
                  + ".");
      case MAJOR ->
          new DramaEventTemplate(
              "SCANDAL: " + primary + " Social Media Meltdown",
              primary
                  + " went on a shocking social media rant targeting "
                  + secondary
                  + " and the entire wrestling industry, causing major controversy.");
    };
  }

  private DramaEventTemplate generateInjuryIncident(
      @NonNull DramaEventSeverity severity, @NonNull String primary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Injury Scare Turns Out Minor",
              "Initial fears about "
                  + primary
                  + "'s injury were unfounded - medical tests show only minor strain that should"
                  + " heal quickly.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Training Incident",
              primary
                  + " suffered a minor bump during training but is expected to continue competing"
                  + " without issues.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Injured in Accident",
              primary
                  + " was injured in a training accident and may need time off to recover"
                  + " properly.");
      case MAJOR ->
          new DramaEventTemplate(
              "BREAKING: " + primary + " Serious Injury",
              primary
                  + " has suffered a serious injury that could potentially threaten their wrestling"
                  + " career.");
    };
  }

  private DramaEventTemplate generateFanInteraction(
      @NonNull DramaEventSeverity severity, @NonNull String primary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Heartwarming Fan Moment",
              primary
                  + " made a young fan's day by spending extra time signing autographs and taking"
                  + " photos, creating a viral feel-good moment.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Fan Meet and Greet",
              primary
                  + " participated in a standard fan meet and greet event with mixed reactions from"
                  + " attendees.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Fan Incident",
              "An incident occurred during "
                  + primary
                  + "'s fan appearance, leading to disappointed fans and negative publicity.");
      case MAJOR ->
          new DramaEventTemplate(
              "CONTROVERSY: " + primary + " Fan Altercation",
              primary
                  + " was involved in a serious altercation with fans that required security"
                  + " intervention and is generating major negative publicity.");
    };
  }

  private DramaEventTemplate generateContractDispute(
      @NonNull DramaEventSeverity severity, @NonNull String primary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Contract Extension Signed",
              primary
                  + " has successfully negotiated a new contract extension with improved terms,"
                  + " securing their future.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Contract Negotiations",
              primary
                  + " is currently in contract negotiations with management, with both sides"
                  + " working toward a resolution.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Contract Dispute",
              primary
                  + " is reportedly unhappy with their current contract terms and may be"
                  + " considering their options.");
      case MAJOR ->
          new DramaEventTemplate(
              "BREAKING: " + primary + " Contract Standoff",
              primary
                  + " has reportedly walked out of contract negotiations and may be considering"
                  + " leaving the promotion entirely.");
    };
  }

  private DramaEventTemplate generateBetrayal(
      @NonNull DramaEventSeverity severity, @NonNull String primary, @NonNull String secondary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Refuses to Betray " + secondary,
              primary
                  + " was offered incentives to turn on their partner "
                  + secondary
                  + " but refused, strengthening their bond.");
      case NEUTRAL ->
          new DramaEventTemplate(
              "Tension Between " + primary + " and " + secondary,
              "Subtle signs of tension have emerged between former allies "
                  + primary
                  + " and "
                  + secondary
                  + ".");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Betrays " + secondary,
              "In a shocking turn of events, "
                  + primary
                  + " turned on their longtime partner "
                  + secondary
                  + ", ending their alliance.");
      case MAJOR ->
          new DramaEventTemplate(
              "SHOCKING BETRAYAL: " + primary + " Destroys " + secondary,
              primary
                  + " brutally attacked "
                  + secondary
                  + " in a shocking betrayal that has left the wrestling world stunned.");
    };
  }

  private DramaEventTemplate generateAllianceFormed(
      @NonNull DramaEventSeverity severity, @NonNull String primary, @NonNull String secondary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " and " + secondary + " Form Powerful Alliance",
              primary
                  + " and "
                  + secondary
                  + " have announced a new partnership that could dominate the wrestling"
                  + " landscape.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " and " + secondary + " Team Up",
              primary
                  + " and "
                  + secondary
                  + " have decided to work together for their mutual benefit.");
      case NEGATIVE ->
          new DramaEventTemplate(
              "Questionable Alliance: " + primary + " and " + secondary,
              primary
                  + " and "
                  + secondary
                  + " have formed an alliance, but fans are skeptical about their motives.");
      case MAJOR ->
          new DramaEventTemplate(
              "GAME CHANGER: " + primary + " and " + secondary + " Mega Alliance",
              "The wrestling world is buzzing about the shocking alliance between "
                  + primary
                  + " and "
                  + secondary
                  + " that could change everything.");
    };
  }

  private DramaEventTemplate generateSurpriseReturn(
      @NonNull DramaEventSeverity severity, @NonNull String primary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Triumphant Return",
              primary
                  + " made a surprise return to thunderous applause, clearly ready to reclaim their"
                  + " place in wrestling.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Returns from Hiatus",
              primary
                  + " has returned from their time away with mixed reactions from fans and fellow"
                  + " wrestlers.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Controversial Return",
              primary
                  + "'s return has been met with controversy and questions about whether they're"
                  + " truly ready to compete again.");
      case MAJOR ->
          new DramaEventTemplate(
              "SHOCKING RETURN: " + primary + " is Back!",
              "In a moment that sent shockwaves through the wrestling world, "
                  + primary
                  + " made a stunning return that nobody saw coming.");
    };
  }

  private DramaEventTemplate generateRetirementTease(
      @NonNull DramaEventSeverity severity, @NonNull String primary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Commits to Wrestling Future",
              primary
                  + " has dismissed retirement rumors and committed to continuing their wrestling"
                  + " career for years to come.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Hints at Retirement",
              primary
                  + " made vague comments about their future in wrestling, sparking speculation"
                  + " about potential retirement.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Seriously Considering Retirement",
              primary
                  + " has reportedly been seriously considering retirement, citing various personal"
                  + " and professional factors.");
      case MAJOR ->
          new DramaEventTemplate(
              "BREAKING: " + primary + " Retirement Announcement",
              primary
                  + " shocked the wrestling world by announcing their intention to retire, sending"
                  + " fans into a frenzy.");
    };
  }

  private DramaEventTemplate generateChampionshipChallenge(
      @NonNull DramaEventSeverity severity, @NonNull String primary, @NonNull String secondary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Earns Championship Opportunity",
              primary
                  + " has earned a well-deserved championship opportunity against "
                  + secondary
                  + " through their recent performances.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Challenges " + secondary,
              primary
                  + " has officially challenged "
                  + secondary
                  + " for their championship in what should be an interesting segment.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Demands Title Shot from " + secondary,
              primary
                  + " has been making controversial demands for a title shot against "
                  + secondary
                  + ", causing backstage tension.");
      case MAJOR ->
          new DramaEventTemplate(
              "EXPLOSIVE: " + primary + " Confronts Champion " + secondary,
              primary
                  + " made a shocking confrontation with champion "
                  + secondary
                  + ", demanding an immediate title segment in dramatic fashion.");
    };
  }

  private DramaEventTemplate generatePersonalIssue(DramaEventSeverity severity, String primary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Overcomes Personal Challenge",
              primary
                  + " has successfully overcome a personal challenge and is stronger than ever,"
                  + " inspiring fans worldwide.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Dealing with Personal Matter",
              primary
                  + " is currently dealing with a personal matter that may affect their wrestling"
                  + " schedule temporarily.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Personal Issues Affecting Career",
              primary
                  + " is struggling with personal issues that are beginning to impact their"
                  + " wrestling performance and fan support.");
      case MAJOR ->
          new DramaEventTemplate(
              "SERIOUS: " + primary + " Major Personal Crisis",
              primary
                  + " is facing a major personal crisis that could significantly impact their"
                  + " wrestling career and future.");
    };
  }

  private DramaEventTemplate generateMediaControversy(
      @NonNull DramaEventSeverity severity, @NonNull String primary) {
    return switch (severity) {
      case POSITIVE ->
          new DramaEventTemplate(
              primary + " Positive Media Coverage",
              primary
                  + " has been featured in positive media coverage highlighting their contributions"
                  + " to wrestling and community.");
      case NEUTRAL ->
          new DramaEventTemplate(
              primary + " Media Interview",
              primary
                  + " gave a standard media interview that generated some discussion among"
                  + " wrestling fans.");
      case NEGATIVE ->
          new DramaEventTemplate(
              primary + " Media Controversy",
              primary
                  + " is facing negative media attention due to controversial statements made"
                  + " during a recent interview.");
      case MAJOR ->
          new DramaEventTemplate(
              "SCANDAL: " + primary + " Media Firestorm",
              primary
                  + " is at the center of a major media scandal that is generating widespread"
                  + " negative publicity and fan backlash.");
    };
  }

  /** Record for event templates. */
  private record DramaEventTemplate(String title, String description) {}
}
