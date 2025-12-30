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
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TierBoundaryService {

  private final TierBoundaryRepository tierBoundaryRepository;

  @PreAuthorize("isAuthenticated()")
  public Optional<TierBoundary> findByTierAndGender(WrestlerTier tier, Gender gender) {
    return tierBoundaryRepository.findByTierAndGender(tier, gender);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public TierBoundary save(TierBoundary tierBoundary) {
    return tierBoundaryRepository.save(tierBoundary);
  }

  @PreAuthorize("isAuthenticated()")
  public List<TierBoundary> findAll() {
    return tierBoundaryRepository.findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public List<TierBoundary> findAllByGender(Gender gender) {
    return tierBoundaryRepository.findAllByGender(gender);
  }

  @PreAuthorize("isAuthenticated()")
  public WrestlerTier findTierForFans(long fans, Gender gender) {
    List<TierBoundary> boundaries =
        findAllByGender(gender).stream()
            .sorted((b1, b2) -> b2.getMinFans().compareTo(b1.getMinFans()))
            .toList();

    for (TierBoundary boundary : boundaries) {
      if (fans >= boundary.getMinFans() && fans <= boundary.getMaxFans()) {
        return boundary.getTier();
      }
    }
    return null; // Or throw an exception if a tier should always be found
  }
}
