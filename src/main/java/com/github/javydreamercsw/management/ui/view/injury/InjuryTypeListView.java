package com.github.javydreamercsw.management.ui.view.injury;

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

/**
 * View for managing injury types in the ATW RPG system. Provides CRUD operations for injury type
 * reference data.
 */
@Route(value = "injury-types", layout = MainLayout.class)
@PageTitle("Injury Types | ATW RPG")
@Menu(order = 6, icon = "vaadin:plus-circle", title = "Injury Types")
@PermitAll
@Slf4j
public class InjuryTypeListView extends Main {

  private final InjuryTypeService injuryTypeService;
  private final Grid<InjuryType> grid;
  private final TextField searchField;
  private final Button createButton;

  private Dialog editDialog;
  private TextField editName;
  private IntegerField editHealthEffect;
  private IntegerField editStaminaEffect;
  private IntegerField editCardEffect;
  private TextArea editSpecialEffects;
  private InjuryType editingInjuryType;
  private Binder<InjuryType> binder;

  public InjuryTypeListView(InjuryTypeService injuryTypeService) {
    this.injuryTypeService = injuryTypeService;
    this.grid = new Grid<>(InjuryType.class, false);
    this.searchField = new TextField();
    this.createButton = new Button("Create Injury Type", VaadinIcon.PLUS.create());

    setSizeFull();
    configureGrid();
    configureToolbar();
    configureEditDialog();
    updateGrid();

    add(createToolbar(), grid);
  }

  private void configureGrid() {
    grid.setSizeFull();

    // Configure columns
    grid.addColumn(InjuryType::getInjuryName)
        .setHeader("Injury Name")
        .setSortable(true)
        .setFlexGrow(2);

    grid.addColumn(injuryType -> formatEffect(injuryType.getHealthEffect()))
        .setHeader("Health Effect")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addColumn(injuryType -> formatEffect(injuryType.getStaminaEffect()))
        .setHeader("Stamina Effect")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addColumn(injuryType -> formatEffect(injuryType.getCardEffect()))
        .setHeader("Card Effect")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addColumn(injuryType -> injuryType.getTotalPenalty())
        .setHeader("Total Penalty")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addComponentColumn(
            injuryType -> {
              Span specialEffects = new Span();
              if (injuryType.getSpecialEffects() != null
                  && !injuryType.getSpecialEffects().trim().isEmpty()) {
                String effects = injuryType.getSpecialEffects();
                if (effects.length() > 50) {
                  effects = effects.substring(0, 47) + "...";
                }
                specialEffects.setText(effects);
                specialEffects.setTitle(injuryType.getSpecialEffects()); // Full text on hover
              } else {
                specialEffects.setText("None");
                specialEffects.addClassNames(LumoUtility.TextColor.SECONDARY);
              }
              return specialEffects;
            })
        .setHeader("Special Effects")
        .setFlexGrow(2);

    // Actions column
    grid.addComponentColumn(
            injuryType -> {
              HorizontalLayout actions = new HorizontalLayout();
              actions.setSpacing(true);

              Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
              editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
              editButton.addClickListener(e -> editInjuryType(injuryType));

              Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
              deleteButton.addThemeVariants(
                  ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
              deleteButton.addClickListener(e -> confirmDelete(injuryType));

              actions.add(editButton, deleteButton);
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(1);

    // Configure data provider
    grid.setDataProvider(
        DataProvider.fromCallbacks(
            query -> {
              int offset = query.getOffset();
              int limit = query.getLimit();

              // Apply sorting
              String sortProperty = "injuryName"; // default
              boolean ascending = true;

              if (!query.getSortOrders().isEmpty()) {
                var sortOrder = query.getSortOrders().get(0);
                sortProperty = sortOrder.getSorted();
                ascending = sortOrder.getDirection() == SortDirection.ASCENDING;
              }

              return injuryTypeService
                  .getAllInjuryTypes(
                      org.springframework.data.domain.PageRequest.of(
                          offset / limit,
                          limit,
                          ascending
                              ? org.springframework.data.domain.Sort.by(sortProperty).ascending()
                              : org.springframework.data.domain.Sort.by(sortProperty).descending()))
                  .stream();
            },
            query -> (int) injuryTypeService.countAll()));
  }

  private void configureToolbar() {
    searchField.setPlaceholder("Search injury types...");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.addValueChangeListener(e -> updateGrid());

    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> createInjuryType());
  }

  private HorizontalLayout createToolbar() {
    HorizontalLayout toolbar = new HorizontalLayout();
    toolbar.addClassNames(LumoUtility.Gap.MEDIUM, LumoUtility.Padding.MEDIUM);
    toolbar.setWidthFull();
    toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);

    // Left side - title and search
    VerticalLayout leftSide = new VerticalLayout();
    leftSide.setSpacing(false);
    leftSide.setPadding(false);

    H3 title = new H3("Injury Types");
    title.addClassNames(LumoUtility.Margin.NONE);

    leftSide.add(title, searchField);

    // Right side - actions
    HorizontalLayout rightSide = new HorizontalLayout();
    rightSide.add(createButton);

    toolbar.add(leftSide, rightSide);
    return toolbar;
  }

  private void configureEditDialog() {
    editDialog = new Dialog();
    editDialog.setWidth("600px");
    editDialog.setCloseOnEsc(true);
    editDialog.setCloseOnOutsideClick(false);

    // Form fields
    editName = new TextField("Injury Name");
    editName.setRequired(true);
    editName.setMaxLength(100);
    editName.setWidthFull();

    editHealthEffect = new IntegerField("Health Effect");
    editHealthEffect.setHelperText("Typically negative (e.g., -3, -1, -2)");
    editHealthEffect.setWidthFull();

    editStaminaEffect = new IntegerField("Stamina Effect");
    editStaminaEffect.setHelperText("Typically negative or zero (e.g., 0, -3, -2)");
    editStaminaEffect.setWidthFull();

    editCardEffect = new IntegerField("Card Effect");
    editCardEffect.setHelperText("Typically negative or zero (e.g., -2, 0, -1)");
    editCardEffect.setWidthFull();

    editSpecialEffects = new TextArea("Special Effects");
    editSpecialEffects.setHelperText("Describe any special game rule modifications");
    editSpecialEffects.setMaxLength(2000);
    editSpecialEffects.setWidthFull();
    editSpecialEffects.setHeight("100px");

    // Buttons
    Button saveButton = new Button("Save", e -> saveInjuryType());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> editDialog.close());

    HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
    buttons.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

    // Layout
    VerticalLayout layout = new VerticalLayout();
    layout.add(
        editName, editHealthEffect, editStaminaEffect, editCardEffect, editSpecialEffects, buttons);
    layout.setPadding(true);
    layout.setSpacing(true);

    editDialog.add(layout);

    // Configure binder
    binder = new Binder<>(InjuryType.class);
    binder
        .forField(editName)
        .asRequired("Injury name is required")
        .bind(InjuryType::getInjuryName, InjuryType::setInjuryName);
    binder
        .forField(editHealthEffect)
        .bind(InjuryType::getHealthEffect, InjuryType::setHealthEffect);
    binder
        .forField(editStaminaEffect)
        .bind(InjuryType::getStaminaEffect, InjuryType::setStaminaEffect);
    binder.forField(editCardEffect).bind(InjuryType::getCardEffect, InjuryType::setCardEffect);
    binder
        .forField(editSpecialEffects)
        .bind(InjuryType::getSpecialEffects, InjuryType::setSpecialEffects);
  }

