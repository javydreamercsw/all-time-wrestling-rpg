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
package com.github.javydreamercsw.management.service.show;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

class ShowServiceIT extends ManagementIntegrationTest {

  @Autowired private ShowService showService;

  @Autowired private ShowTypeService showTypeService;

  @SpyBean private ShowRepository showRepository;

  @Test
  @DisplayName("Test that createShow evicts cache")
  void testCreateShowEvictsCache() {
    // First call, should hit the repository
    showService.findAll();
    verify(showRepository, times(1)).findAll();

    ShowType showType =
        showTypeService.createOrUpdateShowType("Weekly Test Show", "Weekly Show", 5, 2);

    // Create a new show, should evict the cache
    showService.createShow("Test Show", "Test Show", showType.getId(), null, null, null);

    // Second call, should hit the repository again
    showService.findAll();
    verify(showRepository, times(2)).findAll();
  }
}
