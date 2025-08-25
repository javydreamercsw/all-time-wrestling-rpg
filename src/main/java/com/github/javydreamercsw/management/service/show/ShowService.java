package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShowService {
  private final ShowRepository showRepository;
  private final ShowTypeRepository showTypeRepository;
  private final SeasonRepository seasonRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final Clock clock;

  ShowService(
      ShowRepository showRepository,
      ShowTypeRepository showTypeRepository,
      SeasonRepository seasonRepository,
      ShowTemplateRepository showTemplateRepository,
      Clock clock) {
    this.showRepository = showRepository;
    this.showTypeRepository = showTypeRepository;
    this.seasonRepository = seasonRepository;
    this.showTemplateRepository = showTemplateRepository;
    this.clock = clock;
  }

  public List<Show> list(Pageable pageable) {
    return showRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return showRepository.count();
  }

  public Show save(@NonNull Show show) {
    show.setCreationDate(clock.instant());
    return showRepository.saveAndFlush(show);
  }

  public List<Show> findAll() {
    return showRepository.findAll();
  }

  /**
   * Find all shows with eagerly loaded relationships. This is useful for export operations to
   * prevent LazyInitializationException.
   *
   * @return List of all shows with eagerly loaded relationships
   */
  public List<Show> findAllWithRelationships() {
    return showRepository.findAllWithRelationships();
  }

  public Optional<Show> findByName(String showName) {
    return showRepository.findByName(showName);
  }

  public Optional<Show> findByExternalId(String externalId) {
    return showRepository.findByExternalId(externalId);
  }

  // ==================== CALENDAR-SPECIFIC METHODS ====================

  /**
   * Get all shows with pagination.
   *
   * @param pageable Pagination information
   * @return Page of shows
   */
  public Page<Show> getAllShows(Pageable pageable) {
    return showRepository.findAllBy(pageable);
  }

  /**
   * Get show by ID.
   *
   * @param id Show ID
   * @return Optional containing the show if found
   */
  public Optional<Show> getShowById(Long id) {
    return showRepository.findById(id);
  }

  /**
   * Get shows within a date range for calendar view.
   *
   * @param startDate Start date (inclusive)
   * @param endDate End date (inclusive)
   * @return List of shows in the date range
   */
  public List<Show> getShowsByDateRange(LocalDate startDate, LocalDate endDate) {
    return showRepository.findByShowDateBetweenOrderByShowDate(startDate, endDate);
  }

  /**
   * Get shows for a specific month and year.
   *
   * @param year Year
   * @param month Month (1-12)
   * @return List of shows in the specified month
   */
  public List<Show> getShowsForMonth(int year, int month) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    return getShowsByDateRange(startDate, endDate);
  }

  /**
   * Get upcoming shows from today onwards.
   *
   * @param limit Maximum number of shows to return
   * @return List of upcoming shows
   */
  public List<Show> getUpcomingShows(int limit) {
    LocalDate today = LocalDate.now(clock);
    Pageable pageable = PageRequest.of(0, limit, Sort.by("showDate").ascending());
    return showRepository.findByShowDateGreaterThanEqualOrderByShowDate(today, pageable);
  }

  /**
   * Create a new show.
   *
   * @param name Show name
   * @param description Show description
   * @param showTypeId Show type ID
   * @param showDate Show date (optional)
   * @param seasonId Season ID (optional)
   * @param templateId Template ID (optional)
   * @return Created show
   */
  public Show createShow(
      String name,
      String description,
      Long showTypeId,
      LocalDate showDate,
      Long seasonId,
      Long templateId) {

    Show show = new Show();
    show.setName(name);
    show.setDescription(description);
    show.setShowDate(showDate);
    show.setCreationDate(clock.instant());

    // Set show type (required)
    ShowType showType =
        showTypeRepository
            .findById(showTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Show type not found: " + showTypeId));
    show.setType(showType);

    // Set season (optional)
    if (seasonId != null) {
      Season season =
          seasonRepository
              .findById(seasonId)
              .orElseThrow(() -> new IllegalArgumentException("Season not found: " + seasonId));
      show.setSeason(season);
    }

    // Set template (optional)
    if (templateId != null) {
      ShowTemplate template =
          showTemplateRepository
              .findById(templateId)
              .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
      show.setTemplate(template);
    }

    return showRepository.saveAndFlush(show);
  }

  /**
   * Update an existing show.
   *
   * @param id Show ID
   * @param name Show name (optional)
   * @param description Show description (optional)
   * @param showTypeId Show type ID (optional)
   * @param showDate Show date (optional)
   * @param seasonId Season ID (optional)
   * @param templateId Template ID (optional)
   * @return Updated show if found
   */
  public Optional<Show> updateShow(
      Long id,
      String name,
      String description,
      Long showTypeId,
      LocalDate showDate,
      Long seasonId,
      Long templateId) {

    return showRepository
        .findById(id)
        .map(
            show -> {
              if (name != null) show.setName(name);
              if (description != null) show.setDescription(description);
              if (showDate != null) show.setShowDate(showDate);

              if (showTypeId != null) {
                ShowType showType =
                    showTypeRepository
                        .findById(showTypeId)
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException("Show type not found: " + showTypeId));
                show.setType(showType);
              }

              if (seasonId != null) {
                Season season =
                    seasonRepository
                        .findById(seasonId)
                        .orElseThrow(
                            () -> new IllegalArgumentException("Season not found: " + seasonId));
                show.setSeason(season);
              } else if (seasonId == null) {
                show.setSeason(null);
              }

              if (templateId != null) {
                ShowTemplate template =
                    showTemplateRepository
                        .findById(templateId)
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException("Template not found: " + templateId));
                show.setTemplate(template);
              } else if (templateId == null) {
                show.setTemplate(null);
              }

              return showRepository.saveAndFlush(show);
            });
  }

  /**
   * Delete a show by ID.
   *
   * @param id Show ID
   * @return true if deleted, false if not found
   */
  public boolean deleteShow(Long id) {
    if (showRepository.existsById(id)) {
      showRepository.deleteById(id);
      return true;
    }
    return false;
  }
}
