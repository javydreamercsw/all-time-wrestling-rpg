package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;

@Route("wrestler-list")
@PageTitle("Wrestler List")
@Menu(order = 0, icon = "vaadin:user", title = "Wrestler List")
@PermitAll // When security is enabled, allow all authenticated users
public class WrestlerListView extends Main {

  private final WrestlerService wrestlerService;

  final TextField name;
  final Button createBtn;
  final Grid<Wrestler> wrestlerGrid;

  public WrestlerListView(@NonNull WrestlerService wrestlerService) {
    this.wrestlerService = wrestlerService;

    name = new TextField();
    name.setPlaceholder("What do you want the wrestler name to be?");
    name.setAriaLabel("Wrestler Name");
    name.setId("wrestler-name-field");
    name.setMaxLength(Card.DESCRIPTION_MAX_LENGTH);
    name.setMinWidth("20em");

    createBtn = new Button("Create", event -> createWrestler());
    createBtn.setId("create-wrestler-button");
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    wrestlerGrid = new Grid<>();

    wrestlerGrid.setItems(query -> wrestlerService.list(toSpringPageRequest(query)).stream());
    wrestlerGrid.addColumn(Wrestler::getName).setHeader("Name").setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getGender).setHeader("Gender").setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getDeckSize).setHeader("Deck Size").setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getStartingHealth)
        .setHeader("Starting Health")
        .setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getLowHealth).setHeader("Low Health").setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getStartingStamina)
        .setHeader("Starting Stamina")
        .setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getLowStamina).setHeader("Low Stamina").setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getFans).setHeader("Fans").setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getCreationDate).setHeader("Creation Date");
    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              Button addFansButton = new Button("Add Fans");
              addFansButton.addClickListener(
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
                            wrestlerService.awardFans(
                                wrestler.getId(), fanAmount.getValue().longValue());
                            wrestlerGrid.getDataProvider().refreshAll();
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
                        new VerticalLayout(
                            fanAmount, new HorizontalLayout(confirmButton, cancelButton)));
                    dialog.open();
                  });
              Button removeFansButton = new Button("Remove Fans");
              removeFansButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
              removeFansButton.addClickListener(
                  e -> {
                    Dialog dialog = new Dialog();
                    NumberField fanAmount = new NumberField("Fan Amount");
                    fanAmount.setPlaceholder("Enter amount");
                    fanAmount.setMin(1);
                    Button confirmButton = new Button("Confirm");
                    confirmButton.addThemeVariants(
                        ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
                    confirmButton.addClickListener(
                        event -> {
                          if (fanAmount.getValue() != null) {
                            wrestlerService.awardFans(
                                wrestler.getId(), -fanAmount.getValue().longValue());
                            wrestlerGrid.getDataProvider().refreshAll();
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
                        new VerticalLayout(
                            fanAmount, new HorizontalLayout(confirmButton, cancelButton)));
                    dialog.open();
                  });
              return new HorizontalLayout(addFansButton, removeFansButton);
            })
        .setHeader("Fan Actions");
    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              return new RouterLink(
                  "View Profile",
                  WrestlerProfileView.class,
                  new RouteParameters("wrestlerId", String.valueOf(wrestler.getId())));
            })
        .setHeader("Profile");
    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              Button editButton = new Button("Edit");
              editButton.setId("edit-" + wrestler.getId());
              editButton.addClickListener(
                  e -> {
                    WrestlerDialog dialog =
                        new WrestlerDialog(
                            wrestlerService, wrestler, wrestlerGrid.getDataProvider()::refreshAll);
                    dialog.open();
                  });
              return editButton;
            })
        .setHeader("Actions");
    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              Button deleteButton = new Button("Delete");
              deleteButton.setId("delete-" + wrestler.getId());
              deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
              deleteButton.addClickListener(
                  e -> {
                    wrestlerService.delete(wrestler);
                    wrestlerGrid.getDataProvider().refreshAll();
                    Notification.show("Wrestler deleted", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                  });
              return deleteButton;
            })
        .setHeader("Delete");
    wrestlerGrid.setSizeFull();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(new ViewToolbar("Wrestler List", ViewToolbar.group(name, createBtn)));
    add(wrestlerGrid);
  }

  private void createWrestler() {
    wrestlerService.createWrestler(name.getValue());
    wrestlerGrid.getDataProvider().refreshAll();
    name.clear();
    Notification.show("Wrestler added", 3_000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
