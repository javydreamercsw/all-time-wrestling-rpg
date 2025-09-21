package com.github.javydreamercsw.management.ui.view.title;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.stream.Collectors;

@Route("title-list")
@PageTitle("Title List")
@Menu(order = 1, icon = "vaadin:trophy", title = "Title List")
@PermitAll
public class TitleListView extends Main {

  private final TitleService titleService;

  private final Grid<Title> grid = new Grid<>(Title.class);
  private final Button deleteButton = new Button("Delete");

  private final Binder<Title> binder = new Binder<>(Title.class);

  public TitleListView(TitleService titleService) {
    this.titleService = titleService;

    ComboBox<WrestlerTier> tier = new ComboBox<>("Tier");
    tier.setItems(WrestlerTier.values());
    grid.removeAllColumns();

    // Add columns with minimum widths and no flex grow
    grid.addColumn(Title::getName).setHeader("Name").setWidth("300px").setFlexGrow(0);
    grid.addColumn(Title::getDescription).setHeader("Description").setWidth("300px").setFlexGrow(0);
    grid.addColumn(
            title ->
                title.getChampion().stream()
                    .map(Wrestler::getName)
                    .collect(Collectors.joining(" & ")))
        .setHeader("Champion")
        .setWidth("300px")
        .setFlexGrow(0);
    grid.addColumn(
            title ->
                title.getContender().stream()
                    .map(Wrestler::getName)
                    .collect(Collectors.joining(" & ")))
        .setHeader("Contender")
        .setWidth("300px")
        .setFlexGrow(0);
    grid.addColumn(Title::getTier).setHeader("Tier").setWidth("150px").setFlexGrow(0);
    grid.setWidthFull();

    // Optionally wrap grid in a scrollable container for horizontal scrolling
    com.vaadin.flow.component.html.Div gridContainer = new com.vaadin.flow.component.html.Div(grid);
    gridContainer.getStyle().set("overflow-x", "auto");
    gridContainer.setWidth("100%");

    grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));

    TextField name = new TextField("Name");
    binder.forField(name).bind(Title::getName, Title::setName);
    TextArea description = new TextArea("Description");
    binder.forField(description).bind(Title::getDescription, Title::setDescription);
    binder.forField(tier).bind(Title::getTier, Title::setTier);

    Button saveButton = new Button("Save");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.addClickListener(event -> saveTitle());

    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(event -> deleteTitle());

    Button newButton = new Button("New");
    newButton.addClickListener(event -> createTitle());

    FormLayout formLayout = new FormLayout(name, description, tier);
    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, deleteButton, newButton);
    VerticalLayout form = new VerticalLayout(formLayout, buttonLayout);

    add(gridContainer, form);

    refreshGrid();
  }

  private void refreshGrid() {
    grid.setItems(titleService.findAll());
  }

  private void populateForm(Title title) {
    binder.setBean(title);
    deleteButton.setEnabled(title != null);
  }

  private void saveTitle() {
    Title title = binder.getBean();
    if (title != null) {
      titleService.save(title);
      refreshGrid();
      populateForm(null);
      Notification.show("Title saved successfully.", 3000, Notification.Position.BOTTOM_END);
    }
  }

  private void createTitle() {
    populateForm(new Title());
  }

  private void deleteTitle() {
    Title title = binder.getBean();
    if (title != null) {
      titleService.deleteTitle(title.getId());
      refreshGrid();
      populateForm(null);
      Notification.show("Title deleted successfully.", 3000, Notification.Position.BOTTOM_END);
    }
  }
}
