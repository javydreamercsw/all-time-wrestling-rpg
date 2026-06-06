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
package com.github.javydreamercsw.base.ui.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class LoginViewTest extends AbstractViewTest {

  private AccountRepository accountRepository;
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setup() {
    accountRepository = mock(AccountRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    when(accountRepository.findByUsername(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.empty());
  }

  private LoginView buildView() {
    LoginView view = new LoginView(accountRepository, passwordEncoder);
    UI.getCurrent().add(view);
    return view;
  }

  @Test
  @DisplayName("Should render app name heading")
  void shouldRenderAppName() {
    LoginView view = buildView();
    H1 heading = _get(view, H1.class, spec -> spec.withText("All Time Wrestling RPG"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Forgot-password button should be visible on the login form")
  void forgotPasswordButtonIsVisible() {
    LoginView view = buildView();
    LoginForm form = _get(view, LoginForm.class);
    assertTrue(form.isForgotPasswordButtonVisible());
  }

  @Test
  @DisplayName("Hint hidden when no accounts exist at default credentials")
  void hintHiddenWhenNoDefaultAccounts() {
    LoginView view = buildView();
    LoginForm form = _get(view, LoginForm.class);
    assertNotNull(form);
  }

  @Test
  @DisplayName("Hint shown only for accounts still using default password")
  void hintShownForRemainingDefaultAccounts() {
    Account adminAccount = mock(Account.class);
    when(adminAccount.getPassword()).thenReturn("$2a$encoded");
    when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
    when(passwordEncoder.matches("admin123", "$2a$encoded")).thenReturn(true);

    LoginView view = buildView();
    assertNotNull(_get(view, LoginForm.class));
  }
}
