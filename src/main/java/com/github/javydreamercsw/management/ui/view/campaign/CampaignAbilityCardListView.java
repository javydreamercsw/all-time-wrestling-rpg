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
import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import lombok.NonNull;

@Route("campaign-cards")
@PageTitle("Campaign Ability Cards")
@Menu(order = 12, icon = "vaadin:magic-wand", title = "Campaign Cards")
@RolesAllowed(ADMIN_ROLE)
public class CampaignAbilityCardListView extends Main {

  private final CampaignAbilityCardService cardService;

  final TextField name;
  final Button createBtn;
  final Grid<CampaignAbilityCard> cardGrid;

  public CampaignAbilityCardListView(
      @NonNull CampaignAbilityCardService cardService, @NonNull SecurityUtils securityUtils) {
    this.cardService = cardService;

    name = new TextField();
    name.setPlaceholder("New card name");
    name.setAriaLabel("Card Name");
    name.setMinWidth("20em");

    createBtn = new Button("Create", event -> createCard());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    cardGrid = new Grid<>(CampaignAbilityCard.class, false);
    // Enable grid editor
    Editor<CampaignAbilityCard> editor = cardGrid.getEditor();
    editor.setBuffered(true);

    editor.addSaveListener(
        event -> {
          cardService.save(event.getItem());
          cardGrid.getDataProvider().refreshAll();
          Notification.show("Card updated", 3_000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    // Create and set the binder
    Binder<CampaignAbilityCard> binder = new Binder<>(CampaignAbilityCard.class);
    editor.setBinder(binder);

    // Editor fields
    TextField nameField = new TextField();
    TextField descriptionField = new TextField();
    ComboBox<AlignmentType> alignmentField = new ComboBox<>();
    alignmentField.setItems(AlignmentType.values());
    TextField levelField = new TextField();
    Checkbox oneTimeField = new Checkbox();
    ComboBox<AbilityTiming> timingField = new ComboBox<>();
    timingField.setItems(AbilityTiming.values());
    TextField scriptField = new TextField();
    TextField secondaryScriptField = new TextField();
    Checkbox secondaryOneTimeField = new Checkbox();
    ComboBox<AbilityTiming> secondaryTimingField = new ComboBox<>();
    secondaryTimingField.setItems(AbilityTiming.values());

    cardGrid.setItems(query -> cardService.list(toSpringPageRequest(query)).stream());
    cardGrid
        .addColumn(CampaignAbilityCard::getName)
        .setHeader("Name")
        .setEditorComponent(nameField)
        .setSortable(true);
    cardGrid
        .addColumn(CampaignAbilityCard::getAlignmentType)
        .setHeader("Alignment")
        .setEditorComponent(alignmentField)
        .setSortable(true);
    cardGrid
        .addColumn(CampaignAbilityCard::getLevel)
        .setHeader("Level")
        .setEditorComponent(levelField)
        .setSortable(true);
    cardGrid
        .addColumn(CampaignAbilityCard::isOneTimeUse)
        .setHeader("1-Time?")
        .setEditorComponent(oneTimeField);
    cardGrid
        .addColumn(CampaignAbilityCard::getTiming)
        .setHeader("Timing")
        .setEditorComponent(timingField)
        .setSortable(true);

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
    binder.forField(nameField).bind("name");
    binder.forField(descriptionField).bind("description");
    binder.forField(alignmentField).bind("alignmentType");
    binder
        .forField(levelField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("level");
    binder.forField(oneTimeField).bind("oneTimeUse");
    binder.forField(timingField).bind("timing");
    binder.forField(scriptField).bind("effectScript");
    binder.forField(secondaryScriptField).bind("secondaryEffectScript");
    binder.forField(secondaryOneTimeField).bind("secondaryOneTimeUse");
    binder.forField(secondaryTimingField).bind("secondaryTiming");

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

    add(new ViewToolbar("Campaign Cards", ViewToolbar.group(name, createBtn)));
    add(cardGrid);
  }

  private void createCard() {
    if (name.getValue().isEmpty()) return;
    CampaignAbilityCard card = new CampaignAbilityCard();
    card.setName(name.getValue());
    card.setAlignmentType(AlignmentType.FACE); // Default
    cardService.save(card);
    cardGrid.getDataProvider().refreshAll();
    name.clear();
    Notification.show("Card added", 3_000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void deleteCard(@NonNull CampaignAbilityCard card) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete Card?");
    dialog.setText("Are you sure you want to delete this card?");
    dialog.setConfirmButton(
        "Delete",
        e -> {
          cardService.delete(card.getId());
          cardGrid.getDataProvider().refreshAll();
          Notification.show("Card deleted");
        });
    dialog.setCancelButton("Cancel", e -> dialog.close());
    dialog.open();
  }
}
