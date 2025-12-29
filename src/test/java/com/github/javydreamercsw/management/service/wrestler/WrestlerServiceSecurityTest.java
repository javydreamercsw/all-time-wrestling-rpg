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

import com.github.javydreamercsw.base.security.TestCustomUserDetailsService;
import com.github.javydreamercsw.base.test.AbstractSecurityTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WrestlerServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private TestCustomUserDetailsService userDetailsService;

  private Wrestler getWrestler(@NonNull String username) {
    // Ensure the user and their wrestler are created/loaded via the userDetailsService
    userDetailsService.loadUserByUsername(username);
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
  void testPlayerCanSaveOwnedWrestlerAndCannotSaveUnownedWrestler() {
    Wrestler ownedWrestler = getWrestler("not_owner"); // Wrestler owned by the current user
    Wrestler unownedWrestler = getWrestler("owner"); // Wrestler not owned by the current user

    assertNotNull(ownedWrestler);
    assertNotNull(unownedWrestler);

    // Should be able to save own wrestler
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));

    // Should NOT be able to save unowned wrestler
    assertThrows(AuthorizationDeniedException.class, () -> wrestlerService.save(unownedWrestler));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotSaveWrestler() {
    assertThrows(
        AuthorizationDeniedException.class, () -> wrestlerService.save(getWrestler("owner")));
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
    assertThrows(
        AuthorizationDeniedException.class, () -> wrestlerService.delete(getWrestler("owner")));
    assertThrows(
        AuthorizationDeniedException.class, () -> wrestlerService.delete(getWrestler("not_owner")));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotDeleteWrestler() {
    assertThrows(
        AuthorizationDeniedException.class, () -> wrestlerService.delete(getWrestler("owner")));
  }

  // --- Read Method Tests (All authenticated) ---

  // Count tests
  @Test
  @WithUserDetails("admin")
  void testAdminCanCountWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanCountWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanCountWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanCountWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  void testUnauthenticatedCannotCountWrestlers() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> wrestlerService.count());
  }

  // FindAll tests
  @Test
  @WithUserDetails("admin")
  void testAdminCanFindAllWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanFindAllWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanFindAllWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanFindAllWrestlers() {
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  void testUnauthenticatedCannotFindAllWrestlers() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> wrestlerService.findAll());
  }

  // List (Pageable) tests
  @Test
  @WithUserDetails("admin")
  void testAdminCanListWrestlers() {
    Pageable pageable = PageRequest.of(0, 10);
    assertDoesNotThrow(() -> wrestlerService.list(pageable));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanListWrestlers() {
    Pageable pageable = PageRequest.of(0, 10);
    assertDoesNotThrow(() -> wrestlerService.list(pageable));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanListWrestlers() {
    Pageable pageable = PageRequest.of(0, 10);
    assertDoesNotThrow(() -> wrestlerService.list(pageable));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanListWrestlers() {
    Pageable pageable = PageRequest.of(0, 10);
    assertDoesNotThrow(() -> wrestlerService.list(pageable));
  }

  @Test
  void testUnauthenticatedCannotListWrestlers() {
    Pageable pageable = PageRequest.of(0, 10);
    assertThrows(
        AuthenticationCredentialsNotFoundException.class, () -> wrestlerService.list(pageable));
  }

  // findById and getWrestlerById (same underlying method) tests
  @Test
  @WithUserDetails("admin")
  void testAdminCanFindWrestlerById() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.getWrestlerById(ownedWrestler.getId()));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanFindWrestlerById() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanFindAnyWrestlerById() { // Player can read any wrestler by ID
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findById(notOwnedWrestler.getId()));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanFindAnyWrestlerById() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findById(notOwnedWrestler.getId()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerById() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findById(ownedWrestler.getId()));
  }

  // findByIdWithInjuries tests
  @Test
  @WithUserDetails("admin")
  void testAdminCanFindWrestlerByIdWithInjuries() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanFindWrestlerByIdWithInjuries() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanFindAnyWrestlerByIdWithInjuries() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(notOwnedWrestler.getId()));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanFindAnyWrestlerByIdWithInjuries() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(notOwnedWrestler.getId()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerByIdWithInjuries() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
  }

  // findByName tests
  @Test
  @WithUserDetails("admin")
  void testAdminCanFindWrestlerByName() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanFindWrestlerByName() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanFindAnyWrestlerByName() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
    assertDoesNotThrow(() -> wrestlerService.findByName(notOwnedWrestler.getName()));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanFindAnyWrestlerByName() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
    assertDoesNotThrow(() -> wrestlerService.findByName(notOwnedWrestler.getName()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerByName() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findByName(ownedWrestler.getName()));
  }

  // findByExternalId tests
  @Test
  @WithUserDetails("admin")
  void testAdminCanFindWrestlerByExternalId() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanFindWrestlerByExternalId() {
    Wrestler ownedWrestler = getWrestler("owner");
    System.out.println("DEBUG: Wrestlers in DB:");
    wrestlerRepository
        .findAll()
        .forEach(
            w ->
                System.out.println(
                    "ID: "
                        + w.getId()
                        + ", Name: "
                        + w.getName()
                        + ", ExternalID: "
                        + w.getExternalId()));
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanFindAnyWrestlerByExternalId() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(notOwnedWrestler.getExternalId()));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanFindAnyWrestlerByExternalId() {
    Wrestler ownedWrestler = getWrestler("owner");
    Wrestler notOwnedWrestler = getWrestler("not_owner");
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(notOwnedWrestler.getExternalId()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerByExternalId() {
    Wrestler ownedWrestler = getWrestler("owner");
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
  }
}
