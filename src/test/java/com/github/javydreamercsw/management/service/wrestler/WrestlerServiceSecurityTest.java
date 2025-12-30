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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.PermissionService;
import com.github.javydreamercsw.base.test.AbstractSecurityTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WrestlerServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private PermissionService permissionService;

  private Wrestler ownedWrestler;
  private Wrestler unownedWrestler;
  private final String prefix = "-" + UUID.randomUUID();

  @BeforeEach
  protected void setup() {
    super.setup();
    Account ownerAccount = createTestAccount("owner", RoleName.PLAYER);
    ownedWrestler = createTestWrestler("owner");
    ownedWrestler.setAccount(ownerAccount);
    wrestlerRepository.save(ownedWrestler);

    Account notOwnerAccount = createTestAccount("not_owner" + prefix, RoleName.PLAYER);
    unownedWrestler = createTestWrestler("not_owner" + prefix);
    unownedWrestler.setAccount(notOwnerAccount);
    wrestlerRepository.save(unownedWrestler);

    createTestAccount("admin" + prefix, RoleName.ADMIN);
    createTestAccount("booker" + prefix, RoleName.BOOKER);
    createTestAccount("viewer" + prefix, RoleName.VIEWER);
  }

  @Test
  void testPermissionServiceIsOwner() {
    login("owner" + prefix);
    assertTrue(
        permissionService.isOwner(ownedWrestler),
        "PermissionService.isOwner should return true for owner");
  }

  @Test
  void testAdminCanSaveAnyWrestler() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));
    assertDoesNotThrow(() -> wrestlerService.save(unownedWrestler));
  }

  @Test
  void testBookerCanSaveAnyWrestler() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));
    assertDoesNotThrow(() -> wrestlerService.save(unownedWrestler));
  }

  @Test
  void testPlayerCanSaveOwnedWrestler() {
    login("owner" + prefix);
    assertNotNull(ownedWrestler);
    assertNotNull(ownedWrestler.getAccount(), "Account should not be null");
    assertEquals(
        "owner" + prefix, ownedWrestler.getAccount().getUsername(), "Username should match");
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));
  }

  @Test
  void testPlayerCanSaveOwnedWrestlerAndCannotSaveUnownedWrestler() {
    login("not_owner" + prefix);
    assertNotNull(ownedWrestler);
    assertNotNull(unownedWrestler);

    // Should be able to save own wrestler
    assertDoesNotThrow(() -> wrestlerService.save(unownedWrestler));

    // Should NOT be able to save unowned wrestler
    assertThrows(AuthorizationDeniedException.class, () -> wrestlerService.save(ownedWrestler));
  }

  @Test
  void testViewerCannotSaveWrestler() {
    login("viewer" + prefix);
    assertThrows(AuthorizationDeniedException.class, () -> wrestlerService.save(ownedWrestler));
  }

  // --- Delete Method Tests ---

  @Test
  void testAdminCanDeleteWrestler() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.delete(unownedWrestler));
  }

  @Test
  void testBookerCanDeleteWrestler() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.delete(unownedWrestler));
  }

  @Test
  void testPlayerCannotDeleteWrestler() {
    login("owner" + prefix);
    assertThrows(AuthorizationDeniedException.class, () -> wrestlerService.delete(ownedWrestler));
    assertThrows(AuthorizationDeniedException.class, () -> wrestlerService.delete(unownedWrestler));
  }

  @Test
  void testViewerCannotDeleteWrestler() {
    login("viewer" + prefix);
    assertThrows(AuthorizationDeniedException.class, () -> wrestlerService.delete(ownedWrestler));
  }

  // --- Read Method Tests (All authenticated) ---

  // Count tests
  @Test
  void testAdminCanCountWrestlers() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  void testBookerCanCountWrestlers() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  void testPlayerCanCountWrestlers() {
    login("owner" + prefix);
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  void testViewerCanCountWrestlers() {
    login("viewer" + prefix);
    assertDoesNotThrow(() -> wrestlerService.count());
  }

  @Test
  void testUnauthenticatedCannotCountWrestlers() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> wrestlerService.count());
  }

  // FindAll tests
  @Test
  void testAdminCanFindAllWrestlers() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  void testBookerCanFindAllWrestlers() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  void testPlayerCanFindAllWrestlers() {
    login("owner" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  void testViewerCanFindAllWrestlers() {
    login("viewer" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findAll());
  }

  @Test
  void testUnauthenticatedCannotFindAllWrestlers() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> wrestlerService.findAll());
  }

  // List (Pageable) tests
  @Test
  void testAdminCanListWrestlers() {
    login("admin" + prefix);
    Pageable pageable = PageRequest.of(0, 10);
    assertDoesNotThrow(() -> wrestlerService.list(pageable));
  }

  @Test
  void testBookerCanListWrestlers() {
    login("booker" + prefix);
    Pageable pageable = PageRequest.of(0, 10);
    assertDoesNotThrow(() -> wrestlerService.list(pageable));
  }

  @Test
  void testPlayerCanListWrestlers() {
    login("owner" + prefix);
    Pageable pageable = PageRequest.of(0, 10);
    assertDoesNotThrow(() -> wrestlerService.list(pageable));
  }

  @Test
  void testViewerCanListWrestlers() {
    login("viewer" + prefix);
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
  void testAdminCanFindWrestlerById() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.getWrestlerById(ownedWrestler.getId()));
  }

  @Test
  void testBookerCanFindWrestlerById() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
  }

  @Test
  void testPlayerCanFindAnyWrestlerById() { // Player can read any wrestler by ID
    login("owner" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findById(unownedWrestler.getId()));
  }

  @Test
  void testViewerCanFindAnyWrestlerById() {
    login("viewer" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findById(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findById(unownedWrestler.getId()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerById() {
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findById(ownedWrestler.getId()));
  }

  // findByIdWithInjuries tests
  @Test
  void testAdminCanFindWrestlerByIdWithInjuries() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
  }

  @Test
  void testBookerCanFindWrestlerByIdWithInjuries() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
  }

  @Test
  void testPlayerCanFindAnyWrestlerByIdWithInjuries() {
    login("owner" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(unownedWrestler.getId()));
  }

  @Test
  void testViewerCanFindAnyWrestlerByIdWithInjuries() {
    login("viewer" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
    assertDoesNotThrow(() -> wrestlerService.findByIdWithInjuries(unownedWrestler.getId()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerByIdWithInjuries() {
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findByIdWithInjuries(ownedWrestler.getId()));
  }

  // findByName tests
  @Test
  void testAdminCanFindWrestlerByName() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
  }

  @Test
  void testBookerCanFindWrestlerByName() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
  }

  @Test
  void testPlayerCanFindAnyWrestlerByName() {
    login("owner" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
    assertDoesNotThrow(() -> wrestlerService.findByName(unownedWrestler.getName()));
  }

  @Test
  void testViewerCanFindAnyWrestlerByName() {
    login("viewer" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByName(ownedWrestler.getName()));
    assertDoesNotThrow(() -> wrestlerService.findByName(unownedWrestler.getName()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerByName() {
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findByName(ownedWrestler.getName()));
  }

  // findByExternalId tests
  @Test
  void testAdminCanFindWrestlerByExternalId() {
    login("admin" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
  }

  @Test
  void testBookerCanFindWrestlerByExternalId() {
    login("booker" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
  }

  @Test
  void testPlayerCanFindAnyWrestlerByExternalId() {
    login("owner" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(unownedWrestler.getExternalId()));
  }

  @Test
  void testViewerCanFindAnyWrestlerByExternalId() {
    login("viewer" + prefix);
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
    assertDoesNotThrow(() -> wrestlerService.findByExternalId(unownedWrestler.getExternalId()));
  }

  @Test
  void testUnauthenticatedCannotFindWrestlerByExternalId() {
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> wrestlerService.findByExternalId(ownedWrestler.getExternalId()));
  }
}
