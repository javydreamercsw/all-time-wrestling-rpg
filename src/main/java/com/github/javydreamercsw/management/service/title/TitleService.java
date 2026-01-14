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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TitleService {

  private final TierBoundaryService tierBoundaryService;
  private final TitleRepository titleRepository;
  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;

  public boolean isWrestlerEligible(@NonNull Wrestler wrestler, @NonNull Title title) {
    if (title.getGender() != null && title.getGender() != wrestler.getGender()) {
      return false;
    }
    // A wrestler is eligible if their tier is the same or higher than the title's tier.
    return wrestler.getTier().ordinal() >= title.getTier().ordinal();
  }

  @PreAuthorize("isAuthenticated()")
  public boolean titleNameExists(@NonNull String name) {
    return titleRepository.findByName(name).isPresent();
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Title createTitle(
      @NonNull String name,
      @NonNull String description,
      @NonNull WrestlerTier tier,
      @NonNull ChampionshipType type) {
    Title title = new Title();
    title.setName(name);
    title.setDescription(description);
    title.setTier(tier);
    title.setCreationDate(Instant.now(clock));
    title.setChampionshipType(type);
    return titleRepository.save(title);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "#id")
  public Optional<Title> getTitleById(@NonNull Long id) {
    return titleRepository.findById(id);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "#name")
  public Optional<Title> findByName(@NonNull String name) {
    return titleRepository.findByName(name);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Title> findByExternalId(@NonNull String externalId) {
    return titleRepository.findByExternalId(externalId);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Title save(@NonNull Title title) {
    return titleRepository.save(title);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "'all'")
  public List<Title> findAll() {
    return (List<Title>) titleRepository.findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public Page<Title> getAllTitles(Pageable pageable) {
    return titleRepository.findAll(pageable);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "'active'")
  public List<Title> getActiveTitles() {
    return titleRepository.findByIsActiveTrue();
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "'vacant'")
  public List<Title> getVacantTitles() {
    return titleRepository.findByIsActiveTrue().stream()
        .filter(Title::isVacant)
        .collect(Collectors.toList());
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "#tier")
  public List<Title> getTitlesByTier(@NonNull WrestlerTier tier) {
    return titleRepository.findByIsActiveTrueAndTier(tier);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public void awardTitleTo(@NonNull Title title, @NonNull List<Wrestler> newChampions) {
    awardTitleTo(title, newChampions, null);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public void awardTitleTo(
      @NonNull Title title,
      @NonNull List<Wrestler> newChampions,
      com.github.javydreamercsw.management.domain.show.segment.Segment wonAtSegment) {
    title.awardTitleTo(newChampions, Instant.now(clock), wonAtSegment);
    titleRepository.save(title);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Optional<Title> vacateTitle(@NonNull Long titleId) {
    return titleRepository
        .findById(titleId)
        .map(
            title -> {
              title.vacateTitle(Instant.now(clock));

              return titleRepository.save(title);
            });
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Optional<Title> updateTitle(
      @NonNull Long id, String name, String description, Boolean isActive) {

    return titleRepository
        .findById(id)
        .map(
            title -> {
              if (name != null && !name.isBlank()) {

                title.setName(name);
              }

              if (description != null) {

                title.setDescription(description);
              }

              if (isActive != null) {

                title.setIsActive(isActive);
              }

              return titleRepository.save(title);
            });
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public boolean deleteTitle(@NonNull Long id) {

    return titleRepository
        .findById(id)
        .map(
            title -> {
              if (!title.getIsActive() && title.isVacant()) {

                titleRepository.delete(title);

                return true;
              }

              return false;
            })
        .orElse(false);
  }

  @PreAuthorize("isAuthenticated()")
  public Long getChallengeCost(@NonNull Title title) {
    Gender gender = title.getGender() == null ? Gender.MALE : title.getGender();
    return tierBoundaryService
        .findByTierAndGender(title.getTier(), gender)
        .map(TierBoundary::getChallengeCost)
        .orElse(title.getTier().getChallengeCost()); // Fallback to static
  }

  @PreAuthorize("isAuthenticated()")
  public Long getContenderEntryFee(@NonNull Title title) {
    Gender gender = title.getGender() == null ? Gender.MALE : title.getGender();
    return tierBoundaryService
        .findByTierAndGender(title.getTier(), gender)
        .map(TierBoundary::getContenderEntryFee)
        .orElse(title.getTier().getContenderEntryFee()); // Fallback to static
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> findTitlesByChampion(@NonNull Wrestler wrestler) {
    return titleRepository.findTitlesHeldByWrestler(wrestler);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public ChallengeResult challengeForTitle(@NonNull Long wrestlerId, @NonNull Long titleId) {
    Optional<Wrestler> challengerOpt = wrestlerRepository.findById(wrestlerId);
    Optional<Title> titleOpt = titleRepository.findById(titleId);
    if (challengerOpt.isEmpty()) {
      return new ChallengeResult(false, "Challenger not found.");
    }

    if (titleOpt.isEmpty()) {
      return new ChallengeResult(false, "Title not found.");
    }

    Wrestler challenger = challengerOpt.get();
    Title title = titleOpt.get();
    if (!title.getIsActive()) {
      return new ChallengeResult(false, "Title is not active.");
    }

    if (title.getCurrentChampions().contains(challenger)) {
      return new ChallengeResult(false, "Wrestler is already a champion of this title.");
    }

    if (!this.isWrestlerEligible(challenger, title)) {
      return new ChallengeResult(false, "Wrestler is not eligible for this title based on tier.");
    }

    Long entryFee = getContenderEntryFee(title);
    if (!challenger.canAfford(entryFee)) {
      return new ChallengeResult(false, "Wrestler cannot afford the contender entry fee.");
    }

    // All checks pass, add as a challenger and deduct fee
    challenger.spendFans(entryFee);
    wrestlerRepository.save(challenger);
    title.addChallenger(challenger);
    titleRepository.save(title);
    return new ChallengeResult(
        true,
        "Challenge successful! "
            + challenger.getName()
            + " is now a challenger for the "
            + title.getName()
            + ".");
  }

  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getEligibleChallengers(@NonNull Long titleId) {
    Optional<Title> titleOpt = titleRepository.findById(titleId);
    if (titleOpt.isEmpty()) {
      return List.of();
    }

    Title title = titleOpt.get();
    return wrestlerRepository.findAll().stream()
        .filter(wrestler -> this.isWrestlerEligible(wrestler, title))
        .collect(Collectors.toList());
  }

  @PreAuthorize("isAuthenticated()")
  public TitleStats getTitleStats(@NonNull Long id) {
    // Basic implementation for now, can be expanded later
    return titleRepository
        .findById(id)
        .map(
            title ->
                new TitleStats(
                    title.getName(),
                    title.getTotalReigns(),
                    title.getCurrentReignDays(Instant.now(clock)),
                    title.getCurrentChampions().size()))
        .orElse(null);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public ChallengeResult addChallengerToTitle(@NonNull Long titleId, @NonNull Long wrestlerId) {
    Optional<Title> titleOpt = titleRepository.findById(titleId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (titleOpt.isEmpty()) {
      return new ChallengeResult(false, "Title not found.");
    }
    if (wrestlerOpt.isEmpty()) {
      return new ChallengeResult(false, "Wrestler not found.");
    }

    Title title = titleOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    if (!isWrestlerEligible(wrestler, title)) {
      return new ChallengeResult(false, "Wrestler is not eligible for this title.");
    }

    title.addChallenger(wrestler);
    titleRepository.save(title);
    return new ChallengeResult(
        true, wrestler.getName() + " has been added as a challenger for " + title.getName());
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public ChallengeResult removeChallengerFromTitle(
      @NonNull Long titleId, @NonNull Long wrestlerId) {
    Optional<Title> titleOpt = titleRepository.findById(titleId);
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (titleOpt.isEmpty()) {
      return new ChallengeResult(false, "Title not found.");
    }
    if (wrestlerOpt.isEmpty()) {
      return new ChallengeResult(false, "Wrestler not found.");
    }

    Title title = titleOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    if (title.getChallengers().contains(wrestler)) {
      title.getChallengers().remove(wrestler);
      titleRepository.save(title);
      return new ChallengeResult(
          true, wrestler.getName() + " is no longer a challenger for " + title.getName());
    } else {
      return new ChallengeResult(false, "Wrestler is not a challenger for this title.");
    }
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> getTitlesHeldBy(@NonNull Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    return wrestlerOpt.map(titleRepository::findTitlesHeldByWrestler).orElse(List.of());
  }

  // Nested records for API responses/requests
  public record ChallengeResult(boolean success, @NonNull String message) {}

  public record TitleStats(
      String titleName, int totalReigns, long currentReignDays, int currentChampionsCount) {}
}
