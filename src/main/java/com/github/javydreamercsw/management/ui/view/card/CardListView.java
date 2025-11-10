package com.github.javydreamercsw.management.ui.view.card;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
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
import jakarta.annotation.security.PermitAll;
import java.time.Clock;
import lombok.NonNull;

@Route("card-list")
@PageTitle("Card List")
@Menu(order = 2, icon = "vaadin:credit-card", title = "Card List")
@PermitAll // When security is enabled, allow all authenticated users
public class CardListView extends Main {

  private final CardService cardService;
  private final CardSetService cardSetService;

  final TextField name;
  final Button createBtn;
  final Grid<Card> cardGrid;

  public CardListView(
      @NonNull CardService cardService,
      @NonNull CardSetService cardSetService,
      @NonNull Clock clock) {
    this.cardService = cardService;
    this.cardSetService = cardSetService;

    name = new TextField();
    name.setPlaceholder("What do you want the card name to be?");
    name.setAriaLabel("Card Name");
    name.setMaxLength(Card.DESCRIPTION_MAX_LENGTH);
    name.setMinWidth("20em");

    createBtn = new Button("Create", event -> createTask());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    cardGrid = new Grid<>();
    // Enable grid editor
    Editor<Card> editor = cardGrid.getEditor();
    editor.setBuffered(true);

    editor.addSaveListener(
        event -> {
          cardService.save(event.getItem());
          cardGrid.getDataProvider().refreshAll();
          Notification.show("Card updated", 3_000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    // Create and set the binder
    Binder<Card> binder = new Binder<>(Card.class);
    editor.setBinder(binder);

    // Editor fields
    TextField nameField = new TextField();
    nameField.getElement().setAttribute("data-testid", "name-editor");
    TextField damageField = new TextField();
    TextField targetField = new TextField();
    TextField momentumField = new TextField();
    TextField staminaField = new TextField();
    Checkbox signatureField = new Checkbox();
    Checkbox finisherField = new Checkbox();
    Checkbox tauntField = new Checkbox();
    Checkbox recoverField = new Checkbox();
    Checkbox pinField = new Checkbox();
    ComboBox<String> typeField = new ComboBox<>();
    typeField.setItems("Strike", "Grapple", "Aerial", "Throw");
    typeField.setPlaceholder("Select type");

    ComboBox<CardSet> setField = new ComboBox<>();
    setField.setPlaceholder("Select set");
    setField.setItems(cardSetService.findAll());
    setField.setItemLabelGenerator(CardSet::getName);

    cardGrid.setItems(query -> cardService.list(toSpringPageRequest(query)).stream());
    cardGrid
        .addColumn(Card::getName)
        .setHeader("Name")
        .setEditorComponent(nameField)
        .setSortable(true)
        .setSortProperty("name");
    cardGrid
        .addColumn(card -> card.getSet() != null ? card.getSet().getName() : "")
        .setHeader("Set")
        .setEditorComponent(setField)
        .setSortable(true)
        .setSortProperty("set.name");
    cardGrid
        .addColumn(Card::getType)
        .setHeader("Type")
        .setEditorComponent(typeField)
        .setSortable(true)
        .setSortProperty("type");
    cardGrid
        .addColumn(Card::getDamage)
        .setHeader("Damage")
        .setEditorComponent(damageField)
        .setSortable(true)
        .setSortProperty("damage");
    cardGrid
        .addColumn(Card::getTarget)
        .setHeader("Target")
        .setEditorComponent(targetField)
        .setSortable(true)
        .setSortProperty("target");
    cardGrid
        .addColumn(Card::getMomentum)
        .setHeader("Momentum")
        .setEditorComponent(momentumField)
        .setSortable(true)
        .setSortProperty("momentum");
    cardGrid
        .addColumn(Card::getStamina)
        .setHeader("Stamina")
        .setEditorComponent(staminaField)
        .setSortable(true)
        .setSortProperty("stamina");
    cardGrid
        .addColumn(Card::getSignature)
        .setHeader("Is Signature?")
        .setEditorComponent(signatureField)
        .setSortable(true)
        .setSortProperty("signature");
    cardGrid
        .addColumn(Card::getFinisher)
        .setHeader("Is Finisher?")
        .setEditorComponent(finisherField)
        .setSortable(true)
        .setSortProperty("finisher");
    cardGrid
        .addColumn(Card::getTaunt)
        .setHeader("Is Taunt?")
        .setEditorComponent(tauntField)
        .setSortable(true)
        .setSortProperty("taunt");
    cardGrid
        .addColumn(Card::getRecover)
        .setHeader("Is Recover?")
        .setEditorComponent(recoverField)
        .setSortable(true)
        .setSortProperty("recover");
    cardGrid
        .addColumn(Card::getPin)
        .setHeader("Is Pin?")
        .setEditorComponent(pinField)
        .setSortable(true)
        .setSortProperty("pin");
    cardGrid.addColumn(Card::getCreationDate).setHeader("Creation Date");
    cardGrid
        .addComponentColumn(
            card -> {
              Button editButton = new Button("Edit");
              editButton.addClickListener(
                  e -> {
                    cardGrid.getEditor().editItem(card);
                  });
              Button deleteButton = new Button("Delete");
              deleteButton.getElement().setAttribute("data-testid", "delete-button");
              deleteButton.addClickListener(e -> deleteCard(card));
              return new HorizontalLayout(editButton, deleteButton);
            })
        .setHeader("Actions");
    cardGrid.setSizeFull();

    // Bind editor fields
    binder.forField(nameField).bind("name");
    binder.forField(setField).bind("set");
    binder.forField(typeField).bind("type");
    binder
        .forField(damageField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("damage");
    binder
        .forField(targetField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("target");
    binder
        .forField(momentumField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("momentum");
    binder
        .forField(staminaField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("stamina");
    binder.forField(signatureField).bind("signature");
    binder.forField(finisherField).bind("finisher");
    binder.forField(tauntField).bind("taunt");
    binder.forField(recoverField).bind("recover");
    binder.forField(pinField).bind("pin");

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
    HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
    cardGrid.getElement().appendChild(actions.getElement());

    add(new ViewToolbar("Card List", ViewToolbar.group(name, createBtn)));
    add(cardGrid, actions);
  }

  private void createTask() {
    cardService.createCard(name.getValue());
    cardGrid.getDataProvider().refreshAll();
    name.clear();
    Notification.show("Card added", 3_000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void deleteCard(@NonNull Card card) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete Card?");
    dialog.setText("Are you sure you want to delete this card?");

    dialog.setConfirmButton(
        "Delete",
        e -> {
          cardService.delete(card.getId());
          cardGrid.getDataProvider().refreshAll();
          Notification.show("Card deleted", 3_000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    dialog.setCancelButton("Cancel", e -> dialog.close());

    dialog.open();
  }
}
