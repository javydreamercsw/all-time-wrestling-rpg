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
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.TierBoundary;
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

  private boolean isWrestlerEligible(@NonNull Wrestler wrestler, @NonNull Title title) {
    if (title.getGender() != null && title.getGender() != wrestler.getGender()) {
      return false;
    }
    // A wrestler is eligible if their current fan count falls within the title's tier boundary
    // OR if the tier boundary is not yet defined, use the static enum values.
    Optional<TierBoundary> boundary =
        tierBoundaryService.findByTierAndGender(title.getTier(), wrestler.getGender());

    // Fallback to static eligibility if dynamic boundaries are not set
    return boundary
        .map(
            tierBoundary -> {
              boolean meetsMin;
              if (title.getTier() == WrestlerTier.ROOKIE) {
                meetsMin = wrestler.getFans() >= tierBoundary.getMinFans();
              } else {
                meetsMin = wrestler.getFans() > tierBoundary.getMinFans();
              }
              return meetsMin && wrestler.getFans() <= tierBoundary.getMaxFans();
            })
        .orElseGet(
            () ->
                wrestler.getFans() >= title.getTier().getMinFans()
                    && wrestler.getFans() <= title.getTier().getMaxFans());
  }

  @PreAuthorize("isAuthenticated()")
  public boolean titleNameExists(@NonNull String name) {
    return titleRepository.findByName(name).isPresent();
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public Title createTitle(
      @NonNull String name, @NonNull String description, @NonNull WrestlerTier tier) {
    Title title = new Title();
    title.setName(name);
    title.setDescription(description);
    title.setTier(tier);
    title.setCreationDate(Instant.now(clock));
    return titleRepository.save(title);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Title> getTitleById(@NonNull Long id) {
    return titleRepository.findById(id);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Title> findByName(@NonNull String name) {
    return titleRepository.findByName(name);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Title> findByExternalId(@NonNull String externalId) {
    return titleRepository.findByExternalId(externalId);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public Title save(@NonNull Title title) {
    return titleRepository.save(title);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> findAll() {
    return (List<Title>) titleRepository.findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public Page<Title> getAllTitles(Pageable pageable) {
    return titleRepository.findAll(pageable);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> getActiveTitles() {
    return titleRepository.findByIsActiveTrue();
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> getVacantTitles() {
    return titleRepository.findByIsActiveTrue().stream()
        .filter(Title::isVacant)
        .collect(Collectors.toList());
  }

  @PreAuthorize("isAuthenticated()")
  public List<Title> getTitlesByTier(@NonNull WrestlerTier tier) {
    return titleRepository.findByIsActiveTrueAndTier(tier);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public void awardTitleTo(@NonNull Title title, @NonNull List<Wrestler> newChampions) {
    // Validate if all new champions are eligible for the title
    for (Wrestler champion : newChampions) {
      if (!this.isWrestlerEligible(champion, title)) {
        throw new IllegalArgumentException(
            "Wrestler " + champion.getName() + " is not eligible for title " + title.getName());
      }
    }
    title.awardTitleTo(newChampions, Instant.now(clock));
    titleRepository.save(title);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public Optional<Title> vacateTitle(@NonNull Long titleId) {

    return titleRepository
        .findById(titleId)
        .map(
            title -> {
              title.vacateTitle(Instant.now(clock));

              return titleRepository.save(title);
            });
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
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

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
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

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
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

    // All checks pass, set as contender and deduct fee

    challenger.spendFans(entryFee);

    wrestlerRepository.save(challenger);

    title.setNumberOneContender(challenger);

    titleRepository.save(title);

    return new ChallengeResult(
        true,
        "Challenge successful! "
            + challenger.getName()
            + " is now the #1 contender for the "
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

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public Optional<Title> updateNumberOneContender(@NonNull Long titleId, @NonNull Long wrestlerId) {

    Optional<Title> titleOpt = titleRepository.findById(titleId);

    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);

    if (titleOpt.isEmpty() || wrestlerOpt.isEmpty()) {
      return Optional.empty();
    }

    Title title = titleOpt.get();
    Wrestler wrestler = wrestlerOpt.get();

    title.setNumberOneContender(wrestler);
    return Optional.of(titleRepository.save(title));
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public Optional<Title> clearNumberOneContender(Long titleId) {
    Optional<Title> titleOpt = titleRepository.findById(titleId);

    if (titleOpt.isEmpty()) {
      return Optional.empty();
    }

    Title title = titleOpt.get();
    title.setNumberOneContender(null);
    return Optional.of(titleRepository.save(title));
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
