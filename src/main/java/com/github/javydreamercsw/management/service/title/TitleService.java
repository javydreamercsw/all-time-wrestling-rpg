package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.TitleTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing titles in the ATW RPG system. Handles championship management, title
 * challenges, and reign tracking.
 */
@Service
@Transactional
public class TitleService {

  private final TitleRepository titleRepository;
  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;

  public TitleService(
      TitleRepository titleRepository, WrestlerRepository wrestlerRepository, Clock clock) {
    this.titleRepository = titleRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.clock = clock;
  }

  /** Create a new title. */
  public Title createTitle(String name, String description, TitleTier tier) {
    Title title = new Title();
    title.setName(name);
    title.setDescription(description);
    title.setTier(tier);
    title.setIsActive(true);
    title.setIsVacant(true);
    title.setCreationDate(Instant.now(clock));

    return titleRepository.saveAndFlush(title);
  }

  /** Get title by ID. */
  @Transactional(readOnly = true)
  public Optional<Title> getTitleById(Long titleId) {
    return titleRepository.findById(titleId);
  }

  /** Get all titles with pagination. */
  @Transactional(readOnly = true)
  public Page<Title> getAllTitles(Pageable pageable) {
    return titleRepository.findAllBy(pageable);
  }

  /** Get active titles. */
  @Transactional(readOnly = true)
  public List<Title> getActiveTitles() {
    return titleRepository.findByIsActiveTrue();
  }

  /** Get vacant titles. */
  @Transactional(readOnly = true)
  public List<Title> getVacantTitles() {
    return titleRepository.findVacantActiveTitles();
  }

  /** Get titles by tier. */
  @Transactional(readOnly = true)
  public List<Title> getTitlesByTier(TitleTier tier) {
    return titleRepository.findActiveTitlesByTier(tier);
  }

  /** Award title to a wrestler. */
  public Optional<Title> awardTitle(Long titleId, Long wrestlerId) {
    Optional<Title> titleOpt = titleRepository.findById(titleId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (titleOpt.isPresent() && wrestlerOpt.isPresent()) {
      Title title = titleOpt.get();
      Wrestler wrestler = wrestlerOpt.get();

      // Check if wrestler is eligible
      if (!title.isWrestlerEligible(wrestler)) {
        return Optional.empty();
      }

      title.awardTitle(wrestler);
      return Optional.of(titleRepository.saveAndFlush(title));
    }

    return Optional.empty();
  }

  /** Vacate a title. */
  public Optional<Title> vacateTitle(Long titleId) {
    return titleRepository
        .findById(titleId)
        .map(
            title -> {
              title.vacateTitle();
              return titleRepository.saveAndFlush(title);
            });
  }

  /** Challenge for a title (spend fans to challenge). */
  public ChallengeResult challengeForTitle(Long challengerId, Long titleId) {
    Optional<Title> titleOpt = titleRepository.findById(titleId);
    Optional<Wrestler> challengerOpt = wrestlerRepository.findById(challengerId);

    if (titleOpt.isEmpty() || challengerOpt.isEmpty()) {
      return new ChallengeResult(false, "Title or challenger not found", null, null);
    }

    Title title = titleOpt.get();
    Wrestler challenger = challengerOpt.get();

    // Check if title is active
    if (!title.getIsActive()) {
      return new ChallengeResult(false, "Title is not active", title, challenger);
    }

    // Check if wrestler is eligible
    if (!title.isWrestlerEligible(challenger)) {
      return new ChallengeResult(
          false,
          String.format(
              "Wrestler needs %,d fans to challenge for this title",
              title.getTier().getRequiredFans()),
          title,
          challenger);
    }

    // Check if wrestler can afford the challenge
    Long challengeCost = title.getChallengeCost();
    if (!challenger.canAfford(challengeCost)) {
      return new ChallengeResult(
          false,
          String.format("Wrestler cannot afford %,d fans challenge cost", challengeCost),
          title,
          challenger);
    }

    // Spend the fans
    boolean success = challenger.spendFans(challengeCost);
    if (!success) {
      return new ChallengeResult(false, "Failed to spend fans", title, challenger);
    }

    // Save the challenger with updated fan count
    wrestlerRepository.saveAndFlush(challenger);

    return new ChallengeResult(true, "Challenge accepted", title, challenger);
  }

  /** Get eligible challengers for a title. */
  @Transactional(readOnly = true)
  public List<Wrestler> getEligibleChallengers(Long titleId) {
    return titleRepository
        .findById(titleId)
        .map(
            title ->
                wrestlerRepository.findAll().stream()
                    .filter(
                        wrestler ->
                            title.isWrestlerEligible(wrestler)
                                && wrestler.canAfford(title.getChallengeCost()))
                    .toList())
        .orElse(List.of());
  }

  /** Get titles held by a wrestler. */
  @Transactional(readOnly = true)
  public List<Title> getTitlesHeldBy(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(titleRepository::findByCurrentChampion)
        .orElse(List.of());
  }

  /** Update title information. */
  public Optional<Title> updateTitle(
      Long titleId, String name, String description, Boolean isActive) {
    return titleRepository
        .findById(titleId)
        .map(
            title -> {
              if (name != null) title.setName(name);
              if (description != null) title.setDescription(description);
              if (isActive != null) title.setIsActive(isActive);
              return titleRepository.saveAndFlush(title);
            });
  }

  /** Delete a title (only if vacant and inactive). */
  public boolean deleteTitle(Long titleId) {
    return titleRepository
        .findById(titleId)
        .filter(title -> !title.getIsActive() && title.getIsVacant())
        .map(
            title -> {
              titleRepository.delete(title);
              return true;
            })
        .orElse(false);
  }

  /** Get title statistics. */
  @Transactional(readOnly = true)
  public TitleStats getTitleStats(Long titleId) {
    return titleRepository
        .findById(titleId)
        .map(
            title ->
                new TitleStats(
                    title.getId(),
                    title.getName(),
                    title.getTier(),
                    title.getIsVacant(),
                    title.getCurrentChampion() != null
                        ? title.getCurrentChampion().getName()
                        : null,
                    title.getCurrentReignDays(),
                    title.getTotalReigns(),
                    title.getIsActive()))
        .orElse(null);
  }

  /** Check if a title name already exists. */
  @Transactional(readOnly = true)
  public boolean titleNameExists(String name) {
    return titleRepository.existsByName(name);
  }

  /** Challenge result data class. */
  public record ChallengeResult(
      boolean success, String message, Title title, Wrestler challenger) {}

  /** Title statistics data class. */
  public record TitleStats(
      Long titleId,
      String name,
      TitleTier tier,
      boolean isVacant,
      String currentChampion,
      long currentReignDays,
      int totalReigns,
      boolean isActive) {}
}
