package com.github.javydreamercsw.management.service.storyline;

import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing storyline continuity across shows and seasons in the ATW RPG system. Tracks
 * narrative threads, suggests storyline developments, and maintains story coherence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StorylineContinuityService {

  @Autowired private final SeasonRepository seasonRepository;
  @Autowired private final ShowRepository showRepository;
  @Autowired private final MatchRepository matchRepository;
  @Autowired private final DramaEventRepository dramaEventRepository;
  @Autowired private final RivalryService rivalryService;
  @Autowired private final Clock clock;

  /**
   * Get active storylines for the current season.
   *
   * @return List of active storylines
   */
  public List<ActiveStoryline> getActiveStorylines() {
    Optional<Season> activeSeasonOpt = seasonRepository.findActiveSeason();
    if (activeSeasonOpt.isEmpty()) {
      return List.of();
    }

    Season activeSeason = activeSeasonOpt.get();
    List<ActiveStoryline> storylines = new ArrayList<>();

    // 1. Rivalry-based storylines
    List<Rivalry> activeRivalries = rivalryService.getActiveRivalries();
    for (Rivalry rivalry : activeRivalries) {
      storylines.add(createRivalryStoryline(rivalry));
    }

    // 2. Drama event storylines
    List<DramaEvent> recentDramaEvents =
        dramaEventRepository.findRecentEvents(clock.instant().minus(30, ChronoUnit.DAYS));

    Map<Wrestler, List<DramaEvent>> wrestlerDramaEvents =
        recentDramaEvents.stream().collect(Collectors.groupingBy(DramaEvent::getPrimaryWrestler));

    for (Map.Entry<Wrestler, List<DramaEvent>> entry : wrestlerDramaEvents.entrySet()) {
      if (entry.getValue().size() >= 2) { // Multiple drama events create storyline
        storylines.add(createDramaStoryline(entry.getKey(), entry.getValue()));
      }
    }

    // 3. Championship storylines (future enhancement)
    // TODO: Add championship-based storylines when title system is enhanced

    return storylines;
  }

  /**
   * Get storyline suggestions for upcoming shows.
   *
   * @return List of storyline suggestions
   */
  public List<StorylineSuggestion> getStorylineSuggestions() {
    List<StorylineSuggestion> suggestions = new ArrayList<>();

    // 1. Rivalry escalation suggestions
    List<Rivalry> activeRivalries = rivalryService.getActiveRivalries();
    for (Rivalry rivalry : activeRivalries) {
      suggestions.addAll(getRivalryEscalationSuggestions(rivalry));
    }

    // 2. Drama event follow-up suggestions
    List<DramaEvent> unprocessedEvents =
        dramaEventRepository.findByIsProcessedFalseOrderByEventDateAsc();
    for (DramaEvent event : unprocessedEvents) {
      suggestions.addAll(getDramaFollowUpSuggestions(event));
    }

    // 3. New storyline suggestions
    suggestions.addAll(getNewStorylineSuggestions());

    return suggestions;
  }

  /**
   * Track storyline progression across shows.
   *
   * @param showId ID of the show to analyze
   * @return Storyline progression analysis
   */
  public StorylineProgression analyzeStorylineProgression(@NonNull Long showId) {
    List<Match> matches =
        showRepository.findById(showId).map(matchRepository::findByShow).orElse(List.of());

    int rivalryMatches = 0;
    int storylineAdvancement = 0;
    List<String> progressionNotes = new ArrayList<>();

    for (Match match : matches) {
      // Check if match involves rivalry
      List<Wrestler> wrestlers = match.getWrestlers();
      if (wrestlers.size() == 2) {
        Optional<Rivalry> rivalryOpt =
            rivalryService.getRivalryBetweenWrestlers(
                wrestlers.get(0).getId(), wrestlers.get(1).getId());

        if (rivalryOpt.isPresent()) {
          rivalryMatches++;
          Rivalry rivalry = rivalryOpt.get();

          // Analyze storyline impact
          if (match.getMatchRating() >= 4) {
            storylineAdvancement++;
            progressionNotes.add(
                String.format(
                    "High-quality rivalry match between %s and %s (Rating: %d)",
                    wrestlers.get(0).getName(),
                    wrestlers.get(1).getName(),
                    match.getMatchRating()));
          }

          if (rivalry.getHeat() >= 20) {
            progressionNotes.add(
                String.format(
                    "High-heat rivalry match: %s vs %s (Heat: %d)",
                    wrestlers.get(0).getName(), wrestlers.get(1).getName(), rivalry.getHeat()));
          }
        }
      }
    }

    // Check for drama events on the same day
    Instant showDate = matches.isEmpty() ? clock.instant() : matches.get(0).getMatchDate();
    List<DramaEvent> showDayEvents =
        dramaEventRepository.findRecentEvents(showDate.minus(1, ChronoUnit.DAYS)).stream()
            .filter(event -> ChronoUnit.DAYS.between(event.getEventDate(), showDate) <= 1)
            .toList();

    for (DramaEvent event : showDayEvents) {
      storylineAdvancement++;
      progressionNotes.add(
          String.format("Drama event: %s (%s)", event.getTitle(), event.getSeverity()));
    }

    return new StorylineProgression(
        matches.size(),
        rivalryMatches,
        storylineAdvancement,
        showDayEvents.size(),
        progressionNotes);
  }

  /**
   * Get storyline continuity health for the active season.
   *
   * @return Continuity health assessment
   */
  public StorylineContinuityHealth assessContinuityHealth() {
    Optional<Season> activeSeasonOpt = seasonRepository.findActiveSeason();
    if (activeSeasonOpt.isEmpty()) {
      return new StorylineContinuityHealth(0, 0, 0, List.of("No active season found"));
    }

    Season activeSeason = activeSeasonOpt.get();
    List<String> issues = new ArrayList<>();
    List<String> recommendations = new ArrayList<>();

    // Check rivalry activity
    List<Rivalry> activeRivalries = rivalryService.getActiveRivalries();
    int healthyRivalries =
        (int) activeRivalries.stream().filter(rivalry -> rivalry.getHeat() >= 10).count();

    if (activeRivalries.isEmpty()) {
      issues.add("No active rivalries found");
      recommendations.add("Create new rivalries through drama events or match outcomes");
    } else if (healthyRivalries < activeRivalries.size() / 2) {
      issues.add("Many rivalries have low heat");
      recommendations.add("Book more matches between rivals to increase heat");
    }

    // Check drama event frequency
    List<DramaEvent> recentEvents =
        dramaEventRepository.findRecentEvents(clock.instant().minus(14, ChronoUnit.DAYS));

    if (recentEvents.isEmpty()) {
      issues.add("No recent drama events");
      recommendations.add("Generate drama events to create new storylines");
    } else if (recentEvents.size() > 10) {
      issues.add("Too many drama events recently");
      recommendations.add("Allow current storylines to develop before adding new drama");
    }

    // Check match variety
    List<Show> recentShows =
        activeSeason.getShows().stream()
            .filter(
                show -> {
                  // Get first match date as proxy for show date
                  List<Match> matches = matchRepository.findByShow(show);
                  if (matches.isEmpty()) return false;

                  Instant showDate = matches.get(0).getMatchDate();
                  return ChronoUnit.DAYS.between(showDate, clock.instant()) <= 30;
                })
            .toList();

    int totalRecentMatches =
        recentShows.stream().mapToInt(show -> matchRepository.findByShow(show).size()).sum();

    if (totalRecentMatches < 10) {
      issues.add("Low match activity in recent shows");
      recommendations.add("Increase match frequency to maintain storyline momentum");
    }

    // Calculate health score (0-100)
    int healthScore = 100;
    healthScore -= issues.size() * 15; // -15 per issue
    healthScore = Math.max(0, Math.min(100, healthScore));

    return new StorylineContinuityHealth(
        healthScore,
        activeRivalries.size(),
        recentEvents.size(),
        issues.isEmpty() ? recommendations : issues);
  }

  // ==================== PRIVATE HELPER METHODS ====================

  private ActiveStoryline createRivalryStoryline(@NonNull Rivalry rivalry) {
    String description =
        String.format(
            "Rivalry between %s and %s (Heat: %d)",
            rivalry.getWrestler1().getName(), rivalry.getWrestler2().getName(), rivalry.getHeat());

    StorylineType type =
        rivalry.getHeat() >= 20
            ? StorylineType.HIGH_INTENSITY_RIVALRY
            : StorylineType.DEVELOPING_RIVALRY;

    List<String> keyEvents =
        rivalry.getHeatEvents().stream().limit(3).map(event -> event.getReason()).toList();

    return new ActiveStoryline(
        "Rivalry: " + rivalry.getWrestler1().getName() + " vs " + rivalry.getWrestler2().getName(),
        type,
        description,
        List.of(rivalry.getWrestler1(), rivalry.getWrestler2()),
        keyEvents,
        rivalry.getCreationDate());
  }

  private ActiveStoryline createDramaStoryline(
      @NonNull Wrestler wrestler, @NonNull List<DramaEvent> events) {
    String description =
        String.format("%s involved in %d recent drama events", wrestler.getName(), events.size());

    List<String> keyEvents = events.stream().limit(3).map(DramaEvent::getTitle).toList();

    return new ActiveStoryline(
        "Drama: " + wrestler.getName(),
        StorylineType.PERSONAL_DRAMA,
        description,
        List.of(wrestler),
        keyEvents,
        events.get(0).getEventDate());
  }

  private List<StorylineSuggestion> getRivalryEscalationSuggestions(@NonNull Rivalry rivalry) {
    List<StorylineSuggestion> suggestions = new ArrayList<>();

    if (rivalry.getHeat() >= 20 && rivalry.getHeat() < 30) {
      suggestions.add(
          new StorylineSuggestion(
              "Escalate "
                  + rivalry.getWrestler1().getName()
                  + " vs "
                  + rivalry.getWrestler2().getName(),
              "Book a high-stakes match with special rule",
              StorylinePriority.HIGH));
    } else if (rivalry.getHeat() >= 10 && rivalry.getHeat() < 20) {
      suggestions.add(
          new StorylineSuggestion(
              "Develop "
                  + rivalry.getWrestler1().getName()
                  + " vs "
                  + rivalry.getWrestler2().getName(),
              "Add drama event or interference to increase heat",
              StorylinePriority.MEDIUM));
    }

    return suggestions;
  }

  private List<StorylineSuggestion> getDramaFollowUpSuggestions(@NonNull DramaEvent event) {
    List<StorylineSuggestion> suggestions = new ArrayList<>();

    if (event.getRivalryCreated()) {
      suggestions.add(
          new StorylineSuggestion(
              "Follow up on " + event.getTitle(),
              "Book a match between the involved wrestlers",
              StorylinePriority.HIGH));
    }

    return suggestions;
  }

  private List<StorylineSuggestion> getNewStorylineSuggestions() {
    // For now, return empty list - could be enhanced with AI-generated suggestions
    return List.of();
  }

  // ==================== RECORD CLASSES ====================

  public record ActiveStoryline(
      @NonNull String title,
      @NonNull StorylineType type,
      @NonNull String description,
      @NonNull List<Wrestler> involvedWrestlers,
      @NonNull List<String> keyEvents,
      @NonNull Instant startDate) {}

  public record StorylineSuggestion(
      @NonNull String title, @NonNull String description, @NonNull StorylinePriority priority) {}

  public record StorylineProgression(
      int totalMatches,
      int rivalryMatches,
      int storylineAdvancement,
      int dramaEvents,
      @NonNull List<String> progressionNotes) {}

  public record StorylineContinuityHealth(
      int healthScore,
      int activeRivalries,
      int recentDramaEvents,
      @NonNull List<String> recommendations) {}

  public enum StorylineType {
    DEVELOPING_RIVALRY,
    HIGH_INTENSITY_RIVALRY,
    PERSONAL_DRAMA,
    CHAMPIONSHIP_PURSUIT,
    FACTION_CONFLICT
  }

  public enum StorylinePriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
  }
}