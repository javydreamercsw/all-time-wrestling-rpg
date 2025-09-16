package com.github.javydreamercsw.management.ui.view.title;

import com.github.javydreamercsw.management.domain.title.Title;
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

@Route("title-list")
@PageTitle("Title List")
@Menu(order = 1, icon = "vaadin:trophy", title = "Title List")
@PermitAll
public class TitleListView extends Main {

  private final TitleService titleService;

  private final Grid<Title> grid = new Grid<>(Title.class);
  private final TextField name = new TextField("Name");
  private final TextArea description = new TextArea("Description");
  private final ComboBox<WrestlerTier> tier = new ComboBox<>("Tier");
  private final Button saveButton = new Button("Save");
  private final Button deleteButton = new Button("Delete");

  private final Binder<Title> binder = new Binder<>(Title.class);

  private final Button newButton = new Button("New");

  public TitleListView(TitleService titleService) {
    this.titleService = titleService;

    tier.setItems(WrestlerTier.values());

    grid.removeColumnByKey("id");
    grid.removeColumnByKey("externalId");
    grid.removeColumnByKey("description");
    grid.removeColumnByKey("gender");
    grid.removeColumnByKey("contender");
    grid.removeColumnByKey("champion");
    grid.removeColumnByKey("titleReigns");
    grid.removeColumnByKey("segments");

    grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));

    binder.forField(name).bind(Title::getName, Title::setName);
    binder.forField(description).bind(Title::getDescription, Title::setDescription);
    binder.forField(tier).bind(Title::getTier, Title::setTier);

    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.addClickListener(event -> saveTitle());

    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(event -> deleteTitle());
    
    newButton.addClickListener(event -> createTitle());

    FormLayout formLayout = new FormLayout(name, description, tier);
    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, deleteButton, newButton);
    VerticalLayout form = new VerticalLayout(formLayout, buttonLayout);

    add(grid, form);

    refreshGrid();
  }

  private void refreshGrid() {
    grid.setItems(titleService.findAll());
  }

  private void populateForm(Title title) {
    binder.setBean(title);
    if (title != null) {
      deleteButton.setEnabled(true);
    } else {
      deleteButton.setEnabled(false);
    }
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
