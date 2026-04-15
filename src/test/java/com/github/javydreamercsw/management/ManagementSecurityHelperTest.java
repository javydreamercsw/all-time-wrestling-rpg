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
package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.TestSecurityContextHolder;

class ManagementSecurityHelperTest extends ManagementIntegrationTest {

  private String testUsername = "helper_test_user";

  @BeforeEach
  void init() {
    createTestAccount(testUsername, RoleName.PLAYER);
  }

  @Test
  void testLoginAs() {
    loginAs(testUsername);
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(auth);
    Assertions.assertEquals(testUsername, auth.getName());

    Authentication testAuth = TestSecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(testAuth);
    Assertions.assertEquals(testUsername, testAuth.getName());
  }

  @Test
  void testLoginAsNotFound() {
    Assertions.assertThrows(RuntimeException.class, () -> loginAs("non_existent_user"));
  }

  @Test
  void testClearSecurityContext() {
    loginAs(testUsername);
    clearSecurityContext();

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    Assertions.assertNull(TestSecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void testRefreshSecurityContextCustomUserDetails() {
    Account account = accountRepository.findByUsername(testUsername).orElseThrow();
    CustomUserDetails principal = new CustomUserDetails(account, null);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            principal, "pass", Collections.singleton(new SimpleGrantedAuthority("ROLE_PLAYER")));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // This should trigger the first block in refreshSecurityContext and call this.login(account)
    refreshSecurityContext();

    Authentication refreshedAuth = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(refreshedAuth);
    Assertions.assertEquals(testUsername, refreshedAuth.getName());
  }

  @Test
  void testRefreshSecurityContextStandardUserDetails() {
    UserDetails userDetails =
        User.withUsername(testUsername).password("pass").roles("PLAYER").build();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, "pass", userDetails.getAuthorities());

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // This should trigger the second block in refreshSecurityContext
    refreshSecurityContext();

    Authentication refreshedAuth = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(refreshedAuth);
    Assertions.assertEquals(testUsername, refreshedAuth.getName());
  }

  @Test
  void testRefreshSecurityContextNotFound() {
    CustomUserDetails principal =
        new CustomUserDetails(new Account("unknown", "pass", "unknown@example.com"), null);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, "pass", Collections.emptyList());

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Should trigger ifPresentOrElse's else block and clear context
    refreshSecurityContext();

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
