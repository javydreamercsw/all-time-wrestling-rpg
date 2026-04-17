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

import com.github.javydreamercsw.base.ai.SegmentNarrationController;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.controller.show.ShowController;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/** Detail view for displaying comprehensive information about a specific show. */
@Route("show-detail")
@PageTitle("Show Details")
@PermitAll
@Slf4j
public class ShowDetailView extends Main
    implements HasUrlParameter<Long>, ApplicationListener<ApplicationEvent> {

  private final ShowService showService;
  private final SegmentService segmentService;
  private final SegmentRepository segmentRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final NpcService npcService;
  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final SegmentRuleRepository segmentRuleRepository;
  private final ShowTypeService showTypeService;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;
  private final RivalryService rivalryService;
  private final SegmentNarrationServiceFactory segmentNarrationServiceFactory;
  private final SegmentNarrationController segmentNarrationController;
  private final ShowController showController;
  private final MatchFulfillmentRepository matchFulfillmentRepository;
  private final UniverseRepository universeRepository;
  private final CommentaryTeamRepository commentaryTeamRepository;
  private final RingsideActionService ringsideActionService;
  private final ArenaService arenaService;
  private final com.github.javydreamercsw.management.service.relationship
          .WrestlerRelationshipService
      relationshipService;
  private Button backButton;
  private Registration backButtonListener;
  private H2 showTitle;
  private VerticalLayout contentLayout;
  private Long currentShowId;
  private Show currentShow;
  private Grid<Segment> segmentsGrid;

  public ShowDetailView(
      ShowService showService,
      SegmentService segmentService,
      SegmentRepository segmentRepository,
      SegmentTypeRepository segmentTypeRepository,
      NpcService npcService,
      WrestlerService wrestlerService,
      TitleService titleService,
      SegmentRuleRepository segmentRuleRepository,
      ShowTypeService showTypeService,
      SeasonService seasonService,
      ShowTemplateService showTemplateService,
      RivalryService rivalryService,
      SegmentNarrationServiceFactory segmentNarrationServiceFactory,
      SegmentNarrationController segmentNarrationController,
      ShowController showController,
      MatchFulfillmentRepository matchFulfillmentRepository,
      UniverseRepository universeRepository,
      CommentaryTeamRepository commentaryTeamRepository,
      RingsideActionService ringsideActionService,
      ArenaService arenaService,
      com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService
          relationshipService) {
    this.showService = showService;
    this.segmentService = segmentService;
    this.segmentRepository = segmentRepository;
    this.segmentTypeRepository = segmentTypeRepository;
    this.npcService = npcService;
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    this.segmentRuleRepository = segmentRuleRepository;
    this.showTypeService = showTypeService;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.rivalryService = rivalryService;
    this.segmentNarrationServiceFactory = segmentNarrationServiceFactory;
    this.segmentNarrationController = segmentNarrationController;
    this.showController = showController;
    this.matchFulfillmentRepository = matchFulfillmentRepository;
    this.universeRepository = universeRepository;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.ringsideActionService = ringsideActionService;
    this.arenaService = arenaService;
    this.relationshipService = relationshipService;
    initializeComponents();
  }

  private void initializeComponents() {
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    backButton = new Button("Back", new Icon(VaadinIcon.ARROW_LEFT));
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    showTitle = new H2("Show Details");
    showTitle.addClassNames(LumoUtility.Margin.NONE);

    HorizontalLayout headerLayout = new HorizontalLayout(backButton, showTitle);
    headerLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    headerLayout.setSpacing(true);

    contentLayout = new VerticalLayout();
    contentLayout.setSizeFull();
    contentLayout.setSpacing(true);
    contentLayout.setPadding(false);

    add(new ViewToolbar("Show Details", ViewToolbar.group(headerLayout)));
    add(contentLayout);
  }

  @Override
  public void setParameter(BeforeEvent event, Long showId) {
    String referrer =
        event
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .getOrDefault("ref", List.of("shows"))
            .get(0);

    updateBackButton(referrer);
    this.currentShowId = showId;
    if (showId != null) {
      loadShow(showId);
    } else {
      showNotFound();
    }
  }

  private void updateBackButton(@NonNull String referrer) {
    String navigationTarget =
        switch (referrer) {
          case "calendar" -> "show-calendar";
          case "booker" -> "booker";
          default -> "show-list";
        };

    backButton.setText("Back to " + referrer);
    if (backButtonListener != null) {
      backButtonListener.remove();
    }
    backButtonListener =
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(navigationTarget)));
  }

  private void loadShow(@NonNull Long showId) {
    Optional<Show> showOpt = showService.getShowById(showId);
    if (showOpt.isPresent()) {
      currentShow = showOpt.get();
      displayShow(currentShow);
    } else {
      showNotFound();
    }
    refreshSegmentsGrid();
  }

  private void displayShow(@NonNull Show show) {
    contentLayout.removeAll();
    showTitle.setText(show.getName());
    contentLayout.add(createHeaderCard(show), createDetailsCard(show));
    if (show.getDescription() != null && !show.getDescription().trim().isEmpty()) {
      contentLayout.add(createDescriptionCard(show));
    }
    contentLayout.add(createSegmentsCard(show));
  }

  private Div createHeaderCard(@NonNull Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);

    H3 title = new H3(show.getName());
    title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.PRIMARY);

    Button editNameButton = new Button(new Icon(VaadinIcon.EDIT));
    editNameButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editNameButton.setTooltipText("Edit Show Name");
    editNameButton.addClickListener(e -> new EditShowNameDialog(showService, show).open());

    Span typeBadge = new Span(show.getType().getName());
    typeBadge.addClassNames(
        LumoUtility.Padding.Horizontal.SMALL,
        LumoUtility.Padding.Vertical.XSMALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.FontSize.SMALL,
        LumoUtility.FontWeight.SEMIBOLD);

    Image showImage = new Image();
    if (show.getTemplate() != null) {
      showImage.setSrc(showTemplateService.resolveShowTemplateImage(show.getTemplate()));
    } else {
      showImage.setSrc("images/generic-show.png");
    }
    showImage.setHeight("100px");
    showImage.setWidth("100px");
    showImage.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Right.MEDIUM);

    VerticalLayout titleInfo = new VerticalLayout(title, typeBadge);
    titleInfo.setSpacing(false);
    titleInfo.setPadding(false);

    HorizontalLayout titleLayout = new HorizontalLayout(showImage, titleInfo, editNameButton);
    titleLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    titleLayout.setSpacing(true);

    card.add(titleLayout);
    return card;
  }

  private Div createDetailsCard(@NonNull Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);

    H3 detailsTitle = new H3("Show Information");
    detailsTitle.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

    VerticalLayout detailsLayout = new VerticalLayout();
    detailsLayout.setSpacing(false);
    detailsLayout.setPadding(false);

    detailsLayout.add(
        createDetailRow(
            "Show Date:",
            show.getShowDate() != null
                ? show.getShowDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                : "Not scheduled"));
    detailsLayout.add(
        createDetailRow(
            "Type:", show.getType() != null ? show.getType().getName() : "No type assigned"));
    detailsLayout.add(
        createDetailRow(
            "Season:",
            show.getSeason() != null ? show.getSeason().getName() : "No season assigned"));
    detailsLayout.add(
        createDetailRow(
            "Universe:",
            show.getUniverse() != null ? show.getUniverse().getName() : "No universe assigned"));
    detailsLayout.add(
        createDetailRow(
            "Arena:", show.getArena() != null ? show.getArena().getName() : "No arena assigned"));

    Button editDetailsButton =
        new Button(
            new Icon(VaadinIcon.EDIT),
            e ->
                new EditShowDetailsDialog(
                        showService,
                        showTypeService,
                        seasonService,
                        showTemplateService,
                        universeRepository,
                        commentaryTeamRepository,
                        arenaService,
                        show)
                    .open());
    editDetailsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

    HorizontalLayout header = new HorizontalLayout(detailsTitle, editDetailsButton);
    header.setWidthFull();
    header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

    card.add(header, detailsLayout);
    return card;
  }

  private HorizontalLayout createDetailRow(@NonNull String label, @NonNull String value) {
    Span labelSpan = new Span(label);
    labelSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextColor.SECONDARY);
    Span valueSpan = new Span(value);
    HorizontalLayout layout = new HorizontalLayout(labelSpan, valueSpan);
    layout.setSpacing(true);
    return layout;
  }

  private Div createDescriptionCard(@NonNull Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);
    H3 title = new H3("Description");
    card.add(title, new Paragraph(show.getDescription()));
    return card;
  }

  private void showNotFound() {
    contentLayout.removeAll();
    contentLayout.add(new H3("Show Not Found"));
  }

  private Div createSegmentsCard(@NonNull Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);
    card.setSizeFull();

    H3 title = new H3("Segments");
    Button addSegmentBtn =
        new Button("Add Segment", new Icon(VaadinIcon.PLUS), e -> openAddSegmentDialog(show));
    addSegmentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout header = new HorizontalLayout(title, addSegmentBtn);
    header.setWidthFull();
    header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

    segmentsGrid = new Grid<>(Segment.class, false);
    segmentsGrid.addColumn(s -> s.getSegmentType().getName()).setHeader("Type");
    segmentsGrid.addColumn(Segment::getSummary).setHeader("Summary");
    segmentsGrid.setHeight("400px");

    card.add(header, segmentsGrid);
    return card;
  }

  private void refreshSegmentsGrid() {
    if (currentShow != null && segmentsGrid != null) {
      segmentsGrid.setItems(segmentRepository.findByShow(currentShow));
    }
  }

  private void openAddSegmentDialog(@NonNull Show show) {
    // Basic implementation for now
    Notification.show("Add segment dialog would open here.");
  }

  private void adjudicateShow(@NonNull Show show) {
    showService.adjudicateShow(show.getId());
    refreshSegmentsGrid();
  }

  @Override
  public void onApplicationEvent(@NonNull ApplicationEvent event) {
    if (event instanceof AdjudicationCompletedEvent e
        && e.getShow().getId().equals(currentShowId)) {
      getUI().ifPresent(ui -> ui.access(this::refreshSegmentsGrid));
    }
  }
}
