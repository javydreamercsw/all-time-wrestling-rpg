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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;

@Route("hall-of-fame")
@PageTitle("Hall of Fame")
@Menu(order = 6, icon = "vaadin:academy-cap", title = "Hall of Fame")
@PermitAll
@Slf4j
public class HallOfFameView extends Main {

  private final AccountRepository accountRepository;
  private final TransactionTemplate transactionTemplate;
  private final Grid<Account> grid = new Grid<>(Account.class, false);

  public HallOfFameView(
      AccountRepository accountRepository, TransactionTemplate transactionTemplate) {
    this.accountRepository = accountRepository;
    this.transactionTemplate = transactionTemplate;
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);
    setHeightFull();

    add(new ViewToolbar("Hall of Fame"));
    configureGrid();
    grid.setId("hall-of-fame-grid");

    VerticalLayout content = new VerticalLayout(grid);
    content.setSizeFull();
    add(content);

    updateList();
  }

  private void configureGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setSizeFull();

    grid.addColumn(Account::getUsername).setHeader("Player").setSortable(true);
    grid.addColumn(Account::getLegacyScore).setHeader("Legacy Score").setSortable(true);
    grid.addColumn(Account::getPrestige).setHeader("Prestige").setSortable(true);

    grid.addComponentColumn(
            account -> {
              HorizontalLayout layout = new HorizontalLayout();
              account
                  .getAchievements()
                  .forEach(
                      achievement -> {
                        Span badge = new Span(achievement.getName());
                        badge.getElement().getThemeList().add("badge success");
                        Tooltip.forComponent(badge).setText(achievement.getDescription());
                        layout.add(badge);
                      });
              return layout;
            })
        .setHeader("Achievements");
  }

  private void updateList() {
    transactionTemplate.execute(
        status -> {
          java.util.List<Account> accounts =
              accountRepository.findAll(Sort.by(Sort.Direction.DESC, "legacyScore"));
          accounts.forEach(
              account -> org.hibernate.Hibernate.initialize(account.getAchievements()));
          grid.setItems(accounts);
          return null;
        });
  }
}
