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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.injury.InjuryDialog;
import com.github.javydreamercsw.management.ui.view.wrestler.WrestlerDialog;
import com.github.javydreamercsw.management.ui.view.wrestler.WrestlerListView;
import com.github.javydreamercsw.management.ui.view.wrestler.WrestlerProfileView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.RouteParameters;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;

public class WrestlerActionMenu extends MenuBar {
  private final AccountService accountService;

  public WrestlerActionMenu(
      @NonNull Wrestler wrestler,
      @NonNull WrestlerService wrestlerService,
      @NonNull InjuryService injuryService,
      @NonNull Runnable refreshProvider,
      boolean isProfileView,
      @NonNull SecurityUtils securityUtils,
      @NonNull @Qualifier("baseAccountService") AccountService accountService) {
    this.accountService = accountService;
    addThemeVariants(MenuBarVariant.LUMO_PRIMARY);
    MenuItem menuItem = addItem("Actions");
    SubMenu subMenu = menuItem.getSubMenu();
    MenuItem viewProfileItem =
        subMenu.addItem(
            "View Profile",
            e ->
                UI.getCurrent()
                    .navigate(
                        WrestlerProfileView.class,
                        new RouteParameters("wrestlerId", String.valueOf(wrestler.getId()))));
    viewProfileItem.addComponentAsFirst(new Icon(VaadinIcon.USER));
    viewProfileItem.setEnabled(!isProfileView);

    MenuItem editItem =
        subMenu.addItem(
            "Edit",
            e -> {
              WrestlerDialog dialog =
                  new WrestlerDialog(
                      wrestlerService, accountService, wrestler, refreshProvider, securityUtils);
              dialog.open();
            });
    editItem.addComponentAsFirst(new Icon(VaadinIcon.EDIT));
    editItem.setId("edit-" + wrestler.getId());
    editItem.setVisible(securityUtils.canEdit(wrestler));

    MenuItem deleteItem =
        subMenu.addItem(
            "Delete",
            e -> {
              wrestlerService.delete(wrestler);
              Notification.show("Wrestler deleted", 2000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_ERROR);
              if (isProfileView) {
                UI.getCurrent().navigate(WrestlerListView.class);
              } else {
                refreshProvider.run();
              }
            });
    deleteItem.addComponentAsFirst(new Icon(VaadinIcon.TRASH));
    deleteItem.setId("delete-" + wrestler.getId());
    deleteItem.setVisible(securityUtils.canDelete(wrestler));

    MenuItem addFansItem =
        subMenu.addItem(
            "Add Fans",
            e -> {
              Dialog dialog = new Dialog();
              NumberField fanAmount = new NumberField("Fan Amount");
              fanAmount.setPlaceholder("Enter amount");
              fanAmount.setMin(1);
              Button confirmButton = new Button("Confirm");
              confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
              confirmButton.addClickListener(
                  event -> {
                    if (fanAmount.getValue() != null) {
                      wrestlerService
                          .awardFans(wrestler.getId(), fanAmount.getValue().longValue())
                          .ifPresent(
                              w -> {
                                refreshProvider.run();
                                Notification.show(
                                        "Added "
                                            + fanAmount.getValue().longValue()
                                            + " fans to "
                                            + wrestler.getName(),
                                        3000,
                                        Notification.Position.BOTTOM_END)
                                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                              });
                      dialog.close();
                    }
                  });
              Button cancelButton = new Button("Cancel", event -> dialog.close());
              dialog.add(
                  new VerticalLayout(fanAmount, new HorizontalLayout(confirmButton, cancelButton)));
              dialog.open();
            });
    addFansItem.addComponentAsFirst(new Icon(VaadinIcon.PLUS));
    addFansItem.setVisible(securityUtils.canEdit());

    MenuItem removeFansItem =
        subMenu.addItem(
            "Remove Fans",
            e -> {
              Dialog dialog = new Dialog();
              NumberField fanAmount = new NumberField("Fan Amount");
              fanAmount.setPlaceholder("Enter amount");
              fanAmount.setMin(1);
              Button confirmButton = new Button("Confirm");
              confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
              confirmButton.addClickListener(
                  event -> {
                    if (fanAmount.getValue() != null) {
                      wrestlerService
                          .awardFans(wrestler.getId(), -fanAmount.getValue().longValue())
                          .ifPresent(
                              w -> {
                                refreshProvider.run();
                                Notification.show(
                                        "Removed "
                                            + fanAmount.getValue().longValue()
                                            + " fans from "
                                            + wrestler.getName(),
                                        3000,
                                        Notification.Position.BOTTOM_END)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                              });
                      dialog.close();
                    }
                  });
              Button cancelButton = new Button("Cancel", event -> dialog.close());
              dialog.add(
                  new VerticalLayout(fanAmount, new HorizontalLayout(confirmButton, cancelButton)));
              dialog.open();
            });
    removeFansItem.addComponentAsFirst(new Icon(VaadinIcon.MINUS));
    removeFansItem.setEnabled(wrestler.getFans() > 0);
    removeFansItem.setVisible(securityUtils.canEdit());

    MenuItem addBumpItem =
        subMenu.addItem(
            "Add Bump",
            e -> {
              wrestlerService
                  .addBump(wrestler.getId())
                  .ifPresent(
                      w -> {
                        refreshProvider.run();
                        Notification.show(
                                "Added bump to " + wrestler.getName(),
                                3000,
                                Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                      });
            });
    addBumpItem.setId("add-bump-" + wrestler.getId());
    addBumpItem.addComponentAsFirst(new Icon(VaadinIcon.PLUS_CIRCLE));
    addBumpItem.setVisible(securityUtils.canEdit());

    MenuItem healBump =
        subMenu.addItem(
            "Heal Bump",
            e -> {
              wrestlerService
                  .healBump(wrestler.getId())
                  .ifPresent(
                      w -> {
                        refreshProvider.run();
                        Notification.show(
                                "Healed bump for " + wrestler.getName(),
                                3000,
                                Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                      });
            });
    healBump.addComponentAsFirst(new Icon(VaadinIcon.HEART));
    healBump.setId("heal-bump-" + wrestler.getId());
    healBump.setEnabled(wrestler.getBumps() > 0);
    healBump.setVisible(securityUtils.canEdit());

    MenuItem manageInjuriesItem =
        subMenu.addItem(
            "Manage Injuries",
            e -> {
              InjuryDialog dialog =
                  new InjuryDialog(wrestler, injuryService, refreshProvider, securityUtils);
              dialog.setId("injury-dialog");
              dialog.open();
            });
    manageInjuriesItem.setId("manage-injuries-" + wrestler.getId());
    manageInjuriesItem.addComponentAsFirst(new Icon(VaadinIcon.AMBULANCE));
    manageInjuriesItem.setVisible(securityUtils.canEdit());

    // The menu should only be visible if there are any actions to perform
    setVisible(securityUtils.canEdit() || securityUtils.canDelete());
  }
}
