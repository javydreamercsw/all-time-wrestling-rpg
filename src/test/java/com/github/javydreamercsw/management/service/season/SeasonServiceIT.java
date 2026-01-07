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
package com.github.javydreamercsw.management.service.season;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

class SeasonServiceIT extends ManagementIntegrationTest {

  @Autowired private SeasonService seasonService;

  @SpyBean private SeasonRepository seasonRepository;

  @Test
  @DisplayName("Test that createSeason evicts cache")
  void testCreateSeasonEvictsCache() {
    // First call, should hit the repository
    seasonRepository.findAll();
    verify(seasonRepository, times(1)).findAll();

    // Create a new season, should evict the cache
    seasonService.createSeason("Test Season", "Test Description", 4);

    // Second call, should hit the repository again
    seasonRepository.findAll();
    verify(seasonRepository, times(2)).findAll();
  }
}
