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
package com.github.javydreamercsw.base.security;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.PasswordResetService;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
class SecurityServiceIT extends ManagementIntegrationTest {

  @Autowired private AccountService accountService;
  @Autowired private GameSettingService gameSettingService;
  @Autowired private PasswordResetService passwordResetService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;

  private static final String TEST_PASSWORD = "ValidPassword1!";

  private Account createAccountDirectly(
      String username, String password, String email, RoleName roleName) {
    Role role = roleRepository.findByName(roleName).orElseThrow();
    Account account = new Account(username, passwordEncoder.encode(password), email);
    account.setRoles(Set.of(role));
    return accountRepository.save(account);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateAccount() {

    accountService.createAccount(
        "admin_can_create",
        TEST_PASSWORD,
        "admin_can_create-" + UUID.randomUUID() + "@test.com",
        RoleName.PLAYER);

    // No exception means success

  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCannotCreateAccount() {

    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            accountService.createAccount(
                "booker_cannot_create",
                TEST_PASSWORD,
                "booker_cannot_create-" + UUID.randomUUID() + "@test.com",
                RoleName.PLAYER));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateAccount() {

    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            accountService.createAccount(
                "player_cannot_create",
                TEST_PASSWORD,
                "player_cannot_create-" + UUID.randomUUID() + "@test.com",
                RoleName.PLAYER));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotCreateAccount() {

    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            accountService.createAccount(
                "viewer_cannot_create",
                TEST_PASSWORD,
                "viewer_cannot_create-" + UUID.randomUUID() + "@test.com",
                RoleName.PLAYER));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanUpdateAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(authentication, "Authentication should not be null");

    Object principalObj = authentication.getPrincipal();
    Assertions.assertTrue(
        principalObj instanceof CustomUserDetails, "Principal should be CustomUserDetails");

    CustomUserDetails principal = (CustomUserDetails) principalObj;
    Account account = principal.getAccount();
    Assertions.assertNotNull(account, "Account should not be null");

    account.setEmail("new_admin_email@test.com");
    accountService.update(account);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanUpdateAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(authentication, "Authentication should not be null");

    Object principalObj = authentication.getPrincipal();
    Assertions.assertTrue(
        principalObj instanceof CustomUserDetails, "Principal should be CustomUserDetails");

    CustomUserDetails principal = (CustomUserDetails) principalObj;
    Account account = principal.getAccount();
    Assertions.assertNotNull(account, "Account should not be null");

    account.setEmail("new_booker_email@test.com");
    account.setPassword("ValidPassword1!");
    accountService.update(account);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCanUpdateOwnAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(authentication, "Authentication should not be null");

    Object principalObj = authentication.getPrincipal();
    Assertions.assertTrue(
        principalObj instanceof CustomUserDetails, "Principal should be CustomUserDetails");

    CustomUserDetails principal = (CustomUserDetails) principalObj;
    Account playerAccount = principal.getAccount();
    Assertions.assertNotNull(playerAccount, "Account should not be null");

    playerAccount.setEmail("new_player_email@test.com");
    accountService.update(playerAccount);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotUpdateOtherAccount() {
    // Create an account for another player directly (not acting as this player for creation)
    Account otherAccount =
        createAccountDirectly(
            "other_player_account",
            TEST_PASSWORD,
            "other_player_account@test.com",
            RoleName.PLAYER);
    otherAccount.setEmail("new_other_email@test.com");
    Assertions.assertThrows(AccessDeniedException.class, () -> accountService.update(otherAccount));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotUpdateAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(authentication, "Authentication should not be null");

    Object principalObj = authentication.getPrincipal();
    Assertions.assertTrue(
        principalObj instanceof CustomUserDetails, "Principal should be CustomUserDetails");

    CustomUserDetails principal = (CustomUserDetails) principalObj;
    Account account = principal.getAccount();
    Assertions.assertNotNull(account, "Account should not be null");

    account.setEmail("new_viewer_email@test.com");
    Assertions.assertThrows(AccessDeniedException.class, () -> accountService.update(account));
  }

  @Test
  @WithAnonymousUser
  void testPasswordResetServiceResetsPassword() {
    // Need to create an account directly in the test to control its initial state.
    Account account =
        createAccountDirectly(
            "reset_user",
            TEST_PASSWORD,
            "reset_user-" + UUID.randomUUID() + "@test.com",
            RoleName.PLAYER);

    String token = passwordResetService.createPasswordResetTokenForUser(account);
    Assertions.assertNotNull(token);

    String newPassword = "NewValidPassword1!";
    passwordResetService.resetPassword(token, newPassword);

    // Verify password change
    UserDetails userDetails = userDetailsService.loadUserByUsername(account.getUsername());
    Assertions.assertTrue(passwordEncoder.matches(newPassword, userDetails.getPassword()));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanGetCurrentGameDate() {
    gameSettingService.getCurrentGameDate();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanGetCurrentGameDate() {
    gameSettingService.getCurrentGameDate();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCanGetCurrentGameDate() {
    gameSettingService.getCurrentGameDate();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCanGetCurrentGameDate() {
    gameSettingService.getCurrentGameDate();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanSaveCurrentGameDate() {
    gameSettingService.saveCurrentGameDate(LocalDate.now());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanSaveCurrentGameDate() {
    gameSettingService.saveCurrentGameDate(LocalDate.now());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotSaveCurrentGameDate() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> gameSettingService.saveCurrentGameDate(LocalDate.now()));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotSaveCurrentGameDate() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> gameSettingService.saveCurrentGameDate(LocalDate.now()));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanFindGameSettingById() {
    gameSettingService.findById("some_key");
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanFindGameSettingById() {
    gameSettingService.findById("some_key");
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCanFindGameSettingById() {
    gameSettingService.findById("some_key");
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCanFindGameSettingById() {
    gameSettingService.findById("some_key");
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanSaveGameSetting() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setId("test_key");
    gameSetting.setValue("test_value");
    gameSettingService.save(gameSetting);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanSaveGameSetting() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setId("test_key_booker");
    gameSetting.setValue("test_value_booker");
    gameSettingService.save(gameSetting);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotSaveGameSetting() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setId("test_key_player");
    gameSetting.setValue("test_value_player");
    Assertions.assertThrows(
        AccessDeniedException.class, () -> gameSettingService.save(gameSetting));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotSaveGameSetting() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setId("test_key_viewer");
    gameSetting.setValue("test_value_viewer");
    Assertions.assertThrows(
        AccessDeniedException.class, () -> gameSettingService.save(gameSetting));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanFindAllGameSettings() {
    gameSettingService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanFindAllGameSettings() {
    gameSettingService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCanFindAllGameSettings() {
    gameSettingService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCanFindAllGameSettings() {
    gameSettingService.findAll();
    // No exception means success
  }
}
