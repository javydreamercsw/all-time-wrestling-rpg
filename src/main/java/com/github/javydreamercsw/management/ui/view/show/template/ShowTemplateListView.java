package com.github.javydreamercsw.management.ui.view.show.template;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * View for managing Show Templates. Provides a list of show templates with create, edit, and delete
 * functionality.
 */
@Route("show-template-list")
@PageTitle("Show Template List")
@Menu(order = 4, icon = "vaadin:clipboard-text", title = "Show Templates")
@PermitAll
@Slf4j
public class ShowTemplateListView extends Main {

  private final ShowTemplateService showTemplateService;
  private final ShowTypeService showTypeService;

  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private ComboBox<ShowType> editShowType;
  private TextField editNotionUrl;
  private ShowTemplate editingTemplate;
  private Binder<ShowTemplate> binder;

  final TextField nameFilter;
  final ComboBox<ShowType> showTypeFilter;
  final Button createBtn;
  final Grid<ShowTemplate> templateGrid;

  public ShowTemplateListView(
      ShowTemplateService showTemplateService, ShowTypeService showTypeService) {
    this.showTemplateService = showTemplateService;
    this.showTypeService = showTypeService;

    // Initialize filters
    nameFilter = new TextField();
    nameFilter.setPlaceholder("Filter by name...");
    nameFilter.setAriaLabel("Name Filter");
    nameFilter.setClearButtonVisible(true);
    nameFilter.addValueChangeListener(e -> refreshGrid());

    showTypeFilter = new ComboBox<>("Show Type");
    showTypeFilter.setItems(showTypeService.findAll());
    showTypeFilter.setItemLabelGenerator(ShowType::getName);
    showTypeFilter.setPlaceholder("All types");
    showTypeFilter.setClearButtonVisible(true);
    showTypeFilter.addValueChangeListener(e -> refreshGrid());

    createBtn = new Button("Create Template", new Icon(VaadinIcon.PLUS));
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createBtn.addClickListener(e -> openCreateDialog());

    // Initialize grid
    templateGrid = new Grid<>(ShowTemplate.class, false);
    templateGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

    setupGrid();
    setupEditDialog();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    // Create filter layout
    HorizontalLayout filterLayout = new HorizontalLayout(nameFilter, showTypeFilter);
    filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
    filterLayout.setWidthFull();

    // Toolbar with filters and create button
    add(new ViewToolbar("Show Templates", ViewToolbar.group(filterLayout, createBtn)));
    add(templateGrid);

    refreshGrid();
  }

