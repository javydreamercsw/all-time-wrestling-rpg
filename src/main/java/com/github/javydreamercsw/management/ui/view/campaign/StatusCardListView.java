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
package com.github.javydreamercsw.management.ui.view.campaign;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;
import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import lombok.NonNull;

@Route("status-cards")
@PageTitle("Status Cards Management")
@Menu(order = 13, icon = "vaadin:clipboard-user", title = "Status Cards")
@RolesAllowed(ADMIN_ROLE)
public class StatusCardListView extends Main {

  private final StatusCardService statusCardService;

  final TextField key;
  final TextField level1Name;
  final Button createBtn;
  final Grid<StatusCard> cardGrid;

  public StatusCardListView(
      @NonNull StatusCardService statusCardService, @NonNull SecurityUtils securityUtils) {
    this.statusCardService = statusCardService;

    key = new TextField();
    key.setPlaceholder("Unique key (e.g. status_draw)");
    key.setAriaLabel("Status Key");

    level1Name = new TextField();
    level1Name.setPlaceholder("Level 1 Name");
    level1Name.setAriaLabel("Level 1 Name");

    createBtn = new Button("Create", event -> createCard());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    cardGrid = new Grid<>(StatusCard.class, false);
    // Enable grid editor
    Editor<StatusCard> editor = cardGrid.getEditor();
    editor.setBuffered(true);

    editor.addSaveListener(
        event -> {
          statusCardService.save(event.getItem());
          cardGrid.getDataProvider().refreshAll();
          Notification.show("Status Card updated", 3_000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    // Create and set the binder
    Binder<StatusCard> binder = new Binder<>(StatusCard.class);
    editor.setBinder(binder);

    // Editor fields
    TextField keyField = new TextField();
    TextField l1NameField = new TextField();
    TextField l2NameField = new TextField();
    TextField descriptionField = new TextField();
    Checkbox positiveField = new Checkbox();
    TextField l1EffectField = new TextField();
    TextField l2EffectField = new TextField();
    TextField flipUpField = new TextField();
    TextField flipDownField = new TextField();
    TextField discardField = new TextField();

    cardGrid.setItems(query -> statusCardService.list(toSpringPageRequest(query)).stream());

    cardGrid
        .addColumn(StatusCard::getKey)
        .setHeader("Key")
        .setEditorComponent(keyField)
        .setSortable(true);
    cardGrid
        .addColumn(StatusCard::getLevel1Name)
        .setHeader("L1 Name")
        .setEditorComponent(l1NameField)
        .setSortable(true);
    cardGrid
        .addColumn(StatusCard::getLevel2Name)
        .setHeader("L2 Name")
        .setEditorComponent(l2NameField)
        .setSortable(true);
    cardGrid
        .addColumn(StatusCard::isPositive)
        .setHeader("Positive?")
        .setEditorComponent(positiveField);

    cardGrid
        .addComponentColumn(
            card -> {
              Button editButton = new Button("Edit");
              editButton.addClickListener(e -> cardGrid.getEditor().editItem(card));
              Button deleteButton = new Button("Delete");
              deleteButton.addClickListener(e -> deleteCard(card));
              return new HorizontalLayout(editButton, deleteButton);
            })
        .setHeader("Actions");

    // Bind editor fields
    binder.forField(keyField).bind("key");
    binder.forField(l1NameField).bind("level1Name");
    binder.forField(l2NameField).bind("level2Name");
    binder.forField(descriptionField).bind("description");
    binder.forField(positiveField).bind("positive");
    binder.forField(l1EffectField).bind("level1Effect");
    binder.forField(l2EffectField).bind("level2Effect");
    binder.forField(flipUpField).bind("flipUpCondition");
    binder.forField(flipDownField).bind("flipDownCondition");
    binder.forField(discardField).bind("discardCondition");

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    // Save and cancel buttons for the editor
    Button saveButton = new Button("Save", e -> editor.save());
    Button cancelButton = new Button("Cancel", e -> editor.cancel());
    HorizontalLayout editorActions = new HorizontalLayout(saveButton, cancelButton);
    cardGrid.getElement().appendChild(editorActions.getElement());

    add(new ViewToolbar("Status Cards", ViewToolbar.group(key, level1Name, createBtn)));
    add(cardGrid);
  }

  public void refresh() {
    cardGrid.getDataProvider().refreshAll();
  }

  private void createCard() {
    if (key.getValue().isEmpty() || level1Name.getValue().isEmpty()) {
      Notification.show("Key and Level 1 Name are required", 3_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }
    StatusCard card = new StatusCard();
    card.setKey(key.getValue());
    card.setLevel1Name(level1Name.getValue());
    card.setPositive(true); // Default
    statusCardService.save(card);
    cardGrid.getDataProvider().refreshAll();
    key.clear();
    level1Name.clear();
    Notification.show("Status Card added", 3_000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void deleteCard(@NonNull StatusCard card) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete Status Card?");
    dialog.setText(
        "Are you sure you want to delete this status card? This may cause issues with wrestlers"
            + " currently holding it.");
    dialog.setConfirmButton(
        "Delete",
        e -> {
          statusCardService.delete(card.getId());
          cardGrid.getDataProvider().refreshAll();
          Notification.show("Status Card deleted");
        });
    dialog.setCancelButton("Cancel", e -> dialog.close());
    dialog.open();
  }
}