  private String formatEffect(Integer effect) {
    if (effect == null) return "0";
    return effect >= 0 ? "+" + effect : effect.toString();
  }

  private void updateGrid() {
    grid.getDataProvider().refreshAll();
  }

  private void createInjuryType() {
    editingInjuryType = null;
    editDialog.setHeaderTitle("Create New Injury Type");
    binder.setBean(new InjuryType());
    editDialog.open();
  }

  private void editInjuryType(InjuryType injuryType) {
    editingInjuryType = injuryType;
    editDialog.setHeaderTitle("Edit Injury Type: " + injuryType.getInjuryName());
    binder.setBean(injuryType);
    editDialog.open();
  }

  private void saveInjuryType() {
    try {
      InjuryType injuryType = binder.getBean();
      binder.writeBean(injuryType);

      if (editingInjuryType == null) {
        // Create new
        injuryTypeService.createInjuryType(
            injuryType.getInjuryName(),
            injuryType.getHealthEffect(),
            injuryType.getStaminaEffect(),
            injuryType.getCardEffect(),
            injuryType.getSpecialEffects());
        showSuccessNotification("Injury type created successfully");
      } else {
        // Update existing
        injuryTypeService.updateInjuryType(
            editingInjuryType.getId(),
            injuryType.getInjuryName(),
            injuryType.getHealthEffect(),
            injuryType.getStaminaEffect(),
            injuryType.getCardEffect(),
            injuryType.getSpecialEffects());
        showSuccessNotification("Injury type updated successfully");
      }

      editDialog.close();
      updateGrid();
    } catch (ValidationException e) {
      showErrorNotification("Please fix the validation errors");
    } catch (IllegalArgumentException e) {
      showErrorNotification(e.getMessage());
    } catch (Exception e) {
      log.error("Error saving injury type", e);
      showErrorNotification("Failed to save injury type: " + e.getMessage());
    }
  }

  private void confirmDelete(InjuryType injuryType) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete Injury Type");
    dialog.setText(
        "Are you sure you want to delete '"
            + injuryType.getInjuryName()
            + "'? This action cannot be undone.");
    dialog.setCancelable(true);
    dialog.setConfirmText("Delete");
    dialog.setConfirmButtonTheme("error primary");
    dialog.addConfirmListener(e -> deleteInjuryType(injuryType));
    dialog.open();
  }

  private void deleteInjuryType(InjuryType injuryType) {
    try {
      boolean deleted = injuryTypeService.deleteInjuryType(injuryType.getId());
      if (deleted) {
        showSuccessNotification("Injury type deleted successfully");
        updateGrid();
      } else {
        showErrorNotification("Failed to delete injury type");
      }
    } catch (IllegalStateException e) {
      showErrorNotification(e.getMessage());
    } catch (Exception e) {
      log.error("Error deleting injury type", e);
      showErrorNotification("Failed to delete injury type: " + e.getMessage());
    }
  }

  private void showSuccessNotification(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showErrorNotification(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}