  private void setupGrid() {
    // Name column
    templateGrid
        .addColumn(ShowTemplate::getName)
        .setHeader("Name")
        .setSortable(true)
        .setFlexGrow(2);

    // Description column (truncated)
    templateGrid
        .addColumn(
            template -> {
              String description = template.getDescription();
              if (description != null && description.length() > 50) {
                return description.substring(0, 50) + "...";
              }
              return description != null ? description : "";
            })
        .setHeader("Description")
        .setFlexGrow(3);

    // Show Type column
    templateGrid
        .addColumn(
            template -> template.getShowType() != null ? template.getShowType().getName() : "")
        .setHeader("Show Type")
        .setSortable(true)
        .setFlexGrow(1);

    // Notion URL column (show if exists)
    templateGrid
        .addColumn(template -> template.getNotionUrl() != null ? "Yes" : "No")
        .setHeader("Has Notion URL")
        .setFlexGrow(1);

    // Creation date column
    templateGrid
        .addColumn(
            template ->
                template.getCreationDate() != null
                    ? template.getCreationDate().toString().substring(0, 10)
                    : "")
        .setHeader("Created")
        .setSortable(true)
        .setFlexGrow(1);

    // Actions column
    templateGrid
        .addComponentColumn(
            template -> {
              HorizontalLayout actions = new HorizontalLayout();
              actions.setSpacing(true);

              Button editBtn = new Button("Edit", new Icon(VaadinIcon.EDIT));
              editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              editBtn.addClickListener(e -> openEditDialog(template));

              Button deleteBtn = new Button("Delete", new Icon(VaadinIcon.TRASH));
              deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteBtn.addClickListener(e -> deleteTemplate(template));

              actions.add(editBtn, deleteBtn);
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(0);

    templateGrid.setSizeFull();
  }

  private void setupEditDialog() {
    editDialog = new Dialog();
    editDialog.setWidth("600px");
    editDialog.setMaxWidth("90vw");
    editDialog.setMaxHeight("80vh");

    editName = new TextField("Name");
    editName.setWidthFull();
    editName.setRequired(true);

    editDescription = new TextArea("Description");
    editDescription.setWidthFull();
    editDescription.setHeight("100px");

    editShowType = new ComboBox<>("Show Type");
    editShowType.setItems(showTypeService.findAll());
    editShowType.setItemLabelGenerator(ShowType::getName);
    editShowType.setWidthFull();
    editShowType.setRequired(true);

    editNotionUrl = new TextField("Notion URL");
    editNotionUrl.setWidthFull();
    editNotionUrl.setPlaceholder("https://notion.so/...");

    Button saveBtn = new Button("Save", e -> saveTemplate());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancelBtn = new Button("Cancel", e -> editDialog.close());

    FormLayout formLayout = new FormLayout();
    formLayout.add(editName, editDescription, editShowType, editNotionUrl);
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
    formLayout.setColspan(editDescription, 2);

    HorizontalLayout buttonLayout = new HorizontalLayout(saveBtn, cancelBtn);
    buttonLayout.setWidthFull();
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
    dialogLayout.setWidthFull();
    dialogLayout.setSpacing(true);

    editDialog.add(dialogLayout);

    // Setup binder
    binder = new Binder<>(ShowTemplate.class);
    binder
        .forField(editName)
        .asRequired("Name is required")
        .bind(ShowTemplate::getName, ShowTemplate::setName);
    binder
        .forField(editDescription)
        .bind(ShowTemplate::getDescription, ShowTemplate::setDescription);
    binder
        .forField(editShowType)
        .asRequired("Show type is required")
        .bind(ShowTemplate::getShowType, ShowTemplate::setShowType);
    binder.forField(editNotionUrl).bind(ShowTemplate::getNotionUrl, ShowTemplate::setNotionUrl);
  }

  private void openCreateDialog() {
    editingTemplate = new ShowTemplate();
    editDialog.setHeaderTitle("Create Show Template");
    binder.readBean(editingTemplate);
    editDialog.open();
  }

  private void openEditDialog(ShowTemplate template) {
    editingTemplate = template;
    editDialog.setHeaderTitle("Edit Show Template");
    binder.readBean(template);
    editDialog.open();
  }

  private void saveTemplate() {
    try {
      binder.writeBean(editingTemplate);

      if (editingTemplate.getId() == null) {
        // Create new template
        ShowTemplate savedTemplate =
            showTemplateService.createOrUpdateTemplate(
                editingTemplate.getName(),
                editingTemplate.getDescription(),
                editingTemplate.getShowType().getName(),
                editingTemplate.getNotionUrl());

        if (savedTemplate != null) {
          Notification.show("Template created successfully", 3000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
          Notification.show("Failed to create template", 3000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_ERROR);
          return;
        }
      } else {
        // Update existing template
        showTemplateService.updateTemplate(
            editingTemplate.getId(),
            editingTemplate.getName(),
            editingTemplate.getDescription(),
            editingTemplate.getShowType().getName(),
            editingTemplate.getNotionUrl());

        Notification.show("Template updated successfully", 3000, Notification.Position.BOTTOM_END)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      }

      editDialog.close();
      refreshGrid();

    } catch (ValidationException e) {
      Notification.show("Please fix validation errors", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void deleteTemplate(ShowTemplate template) {
    Dialog confirmDialog = new Dialog();
    confirmDialog.setHeaderTitle("Confirm Delete");

    Div content = new Div();
    content.setText("Are you sure you want to delete the template '" + template.getName() + "'?");

    Button confirmBtn =
        new Button(
            "Delete",
            e -> {
              boolean deleted = showTemplateService.deleteTemplate(template.getId());
              if (deleted) {
                Notification.show(
                        "Template deleted successfully", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
              } else {
                Notification.show(
                        "Failed to delete template", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
              confirmDialog.close();
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

    Button cancelBtn = new Button("Cancel", e -> confirmDialog.close());

    HorizontalLayout buttonLayout = new HorizontalLayout(confirmBtn, cancelBtn);
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    VerticalLayout dialogLayout = new VerticalLayout(content, buttonLayout);
    confirmDialog.add(dialogLayout);
    confirmDialog.open();
  }

  private void refreshGrid() {
    List<ShowTemplate> templates = showTemplateService.findAll();

    // Apply filters
    String nameFilterValue = nameFilter.getValue();
    ShowType showTypeFilterValue = showTypeFilter.getValue();

    if (nameFilterValue != null && !nameFilterValue.trim().isEmpty()) {
      templates =
          templates.stream()
              .filter(
                  template ->
                      template.getName().toLowerCase().contains(nameFilterValue.toLowerCase()))
              .toList();
    }

    if (showTypeFilterValue != null) {
      templates =
          templates.stream()
              .filter(
                  template ->
                      template.getShowType() != null
                          && template.getShowType().getId().equals(showTypeFilterValue.getId()))
              .toList();
    }

    templateGrid.setItems(templates);
  }
}
