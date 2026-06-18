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
package com.github.javydreamercsw.management.ui.view.inbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.inbox.OpenProfileDrawerBroadcaster;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.league.MatchReportDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Sort;

@Route(value = "inbox", layout = MainLayout.class)
@PageTitle("Inbox")
@PermitAll
public class InboxView extends VerticalLayout {

  private final InboxService inboxService;
  private final InboxEventTypeRegistry eventTypeRegistry;
  private final WrestlerRepository wrestlerRepository;
  private final MatchFulfillmentService matchFulfillmentService;
  private final ObjectMapper objectMapper;
  private final OpenProfileDrawerBroadcaster openProfileDrawerBroadcaster;
  private final Grid<InboxItem> grid = new Grid<>(InboxItem.class);
  private final MultiSelectComboBox<Wrestler> targetFilter = new MultiSelectComboBox<>("Targets");
  private final ComboBox<String> readStatusFilter = new ComboBox<>("Read Status");
  private final ComboBox<String> eventTypeFilter = new ComboBox<>("Event Type");
  private final VerticalLayout detailsView = new VerticalLayout();
  private final Checkbox selectAllCheckbox = new Checkbox("Select All");
  private final Button markSelectedReadButton = new Button("Mark Selected as Read");
  private final Button markSelectedUnreadButton = new Button("Mark Selected as Unread");
  private final Checkbox hideReadCheckbox = new Checkbox("Hide Read");
  private final Button deleteSelectedButton = new Button("Delete Selected");
  private final Set<InboxItem> selectedItems = new HashSet<>();
  private final SecurityUtils securityUtils;

  public InboxView(
      final InboxService inboxService,
      final InboxEventTypeRegistry eventTypeRegistry,
      final WrestlerRepository wrestlerRepository,
      final MatchFulfillmentService matchFulfillmentService,
      final SecurityUtils securityUtils,
      final ObjectMapper objectMapper,
      final OpenProfileDrawerBroadcaster openProfileDrawerBroadcaster) {
    this.inboxService = inboxService;
    this.eventTypeRegistry = eventTypeRegistry;
    this.wrestlerRepository = wrestlerRepository;
    this.matchFulfillmentService = matchFulfillmentService;
    this.securityUtils = securityUtils;
    this.objectMapper = objectMapper;
    this.openProfileDrawerBroadcaster = openProfileDrawerBroadcaster;

    addClassName("inbox-view");
    setSizeFull();
    configureGrid();

    detailsView.setMinWidth("300px");
    showEmptyDetail();

    SplitLayout splitLayout = new SplitLayout();
    splitLayout.setSizeFull();
    splitLayout.addToPrimary(new VerticalLayout(getToolbar(), grid));
    splitLayout.addToSecondary(detailsView);

    add(splitLayout);
    configureForUser();
    updateList();
  }

  private void configureForUser() {
    // If the user is a player but not an admin or booker, default to their wrestler
    if (securityUtils.isPlayer() && !securityUtils.isAdmin() && !securityUtils.isBooker()) {
      securityUtils
          .getAuthenticatedUser()
          .ifPresent(
              user -> {
                Wrestler wrestler = user.getWrestler();
                if (wrestler != null) {
                  targetFilter.setValue(java.util.Set.of(wrestler));
                  targetFilter.setReadOnly(true);
                }
              });
    }
  }

