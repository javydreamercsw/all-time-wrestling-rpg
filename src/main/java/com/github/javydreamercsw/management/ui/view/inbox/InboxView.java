package com.github.javydreamercsw.management.ui.view.inbox;

import com.github.javydreamercsw.base.ui.view.MainLayout;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "inbox", layout = MainLayout.class)
@PageTitle("Inbox")
public class InboxView extends VerticalLayout {

  private final InboxService inboxService;
  private final Grid<InboxItem> grid = new Grid<>(InboxItem.class);
  private final TextField filterText = new TextField();
  private final ComboBox<String> readStatusFilter = new ComboBox<>("Read Status");
  private final ComboBox<String> eventTypeFilter = new ComboBox<>("Event Type");
  private final Div detailsView = new Div();

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

    eventTypeFilter.setItems(
        "All", "Rivalry Heat Change", "Faction Rivalry Heat Change", "Feud Heat Change");
    eventTypeFilter.setValue("All");
    eventTypeFilter.addValueChangeListener(e -> updateList());

    HorizontalLayout toolbar = new HorizontalLayout(filterText, readStatusFilter, eventTypeFilter);
    toolbar.addClassName("toolbar");
    return toolbar;
  }

  private void configureGrid() {
    grid.addClassName("inbox-grid");
    grid.setSizeFull();
    grid.setColumns("eventType", "description", "eventTimestamp", "read");
    grid.addComponentColumn(item -> createReadToggleButton(item)).setHeader("Mark as Read/Unread");
    grid.getColumns().forEach(col -> col.setAutoWidth(true));

    grid.asSingleSelect()
        .addValueChangeListener(
            event -> {
              showDetails(event.getValue());
            });
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
            filterText.getValue(), readStatusFilter.getValue(), eventTypeFilter.getValue()));
  }
}
