/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TierBoundaryServiceTest {

  @Mock private TierBoundaryRepository repository;
  @InjectMocks private TierBoundaryService service;

  @Test
  void testResetTierBoundaries() {
    service.resetTierBoundaries();

    verify(repository).deleteAllInBatch();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<TierBoundary>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());

    List<TierBoundary> boundaries = captor.getValue();
    assertEquals(WrestlerTier.values().length * Gender.values().length, boundaries.size());
  }
}
