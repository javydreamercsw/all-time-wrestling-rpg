package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private Clock clock;

  /** Create a new title. */
  public Title createTitle(
      @NonNull String name, @NonNull String description, @NonNull WrestlerTier tier) {
    Title title = new Title();
    title.setName(name);
    title.setDescription(description);
    title.setTier(tier);
    title.setIsActive(true);

    title.setCreationDate(Instant.now(clock));

    return titleRepository.saveAndFlush(title);
  }

  /** Get title by ID. */
  @Transactional(readOnly = true)
  public Optional<Title> getTitleById(@NonNull Long titleId) {
    return titleRepository.findById(titleId);
  }

  @Transactional(readOnly = true)
  public Optional<Title> findByName(@NonNull String name) {
    return titleRepository.findByName(name);
  }

  /** Get all titles with pagination. */
  @Transactional(readOnly = true)
  public Page<Title> getAllTitles(@NonNull Pageable pageable) {
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
  public List<Title> getTitlesByTier(@NonNull WrestlerTier tier) {
    return titleRepository.findActiveTitlesByTier(tier);
  }

  public void awardTitleTo(@NonNull Title title, @NonNull List<Wrestler> newChampions) {
    // End the current reign if one exists.
    getCurrentReign(title).ifPresent(reign -> reign.endReign(Instant.now(clock)));

    // Create a new title reign for the new champions.
    TitleReign newReign = new TitleReign();
    newReign.setTitle(title);
    newReign.getChampions().addAll(newChampions);
    newReign.setStartDate(Instant.now(clock));
    title.getTitleReigns().add(newReign);

    title.setChampion(newChampions); // Ensure champion field is updated
    titleRepository.saveAndFlush(title);
  }

  /** Vacate a title. */
  public Optional<Title> vacateTitle(@NonNull Long titleId) {
    return titleRepository
        .findById(titleId)
        .map(
            title -> {
              title.vacateTitle();
              return titleRepository.saveAndFlush(title);
            });
  }

  /** Challenge for a title (spend fans to challenge). */
  public ChallengeResult challengeForTitle(@NonNull Long challengerId, @NonNull Long titleId) {
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
  public List<Wrestler> getEligibleChallengers(@NonNull Long titleId) {
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
  public List<Title> getTitlesHeldBy(@NonNull Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(wrestler -> titleRepository.findTitlesHeldByWrestler(wrestler))
        .orElse(List.of());
  }

  /** Update title information. */
  public Optional<Title> updateTitle(
      @NonNull Long titleId,
      @NonNull String name,
      @NonNull String description,
      @NonNull Boolean isActive) {
    return titleRepository
        .findById(titleId)
        .map(
            title -> {
              title.setName(name);
              title.setDescription(description);
              title.setIsActive(isActive);
              return titleRepository.saveAndFlush(title);
            });
  }

  /** Delete a title (only if vacant and inactive). */
  public boolean deleteTitle(@NonNull Long titleId) {
    return titleRepository
        .findById(titleId)
        .filter(title -> !title.getIsActive() && title.isVacant())
        .map(
            title -> {
              assert title.getId() != null;
              titleRepository.deleteById(title.getId());
              return true;
            })
        .orElse(false);
  }

  /** Get title statistics. */
  @Transactional(readOnly = true)
  public TitleStats getTitleStats(@NonNull Long titleId) {
    return titleRepository
        .findById(titleId)
        .map(
            title ->
                new TitleStats(
                    title.getId(),
                    title.getName(),
                    title.getTier(),
                    title.isVacant(),
                    !title.getCurrentChampions().isEmpty()
                        ? title.getCurrentChampions().stream()
                            .map(Wrestler::getName)
                            .collect(Collectors.joining(" & "))
                        : null,
                    title.getCurrentReignDays(),
                    title.getTotalReigns(),
                    title.getIsActive()))
        .orElse(null);
  }

  /** Check if a title name already exists. */
  @Transactional(readOnly = true)
  public boolean titleNameExists(@NonNull String name) {
    return titleRepository.existsByName(name);
  }

  public List<Title> findAll() {
    return titleRepository.findAll();
  }

  public Title save(Title title) {
    return titleRepository.save(title);
  }

  /** Check if a wrestler is currently a champion of any active title. */
  @Transactional(readOnly = true)
  public boolean isChampion(@NonNull Wrestler wrestler) {
    return titleRepository.findAll().stream()
        .anyMatch(title -> title.getCurrentChampions().contains(wrestler));
  }

  /** Get all active titles held by a specific wrestler. */
  @Transactional(readOnly = true)
  public List<Title> findTitlesByChampion(@NonNull Wrestler wrestler) {
    return titleRepository.findAll().stream()
        .filter(title -> title.getCurrentChampions().contains(wrestler))
        .collect(Collectors.toList());
  }

  /** Challenge result data class. */
  public record ChallengeResult(
      boolean success, @NonNull String message, Title title, Wrestler challenger) {}

  /** Title statistics data class. */
  public record TitleStats(
      Long titleId,
      String name,
      WrestlerTier tier,
      boolean isVacant,
      String currentChampion,
      long currentReignDays,
      int totalReigns,
      boolean isActive) {}

  /** Get current reign days for a title. */
  @Transactional(readOnly = true)
  public long getCurrentReignDays(@NonNull Title title) {
    return getCurrentReign(title).map(TitleReign::getReignLengthDays).orElse(0L);
  }

  /** Get current reign for a title. */
  @Transactional(readOnly = true)
  public Optional<TitleReign> getCurrentReign(@NonNull Title title) {
    return title.getTitleReigns().stream().filter(TitleReign::isCurrentReign).findFirst();
  }

  /** Update the #1 contender for a title. */
  public Optional<Title> updateNumberOneContender(@NonNull Long titleId, @NonNull Long wrestlerId) {
    Optional<Title> titleOpt = titleRepository.findById(titleId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (titleOpt.isPresent() && wrestlerOpt.isPresent()) {
      Title title = titleOpt.get();
      Wrestler wrestler = wrestlerOpt.get();

      // Use the new setter method to update the contender
      title.setNumberOneContender(wrestler);

      return Optional.of(titleRepository.saveAndFlush(title));
    }
    return Optional.empty();
  }
}
