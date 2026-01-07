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
package com.github.javydreamercsw.management.service.rivalry;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

class RivalryServiceIT extends ManagementIntegrationTest {

  @Autowired private RivalryService rivalryService;

  @Autowired private WrestlerService wrestlerService;

  @SpyBean private RivalryRepository rivalryRepository;

  @Test
  @DisplayName("Test that createRivalry evicts cache")
  void testCreateRivalryEvictsCache() {
    // First call, should hit the repository
    rivalryRepository.findAll();
    verify(rivalryRepository, times(1)).findAll();

    // Create a new rivalry, should evict the cache
    Wrestler w1 = new Wrestler();
    w1.setName("Test Wrestler 1");
    wrestlerService.save(w1);
    Wrestler w2 = new Wrestler();
    w2.setName("Test Wrestler 2");
    wrestlerService.save(w2);
    rivalryService.createRivalry(w1.getId(), w2.getId(), "Test Storyline");

    // Second call, should hit the repository again
    rivalryRepository.findAll();
    verify(rivalryRepository, times(2)).findAll();
  }
}
