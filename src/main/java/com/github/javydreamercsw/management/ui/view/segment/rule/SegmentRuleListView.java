package com.github.javydreamercsw.management.ui.view.segment.rule;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;

@Route("segment-rule-list")
@PageTitle("Segment Rules")
@Menu(order = 7, icon = "vaadin:list-ol", title = "Segment Rules")
@PermitAll
public class SegmentRuleListView extends Main {
  @Autowired private SegmentRuleService segmentRuleService;

  private Grid<SegmentRule> segmentRuleGrid;
  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private Checkbox editRequiresHighHeat;
  private SegmentRule editingSegmentRule;
  private Binder<SegmentRule> binder;

  public SegmentRuleListView() {
    // Vaadin requires a no-arg constructor.
  }

  @PostConstruct
  private void initializeUI() {
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    // Toolbar with Create button
    Button createButton = new Button("Create Segment Rule", new Icon(VaadinIcon.PLUS));
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> openCreateDialog());

    HorizontalLayout toolbar = new HorizontalLayout(createButton);
    toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    toolbar.setWidthFull();

    add(new ViewToolbar("Segment Rules", ViewToolbar.group(toolbar)));

    // Grid for displaying Segment Rules
    segmentRuleGrid = new Grid<>(SegmentRule.class, false);
    segmentRuleGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    segmentRuleGrid.setSizeFull();

    segmentRuleGrid.addColumn(SegmentRule::getName).setHeader("Name").setSortable(true);
    segmentRuleGrid
        .addColumn(SegmentRule::getDescription)
        .setHeader("Description")
        .setSortable(true);
    segmentRuleGrid
        .addComponentColumn(
            segmentRule -> {
              Checkbox checkbox = new Checkbox();
              checkbox.setValue(segmentRule.getRequiresHighHeat());
              checkbox.setReadOnly(true); // Make it read-only
              return checkbox;
            })
        .setHeader("Requires High Heat")
        .setSortable(true);

    // Edit and Delete buttons
    segmentRuleGrid.addComponentColumn(this::createActionsColumn).setHeader("Actions");

    add(segmentRuleGrid);

    refreshGrid();
  }

  private HorizontalLayout createActionsColumn(@NonNull SegmentRule segmentRule) {
    Button editButton = new Button(new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editButton.setTooltipText("Edit Segment Rule");
    editButton.addClickListener(e -> openEditDialog(segmentRule));

    Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    deleteButton.setTooltipText("Delete Segment Rule");
    deleteButton.addClickListener(e -> confirmDelete(segmentRule));

    HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
    actions.setSpacing(true);
    return actions;
  }

  private void refreshGrid() {
    segmentRuleGrid.setItems(segmentRuleService.getAllRules());
  }

  private void openCreateDialog() {
    editingSegmentRule = new SegmentRule();
    setupEditDialog("Create Segment Rule");
    binder.readBean(editingSegmentRule);
    editDialog.open();
  }

  private void openEditDialog(@NonNull SegmentRule segmentRule) {
    editingSegmentRule = segmentRule;
    setupEditDialog("Edit Segment Rule");
    binder.readBean(editingSegmentRule);
    editDialog.open();
  }

  private void setupEditDialog(@NonNull String headerTitle) {
    editDialog = new Dialog();
    editDialog.setHeaderTitle(headerTitle);
    editDialog.setWidth("500px");

    editName = new TextField("Name");
    editName.setWidthFull();
    editDescription = new TextArea("Description");
    editDescription.setWidthFull();
    editRequiresHighHeat = new Checkbox("Requires High Heat");

    binder = new Binder<>(SegmentRule.class);
    binder
        .forField(editName)
        .asRequired("Name cannot be empty")
        .bind(SegmentRule::getName, SegmentRule::setName);
    binder.forField(editDescription).bind(SegmentRule::getDescription, SegmentRule::setDescription);
    binder
        .forField(editRequiresHighHeat)
        .bind(SegmentRule::getRequiresHighHeat, SegmentRule::setRequiresHighHeat);

    Button saveButton = new Button("Save", e -> saveSegmentRule());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancelButton = new Button("Cancel", e -> editDialog.close());

    HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();

    VerticalLayout dialogLayout =
        new VerticalLayout(editName, editDescription, editRequiresHighHeat, buttons);
    dialogLayout.setPadding(false);
    dialogLayout.setSpacing(true);

    editDialog.add(dialogLayout);
  }

  private void saveSegmentRule() {
    if (binder.writeBeanIfValid(editingSegmentRule)) {
      try {
        segmentRuleService.createOrUpdateRule(
            editingSegmentRule.getName(),
            editingSegmentRule.getDescription(),
            editingSegmentRule.getRequiresHighHeat());
        Notification.show(
                "Segment rule saved successfully!", 3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        editDialog.close();
        refreshGrid();
      } catch (Exception e) {
        Notification.show(
                "Error saving segment rule: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    } else {
      Notification.show(
              "Validation errors. Please check the form.", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void confirmDelete(@NonNull SegmentRule segmentRule) {
    Dialog confirmDialog = new Dialog();
    confirmDialog.setHeaderTitle("Confirm Delete");
    confirmDialog.add(
        "Are you sure you want to delete segment rule '" + segmentRule.getName() + "'?");

    Button deleteButton = new Button("Delete", e -> deleteSegmentRule(segmentRule, confirmDialog));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
    Button cancelButton = new Button("Cancel", e -> confirmDialog.close());

    HorizontalLayout buttons = new HorizontalLayout(deleteButton, cancelButton);
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();

    confirmDialog.add(buttons);
    confirmDialog.open();
  }

  private void deleteSegmentRule(@NonNull SegmentRule segmentRule, @NonNull Dialog confirmDialog) {
    try {
      segmentRuleService.deactivateRule(segmentRule.getId());
      Notification.show(
              "Segment rule deleted successfully!", 3000, Notification.Position.BOTTOM_START)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      confirmDialog.close();
      refreshGrid();
    } catch (Exception e) {
      Notification.show(
              "Error deleting segment rule: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }
}
