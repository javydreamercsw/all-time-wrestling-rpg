package com.github.javydreamercsw.management.ui.view.inbox;

import com.github.javydreamercsw.base.ui.view.MainLayout;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "inbox", layout = MainLayout.class)
@PageTitle("Inbox")
public class InboxView extends VerticalLayout {

  private final InboxService inboxService;
  private final Grid<InboxItem> grid = new Grid<>(InboxItem.class);
  private final TextField filterText = new TextField();
  private final ComboBox<String> readStatusFilter = new ComboBox<>("Read Status");
  private final ComboBox<String> eventTypeFilter = new ComboBox<>("Event Type");
  private final Div detailsView = new Div();
  private final Checkbox selectAllCheckbox = new Checkbox("Select All");
  private final Button markSelectedReadButton = new Button("Mark Selected as Read");
  private final Button markSelectedUnreadButton = new Button("Mark Selected as Unread");
  private final Checkbox hideReadCheckbox = new Checkbox("Hide Read");
  private final Button deleteSelectedButton = new Button("Delete Selected");
  private final Set<InboxItem> selectedItems = new HashSet<>();

  public InboxView(InboxService inboxService) {
    this.inboxService = inboxService;

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
    filterText.setPlaceholder("Filter by description...");
    filterText.setClearButtonVisible(true);
    filterText.setValueChangeMode(ValueChangeMode.LAZY);
    filterText.addValueChangeListener(e -> updateList());

    readStatusFilter.setItems("All", "Read", "Unread");
    readStatusFilter.setValue("All");
    readStatusFilter.addValueChangeListener(e -> updateList());

    hideReadCheckbox.addValueChangeListener(e -> updateList());

    eventTypeFilter.setItems(
        "All",
        "Rivalry Heat Change",
        "Faction Rivalry Heat Change",
        "Feud Heat Change",
        "Adjudication Completed");
    eventTypeFilter.setValue("All");
    eventTypeFilter.addValueChangeListener(e -> updateList());

    selectAllCheckbox.addValueChangeListener(
        event -> {
          if (event.getValue()) {
            ((GridMultiSelectionModel<InboxItem>) grid.getSelectionModel()).selectAll();
            selectedItems.addAll(
                grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList()));
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
          Notification.show(selectedCount + " items deleted.");
          selectedItems.clear();
          grid.deselectAll();
          selectAllCheckbox.setValue(false);
          updateSelectedButtonsState();
        });

    updateSelectedButtonsState();

    HorizontalLayout toolbar =
        new HorizontalLayout(
            filterText,
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
    grid.addComponentColumn(item -> createReadToggleButton(item)).setHeader("Mark as Read/Unread");
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
            filterText.getValue(),
            readStatusFilter.getValue(),
            eventTypeFilter.getValue(),
            hideReadCheckbox.getValue()));
  }
}