  private HorizontalLayout getToolbar() {
    targetFilter.setItems(wrestlerRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
    targetFilter.setItemLabelGenerator(Wrestler::getName);
    targetFilter.addValueChangeListener(e -> updateList());
    targetFilter.setId("inbox-target-filter");

    readStatusFilter.setItems("All", "Read", "Unread");
    readStatusFilter.setValue("All");
    readStatusFilter.addValueChangeListener(e -> updateList());

    hideReadCheckbox.addValueChangeListener(e -> updateList());

    List<String> eventTypes =
        eventTypeRegistry.getEventTypes().stream()
            .map(Object::toString)
            .sorted()
            .collect(Collectors.toList());
    eventTypes.addFirst("All");
    eventTypeFilter.setItems(eventTypes);
    eventTypeFilter.setPlaceholder("All");
    eventTypeFilter.setValue("All");
    eventTypeFilter.setId("event-type-filter");
    eventTypeFilter.addValueChangeListener(e -> updateList());

    selectAllCheckbox.setId("select-all-checkbox");
    selectAllCheckbox.addValueChangeListener(
        event -> {
          if (event.getValue()) {
            ((GridMultiSelectionModel<InboxItem>) grid.getSelectionModel()).selectAll();
            selectedItems.addAll(grid.getDataProvider().fetch(new Query<>()).toList());
          } else {
            grid.getSelectionModel().deselectAll();
            selectedItems.clear();
          }
          updateSelectedButtonsState();
        });

    markSelectedReadButton.addClickListener(
        event -> {
          int selectedCount = selectedItems.size();
          inboxService.markSelectedAsRead(selectedItems);
          updateList();
          Notification.show(selectedCount + " items marked as read.");
          selectedItems.clear();
          grid.deselectAll();
          selectAllCheckbox.setValue(false);
          updateSelectedButtonsState();
        });

    markSelectedUnreadButton.addClickListener(
        event -> {
          int selectedCount = selectedItems.size();
          inboxService.markSelectedAsUnread(selectedItems);
          updateList();
          Notification.show(selectedCount + " items marked as unread.");
          selectedItems.clear();
          grid.deselectAll();
          selectAllCheckbox.setValue(false);
          updateSelectedButtonsState();
        });

    deleteSelectedButton.addClickListener(
        event -> {
          int selectedCount = selectedItems.size();
          inboxService.deleteSelected(selectedItems);
          updateList();
          Notification.show(selectedCount + " items deleted.")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          selectedItems.clear();
          grid.deselectAll();
          selectAllCheckbox.setValue(false);
          updateSelectedButtonsState();
        });

    markSelectedReadButton.setVisible(securityUtils.canEdit());
    markSelectedUnreadButton.setVisible(securityUtils.canEdit());
    deleteSelectedButton.setVisible(securityUtils.canDelete());
    selectAllCheckbox.setVisible(securityUtils.canEdit());
    updateSelectedButtonsState();

    Button clearTargetFilter = new Button("Clear");
    clearTargetFilter.addClickListener(
        event -> {
          targetFilter.clear();
          updateList();
        });
    // Hide clear target filter button if the target filter is read-only
    clearTargetFilter.setVisible(securityUtils.canEdit() && !targetFilter.isReadOnly());
    HorizontalLayout toolbar =
        new HorizontalLayout(
            targetFilter,
            clearTargetFilter,
            readStatusFilter,
            eventTypeFilter,
            hideReadCheckbox,
            selectAllCheckbox,
            markSelectedReadButton,
            markSelectedUnreadButton,
            deleteSelectedButton);
    toolbar.addClassName("toolbar");
    return toolbar;
  }

  private void updateSelectedButtonsState() {
    boolean hasSelection = !selectedItems.isEmpty();
    markSelectedReadButton.setEnabled(hasSelection);
    markSelectedUnreadButton.setEnabled(hasSelection);
    deleteSelectedButton.setEnabled(hasSelection);
    selectAllCheckbox.setValue(
        selectedItems.size() == grid.getDataProvider().size(new Query<>())
            && grid.getDataProvider().size(new Query<>()) > 0);
  }

  private Component createUrgencyBadge(@NonNull final InboxItem item) {
    InboxItem.Urgency urgency = item.getUrgency();
    if (urgency == null || urgency == InboxItem.Urgency.INFO) {
      return new Span();
    }
    Span badge =
        new Span(urgency == InboxItem.Urgency.ACTION_REQUIRED ? "Action Required" : "Warning");
    badge.getElement().getThemeList().add("badge");
    if (urgency == InboxItem.Urgency.ACTION_REQUIRED) {
      badge.getElement().getThemeList().add("error");
    } else {
      badge.getElement().getThemeList().add("contrast");
    }
    return badge;
  }

  private String getSubjectDisplay(@NonNull final InboxItem item) {
    if (item.getSubject() != null && !item.getSubject().isBlank()) {
      return item.getSubject();
    }
    String desc = item.getDescription();
    if (desc == null) {
      return "";
    }
    return desc.length() > 80 ? desc.substring(0, 80) : desc;
  }

  private void configureGrid() {
    grid.removeAllColumns();
    grid.setId("inbox-grid");
    grid.addClassName("inbox-grid");
    grid.setSizeFull();
    grid.addColumn(InboxItem::getEventType).setHeader("Event Type").setId("event-type-column");
    grid.addComponentColumn(this::createUrgencyBadge)
        .setHeader("Priority")
        .setId("priority-column");
    grid.addColumn(this::getSubjectDisplay).setHeader("Subject").setId("subject-column");
    grid.addColumn(InboxItem::getEventTimestamp)
        .setHeader("Event Timestamp")
        .setId("timestamp-column");
    grid.addColumn(this::getTargetNames).setHeader("Targets").setId("targets-column");
    grid.addComponentColumn(this::createActionComponent)
        .setHeader("Actions")
        .setId("actions-column");
    grid.getColumns().forEach(col -> col.setAutoWidth(true));
    grid.setSelectionMode(Grid.SelectionMode.MULTI);

    grid.asMultiSelect()
        .addValueChangeListener(
            event -> {
              selectedItems.clear();
              selectedItems.addAll(event.getValue());
              updateSelectedButtonsState();
            });

    grid.addItemClickListener(event -> showDetails(event.getItem()));
  }

  private Component createActionComponent(@NonNull final InboxItem item) {
    HorizontalLayout layout = new HorizontalLayout();
    layout.setPadding(false);
    layout.setSpacing(true);

    if (item.getActionType() != null) {
      switch (item.getActionType()) {
        case "MATCH_REPORT" -> layout.add(createMatchReportButton(item));
        case "NAVIGATE" -> createNavigateButton(item).ifPresent(layout::add);
        case "OPEN_DRAWER" -> layout.add(createOpenDrawerButton(item));
        default -> {
          // Unknown action type — no extra button rendered
        }
      }
    } else if ("MATCH_REQUEST".equals(item.getEventType().getName())) {
      // Backward-compat: old items without actionType but with MATCH_REQUEST event type
      layout.add(createMatchReportButton(item));
    }

    layout.add(createReadToggleButton(item));
    return layout;
  }

  private Button createMatchReportButton(@NonNull final InboxItem item) {
    Button reportButton =
        new Button(
            "Report Result",
            e -> {
              // Find fulfillment ID from targets
              item.getTargets().stream()
                  .filter(
                      t ->
                          t.getTargetType()
                              == com.github.javydreamercsw.management.domain.inbox.InboxItemTarget
                                  .TargetType.MATCH_FULFILLMENT)
                  .findFirst()
                  .ifPresent(
                      target -> {
                        try {
                          matchFulfillmentService
                              .getFulfillmentWithDetails(Long.parseLong(target.getTargetId()))
                              .ifPresent(
                                  f ->
                                      new MatchReportDialog(
                                              matchFulfillmentService,
                                              f,
                                              securityUtils,
                                              this::updateList)
                                          .open());
                        } catch (NumberFormatException ex) {
                          // Ignore
                        }
                      });
            });
    reportButton.setId("report-result-btn-" + item.getId());
    reportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    return reportButton;
  }

  private Optional<Button> createNavigateButton(@NonNull final InboxItem item) {
    if (item.getActionPayload() == null) {
      return Optional.empty();
    }
    try {
      java.util.Map<String, String> payload =
          objectMapper.readValue(item.getActionPayload(), new TypeReference<>() {});
      String route = payload.get("route");
      if (route == null || route.isBlank()) {
        return Optional.empty();
      }
      Button button = new Button("View", e -> UI.getCurrent().navigate(route));
      button.setId("navigate-btn-" + item.getId());
      button.addThemeVariants(ButtonVariant.LUMO_SMALL);
      return Optional.of(button);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Button createOpenDrawerButton(@NonNull final InboxItem item) {
    Button button = new Button("Show me", e -> openProfileDrawerBroadcaster.broadcast());
    button.setId("open-drawer-btn-" + item.getId());
    button.addThemeVariants(ButtonVariant.LUMO_SMALL);
    return button;
  }

  private String getTargetNames(@NonNull final InboxItem item) {
    if (item.getTargets() == null || item.getTargets().isEmpty()) {
      // This is for backward compatibility with old data that had the target in the
      // description.
      Pattern pattern = Pattern.compile("(\\d+)$"); // NOSONAR
      Matcher matcher = pattern.matcher(item.getDescription());
      if (matcher.find()) {
        String id = matcher.group(1);
        try {
          Optional<Wrestler> w = wrestlerRepository.findById(Long.parseLong(id));
          if (w.isPresent()) {
            return w.get().getName();
          }
        } catch (NumberFormatException e) {
          // Ignore, not a number
        }
      }
      return "N/A";
    }
    return item.getTargets().stream()
        .map(
            target -> {
              try {
                Long id = Long.parseLong(target.getTargetId());
                switch (target.getTargetType()) {
                  case ACCOUNT:
                    return inboxService
                        .getAccountRepository()
                        .findById(id)
                        .map(com.github.javydreamercsw.base.domain.account.Account::getUsername)
                        .orElse("Unknown Account (" + id + ")");
                  case WRESTLER:
                    return wrestlerRepository
                        .findById(id)
                        .map(Wrestler::getName)
                        .orElse("Unknown Wrestler (" + id + ")");
                  case MATCH_FULFILLMENT:
                    return "Match Fulfillment (" + id + ")";
                  default:
                    return "Unknown (" + id + ")";
                }
              } catch (NumberFormatException e) {
                return target.getTargetId();
              }
            })
        .collect(Collectors.joining(", "));
  }

  private void showEmptyDetail() {
    detailsView.removeAll();
    Paragraph empty = new Paragraph("Select a message to read it.");
    empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
    detailsView.setAlignItems(Alignment.CENTER);
    detailsView.setJustifyContentMode(JustifyContentMode.CENTER);
    detailsView.setSizeFull();
    detailsView.add(empty);
  }

  private void showDetails(final InboxItem item) {
    if (item == null) {
      showEmptyDetail();
      return;
    }

    // Auto-mark as read when the detail pane opens
    if (!item.isRead()) {
      inboxService.toggleReadStatus(item);
      updateList();
    }

    detailsView.removeAll();
    detailsView.setAlignItems(Alignment.START);
    detailsView.setJustifyContentMode(JustifyContentMode.START);
    detailsView.setSizeFull();
    detailsView.setPadding(true);
    detailsView.setSpacing(true);

    // Header row: event type badge + timestamp
    Span eventTypeBadge = new Span(item.getEventType().getFriendlyName());
    eventTypeBadge.getElement().getThemeList().add("badge");

    String formattedTimestamp = "";
    if (item.getEventTimestamp() != null) {
      DateTimeFormatter formatter =
          DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
              .withZone(ZoneId.systemDefault());
      formattedTimestamp = formatter.format(item.getEventTimestamp());
    }
    Span timestampSpan = new Span(formattedTimestamp);
    timestampSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
    timestampSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");

    HorizontalLayout headerRow = new HorizontalLayout(eventTypeBadge, timestampSpan);
    headerRow.setAlignItems(Alignment.BASELINE);
    headerRow.setSpacing(true);
    detailsView.add(headerRow);

    // Subject / title: first line of description (or full description if single-line)
    String subject = item.getDescription();
    if (subject != null && subject.contains("\n")) {
      subject = subject.lines().findFirst().orElse(subject);
    }
    H3 subjectHeading = new H3(subject != null ? subject : "(no subject)");
    subjectHeading.getStyle().set("margin-top", "0");
    detailsView.add(subjectHeading);

    // Body: full description with word-wrap
    if (item.getDescription() != null) {
      Paragraph body = new Paragraph(item.getDescription());
      body.getStyle().set("white-space", "pre-wrap");
      body.getStyle().set("word-break", "break-word");
      detailsView.add(body);
    }

    // Targets section rendered as badge chips
    if (item.getTargets() != null && !item.getTargets().isEmpty()) {
      HorizontalLayout targetsRow = new HorizontalLayout();
      targetsRow.setSpacing(true);
      targetsRow.setAlignItems(Alignment.CENTER);
      Span targetsLabel = new Span("Targets:");
      targetsLabel.getStyle().set("font-weight", "bold");
      targetsRow.add(targetsLabel);
      for (var target : item.getTargets()) {
        String targetName = resolveTargetName(target);
        Span chip = new Span(targetName);
        chip.getElement().getThemeList().add("badge contrast");
        targetsRow.add(chip);
      }
      detailsView.add(targetsRow);
    } else {
      // Backward-compat: try to resolve from description
      String legacyName = getTargetNames(item);
      if (!"N/A".equals(legacyName)) {
        HorizontalLayout targetsRow = new HorizontalLayout();
        targetsRow.setSpacing(true);
        targetsRow.setAlignItems(Alignment.CENTER);
        Span targetsLabel = new Span("Targets:");
        targetsLabel.getStyle().set("font-weight", "bold");
        targetsRow.add(targetsLabel);
        Span chip = new Span(legacyName);
        chip.getElement().getThemeList().add("badge contrast");
        targetsRow.add(chip);
        detailsView.add(targetsRow);
      }
    }

    // Actions area
    Component actions = createActionComponent(item);
    detailsView.add(actions);
  }

  private String resolveTargetName(
      @NonNull final com.github.javydreamercsw.management.domain.inbox.InboxItemTarget target) {
    try {
      Long id = Long.parseLong(target.getTargetId());
      switch (target.getTargetType()) {
        case ACCOUNT:
          return inboxService
              .getAccountRepository()
              .findById(id)
              .map(com.github.javydreamercsw.base.domain.account.Account::getUsername)
              .orElse("Unknown Account (" + id + ")");
        case WRESTLER:
          return wrestlerRepository
              .findById(id)
              .map(Wrestler::getName)
              .orElse("Unknown Wrestler (" + id + ")");
        case MATCH_FULFILLMENT:
          return "Match Fulfillment (" + id + ")";
        default:
          return target.getTargetType().name() + " (" + id + ")";
      }
    } catch (NumberFormatException e) {
      return target.getTargetId();
    }
  }

  private Button createReadToggleButton(@NonNull final InboxItem item) {
    Button button = new Button(item.isRead() ? "Mark as Unread" : "Mark as Read");
    button.addClickListener(
        click -> {
          inboxService.toggleReadStatus(item);
          updateList();
        });
    button.setVisible(securityUtils.canEdit(item));
    return button;
  }

  private void updateList() {
    Long accountId =
        securityUtils.getAuthenticatedUser().map(u -> u.getAccount().getId()).orElse(null);
    grid.setItems(
        inboxService.search(
            targetFilter.getValue(),
            readStatusFilter.getValue(),
            eventTypeFilter.getValue(),
            hideReadCheckbox.getValue(),
            accountId));
  }
}
