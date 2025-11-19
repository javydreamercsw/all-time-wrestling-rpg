package com.github.javydreamercsw.management.ui.view.segment.type;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@Route("segment-type-list")
@PageTitle("Segment Types")
@Menu(order = 6, icon = "vaadin:puzzle-piece", title = "Segment Types")
@PermitAll
public class SegmentTypeListView extends Main {

  private final SegmentTypeService segmentTypeService;

  private Grid<SegmentType> segmentTypeGrid;
  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private SegmentType editingSegmentType;
  private Binder<SegmentType> binder;

  public SegmentTypeListView(SegmentTypeService segmentTypeService) {
    this.segmentTypeService = segmentTypeService;
    initializeUI();
  }

  private void initializeUI() {
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    // Toolbar with Create button
    Button createButton = new Button("Create Segment Type", new Icon(VaadinIcon.PLUS));
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> openCreateDialog());

    HorizontalLayout toolbar = new HorizontalLayout(createButton);
    toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    toolbar.setWidthFull();

    add(new ViewToolbar("Segment Types", ViewToolbar.group(toolbar)));

    // Grid for displaying Segment Types
    segmentTypeGrid = new Grid<>(SegmentType.class, false);
    segmentTypeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    segmentTypeGrid.setSizeFull();

    segmentTypeGrid.addColumn(SegmentType::getName).setHeader("Name").setSortable(true);
    segmentTypeGrid
        .addColumn(SegmentType::getDescription)
        .setHeader("Description")
        .setSortable(true);

    // Edit and Delete buttons
    segmentTypeGrid.addComponentColumn(this::createActionsColumn).setHeader("Actions");

    add(segmentTypeGrid);

    refreshGrid();
  }

  private HorizontalLayout createActionsColumn(SegmentType segmentType) {
    Button editButton = new Button(new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editButton.setTooltipText("Edit Segment Type");
    editButton.addClickListener(e -> openEditDialog(segmentType));

    Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    deleteButton.setTooltipText("Delete Segment Type");
    deleteButton.addClickListener(e -> confirmDelete(segmentType));

    HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
    actions.setSpacing(true);
    return actions;
  }

  private void refreshGrid() {
    segmentTypeGrid.setItems(segmentTypeService.findAll());
  }

  private void openCreateDialog() {
    editingSegmentType = new SegmentType();
    setupEditDialog("Create Segment Type");
    binder.readBean(editingSegmentType);
    editDialog.open();
  }

  private void openEditDialog(SegmentType segmentType) {
    editingSegmentType = segmentType;
    setupEditDialog("Edit Segment Type");
    binder.readBean(editingSegmentType);
    editDialog.open();
  }

  private void setupEditDialog(String headerTitle) {
    editDialog = new Dialog();
    editDialog.setHeaderTitle(headerTitle);
    editDialog.setWidth("500px");

    editName = new TextField("Name");
    editName.setWidthFull();
    editDescription = new TextArea("Description");
    editDescription.setWidthFull();

    binder = new Binder<>(SegmentType.class);
    binder
        .forField(editName)
        .asRequired("Name cannot be empty")
        .bind(SegmentType::getName, SegmentType::setName);
    binder.forField(editDescription).bind(SegmentType::getDescription, SegmentType::setDescription);

    Button saveButton = new Button("Save", e -> saveSegmentType());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancelButton = new Button("Cancel", e -> editDialog.close());

    HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();

    VerticalLayout dialogLayout = new VerticalLayout(editName, editDescription, buttons);
    dialogLayout.setPadding(false);
    dialogLayout.setSpacing(true);

    editDialog.add(dialogLayout);
  }

  private void saveSegmentType() {
    if (binder.writeBeanIfValid(editingSegmentType)) {
      try {
        segmentTypeService.createOrUpdateSegmentType(
            editingSegmentType.getName(), editingSegmentType.getDescription());
        Notification.show(
                "Segment type saved successfully!", 3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        editDialog.close();
        refreshGrid();
      } catch (Exception e) {
        Notification.show(
                "Error saving segment type: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    } else {
      Notification.show(
              "Validation errors. Please check the form.", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void confirmDelete(SegmentType segmentType) {
    Dialog confirmDialog = new Dialog();
    confirmDialog.setHeaderTitle("Confirm Delete");
    confirmDialog.add(
        "Are you sure you want to delete segment type '" + segmentType.getName() + "'?");

    Button deleteButton = new Button("Delete", e -> deleteSegmentType(segmentType, confirmDialog));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
    Button cancelButton = new Button("Cancel", e -> confirmDialog.close());

    HorizontalLayout buttons = new HorizontalLayout(deleteButton, cancelButton);
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();

    confirmDialog.add(buttons);
    confirmDialog.open();
  }

  private void deleteSegmentType(SegmentType segmentType, Dialog confirmDialog) {
    try {
      segmentTypeService.deleteSegmentType(segmentType.getId());
      Notification.show(
              "Segment type deleted successfully!", 3000, Notification.Position.BOTTOM_START)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      confirmDialog.close();
      refreshGrid();
    } catch (Exception e) {
      Notification.show(
              "Error deleting segment type: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }
}
