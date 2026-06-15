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
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;

@PageTitle("Accounts")
@Route(value = "account-list")
@RolesAllowed("ADMIN")
@Menu(order = 1, icon = "vaadin:users", title = "Accounts")
public class AccountListView extends Main {

  private final AccountService accountService;
  private final Grid<Account> grid = new Grid<>(Account.class, false);

  public AccountListView(
      @Qualifier("managementAccountService") final AccountService accountService) {
    this.accountService = accountService;

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
    grid.addColumn(Account::getUsername).setHeader("Username").setSortable(true).setFlexGrow(1);
    grid.addColumn(Account::getEmail).setHeader("Email").setSortable(true).setFlexGrow(2);
    grid.addColumn(
            account ->
                account.getRoles().stream()
                    .findFirst()
                    .map(role -> role.getName().name())
                    .orElse("N/A"))
        .setHeader("Role")
        .setSortable(true)
        .setFlexGrow(0)
        .setWidth("8em");
    grid.setId("account-grid");
    grid.addColumn(Account::getLastLogin).setHeader("Last Login").setSortable(true).setFlexGrow(1);

    grid.addComponentColumn(
            account -> {
              Button editButton = new Button(VaadinIcon.EDIT.create());
              editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
              editButton.setTooltipText("Edit");
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

              Button toggleButton;
              if (account.isEnabled()) {
                toggleButton = new Button(VaadinIcon.BAN.create());
                toggleButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR);
                toggleButton.setTooltipText("Disable");
                toggleButton.setId("disable-button-" + account.getId());
                toggleButton.addClickListener(
                    e -> {
                      ConfirmDialog confirmDialog = new ConfirmDialog();
                      confirmDialog.setHeader("Disable Account");
                      confirmDialog.setText(
                          "This account will be disabled and cannot log in."
                              + " You can re-enable it later.");
                      confirmDialog.setConfirmButton(
                          "Disable",
                          event -> {
                            accountService.delete(account.getId());
                            refreshGrid();
                          });
                      confirmDialog.setConfirmButtonTheme("error");
                      confirmDialog.setCancelable(true);
                      confirmDialog.open();
                    });
              } else {
                toggleButton = new Button(VaadinIcon.CHECK.create());
                toggleButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_SUCCESS);
                toggleButton.setTooltipText("Enable");
                toggleButton.setId("enable-button-" + account.getId());
                toggleButton.addClickListener(
                    e -> {
                      accountService.enable(account.getId());
                      refreshGrid();
                    });
              }

              HorizontalLayout actions = new HorizontalLayout(editButton, toggleButton);
              if (accountService.canDelete(account)) {
                Button deleteButton = new Button(VaadinIcon.TRASH.create());
                deleteButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR);
                deleteButton.setTooltipText("Delete");
                deleteButton.setId("delete-button-" + account.getId());
                deleteButton.addClickListener(
                    e -> {
                      ConfirmDialog confirmDialog = new ConfirmDialog();
                      confirmDialog.setHeader("Delete Account");
                      confirmDialog.setText(
                          "This will permanently delete the account. This cannot be undone.");
                      confirmDialog.setConfirmButton(
                          "Delete",
                          event -> {
                            accountService.hardDelete(account.getId());
                            refreshGrid();
                          });
                      confirmDialog.setConfirmButtonTheme("error");
                      confirmDialog.setCancelable(true);
                      confirmDialog.open();
                    });
                actions.add(deleteButton);
              }
              actions.setSpacing(false);
              actions.setPadding(false);
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(0)
        .setWidth("8em");
  }

  private void refreshGrid() {
    grid.setItems(
        query ->
            accountService.list(PageRequest.of(query.getPage(), query.getPageSize())).stream());
  }
}
