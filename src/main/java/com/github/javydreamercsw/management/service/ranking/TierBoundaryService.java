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
package com.github.javydreamercsw.management.service.ranking;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.config.CacheConfig;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TierBoundaryService {

  private final TierBoundaryRepository tierBoundaryRepository;

  @PreAuthorize("isAuthenticated()")
  public Optional<TierBoundary> findByTierAndGender(final WrestlerTier tier, final Gender gender) {
    return tierBoundaryRepository.findByTierAndGender(tier, gender);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM') or hasAuthority('ROLE_BOOKER')")
  public TierBoundary save(final TierBoundary tierBoundary) {
    return tierBoundaryRepository.save(tierBoundary);
  }

  @PreAuthorize("isAuthenticated()")
  @Cacheable(value = CacheConfig.TIER_BOUNDARIES_CACHE, key = "#gender")
  public List<TierBoundary> findAllByGender(final Gender gender) {
    return tierBoundaryRepository.findAllByGender(gender);
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public void resetTierBoundaries() {
    log.debug("Current boundaries: {}", tierBoundaryRepository.count());
    tierBoundaryRepository.deleteAllInBatch();
    log.debug("Boundaries after delete: {}", tierBoundaryRepository.count());
    List<TierBoundary> boundaries = new java.util.ArrayList<>();
    for (Gender gender : Gender.values()) {
      for (WrestlerTier tier : WrestlerTier.values()) {
        TierBoundary boundary = new TierBoundary();
        boundary.setTier(tier);
        boundary.setGender(gender);
        boundary.setMinFans(tier.getMinFans());
        boundary.setMaxFans(tier.getMaxFans());
        boundary.setChallengeCost(tier.getChallengeCost());
        boundary.setContenderEntryFee(tier.getContenderEntryFee());
        boundaries.add(boundary);
      }
    }
    tierBoundaryRepository.saveAll(boundaries);
  }
}
