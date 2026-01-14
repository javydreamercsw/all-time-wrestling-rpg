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
package com.github.javydreamercsw.management.service.title;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class TitleServiceIT extends ManagementIntegrationTest {

  @Autowired private TitleService titleService;

  @MockitoSpyBean private TitleRepository titleRepository;

  @Test
  @DisplayName("Test that createTitle evicts cache")
  void testCreateTitleEvictsCache() {
    // First call, should hit the repository
    titleService.findAll();
    verify(titleRepository, times(1)).findAll();

    // Create a new title, should evict the cache
    titleService.createTitle(
        "Test Title", "Test Description", WrestlerTier.ROOKIE, ChampionshipType.SINGLE);

    // Second call, should hit the repository again
    titleService.findAll();
    verify(titleRepository, times(2)).findAll();
  }
}
