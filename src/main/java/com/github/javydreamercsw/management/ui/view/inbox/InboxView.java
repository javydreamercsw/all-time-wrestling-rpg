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

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

@Route(value = "inbox", layout = MainLayout.class)
@PageTitle("Inbox")
public class InboxView extends VerticalLayout {

  private final InboxService inboxService;
  private final InboxEventTypeRegistry eventTypeRegistry;
  private final WrestlerRepository wrestlerRepository;
  private final Grid<InboxItem> grid = new Grid<>(InboxItem.class);
  private final MultiSelectComboBox<Wrestler> targetFilter = new MultiSelectComboBox<>("Targets");
  private final ComboBox<String> readStatusFilter = new ComboBox<>("Read Status");
  private final ComboBox<String> eventTypeFilter = new ComboBox<>("Event Type");
  private final Div detailsView = new Div();
  private final Checkbox selectAllCheckbox = new Checkbox("Select All");
  private final Button markSelectedReadButton = new Button("Mark Selected as Read");
  private final Button markSelectedUnreadButton = new Button("Mark Selected as Unread");
  private final Checkbox hideReadCheckbox = new Checkbox("Hide Read");
  private final Button deleteSelectedButton = new Button("Delete Selected");
  private final Set<InboxItem> selectedItems = new HashSet<>();

  public InboxView(
      InboxService inboxService,
      InboxEventTypeRegistry eventTypeRegistry,
      WrestlerRepository wrestlerRepository) {
    this.inboxService = inboxService;
    this.eventTypeRegistry = eventTypeRegistry;
    this.wrestlerRepository = wrestlerRepository;

    addClassName("inbox-view");
    setSizeFull();
    configureGrid();

    SplitLayout splitLayout = new SplitLayout();
    splitLayout.setSizeFull();
    splitLayout.addToPrimary(new VerticalLayout(getToolbar(), grid));
    splitLayout.addToSecondary(detailsView);

    add(splitLayout);
    updateList();
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
        new ArrayList<>(
            eventTypeRegistry.getEventTypes().stream()
                .map(Object::toString)
                .sorted()
                .collect(Collectors.toList()));
    eventTypes.add(0, "All");
    eventTypeFilter.setItems(eventTypes);
    eventTypeFilter.setPlaceholder("All");
    eventTypeFilter.setValue("All");
    eventTypeFilter.addValueChangeListener(e -> updateList());

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

    updateSelectedButtonsState();

    Button clearTargetFilter = new Button("Clear");
    clearTargetFilter.addClickListener(
        event -> {
          targetFilter.clear();
          updateList();
        });
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

  private void configureGrid() {
    grid.addClassName("inbox-grid");
    grid.setSizeFull();
    grid.setColumns("eventType", "description", "eventTimestamp");
    grid.addColumn(this::getTargetNames).setHeader("Targets");
    grid.addComponentColumn(this::createReadToggleButton).setHeader("Mark as Read/Unread");
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

  private String getTargetNames(InboxItem item) {
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
            target ->
                wrestlerRepository
                    .findById(Long.parseLong(target.getTargetId()))
                    .map(Wrestler::getName)
                    .orElse("Unknown"))
        .collect(Collectors.joining(", "));
  }

  private void showDetails(InboxItem item) {
    if (item == null) {
      detailsView.setText("");
    } else {
      detailsView.setText("Details for: " + item.getDescription());
    }
  }

  private Button createReadToggleButton(InboxItem item) {
    Button button = new Button(item.isRead() ? "Mark as Unread" : "Mark as Read");
    button.addClickListener(
        click -> {
          inboxService.toggleReadStatus(item);
          updateList();
        });
    return button;
  }

  private void updateList() {
    grid.setItems(
        inboxService.search(
            targetFilter.getValue(),
            readStatusFilter.getValue(),
            eventTypeFilter.getValue(),
            hideReadCheckbox.getValue()));
  }
}
