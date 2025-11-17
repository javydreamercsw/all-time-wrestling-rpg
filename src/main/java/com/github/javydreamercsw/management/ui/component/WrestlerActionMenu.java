package com.github.javydreamercsw.management.ui.component;

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

public class WrestlerActionMenu extends MenuBar {

  public WrestlerActionMenu(
      @NonNull Wrestler wrestler,
      @NonNull WrestlerService wrestlerService,
      @NonNull InjuryService injuryService,
      @NonNull Runnable refreshProvider,
      boolean isProfileView) {
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

    subMenu
        .addItem(
            "Edit",
            e -> {
              WrestlerDialog dialog =
                  new WrestlerDialog(wrestlerService, wrestler, refreshProvider);
              dialog.open();
            })
        .addComponentAsFirst(new Icon(VaadinIcon.EDIT));

    subMenu
        .addItem(
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
            })
        .addComponentAsFirst(new Icon(VaadinIcon.TRASH));

    subMenu
        .addItem(
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
                      wrestlerService.awardFans(wrestler.getId(), fanAmount.getValue().longValue());
                      refreshProvider.run();
                      Notification.show(
                              "Added "
                                  + fanAmount.getValue().longValue()
                                  + " fans to "
                                  + wrestler.getName(),
                              3000,
                              Notification.Position.BOTTOM_END)
                          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                      dialog.close();
                    }
                  });
              Button cancelButton = new Button("Cancel", event -> dialog.close());
              dialog.add(
                  new VerticalLayout(fanAmount, new HorizontalLayout(confirmButton, cancelButton)));
              dialog.open();
            })
        .addComponentAsFirst(new Icon(VaadinIcon.PLUS));

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
                      wrestlerService.awardFans(
                          wrestler.getId(), -fanAmount.getValue().longValue());
                      refreshProvider.run();
                      Notification.show(
                              "Removed "
                                  + fanAmount.getValue().longValue()
                                  + " fans from "
                                  + wrestler.getName(),
                              3000,
                              Notification.Position.BOTTOM_END)
                          .addThemeVariants(NotificationVariant.LUMO_ERROR);
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

    subMenu
        .addItem(
            "Add Bump",
            e -> {
              wrestlerService.addBump(wrestler.getId());
              refreshProvider.run();
              Notification.show(
                      "Added bump to " + wrestler.getName(), 3000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            })
        .addComponentAsFirst(new Icon(VaadinIcon.PLUS_CIRCLE));
    MenuItem healBump =
        subMenu.addItem(
            "Heal Bump",
            e -> {
              wrestlerService.healBump(wrestler.getId());
              refreshProvider.run();
              Notification.show(
                      "Healed bump for " + wrestler.getName(),
                      3000,
                      Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
    healBump.addComponentAsFirst(new Icon(VaadinIcon.HEART));
    healBump.setEnabled(wrestler.getBumps() > 0);

    subMenu
        .addItem(
            "Manage Injuries",
            e -> {
              InjuryDialog dialog = new InjuryDialog(wrestler, injuryService, refreshProvider);
              dialog.open();
            })
        .addComponentAsFirst(new Icon(VaadinIcon.AMBULANCE));
  }
}
