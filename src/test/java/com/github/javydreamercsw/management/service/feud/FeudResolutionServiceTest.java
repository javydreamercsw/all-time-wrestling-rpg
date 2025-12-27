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
package com.github.javydreamercsw.management.service.feud;

import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FeudResolutionServiceTest {

  @Autowired private FeudResolutionService feudResolutionService;

  @Autowired private MultiWrestlerFeudRepository feudRepository;

  private MultiWrestlerFeud feud;

  @BeforeEach
  void setUp() {
    feudRepository.deleteAll();
    feud = new MultiWrestlerFeud();
    feud.setName("Test Feud");
    feud.setHeat(100);
    feudRepository.save(feud);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void testAdminCanResolveFeud() {
    feudResolutionService.attemptFeudResolution(feud);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER"})
  void testBookerCanResolveFeud() {
    feudResolutionService.attemptFeudResolution(feud);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "player",
      roles = {"PLAYER"})
  void testPlayerCannotResolveFeud() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> feudResolutionService.attemptFeudResolution(feud));
  }
}
