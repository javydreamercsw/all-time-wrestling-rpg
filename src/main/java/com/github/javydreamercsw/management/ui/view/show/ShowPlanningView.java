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
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

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
  private final com.github.javydreamercsw.management.service.npc.NpcService npcService;
  private final ObjectMapper objectMapper;
  private final SegmentNarrationServiceFactory aiFactory;

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

  public ShowPlanningView(
      ShowService showService,
      ShowPlanningService showPlanningService,
      ShowPlanningAiService showPlanningAiService,
      WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      TitleService titleService,
      SegmentTypeRepository segmentTypeRepository,
      SegmentRuleRepository segmentRuleRepository,
      com.github.javydreamercsw.management.service.npc.NpcService npcService,
      ObjectMapper objectMapper,
      SegmentNarrationServiceFactory aiFactory) {

    this.showService = showService;
    this.showPlanningService = showPlanningService;
    this.showPlanningAiService = showPlanningAiService;
    this.wrestlerService = wrestlerService;
    this.npcService = npcService;
    this.objectMapper = objectMapper;
    this.aiFactory = aiFactory;

    templateImage = new Image();

    templateImage.setHeight("150px");

    templateImage.setWidth("150px");

    templateImage.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.MEDIUM);

    templateImage.setVisible(false);

    showComboBox = new ComboBox<>("Select Show");
    showComboBox.setId("select-show-combo-box");
    showComboBox.setItems(
        showService.findAll().stream()
            .sorted(Comparator.comparing(Show::getName))
            .collect(Collectors.toList()));
    showComboBox.setItemLabelGenerator(Show::getName);

    loadContextButton = new Button("Load Context");
    loadContextButton.addClickListener(e -> loadContext());
    loadContextButton.setEnabled(false);

    viewDetailsButton = new Button("View Details");
    viewDetailsButton.addClickListener(e -> navigateToShowDetails());
    viewDetailsButton.setEnabled(false);

    showComboBox.addValueChangeListener(
        event -> {
          loadContextButton.setEnabled(event.getValue() != null);
          viewDetailsButton.setEnabled(event.getValue() != null);
          updateTemplateImage(event.getValue());
        });

    contextArea = new TextArea("Show Planning Context");
    contextArea.setWidthFull();
    contextArea.setId("show-planning-context-area");
    contextArea.setHeight("300px");

    proposeSegmentsButton = new Button("Propose Segments");
    proposeSegmentsButton.addClickListener(e -> proposeSegments());
    proposeSegmentsButton.setId("propose-segments-button");
    proposeSegmentsButton.setEnabled(false);

    proposedSegmentsGrid = new Grid<>(ProposedSegment.class, false);
    proposedSegmentsGrid.addColumn(ProposedSegment::getType).setHeader("Type");
    Grid.Column<ProposedSegment> descriptionColumn =
        proposedSegmentsGrid.addColumn(ProposedSegment::getNarration).setHeader("Description");
    proposedSegmentsGrid
        .addColumn(segment -> String.join(", ", segment.getParticipants()))
        .setHeader("Participants");
    proposedSegmentsGrid.setId("proposed-segments-grid");

    editor = proposedSegmentsGrid.getEditor();
    Binder<ProposedSegment> binder = new Binder<>(ProposedSegment.class);
    editor.setBinder(binder);

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
                        () -> proposedSegmentsGrid.getDataProvider().refreshAll());
                dialog.open();
              });
          return editButton;
        });

    Grid.Column<ProposedSegment> removeColumn =
        proposedSegmentsGrid.addComponentColumn(
            segment -> {
              Button removeButton = new Button(VaadinIcon.TRASH.create());
              removeButton.addClickListener(
                  e -> {
                    segments.remove(segment);
                    proposedSegmentsGrid.setItems(segments);
                  });
              return removeButton;
            });
    approveButton = new Button("Approve Segments");
    approveButton.setId("approve-segments-button");
    approveButton.addClickListener(e -> approveSegments());

    VerticalLayout layout =
        new VerticalLayout(
            templateImage,
            showComboBox,
            loadContextButton,
            viewDetailsButton, // Added here
            proposeSegmentsButton,
            contextArea,
            proposedSegmentsGrid,
            approveButton);
    add(layout);
  }

  private void updateTemplateImage(Show show) {
    if (show != null
        && show.getTemplate() != null
        && show.getTemplate().getImageUrl() != null
        && !show.getTemplate().getImageUrl().isEmpty()) {
      templateImage.setSrc(show.getTemplate().getImageUrl());
      templateImage.setVisible(true);
    } else {
      templateImage.setVisible(false);
    }
  }

  private void navigateToShowDetails() {
    Show selectedShow = showComboBox.getValue();
    if (selectedShow != null) {
      UI.getCurrent().navigate(ShowDetailView.class, selectedShow.getId());
    }
  }

  private void loadContext() {
    Show selectedShow = showComboBox.getValue();
    if (selectedShow != null) {
      try {
        ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(selectedShow);
        contextArea.setValue(
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context));
        proposeSegmentsButton.setEnabled(true);
      } catch (Exception ex) {
        log.error("Error displaying context", ex);
        contextArea.setValue("Error displaying context: " + ex.getMessage());
      }
    }
  }

  private void proposeSegments() {
    if (aiFactory.getAvailableServicesInPriorityOrder().isEmpty()) {
      String reason = "No AI providers are currently enabled or reachable.";
      Notification.show(reason, 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    try {
      ShowPlanningContextDTO context =
          objectMapper.readValue(contextArea.getValue(), ShowPlanningContextDTO.class);

      // Ensure the showDate is preserved
      Show selectedShow = showComboBox.getValue();
      if (selectedShow != null && selectedShow.getShowDate() != null) {
        context.setShowDate(selectedShow.getShowDate().atStartOfDay(ZoneOffset.UTC).toInstant());
      }

      // Log the request context
      log.debug(
          "Sending context to AI: {}",
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context));

      ProposedShow proposedShow = showPlanningAiService.planShow(context);

      // Log the response
      log.debug(
          "Received proposed show from AI: {}",
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(proposedShow));

      if (proposedShow != null) {
        segments = new ArrayList<>(proposedShow.getSegments());
        proposedSegmentsGrid.setItems(segments);
        approveButton.setEnabled(true); // Enable approve button
      } else {
        Notification.show("AI did not propose any segments.", 5_000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    } catch (Exception ex) {
      log.error("Error proposing segments", ex);
      Notification.show(
              "Error proposing segments: " + ex.getMessage(), 5_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void approveSegments() {
    Show selectedShow = showComboBox.getValue();
    if (selectedShow == null) {
      Notification.show("Please select a show first.", 5000, Notification.Position.MIDDLE);
      return;
    }

    try {
      showPlanningService.approveSegments(selectedShow, segments);
      Notification.show("Segments approved successfully!", 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      proposedSegmentsGrid.setItems(new ArrayList<>());
    } catch (Exception ex) {
      log.error("Error approving segments", ex);
      Notification.show(
              "Error approving segments: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter Long parameter) {
    if (parameter != null) {
      showService
          .getShowById(parameter)
          .ifPresent(
              show -> {
                showComboBox.setValue(show);
                updateTemplateImage(show);
                loadContext();
              });
    }
  }
}
