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
package com.github.javydreamercsw.management.ui.view.show;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.BOOKER_ROLE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route("show-planning")
@PageTitle("Show Planning")
@Menu(order = 6, icon = "vaadin:calendar", title = "Show Planning")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE})
@Slf4j
public class ShowPlanningView extends Main implements HasUrlParameter<Long> {

  private final ShowService showService;
  private final ShowPlanningService showPlanningService;
  private final ShowPlanningAiService showPlanningAiService;
  private final WrestlerService wrestlerService;
  private final ShowTemplateService showTemplateService;
  private final com.github.javydreamercsw.management.service.npc.NpcService npcService;
  private final ObjectMapper objectMapper;
  private final SegmentNarrationServiceFactory aiFactory;
  private final com.github.javydreamercsw.management.service.world.ArenaService arenaService;
  private final com.github.javydreamercsw.base.ui.service.NotificationService notificationService;
  private final UniverseContextService universeContextService;
  private final WrestlerRepository wrestlerRepository;
  private final TitleService titleService;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;

  private final ComboBox<Show> showComboBox;
  private final Button loadContextButton;
  private final Button viewDetailsButton;
  private final TextArea contextArea;
  private final Grid<ProposedSegment> proposedSegmentsGrid;
  private final Image templateImage;
  private final Button approveButton;
  private final Button proposeSegmentsButton;
  private final Editor<ProposedSegment> editor;
  private List<ProposedSegment> segments = new ArrayList<>();

  @Autowired
  public ShowPlanningView(
      ShowService showService,
      ShowPlanningService showPlanningService,
      ShowPlanningAiService showPlanningAiService,
      WrestlerService wrestlerService,
      ShowTemplateService showTemplateService,
      WrestlerRepository wrestlerRepository,
      TitleService titleService,
      SegmentTypeRepository segmentTypeRepository,
      SegmentRuleRepository segmentRuleRepository,
      NpcService npcService,
      ObjectMapper objectMapper,
      SegmentNarrationServiceFactory aiFactory,
      ArenaService arenaService,
      com.github.javydreamercsw.base.ui.service.NotificationService notificationService,
      UniverseContextService universeContextService) {

    this.showService = showService;
    this.showPlanningService = showPlanningService;
    this.showPlanningAiService = showPlanningAiService;
    this.wrestlerService = wrestlerService;
    this.showTemplateService = showTemplateService;
    this.npcService = npcService;
    this.objectMapper = objectMapper;
    this.aiFactory = aiFactory;
    this.arenaService = arenaService;
    this.notificationService = notificationService;
    this.universeContextService = universeContextService;
    this.wrestlerRepository = wrestlerRepository;
    this.titleService = titleService;
    this.segmentTypeRepository = segmentTypeRepository;
    this.segmentRuleRepository = segmentRuleRepository;

    setSizeFull();
    addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Gap.MEDIUM);

    showComboBox = new ComboBox<>("Select Show");
    showComboBox.setItems(showService.getUpcomingShows(10));
    showComboBox.setItemLabelGenerator(
        s -> s.getName() + " (" + s.getShowDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + ")");
    showComboBox.setWidth("300px");

    loadContextButton = new Button("Load Context", e -> loadPlanningContext());
    loadContextButton.setEnabled(false);

    viewDetailsButton = new Button("View Show Details", e -> navigateToShowDetail());
    viewDetailsButton.setEnabled(false);

    showComboBox.addValueChangeListener(
        e -> {
          loadContextButton.setEnabled(e.getValue() != null);
          viewDetailsButton.setEnabled(e.getValue() != null);
          updateTemplateImage(e.getValue());
        });

    contextArea = new TextArea("Planning Context (JSON)");
    contextArea.setWidthFull();
    contextArea.setHeight("200px");
    contextArea.setReadOnly(true);

    proposeSegmentsButton = new Button("AI Propose Segments", e -> proposeSegments());
    proposeSegmentsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    proposeSegmentsButton.setEnabled(false);

    proposedSegmentsGrid = new Grid<>(ProposedSegment.class, false);
    proposedSegmentsGrid.addColumn(ProposedSegment::getType).setHeader("Type").setResizable(true);
    Grid.Column<ProposedSegment> summaryColumn =
        proposedSegmentsGrid
            .addColumn(ProposedSegment::getSummary)
            .setHeader("Summary")
            .setFlexGrow(2);
    Grid.Column<ProposedSegment> descriptionColumn =
        proposedSegmentsGrid
            .addColumn(ProposedSegment::getNarration)
            .setHeader("Description")
            .setFlexGrow(3);
    proposedSegmentsGrid
        .addColumn(s -> String.join(", ", s.getParticipants()))
        .setHeader("Participants");

    Binder<ProposedSegment> binder = new Binder<>(ProposedSegment.class);
    editor = proposedSegmentsGrid.getEditor();
    editor.setBinder(binder);
    editor.setBuffered(true);

    TextField summaryField = new TextField();
    binder.bind(summaryField, "summary");
    summaryColumn.setEditorComponent(summaryField);

