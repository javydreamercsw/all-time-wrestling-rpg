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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class HallOfFameViewTest extends AbstractViewTest {

  @Mock private AccountRepository accountRepository;
  @Mock private TransactionTemplate transactionTemplate;

  private HallOfFameView view;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() {
    when(transactionTemplate.execute(any(TransactionCallback.class)))
        .thenAnswer(
            inv -> {
              TransactionCallback<?> callback = inv.getArgument(0);
              return callback.doInTransaction(null);
            });
    when(accountRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

    view = new HallOfFameView(accountRepository, transactionTemplate);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the hall-of-fame grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("hall-of-fame-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Grid should load accounts from repository")
  void gridLoadsFromRepository() {
    Account account = new Account();
    account.setUsername("champion");
    when(accountRepository.findAll(any(Sort.class))).thenReturn(List.of(account));

    view = new HallOfFameView(accountRepository, transactionTemplate);
    UI.getCurrent().add(view);

    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("hall-of-fame-grid"));
    assertEquals(1, grid.getListDataView().getItemCount());
  }
}
