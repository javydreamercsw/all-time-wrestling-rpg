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
package com.github.javydreamercsw.management.service.wrestler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.base.security.PermissionService;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ContextConfiguration(classes = TestSecurityConfig.class)
class WrestlerServiceSecurityTest {

  @Autowired private WrestlerService wrestlerService;
  @MockitoBean private PermissionService permissionService;

  private final Wrestler playerWrestler = new Wrestler();
  private final Wrestler otherWrestler = new Wrestler();

  @Test
  @WithMockUser(roles = "PLAYER")
  void testSaveWrestlerAsPlayer() {
    playerWrestler.setName("Player Wrestler");
    otherWrestler.setName("Other Wrestler");
    when(permissionService.isOwner(playerWrestler)).thenReturn(true);
    when(permissionService.isOwner(otherWrestler)).thenReturn(false);

    assertDoesNotThrow(() -> wrestlerService.save(playerWrestler));
    assertThrows(AccessDeniedException.class, () -> wrestlerService.save(otherWrestler));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void testSaveWrestlerAsBooker() {
    playerWrestler.setName("Player Wrestler");
    otherWrestler.setName("Other Wrestler");
    assertDoesNotThrow(() -> wrestlerService.save(playerWrestler));
    assertDoesNotThrow(() -> wrestlerService.save(otherWrestler));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void testSaveWrestlerAsAdmin() {
    playerWrestler.setName("Player Wrestler");
    otherWrestler.setName("Other Wrestler");
    assertDoesNotThrow(() -> wrestlerService.save(playerWrestler));
    assertDoesNotThrow(() -> wrestlerService.save(otherWrestler));
  }
}
