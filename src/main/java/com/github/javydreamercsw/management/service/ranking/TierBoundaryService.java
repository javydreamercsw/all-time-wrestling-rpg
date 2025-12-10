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

import com.github.javydreamercsw.management.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.management.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TierBoundaryService {

  private final TierBoundaryRepository tierBoundaryRepository;

  public Optional<TierBoundary> findByTier(WrestlerTier tier) {
    return tierBoundaryRepository.findByTier(tier);
  }

  public TierBoundary save(TierBoundary tierBoundary) {
    return tierBoundaryRepository.save(tierBoundary);
  }

  public List<TierBoundary> findAll() {
    return (List<TierBoundary>) tierBoundaryRepository.findAll();
  }
}
