package com.github.javydreamercsw.management.service.season;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing seasons in the ATW RPG system. Handles season lifecycle, show scheduling,
 * and PPV management.
 */
@Service
@Transactional
public class SeasonService {

  @Autowired private SeasonRepository seasonRepository;
  @Autowired private Clock clock;

  /** Create a new season. */
  public Season createSeason(
      @NonNull String name, @NonNull String description, Integer showsPerPpv) {
    // End any currently active season
    Optional<Season> activeSeason = seasonRepository.findActiveSeason();
    activeSeason.ifPresent(this::endSeason);

    // Create new season
    Season season = new Season();
    season.setName(name);
    season.setDescription(description);
    season.setShowsPerPpv(showsPerPpv == null ? 5 : showsPerPpv); // Default to 5 if not provided
    season.setIsActive(true);
    season.setStartDate(Instant.now(clock));
    season.setCreationDate(Instant.now(clock));

    return seasonRepository.saveAndFlush(season);
  }

  /** Get the currently active season. */
  @Transactional(readOnly = true)
  public Optional<Season> getActiveSeason() {
    return seasonRepository.findActiveSeason();
  }

  /** Get season by ID. */
  @Transactional(readOnly = true)
  public Optional<Season> getSeasonById(@NonNull Long seasonId) {
    return seasonRepository.findById(seasonId);
  }

  /** Find season by name. */
  @Transactional(readOnly = true)
  public Season findByName(@NonNull String name) {
    return seasonRepository.findByName(name).orElse(null);
  }

  /** Save a season. */
  public Season save(@NonNull Season season) {
    return seasonRepository.saveAndFlush(season);
  }

  /** Get all seasons with pagination. */
  @Transactional(readOnly = true)
  public Page<Season> getAllSeasons(@NonNull Pageable pageable) {
    return seasonRepository.findAllBy(pageable);
  }

  /** End the current season. */
  public Optional<Season> endCurrentSeason() {
    Optional<Season> activeSeason = seasonRepository.findActiveSeason();
    activeSeason.ifPresent(this::endSeason);
    return activeSeason;
  }

  /** End a specific season. */
  public Season endSeason(@NonNull Season season) {
    season.endSeason();
    return seasonRepository.saveAndFlush(season);
  }

  /** Add a show to the active season. */
  public Optional<Season> addShowToActiveSeason(@NonNull Show show) {
    Optional<Season> activeSeason = seasonRepository.findActiveSeason();
    if (activeSeason.isPresent()) {
      Season season = activeSeason.get();
      season.addShow(show);
      return Optional.of(seasonRepository.saveAndFlush(season));
    }
    return Optional.empty();
  }

  /** Check if it's time for a PPV in the active season. */
  @Transactional(readOnly = true)
  public boolean isTimeForPpv() {
    return seasonRepository.findActiveSeason().map(Season::isTimeForPpv).orElse(false);
  }

  /** Get seasons that need PPVs scheduled. */
  @Transactional(readOnly = true)
  public List<Season> getSeasonsNeedingPpv() {
    return seasonRepository
        .findActiveSeason()
        .filter(Season::isTimeForPpv)
        .map(List::of)
        .orElse(List.of());
  }

  /** Update season settings. */
  public Optional<Season> updateSeason(
      @NonNull Long seasonId,
      @NonNull String name,
      @NonNull String description,
      @NonNull Integer showsPerPpv) {
    return seasonRepository
        .findById(seasonId)
        .map(
            season -> {
              season.setName(name);
              season.setDescription(description);
              season.setShowsPerPpv(showsPerPpv);
              return seasonRepository.saveAndFlush(season);
            });
  }

  /** Update season with Season object. */
  public Season updateSeason(@NonNull Season season) {
    return seasonRepository.saveAndFlush(season);
  }

  /** Search seasons by name or description. */
  @Transactional(readOnly = true)
  public Page<Season> searchSeasons(@NonNull String searchTerm, @NonNull Pageable pageable) {
    return seasonRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        searchTerm, pageable);
  }

  /** Delete a season (only if not active and has no shows). */
  public boolean deleteSeason(@NonNull Long seasonId) {
    return seasonRepository
        .findById(seasonId)
        .filter(season -> !season.getIsActive() && season.getShows().isEmpty())
        .map(
            season -> {
              seasonRepository.delete(season);
              return true;
            })
        .orElse(false);
  }

  /** Get season statistics. */
  @Transactional(readOnly = true)
  public SeasonStats getSeasonStats(@NonNull Long seasonId) {
    return seasonRepository
        .findById(seasonId)
        .map(
            season -> {
              int totalShows = season.getTotalShows();
              int expectedPpvs = season.getExpectedPpvCount();
              long regularShows =
                  season.getShows().stream()
                      .filter(show -> !show.getType().getName().toLowerCase().contains("ple"))
                      .count();
              long ppvShows =
                  season.getShows().stream()
                      .filter(show -> show.getType().getName().toLowerCase().contains("ple"))
                      .count();

              return new SeasonStats(
                  season.getId(),
                  season.getName(),
                  totalShows,
                  (int) regularShows,
                  (int) ppvShows,
                  expectedPpvs,
                  season.isTimeForPpv(),
                  season.getDurationDays(),
                  season.getIsActive());
            })
        .orElse(null);
  }

  /** Get the latest season by creation date. */
  @Transactional(readOnly = true)
  public Optional<Season> getLatestSeason() {
    return seasonRepository.findLatestSeason();
  }

  /** Season statistics data class. */
  public record SeasonStats(
      Long seasonId,
      String name,
      int totalShows,
      int regularShows,
      int ppvShows,
      int expectedPpvs,
      boolean timeForPpv,
      long durationDays,
      boolean isActive) {}
}
