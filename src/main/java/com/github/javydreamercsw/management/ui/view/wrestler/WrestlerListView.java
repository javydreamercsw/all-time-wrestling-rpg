package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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

@Route("wrestler-list")
@PageTitle("Wrestler List")
@Menu(order = 0, icon = "vaadin:user", title = "Wrestler List")
@PermitAll // When security is enabled, allow all authenticated users
public class WrestlerListView extends Main {

  private final WrestlerService wrestlerService;

  final TextField name;
  final Button createBtn;
  final Grid<Wrestler> wrestlerGrid;

  public WrestlerListView(WrestlerService wrestlerService, Clock clock) {
    this.wrestlerService = wrestlerService;

    name = new TextField();
    name.setPlaceholder("What do you want the wrestler name to be?");
    name.setAriaLabel("Wrestler Name");
    name.setMaxLength(Card.DESCRIPTION_MAX_LENGTH);
    name.setMinWidth("20em");

    createBtn = new Button("Create", event -> createWrestler());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    wrestlerGrid = new Grid<>();
    // Enable grid editor
    Editor<Wrestler> editor = wrestlerGrid.getEditor();
    editor.setBuffered(true);
    // Create and set the binder
    Binder<Wrestler> binder = new Binder<>(Wrestler.class);
    editor.setBinder(binder);

    // Editor fields
    TextField nameField = new TextField();
    TextField deckSizeField = new TextField();
    TextField startingHealthField = new TextField();
    TextField lowHealthField = new TextField();
    TextField startingStaminaField = new TextField();
    TextField lowStaminaField = new TextField();
    ComboBox<Gender> genderField = new ComboBox<>();
    genderField.setItems(Gender.values());

    wrestlerGrid.setItems(query -> wrestlerService.list(toSpringPageRequest(query)).stream());
    wrestlerGrid
        .addColumn(Wrestler::getName)
        .setHeader("Name")
        .setEditorComponent(nameField)
        .setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getGender)
        .setHeader("Gender")
        .setEditorComponent(genderField)
        .setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getDeckSize)
        .setHeader("Deck Size")
        .setEditorComponent(deckSizeField)
        .setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getStartingHealth)
        .setHeader("Starting Health")
        .setEditorComponent(startingHealthField)
        .setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getLowHealth)
        .setHeader("Low Health")
        .setEditorComponent(lowHealthField)
        .setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getStartingStamina)
        .setHeader("Starting Stamina")
        .setEditorComponent(startingStaminaField)
        .setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getLowStamina)
        .setHeader("Low Stamina")
        .setEditorComponent(lowStaminaField)
        .setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getCreationDate).setHeader("Creation Date");
    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              Button editButton = new Button("Edit");
              editButton.addClickListener(
                  e -> {
                    wrestlerGrid.getEditor().editItem(wrestler);
                  });
              return editButton;
            })
        .setHeader("Actions");
    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              Button deleteButton = new Button("Delete");
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

    // Bind editor fields
    binder.forField(nameField).bind("name");
    binder.forField(genderField).bind("gender");
    binder
        .forField(deckSizeField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("deckSize");
    binder
        .forField(startingHealthField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("startingHealth");
    binder
        .forField(lowHealthField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("lowHealth");
    binder
        .forField(startingStaminaField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("startingStamina");
    binder
        .forField(lowStaminaField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind("lowStamina");

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
    wrestlerGrid.getElement().appendChild(actions.getElement());

    add(new ViewToolbar("Wrestler List", ViewToolbar.group(name, createBtn)));
    add(wrestlerGrid, actions);
  }

  private void createWrestler() {
    wrestlerService.createWrestler(name.getValue());
    wrestlerGrid.getDataProvider().refreshAll();
    name.clear();
    Notification.show("Wrestler added", 3_000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
