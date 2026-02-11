/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.show.template;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.image.ui.GenericImageGenerationDialog;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
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
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
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
  private final CommentaryTeamRepository commentaryTeamRepository;
  private final SecurityUtils securityUtils;
  private final ImageGenerationServiceFactory imageGenerationServiceFactory;
  private final ImageStorageService imageStorageService;
  private final AiSettingsService aiSettingsService;

  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private ComboBox<ShowType> editShowType;
  private ComboBox<CommentaryTeam> editCommentaryTeam;
  private TextField editNotionUrl;
  private IntegerField editExpectedMatches;
  private IntegerField editExpectedPromos;
  private IntegerField editDurationDays;
  private ComboBox<RecurrenceType> editRecurrenceType;
  private ComboBox<DayOfWeek> editDayOfWeek;
  private IntegerField editDayOfMonth;
  private ComboBox<Integer> editWeekOfMonth;
  private ComboBox<Month> editMonth;
  private ShowTemplate editingTemplate;
  private Binder<ShowTemplate> binder;

  final TextField nameFilter;
  final ComboBox<ShowType> showTypeFilter;
  final Button createBtn;
  final Grid<ShowTemplate> templateGrid;

  public ShowTemplateListView(
      @NonNull ShowTemplateService showTemplateService,
      @NonNull ShowTypeService showTypeService,
      @NonNull CommentaryTeamRepository commentaryTeamRepository,
      @NonNull SecurityUtils securityUtils,
      @NonNull ImageGenerationServiceFactory imageGenerationServiceFactory,
      @NonNull ImageStorageService imageStorageService,
      @NonNull AiSettingsService aiSettingsService) {
    this.showTemplateService = showTemplateService;
    this.showTypeService = showTypeService;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.securityUtils = securityUtils;
    this.imageGenerationServiceFactory = imageGenerationServiceFactory;
    this.imageStorageService = imageStorageService;
    this.aiSettingsService = aiSettingsService;

    // Initialize filters
    nameFilter = new TextField();
    nameFilter.setPlaceholder("Filter by name...");
    nameFilter.setAriaLabel("Name Filter");
    nameFilter.setClearButtonVisible(true);
    nameFilter.addValueChangeListener(e -> refreshGrid());

    showTypeFilter = new ComboBox<>("Show Type");
    List<ShowType> showTypes =
        new ArrayList<>(
            showTypeService.findAll().stream()
                .sorted(Comparator.comparing(ShowType::getName))
                .collect(Collectors.toList()));
    showTypeFilter.setItems(showTypes);
    showTypeFilter.setItemLabelGenerator(ShowType::getName);
    showTypeFilter.setPlaceholder("All types");
    showTypeFilter.setClearButtonVisible(true);
    showTypeFilter.addValueChangeListener(e -> refreshGrid());

    createBtn = new Button("Create Template", new Icon(VaadinIcon.PLUS));
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createBtn.addClickListener(e -> openCreateDialog());
    createBtn.setVisible(securityUtils.canCreate());

    // Initialize grid
    templateGrid = new Grid<>(ShowTemplate.class, false);
    templateGrid.setId("template-grid");
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
    // Image column
    templateGrid
        .addComponentColumn(
            template -> {
              Image image = new Image();
              if (template.getImageUrl() != null && !template.getImageUrl().isEmpty()) {
                image.setSrc(template.getImageUrl());
              } else {
                image.setSrc("https://via.placeholder.com/50");
              }
              image.setHeight("50px");
              image.setWidth("50px");
              image.addClassName(LumoUtility.BorderRadius.SMALL);
              return image;
            })
        .setHeader("Art")
        .setFlexGrow(0)
        .setWidth("70px");

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
              editBtn.setVisible(securityUtils.canEdit());

              Button generateArtBtn = new Button("Generate Art", new Icon(VaadinIcon.PICTURE));
              generateArtBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              generateArtBtn.addClickListener(e -> openGenerateArtDialog(template));
              generateArtBtn.setVisible(securityUtils.canEdit());
              generateArtBtn.setId("generate-art-btn-" + template.getId());

              Button deleteBtn = new Button("Delete", new Icon(VaadinIcon.TRASH));
              deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteBtn.addClickListener(e -> deleteTemplate(template));
              deleteBtn.setVisible(securityUtils.canDelete());

              actions.add(editBtn, generateArtBtn, deleteBtn);
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(0);

    templateGrid.setSizeFull();
  }

  private void openGenerateArtDialog(ShowTemplate template) {
    java.util.function.Supplier<String> promptSupplier =
        () -> {
          StringBuilder sb = new StringBuilder();
          sb.append("A professional wrestling show logo or poster for '")
              .append(template.getName())
              .append("'. ");
          if (template.getShowType() != null) {
            sb.append("This is a ").append(template.getShowType().getName()).append(" show. ");
          }
          if (template.getDescription() != null && !template.getDescription().isEmpty()) {
            sb.append(template.getDescription()).append(". ");
          }
          sb.append(
              "High quality, bold typography, dramatic lighting, exciting atmosphere, sports"
                  + " entertainment style.");
          return sb.toString();
        };

    java.util.function.Consumer<String> imageSaver =
        (imageUrl) -> {
          template.setImageUrl(imageUrl);
          showTemplateService.save(template);
          refreshGrid();
        };

    new GenericImageGenerationDialog(
            promptSupplier,
            imageSaver,
            imageGenerationServiceFactory,
            imageStorageService,
            aiSettingsService,
            this::refreshGrid)
        .open();
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
    editShowType.setItems(
        showTypeService.findAll().stream()
            .sorted(Comparator.comparing(ShowType::getName))
            .collect(Collectors.toList()));
    editShowType.setItemLabelGenerator(ShowType::getName);
    editShowType.setWidthFull();
    editShowType.setRequired(true);

    editCommentaryTeam = new ComboBox<>("Commentary Team");
    editCommentaryTeam.setItems(
        commentaryTeamRepository.findAll().stream()
            .sorted(Comparator.comparing(CommentaryTeam::getName))
            .collect(Collectors.toList()));
    editCommentaryTeam.setItemLabelGenerator(CommentaryTeam::getName);
    editCommentaryTeam.setWidthFull();
    editCommentaryTeam.setClearButtonVisible(true);

    editNotionUrl = new TextField("Notion URL");
    editNotionUrl.setWidthFull();
    editNotionUrl.setPlaceholder("https://notion.so/...");

    editExpectedMatches = new IntegerField("Expected Matches");
    editExpectedMatches.setWidthFull();
    editExpectedMatches.setPlaceholder("Use show type default");
    editExpectedMatches.setClearButtonVisible(true);

    editExpectedPromos = new IntegerField("Expected Promos");
    editExpectedPromos.setWidthFull();
    editExpectedPromos.setPlaceholder("Use show type default");
    editExpectedPromos.setClearButtonVisible(true);

    editDurationDays = new IntegerField("Duration (Days)");
    editDurationDays.setWidthFull();
    editDurationDays.setMin(1);
    editDurationDays.setValue(1);

    editRecurrenceType = new ComboBox<>("Recurrence Type");
    editRecurrenceType.setItems(RecurrenceType.values());
    editRecurrenceType.setItemLabelGenerator(RecurrenceType::name);
    editRecurrenceType.setWidthFull();

    editDayOfWeek = new ComboBox<>("Day of Week");
    editDayOfWeek.setItems(DayOfWeek.values());
    editDayOfWeek.setItemLabelGenerator(DayOfWeek::name);
    editDayOfWeek.setWidthFull();
    editDayOfWeek.setVisible(false);

    editDayOfMonth = new IntegerField("Day of Month");
    editDayOfMonth.setWidthFull();
    editDayOfMonth.setMin(1);
    editDayOfMonth.setMax(31);
    editDayOfMonth.setVisible(false);

    editWeekOfMonth = new ComboBox<>("Week of Month");
    editWeekOfMonth.setItems(1, 2, 3, 4, -1);
    editWeekOfMonth.setItemLabelGenerator(
        i -> {
          if (i == -1) return "Last";
          return switch (i) {
            case 1 -> "First";
            case 2 -> "Second";
            case 3 -> "Third";
            case 4 -> "Fourth";
            default -> String.valueOf(i);
          };
        });
    editWeekOfMonth.setWidthFull();
    editWeekOfMonth.setVisible(false);

    editMonth = new ComboBox<>("Month");
    editMonth.setItems(Month.values());
    editMonth.setItemLabelGenerator(Month::name);
    editMonth.setWidthFull();
    editMonth.setVisible(false);

    editRecurrenceType.addValueChangeListener(
        e -> {
          RecurrenceType type = e.getValue();
          editDayOfWeek.setVisible(
              type == RecurrenceType.WEEKLY
                  || type == RecurrenceType.MONTHLY
                  || type == RecurrenceType.ANNUAL);
          editDayOfMonth.setVisible(
              type == RecurrenceType.MONTHLY || type == RecurrenceType.ANNUAL);
          editWeekOfMonth.setVisible(
              type == RecurrenceType.MONTHLY || type == RecurrenceType.ANNUAL);
          editMonth.setVisible(type == RecurrenceType.ANNUAL);
        });

    Button saveBtn = new Button("Save", e -> saveTemplate());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.setVisible(securityUtils.canEdit());

    Button generateArtDialogBtn = new Button("Generate Art", new Icon(VaadinIcon.PICTURE));
    generateArtDialogBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    generateArtDialogBtn.addClickListener(
        e -> {
          if (editingTemplate != null) {
            openGenerateArtDialog(editingTemplate);
          }
        });
    generateArtDialogBtn.setVisible(securityUtils.canEdit());

    Button cancelBtn = new Button("Cancel", e -> editDialog.close());

    FormLayout formLayout = new FormLayout();
    formLayout.add(
        editName,
        editDescription,
        editShowType,
        editCommentaryTeam,
        editNotionUrl,
        editExpectedMatches,
        editExpectedPromos,
        editDurationDays,
        editRecurrenceType,
        editDayOfWeek,
        editDayOfMonth,
        editWeekOfMonth,
        editMonth);
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
    formLayout.setColspan(editDescription, 2);

    HorizontalLayout buttonLayout = new HorizontalLayout(generateArtDialogBtn, saveBtn, cancelBtn);
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
    binder
        .forField(editCommentaryTeam)
        .bind(ShowTemplate::getCommentaryTeam, ShowTemplate::setCommentaryTeam);
    binder.forField(editNotionUrl).bind(ShowTemplate::getNotionUrl, ShowTemplate::setNotionUrl);
    binder
        .forField(editExpectedMatches)
        .bind(ShowTemplate::getExpectedMatches, ShowTemplate::setExpectedMatches);
    binder
        .forField(editExpectedPromos)
        .bind(ShowTemplate::getExpectedPromos, ShowTemplate::setExpectedPromos);
    binder
        .forField(editDurationDays)
        .bind(ShowTemplate::getDurationDays, ShowTemplate::setDurationDays);
    binder
        .forField(editRecurrenceType)
        .bind(ShowTemplate::getRecurrenceType, ShowTemplate::setRecurrenceType);
    binder
        .forField(editDayOfWeek)
        .withValidator(
            (value, context) -> {
              RecurrenceType type = editRecurrenceType.getValue();
              if (type == RecurrenceType.WEEKLY && value == null) {
                return com.vaadin.flow.data.binder.ValidationResult.error(
                    "Day of Week is required for weekly recurrence");
              }
              if ((type == RecurrenceType.MONTHLY || type == RecurrenceType.ANNUAL)
                  && editDayOfMonth.getValue() == null
                  && value == null) {
                return com.vaadin.flow.data.binder.ValidationResult.error(
                    "Either Day of Month or Day of Week is required");
              }
              return com.vaadin.flow.data.binder.ValidationResult.ok();
            })
        .bind(ShowTemplate::getDayOfWeek, ShowTemplate::setDayOfWeek);
    binder.forField(editDayOfMonth).bind(ShowTemplate::getDayOfMonth, ShowTemplate::setDayOfMonth);
    binder
        .forField(editWeekOfMonth)
        .withValidator(
            (value, context) -> {
              RecurrenceType type = editRecurrenceType.getValue();
              if ((type == RecurrenceType.MONTHLY || type == RecurrenceType.ANNUAL)
                  && editDayOfWeek.getValue() != null
                  && value == null) {
                return com.vaadin.flow.data.binder.ValidationResult.error(
                    "Week of Month is required when using Day of Week");
              }
              return com.vaadin.flow.data.binder.ValidationResult.ok();
            })
        .bind(ShowTemplate::getWeekOfMonth, ShowTemplate::setWeekOfMonth);
    binder
        .forField(editMonth)
        .withValidator(
            (value, context) -> {
              if (editRecurrenceType.getValue() == RecurrenceType.ANNUAL && value == null) {
                return com.vaadin.flow.data.binder.ValidationResult.error(
                    "Month is required for annual recurrence");
              }
              return com.vaadin.flow.data.binder.ValidationResult.ok();
            })
        .bind(ShowTemplate::getMonth, ShowTemplate::setMonth);
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
                editingTemplate.getNotionUrl(),
                null,
                editingTemplate.getCommentaryTeam() != null
                    ? editingTemplate.getCommentaryTeam().getName()
                    : null,
                editingTemplate.getExpectedMatches(),
                editingTemplate.getExpectedPromos(),
                editingTemplate.getDurationDays(),
                editingTemplate.getRecurrenceType(),
                editingTemplate.getDayOfWeek(),
                editingTemplate.getDayOfMonth(),
                editingTemplate.getWeekOfMonth(),
                editingTemplate.getMonth());

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
            editingTemplate.getNotionUrl(),
            editingTemplate.getImageUrl(),
            editingTemplate.getCommentaryTeam() != null
                ? editingTemplate.getCommentaryTeam().getName()
                : null,
            editingTemplate.getExpectedMatches(),
            editingTemplate.getExpectedPromos(),
            editingTemplate.getDurationDays(),
            editingTemplate.getRecurrenceType(),
            editingTemplate.getDayOfWeek(),
            editingTemplate.getDayOfMonth(),
            editingTemplate.getWeekOfMonth(),
            editingTemplate.getMonth());

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
