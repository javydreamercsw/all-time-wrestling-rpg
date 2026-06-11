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
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
  private final TitleReignRepository titleReignRepository;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final UniverseRepository universeRepository;
  private final Clock clock;
  private final DefaultImageService imageService;

  public boolean isWrestlerEligible(@NonNull final Wrestler wrestler, @NonNull final Title title) {
    if (title.getGender() != null && title.getGender() != wrestler.getGender()) {
      return false;
    }

    if (title.getUniverse() == null) {
      return false;
    }

    // A wrestler is eligible if their tier is the same or higher than the title's tier.
    WrestlerTier wrestlerTier =
        wrestlerService.getOrCreateState(wrestler.getId(), title.getUniverse().getId()).getTier();

    return wrestlerTier.ordinal() >= title.getTier().ordinal();
  }

  @PreAuthorize("isAuthenticated()")
  public boolean titleNameExists(@NonNull final String name) {
    return titleRepository.findByName(name).isPresent();
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Title createTitle(
      @NonNull final String name,
      @NonNull final String description,
      @NonNull final WrestlerTier tier,
      @NonNull final ChampionshipType type,
      @NonNull final Long universeId) {
    return createTitle(name, description, tier, type, null, universeId);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Title createTitle(
      @NonNull final String name,
      @NonNull final String description,
      @NonNull final WrestlerTier tier,
      @NonNull final ChampionshipType type,
      final Gender gender,
      @NonNull final Long universeId) {
    Title title = new Title();
    title.setName(name);
    title.setDescription(description);
    title.setTier(tier);
    title.setGender(gender);
    title.setCreationDate(Instant.now(clock));
    title.setChampionshipType(type);
    title.setUniverse(universeRepository.findById(universeId).orElseThrow());
    return titleRepository.save(title);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "#id")
  public Optional<Title> getTitleById(@NonNull final Long id) {
    return titleRepository.findById(id);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "#name")
  public Optional<Title> findByName(@NonNull final String name) {
    return titleRepository.findByName(name);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Title> findByExternalId(@NonNull final String externalId) {
    return titleRepository.findByExternalId(externalId);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Title save(@NonNull final Title title) {
    return titleRepository.save(title);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public List<Title> saveAll(@NonNull final List<Title> titles) {
    return (List<Title>) titleRepository.saveAll(titles);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "'all'")
  public List<Title> findAll() {
    return (List<Title>) titleRepository.findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> findByUniverse(@NonNull final Universe universe) {
    return titleRepository.findByUniverse(universe);
  }

  @PreAuthorize("isAuthenticated()")
  public Page<Title> getAllTitles(final Pageable pageable) {
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
  public Map<Long, String> getCurrentChampionNamesByTitleIds(
      final java.util.Collection<Long> titleIds) {
    Map<Long, String> result = new java.util.HashMap<>();
    for (Long titleId : titleIds) {
      List<com.github.javydreamercsw.management.domain.title.TitleReign> activeReigns =
          titleReignRepository.findByTitleIdAndEndDateIsNull(titleId);
      result.put(
          titleId,
          activeReigns.isEmpty()
              ? "Vacant"
              : activeReigns.get(0).getChampions().stream()
                  .map(com.github.javydreamercsw.management.domain.wrestler.Wrestler::getName)
                  .collect(Collectors.joining(" & ")));
    }
    return result;
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      key = "#tier")
  public List<Title> getTitlesByTier(@NonNull final WrestlerTier tier) {
    return titleRepository.findByIsActiveTrueAndTier(tier);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public void awardTitleTo(@NonNull final Title title, @NonNull final List<Wrestler> newChampions) {
    awardTitleTo(title, newChampions, null);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public void awardTitleTo(
      @NonNull final Title title,
      @NonNull final List<Wrestler> newChampions,
      final com.github.javydreamercsw.management.domain.show.segment.Segment wonAtSegment) {
    title.awardTitleTo(newChampions, Instant.now(clock), wonAtSegment);
    titleRepository.save(title);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Optional<Title> vacateTitle(@NonNull final Long titleId) {
    return titleRepository
        .findById(titleId)
        .map(
            title -> {
              title.vacateTitle(Instant.now(clock));

              return titleRepository.save(title);
            });
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Optional<Title> updateTitle(
      @NonNull final Long id, final String name, final String description, final Boolean isActive) {
    return updateTitle(id, name, description, isActive, null);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public Optional<Title> updateTitle(
      @NonNull final Long id,
      final String name,
      final String description,
      final Boolean isActive,
      final Gender gender) {

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

              if (gender != null) {
                title.setGender(gender);
              }

              return titleRepository.save(title);
            });
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public boolean deleteTitle(@NonNull final Long id) {

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
  public Long getChallengeCost(@NonNull final Title title) {
    Gender gender = title.getGender() == null ? Gender.MALE : title.getGender();
    return tierBoundaryService
        .findByTierAndGender(title.getTier(), gender)
        .map(TierBoundary::getChallengeCost)
        .orElse(title.getTier().getChallengeCost());
  }

  @PreAuthorize("isAuthenticated()")
  public Long getContenderEntryFee(@NonNull final Title title) {
    Gender gender = title.getGender() == null ? Gender.MALE : title.getGender();
    return tierBoundaryService
        .findByTierAndGender(title.getTier(), gender)
        .map(TierBoundary::getContenderEntryFee)
        .orElse(title.getTier().getContenderEntryFee());
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> findTitlesByChampion(
      @NonNull final Wrestler wrestler, @NonNull final Long universeId) {
    return titleRepository.findTitlesHeldByWrestler(wrestler, universeId);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> findTitlesByChampion(@NonNull final Wrestler wrestler) {
    return titleRepository.findTitlesHeldByWrestler(wrestler);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public ChallengeResult challengeForTitle(
      @NonNull final Long wrestlerId, @NonNull final Long titleId, @NonNull final Long universeId) {
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
    if (!wrestlerService.getOrCreateState(challenger.getId(), universeId).canAfford(entryFee)) {
      return new ChallengeResult(false, "Wrestler cannot afford the contender entry fee.");
    }

    // All checks pass, add as a challenger and deduct fee
    wrestlerService.spendFans(challenger.getId(), universeId, entryFee);
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
  public List<Wrestler> getEligibleChallengersFast(
      @NonNull final List<Wrestler> candidates,
      @NonNull final Map<Long, WrestlerState> stateMap,
      @NonNull final Title title) {
    if (title.getUniverse() == null || title.getTier() == null) {
      return List.of();
    }
    WrestlerTier titleTier = title.getTier();
    Gender titleGender = title.getGender();
    return candidates.stream()
        .filter(w -> titleGender == null || titleGender == w.getGender())
        .filter(
            w -> {
              WrestlerState state = stateMap.get(w.getId());
              return state != null && state.getTier().ordinal() >= titleTier.ordinal();
            })
        .collect(Collectors.toList());
  }

  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getEligibleChallengers(@NonNull final Long titleId) {
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
  public TitleStats getTitleStats(@NonNull final Long id) {
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

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public ChallengeResult addChallengerToTitle(
      @NonNull final Long titleId, @NonNull final Long wrestlerId) {
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

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.TITLES_CACHE,
      allEntries = true)
  public ChallengeResult removeChallengerFromTitle(
      @NonNull final Long titleId, @NonNull final Long wrestlerId) {
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
  public List<Title> getTitlesHeldBy(@NonNull final Long wrestlerId) {
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    return wrestlerOpt.map(titleRepository::findTitlesHeldByWrestler).orElse(List.of());
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> findEligibleTitlesForFanCount(@NonNull final Long fanCount) {
    return titleRepository.findEligibleTitlesForFanCount(fanCount);
  }

  public long count() {
    return titleRepository.count();
  }

  public String resolveTitleImage(final Title title) {
    if (title.getImageUrl() != null && !title.getImageUrl().isBlank()) {
      return title.getImageUrl();
    }
    return imageService.resolveImage(title.getName(), ImageCategory.TITLE).url();
  }

  public record ChallengeResult(boolean success, @NonNull String message) {}

  public record TitleStats(
      String titleName, int totalReigns, long currentReignDays, int currentChampionsCount) {}
}
