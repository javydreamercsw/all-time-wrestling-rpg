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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AccountListViewTest extends AbstractViewTest {

  @MockitoBean(name = "managementAccountService")
  private AccountService accountService;

  @MockitoBean private SecurityUtils securityUtils;

  private AccountListView view;

  @BeforeEach
  void setup() {
    when(accountService.list(any(Pageable.class))).thenReturn(Page.empty());

    view = new AccountListView(accountService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render account grid")
  void shouldRenderAccountGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("account-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render new-account button")
  void shouldRenderNewAccountButton() {
    Button newAccountButton = _get(view, Button.class, spec -> spec.withId("new-account-button"));
    assertTrue(newAccountButton.isVisible());
  }

  @Test
  @DisplayName("Clicking new-account button opens account form dialog")
  void clickingNewAccountButtonOpensDialog() {
    Button newAccountButton = _get(view, Button.class, spec -> spec.withId("new-account-button"));
    newAccountButton.click();

    com.vaadin.flow.component.dialog.Dialog dialog =
        _get(com.vaadin.flow.component.dialog.Dialog.class);
    assertTrue(dialog.isOpened());
  }
}