    TextField descriptionField = new TextField();
    binder.bind(descriptionField, "narration");
    descriptionColumn.setEditorComponent(descriptionField);

    proposedSegmentsGrid.addItemDoubleClickListener(
        e -> {
          editor.editItem(e.getItem());
          descriptionField.focus();
        });

    editor.addSaveListener(
        e -> {
          // Save logic will go here
        });

    proposedSegmentsGrid.addComponentColumn(
        segment -> {
          Button editButton = new Button("Edit");
          editButton.addClickListener(
              e -> {
                Show selectedShow = showComboBox.getValue();
                com.github.javydreamercsw.base.domain.wrestler.Gender constraint =
                    (selectedShow != null && selectedShow.getTemplate() != null)
                        ? selectedShow.getTemplate().getGenderConstraint()
                        : null;

                EditSegmentDialog dialog =
                    new EditSegmentDialog(
                        segment,
                        wrestlerRepository,
                        wrestlerService,
                        titleService,
                        segmentTypeRepository,
                        segmentRuleRepository,
                        npcService,
                        constraint,
                        universeContextService.getCurrentUniverseId(),
                        () -> proposedSegmentsGrid.getDataProvider().refreshAll());
                dialog.open();
              });
          return editButton;
        });

    proposedSegmentsGrid.addComponentColumn(
        segment -> {
          Button removeButton = new Button(VaadinIcon.TRASH.create());
          removeButton.addClickListener(
              e -> {
                segments.remove(segment);
                proposedSegmentsGrid.setItems(segments);
              });
          removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
          return removeButton;
        });

    approveButton = new Button("Approve & Create Segments", e -> approvePlanning());
    approveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
    approveButton.setEnabled(false);

    templateImage = new Image();
    templateImage.setHeight("150px");
    templateImage.setVisible(false);

    VerticalLayout leftSide =
        new VerticalLayout(
            new H2("Show Planning"),
            new HorizontalLayout(showComboBox, loadContextButton, viewDetailsButton),
            templateImage,
            contextArea,
            proposeSegmentsButton);
    leftSide.setPadding(false);

    VerticalLayout rightSide =
        new VerticalLayout(new H2("Proposed Segments"), proposedSegmentsGrid, approveButton);
    rightSide.setPadding(false);

    HorizontalLayout mainLayout = new HorizontalLayout(leftSide, rightSide);
    mainLayout.setSizeFull();
    mainLayout.setFlexGrow(1, leftSide);
    mainLayout.setFlexGrow(2, rightSide);

    add(new ViewToolbar("Show Planning"), mainLayout);
  }

  private void navigateToShowDetail() {
    Show show = showComboBox.getValue();
    if (show != null) {
      getUI()
          .ifPresent(
              ui ->
                  ui.navigate(
                      ShowDetailView.class,
                      show.getId(),
                      com.vaadin.flow.router.QueryParameters.of("ref", "booker")));
    }
  }

  private void updateTemplateImage(Show show) {
    if (show != null && show.getTemplate() != null) {
      templateImage.setSrc(showTemplateService.resolveShowTemplateImage(show.getTemplate()));
      templateImage.setVisible(true);
    } else {
      templateImage.setVisible(false);
    }
  }

  private void loadPlanningContext() {
    Show show = showComboBox.getValue();
    if (show == null) return;

    try {
      ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);
      contextArea.setValue(
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context));
      proposeSegmentsButton.setEnabled(true);
      notificationService.showSuccess("Planning context loaded from database.");
    } catch (Exception e) {
      log.error("Error loading planning context", e);
      notificationService.showError("Failed to load planning context: " + e.getMessage());
    }
  }

  private void proposeSegments() {
    Show show = showComboBox.getValue();
    if (show == null) return;

    if (aiFactory.getAvailableServicesInPriorityOrder().isEmpty()) {
      notificationService.showError("No AI providers available.");
      return;
    }

    try {
      ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);
      ProposedShow proposedShow = showPlanningAiService.planShow(context);
      segments = proposedShow.getSegments();
      proposedSegmentsGrid.setItems(segments);
      approveButton.setEnabled(!segments.isEmpty());
      notificationService.showSuccess("AI proposed " + segments.size() + " segments.");
    } catch (Exception e) {
      log.error("Error proposing segments", e);
      notificationService.showAIServiceError(e);
    }
  }

  private void approvePlanning() {
    Show show = showComboBox.getValue();
    if (show == null || segments.isEmpty()) return;

    try {
      showPlanningService.approveSegments(show, segments);
      notificationService.showSuccess("Segments created for " + show.getName());
      getUI().ifPresent(ui -> ui.navigate(ShowDetailView.class, show.getId()));
    } catch (Exception e) {
      log.error("Error approving planning", e);
      notificationService.showError("Failed to approve planning: " + e.getMessage());
    }
  }

  @Override
  public void setParameter(BeforeEvent event, Long parameter) {
    if (parameter != null) {
      showService.getShowById(parameter).ifPresent(showComboBox::setValue);
    }
  }
}
