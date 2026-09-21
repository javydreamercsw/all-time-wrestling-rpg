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
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ImageUploadComponent;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowContextFacade;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
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
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.data.binder.ValidationResult;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
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
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final TournamentService tournamentService;

  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private ComboBox<ShowType> editShowType;
  private ComboBox<CommentaryTeam> editCommentaryTeam;
  private TextField editImageUrl;
  private IntegerField editExpectedMatches;
  private IntegerField editExpectedPromos;
  private IntegerField editDurationDays;
  private ComboBox<RecurrenceType> editRecurrenceType;
  private ComboBox<DayOfWeek> editDayOfWeek;
  private IntegerField editDayOfMonth;
  private ComboBox<Integer> editWeekOfMonth;
  private ComboBox<Month> editMonth;
  private ComboBox<Gender> editGenderConstraint;
  private ShowTemplate editingTemplate;
  private Binder<ShowTemplate> binder;
  private Grid<ShowTemplateSegmentAssignment> assignmentGrid;
  private List<ShowTemplateSegmentAssignment> dialogAssignments;
  private ComboBox<SegmentType> assignmentTypeCombo;
  private ComboBox<SegmentRule> assignmentRuleCombo;
  private ComboBox<ShowTemplateSegmentAssignment.AssignmentMode> assignmentModeCombo;
  private ComboBox<Tournament> assignmentTournamentCombo;

  final TextField nameFilter;
  final ComboBox<ShowType> showTypeFilter;
  final Button createBtn;
  final Grid<ShowTemplate> templateGrid;

  public ShowTemplateListView(
      @NonNull final ShowTemplateService showTemplateService,
      @NonNull final ShowTypeService showTypeService,
      @NonNull final CommentaryTeamRepository commentaryTeamRepository,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final ImageGenerationServiceFactory imageGenerationServiceFactory,
      @NonNull final ImageStorageService imageStorageService,
      @NonNull final AiSettingsService aiSettingsService,
      @NonNull final SegmentTypeService segmentTypeService,
      @NonNull final SegmentRuleService segmentRuleService,
      @NonNull final ShowContextFacade showContextFacade) {
    this.showTemplateService = showTemplateService;
    this.showTypeService = showTypeService;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.securityUtils = securityUtils;
    this.imageGenerationServiceFactory = imageGenerationServiceFactory;
    this.imageStorageService = imageStorageService;
    this.aiSettingsService = aiSettingsService;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.tournamentService = showContextFacade.getTournamentService();

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
    Div gridWrapper = new Div(templateGrid);
    gridWrapper.addClassName("grid-scroll-container");
    add(gridWrapper);

    refreshGrid();
  }

  private void setupGrid() {
    // Image column
    templateGrid
        .addComponentColumn(
            template -> {
              Image image =
                  new Image(
                      showTemplateService.resolveShowTemplateImage(template),
                      "Show Template Image");
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
        .addComponentColumn(
            template -> {
              HorizontalLayout nameLayout = new HorizontalLayout();
              nameLayout.setAlignItems(FlexComponent.Alignment.CENTER);
              nameLayout.setSpacing(false);
              nameLayout.setPadding(false);
              if (!template.isActive()) {
                Icon inactiveIcon = VaadinIcon.EYE_SLASH.create();
                inactiveIcon.setColor("var(--lumo-disabled-text-color)");
                inactiveIcon.getStyle().set("margin-right", "6px");
                nameLayout.add(inactiveIcon);
              }
              nameLayout.add(new Span(template.getName()));
              return nameLayout;
            })
        .setHeader("Name")
        .setComparator(Comparator.comparing(ShowTemplate::getName))
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

    // Gender Constraint column
    templateGrid
        .addColumn(
            template ->
                template.getGenderConstraint() != null
                    ? template.getGenderConstraint().name()
                    : "Both")
        .setHeader("Gender Constraint")
        .setSortable(true)
        .setFlexGrow(1);

    // Commentary Team column
    templateGrid
        .addColumn(
            template ->
                template.getCommentaryTeam() != null ? template.getCommentaryTeam().getName() : "")
        .setHeader("Commentary Team")
        .setSortable(true)
        .setFlexGrow(1);

    // Recurrence column
    templateGrid
        .addColumn(
            template ->
                template.getRecurrenceType() != null
                    ? template.getRecurrenceType().name()
                    : RecurrenceType.NONE.name())
        .setHeader("Recurrence")
        .setSortable(true)
        .setFlexGrow(1);

    // Duration column
    templateGrid
        .addColumn(
            template ->
                template.getDurationDays() != null ? template.getDurationDays() + "d" : "1d")
        .setHeader("Duration")
        .setSortable(true)
        .setFlexGrow(0)
        .setWidth("90px");

    // Expected Matches column
    templateGrid
        .addColumn(
            template ->
                template.getExpectedMatches() != null
                    ? String.valueOf(template.getExpectedMatches())
                    : "—")
        .setHeader("Matches")
        .setSortable(true)
        .setFlexGrow(0)
        .setWidth("90px");

    // Expected Promos column
    templateGrid
        .addColumn(
            template ->
                template.getExpectedPromos() != null
                    ? String.valueOf(template.getExpectedPromos())
                    : "—")
        .setHeader("Promos")
        .setSortable(true)
        .setFlexGrow(0)
        .setWidth("80px");

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
              editBtn.setId("edit-btn-" + template.getId());

              Icon toggleIcon =
                  template.isActive() ? new Icon(VaadinIcon.EYE) : new Icon(VaadinIcon.EYE_SLASH);
              toggleIcon.setColor(
                  template.isActive()
                      ? "var(--lumo-success-color)"
                      : "var(--lumo-disabled-text-color)");
              Button toggleBtn = new Button(toggleIcon);
              toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
              toggleBtn.setTooltipText(template.isActive() ? "Deactivate" : "Activate");
              toggleBtn.setAriaLabel(template.isActive() ? "Deactivate" : "Activate");
              toggleBtn.setVisible(securityUtils.canEdit());
              toggleBtn.addClickListener(
                  e -> {
                    showTemplateService.setActive(template.getId(), !template.isActive());
                    refreshGrid();
                  });

              Button generateArtBtn = new Button("Generate Art", new Icon(VaadinIcon.PICTURE));
              generateArtBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              generateArtBtn.addClickListener(e -> openGenerateArtDialog(template));
              generateArtBtn.setVisible(securityUtils.canEdit());
              generateArtBtn.setId("generate-art-btn-" + template.getId());

              Button deleteBtn = new Button("Delete", new Icon(VaadinIcon.TRASH));
              deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteBtn.addClickListener(e -> deleteTemplate(template));
              deleteBtn.setVisible(securityUtils.canDelete());

              actions.add(editBtn, toggleBtn, generateArtBtn, deleteBtn);
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(0);

    templateGrid.setSizeFull();
  }

  private void openGenerateArtDialog(final ShowTemplate template) {
    Supplier<String> promptSupplier =
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
              """
              High quality, bold typography, dramatic lighting, exciting atmosphere, sports\
               entertainment style.\
              """);
          return sb.toString();
        };

    Consumer<String> imageSaver =
        imageUrl -> {
          template.setImageUrl(imageUrl);
          showTemplateService.save(template);
          editImageUrl.setValue(imageUrl);
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
    editDialog.setWidth("min(600px, 95vw)");
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

    editImageUrl = new TextField("Image URL");
    editImageUrl.setWidthFull();
    editImageUrl.setReadOnly(true);

    ImageUploadComponent imageUpload =
        new ImageUploadComponent(
            imageStorageService,
            url -> {
              editImageUrl.setValue(url);
            });
    imageUpload.setUploadButtonText("Upload Art");

    HorizontalLayout imageEditLayout = new HorizontalLayout(editImageUrl, imageUpload);
    imageEditLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
    imageEditLayout.setWidthFull();

    editExpectedMatches = new IntegerField("Expected Matches");
    editExpectedMatches.setWidthFull();
    editExpectedMatches.setPlaceholder("Use show type default");
    editExpectedMatches.setClearButtonVisible(true);
    editExpectedMatches.setStepButtonsVisible(true);

    editExpectedPromos = new IntegerField("Expected Promos");
    editExpectedPromos.setWidthFull();
    editExpectedPromos.setPlaceholder("Use show type default");
    editExpectedPromos.setClearButtonVisible(true);
    editExpectedPromos.setStepButtonsVisible(true);

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
          if (i == -1) {
            return "Last";
          }
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

    editGenderConstraint = new ComboBox<>("Gender Constraint");
    editGenderConstraint.setItems(Gender.values());
    editGenderConstraint.setItemLabelGenerator(
        g -> {
          if (g == null) {
            return "Both";
          }
          return g.name();
        });
    editGenderConstraint.setPlaceholder("Both (No constraint)");
    editGenderConstraint.setClearButtonVisible(true);
    editGenderConstraint.setWidthFull();

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

    // ── Template segment assignments (ATW-0331): per-row type?/rule? + mode ──
    dialogAssignments = new ArrayList<>();
    assignmentGrid = new Grid<>(ShowTemplateSegmentAssignment.class, false);
    assignmentGrid
        .addColumn(a -> a.getSegmentType() != null ? a.getSegmentType().getName() : "—")
        .setHeader("Segment Type (event-only)")
        .setAutoWidth(true);
    assignmentGrid
        .addColumn(a -> a.getSegmentRule() != null ? a.getSegmentRule().getName() : "—")
        .setHeader("Segment Rule")
        .setAutoWidth(true);
    assignmentGrid
        .addColumn(a -> a.getTournament() != null ? a.getTournament().getName() : "—")
        .setHeader("Tournament")
        .setAutoWidth(true);
    assignmentGrid.addColumn(a -> a.getMode().name()).setHeader("Mode").setAutoWidth(true);
    assignmentGrid.addComponentColumn(
        row -> {
          Button remove = new Button(new Icon(VaadinIcon.TRASH));
          remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
          remove.addClickListener(
              e -> {
                dialogAssignments.remove(row);
                assignmentGrid.getListDataView().refreshAll();
              });
          return remove;
        });
    assignmentGrid.setWidthFull();
    assignmentGrid.setHeight("150px");
    assignmentGrid.setItems(dialogAssignments);

    assignmentTypeCombo = new ComboBox<>("Segment Type");
    // Event-only formats were the original use (ATW-0331); tournament-fed rows may pick any
    // active type now that multi-entrant tournaments exist (ATW-oloa) — e.g. a Free-for-All
    // qualifier feeding a multi-man final. The AI never proposes event-only types (the prompt
    // filters them); these rows are booker-configured, so the manual-selection caveat holds.
    assignmentTypeCombo.setItems(
        segmentTypeService.findAllForAdmin().stream()
            .sorted(Comparator.comparing(SegmentType::getName))
            .toList());
    assignmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    assignmentTypeCombo.setWidthFull();
    assignmentTypeCombo.setPlaceholder("Optional");
    assignmentTypeCombo.setClearButtonVisible(true);

    assignmentRuleCombo = new ComboBox<>("Segment Rule");
    assignmentRuleCombo.setItems(
        segmentRuleService.findAll().stream()
            .sorted(Comparator.comparing(SegmentRule::getName))
            .toList());
    assignmentRuleCombo.setItemLabelGenerator(SegmentRule::getName);
    assignmentRuleCombo.setWidthFull();
    assignmentRuleCombo.setPlaceholder("Optional");
    assignmentRuleCombo.setClearButtonVisible(true);

    assignmentModeCombo = new ComboBox<>("Mode");
    assignmentModeCombo.setItems(ShowTemplateSegmentAssignment.AssignmentMode.values());
    assignmentModeCombo.setItemLabelGenerator(ShowTemplateSegmentAssignment.AssignmentMode::name);
    assignmentModeCombo.setValue(ShowTemplateSegmentAssignment.AssignmentMode.ENCOURAGED);
    assignmentModeCombo.setWidthFull();

    // Tournament pairing (ATW-oahn): its participants feed the auto-attached segment.
    assignmentTournamentCombo = new ComboBox<>("Tournament");
    assignmentTournamentCombo.setItems(
        tournamentService.findAll().stream()
            .sorted(Comparator.comparing(Tournament::getName))
            .toList());
    assignmentTournamentCombo.setItemLabelGenerator(Tournament::getName);
    assignmentTournamentCombo.setWidthFull();
    assignmentTournamentCombo.setPlaceholder("Optional");
    assignmentTournamentCombo.setClearButtonVisible(true);

    Button addAssignmentBtn =
        new Button(
            "Add Assignment",
            new Icon(VaadinIcon.PLUS),
            e -> {
              SegmentType type = assignmentTypeCombo.getValue();
              SegmentRule rule = assignmentRuleCombo.getValue();
              Tournament tournament = assignmentTournamentCombo.getValue();
              // At least one target (type, rule, or tournament) is required per row.
              if (type == null && rule == null && tournament == null) {
                Notification.show(
                    "Pick a segment type, a segment rule, or a tournament for the assignment.",
                    3000,
                    Notification.Position.MIDDLE);
                return;
              }
              // A tournament row must be AUTO_ATTACH: its participants are merged
              // deterministically at approval time; ENCOURAGED is meaningless for it.
              if (tournament != null
                  && ShowTemplateSegmentAssignment.AssignmentMode.ENCOURAGED
                      == assignmentModeCombo.getValue()) {
                Notification.show(
                    "Tournament rows must use AUTO_ATTACH mode.",
                    3000,
                    Notification.Position.MIDDLE);
                return;
              }
              ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
              row.setSegmentType(type);
              row.setSegmentRule(rule);
              row.setTournament(tournament);
              row.setMode(
                  assignmentModeCombo.getValue() != null
                      ? assignmentModeCombo.getValue()
                      : ShowTemplateSegmentAssignment.AssignmentMode.ENCOURAGED);
              dialogAssignments.add(row);
              assignmentGrid.getListDataView().refreshAll();
            });
    addAssignmentBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    addAssignmentBtn.setVisible(securityUtils.canEdit());

    HorizontalLayout assignmentPicker =
        new HorizontalLayout(
            assignmentTypeCombo,
            assignmentRuleCombo,
            assignmentTournamentCombo,
            assignmentModeCombo);
    assignmentPicker.setWidthFull();
    assignmentPicker.setAlignItems(FlexComponent.Alignment.END);
    VerticalLayout assignmentSection =
        new VerticalLayout(
            new Span("Template Assignments (event types, rules, tournament-fed segments)"),
            assignmentPicker,
            addAssignmentBtn,
            assignmentGrid);
    assignmentSection.setWidthFull();
    assignmentSection.setSpacing(false);
    assignmentSection.setPadding(false);

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
        imageEditLayout,
        editExpectedMatches,
        editExpectedPromos,
        editDurationDays,
        editRecurrenceType,
        editDayOfWeek,
        editDayOfMonth,
        editWeekOfMonth,
        editMonth,
        editGenderConstraint);
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
    formLayout.setColspan(editDescription, 2);
    formLayout.setColspan(imageEditLayout, 2);

    HorizontalLayout buttonLayout = new HorizontalLayout(generateArtDialogBtn, saveBtn, cancelBtn);
    buttonLayout.setWidthFull();
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, assignmentSection, buttonLayout);
    dialogLayout.setWidthFull();
    dialogLayout.setSpacing(true);

    editDialog.add(dialogLayout);

    // Setup binder
    binder = new Binder<>(ShowTemplate.class);
    binder
        .forField(editGenderConstraint)
        .bind(ShowTemplate::getGenderConstraint, ShowTemplate::setGenderConstraint);
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
    binder.forField(editImageUrl).bind(ShowTemplate::getImageUrl, ShowTemplate::setImageUrl);
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
                return ValidationResult.error("Day of Week is required for weekly recurrence");
              }
              if ((type == RecurrenceType.MONTHLY || type == RecurrenceType.ANNUAL)
                  && editDayOfMonth.getValue() == null
                  && value == null) {
                return ValidationResult.error("Either Day of Month or Day of Week is required");
              }
              return ValidationResult.ok();
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
                return ValidationResult.error("Week of Month is required when using Day of Week");
              }
              return ValidationResult.ok();
            })
        .bind(ShowTemplate::getWeekOfMonth, ShowTemplate::setWeekOfMonth);
    binder
        .forField(editMonth)
        .withValidator(
            (value, context) -> {
              if (editRecurrenceType.getValue() == RecurrenceType.ANNUAL && value == null) {
                return ValidationResult.error("Month is required for annual recurrence");
              }
              return ValidationResult.ok();
            })
        .bind(ShowTemplate::getMonth, ShowTemplate::setMonth);
  }

  private void openCreateDialog() {
    editingTemplate = new ShowTemplate();
    dialogAssignments.clear();
    assignmentGrid.setItems(dialogAssignments);
    editDialog.setHeaderTitle("Create Show Template");
    binder.readBean(editingTemplate);
    editDialog.open();
  }

  private void openEditDialog(final ShowTemplate template) {
    editingTemplate = template;
    // Eagerly fetch the assignments (they are lazy and this handler runs outside a session);
    // edit detached copies so cancelling doesn't mutate the live rows.
    dialogAssignments = new ArrayList<>();
    showTemplateService
        .getTemplateWithAssignments(template.getId())
        .ifPresent(
            fetched ->
                fetched
                    .getSegmentAssignments()
                    .forEach(
                        a -> {
                          ShowTemplateSegmentAssignment copy = new ShowTemplateSegmentAssignment();
                          copy.setSegmentType(a.getSegmentType());
                          copy.setSegmentRule(a.getSegmentRule());
                          copy.setMode(a.getMode());
                          dialogAssignments.add(copy);
                        }));
    // Rebind: dialogAssignments was reassigned to a fresh list above.
    assignmentGrid.setItems(dialogAssignments);
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
                editingTemplate.getMonth(),
                editingTemplate.getGenderConstraint());

        if (savedTemplate != null) {
          showTemplateService.syncSegmentAssignments(savedTemplate.getId(), dialogAssignments);
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
            editingTemplate.getMonth(),
            editingTemplate.getGenderConstraint());
        // Assignments sync through the transactional service (updateTemplate rebuilds from
        // primitives); orphanRemoval drops rows removed in the dialog.
        showTemplateService.syncSegmentAssignments(editingTemplate.getId(), dialogAssignments);

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

  // --- Test-visible delegates (package-private) for ShowTemplateListViewTest ---

  void openEditDialogForTest(final ShowTemplate template) {
    openEditDialog(template);
  }

  Grid<ShowTemplateSegmentAssignment> getAssignmentGridForTest() {
    return assignmentGrid;
  }

  void addAssignmentForTest(
      final SegmentType type,
      final SegmentRule rule,
      final ShowTemplateSegmentAssignment.AssignmentMode mode) {
    addAssignmentForTest(type, rule, null, mode);
  }

  void addAssignmentForTest(
      final SegmentType type,
      final SegmentRule rule,
      final Tournament tournament,
      final ShowTemplateSegmentAssignment.AssignmentMode mode) {
    ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
    row.setSegmentType(type);
    row.setSegmentRule(rule);
    row.setTournament(tournament);
    row.setMode(mode);
    dialogAssignments.add(row);
    assignmentGrid.getListDataView().refreshAll();
  }

  void saveTemplateForTest() {
    saveTemplate();
  }

  private void deleteTemplate(final ShowTemplate template) {
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
    List<ShowTemplate> templates = showTemplateService.findAllForAdmin();

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
