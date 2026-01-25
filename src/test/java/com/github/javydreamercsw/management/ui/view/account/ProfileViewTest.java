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
package com.github.javydreamercsw.management.ui.view.account;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

class ProfileViewTest extends AbstractViewTest {

  @Mock private AccountService accountService;
  @Mock private SecurityUtils securityUtils;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private ThemeService themeService;

  @BeforeEach
  void setUp() {
    Account account = new Account();
    account.setUsername("testuser");
    account.setEmail("test@test.com");
    account.setThemePreference("light");

    CustomUserDetails userDetails = new CustomUserDetails(account);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(accountService.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(themeService.getAvailableThemes()).thenReturn(java.util.List.of("light", "dark"));
  }

  @Test
  void testThemeSelectionPresence() {
    ProfileView view =
        new ProfileView(accountService, securityUtils, passwordEncoder, themeService);
    assertNotNull(view);

    ComboBox<String> themeSelect =
        _get(view, ComboBox.class, spec -> spec.withId("theme-selection"));
    assertNotNull(themeSelect);
  }
}
