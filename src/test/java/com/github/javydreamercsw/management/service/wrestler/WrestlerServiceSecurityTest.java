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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javydreamercsw.base.test.AbstractSecurityTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;

class WrestlerServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private WrestlerService wrestlerService;

  private Wrestler getWrestler(@NonNull String username) {
    return wrestlerRepository
        .findByAccountUsername(username)
        .orElseThrow(
            () -> new IllegalStateException("Wrestler for user " + username + " not found"));
  }

  @Test
  @WithUserDetails("admin")
  void testAdminCanSaveAnyWrestler() {
    assertDoesNotThrow(() -> wrestlerService.save(getWrestler("owner")));
    assertDoesNotThrow(() -> wrestlerService.save(getWrestler("not_owner")));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanSaveAnyWrestler() {
    assertDoesNotThrow(() -> wrestlerService.save(getWrestler("owner")));
    assertDoesNotThrow(() -> wrestlerService.save(getWrestler("not_owner")));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanSaveOwnedWrestler() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertNotNull(ownedWrestler);
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));
  }

  @Test
  @WithUserDetails("not_owner")
  void testPlayerCannotSaveOtherWrestler() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler otherWrestler = getWrestler("not_owner");
    assertNotNull(ownedWrestler);
    assertThrows(AccessDeniedException.class, () -> wrestlerService.save(ownedWrestler));
    assertThrows(AccessDeniedException.class, () -> wrestlerService.save(otherWrestler));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotSaveWrestler() {
    assertThrows(AccessDeniedException.class, () -> wrestlerService.save(getWrestler("owner")));
  }

  // --- Delete Method Tests ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanDeleteWrestler() {
    assertDoesNotThrow(() -> wrestlerService.delete(getWrestler("not_owner")));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanDeleteWrestler() {
    assertDoesNotThrow(() -> wrestlerService.delete(getWrestler("not_owner")));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCannotDeleteWrestler() {
    assertThrows(AccessDeniedException.class, () -> wrestlerService.delete(getWrestler("owner")));
    assertThrows(
        AccessDeniedException.class, () -> wrestlerService.delete(getWrestler("not_owner")));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotDeleteWrestler() {
    assertThrows(AccessDeniedException.class, () -> wrestlerService.delete(getWrestler("owner")));
  }
}
