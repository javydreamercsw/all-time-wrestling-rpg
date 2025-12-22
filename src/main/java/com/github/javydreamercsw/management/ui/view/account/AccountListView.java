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
package com.github.javydreamercsw.management.ui.view.account;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

@PageTitle("Accounts")
@Route(value = "account-list")
@RolesAllowed("ADMIN")
@Menu(order = 1, icon = "vaadin:users", title = "Accounts")
public class AccountListView extends Main {

  private final AccountService accountService;
  private final SecurityUtils securityUtils;
  private final Grid<Account> grid = new Grid<>(Account.class, false);

  public AccountListView(AccountService accountService, SecurityUtils securityUtils) {
    this.accountService = accountService;
    this.securityUtils = securityUtils;

    Button createButton = new Button("New Account", VaadinIcon.PLUS.create());
    createButton.setId("new-account-button");
    createButton.addClickListener(
        e -> {
          AccountFormDialog dialog = new AccountFormDialog(accountService, new Account());
          dialog.addOpenedChangeListener(
              event -> {
                if (!event.isOpened()) {
                  refreshGrid();
                }
              });
          dialog.open();
        });
    createButton.setId("new-account-button");

    add(new ViewToolbar("Account List", ViewToolbar.group(createButton)));

    setupGrid();
    add(grid);
    refreshGrid();
  }

  private void setupGrid() {
    grid.addColumn(Account::getUsername).setHeader("Username").setSortable(true);
    grid.addColumn(Account::getEmail).setHeader("Email").setSortable(true);
    grid.addColumn(account -> account.getRoles().iterator().next().getName())
        .setHeader("Role")
        .setSortable(true);
    grid.setId("account-grid");
    grid.addColumn(Account::getLastLogin).setHeader("Last Login").setSortable(true);

    grid.addComponentColumn(
            account -> {
              Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
              editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
              editButton.setId("edit-button-" + account.getId());
              editButton.addClickListener(
                  e -> {
                    AccountFormDialog dialog = new AccountFormDialog(accountService, account);
                    dialog.addOpenedChangeListener(
                        event -> {
                          if (!event.isOpened()) {
                            refreshGrid();
                          }
                        });
                    dialog.open();
                  });
              return editButton;
            })
        .setFlexGrow(0);

    LitRenderer<Account> enabledRenderer =
        LitRenderer.<Account>of(
                "<vaadin-icon icon='vaadin:${item.icon}' style='color:"
                    + " ${item.color}'></vaadin-icon>")
            .withProperty("icon", account -> account.isEnabled() ? "check" : "close")
            .withProperty("color", account -> account.isEnabled() ? "green" : "red");

    grid.addColumn(enabledRenderer).setHeader("Enabled").setSortable(true).setFlexGrow(0);
    grid.addComponentColumn(
            account -> {
              Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
              deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteButton.setId("delete-button-" + account.getId());
              deleteButton.addClickListener(
                  e -> {
                    if (accountService.canDelete(account)) {
                      ConfirmDialog confirmDialog = new ConfirmDialog();
                      confirmDialog.setHeader("Confirm Delete");
                      confirmDialog.setText("Are you sure you want to delete this account?");

                      confirmDialog.setConfirmButton(
                          "Delete",
                          event -> {
                            accountService.delete(account.getId());
                            refreshGrid();
                          });
                      confirmDialog.setConfirmButtonTheme("error");
                      confirmDialog.setCancelable(true);
                      confirmDialog.open();
                    } else {
                      new ConfirmDialog(
                              "Cannot Delete Account",
                              "This account is associated with a wrestler and cannot be deleted.",
                              "OK",
                              null)
                          .open();
                    }
                  });
              return deleteButton;
            })
        .setFlexGrow(0);
  }

  private void refreshGrid() {
    grid.setItems(
        query ->
            accountService.list(PageRequest.of(query.getPage(), query.getPageSize())).stream());
  }
}
