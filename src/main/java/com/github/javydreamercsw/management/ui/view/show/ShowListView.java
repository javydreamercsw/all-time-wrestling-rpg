package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Main;
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
import jakarta.annotation.security.PermitAll;
import java.time.Clock;
import java.util.List;

@Route("show-list")
@PageTitle("Show List")
@Menu(order = 3, icon = "vaadin:clipboard-check", title = "Show List")
@PermitAll
public class ShowListView extends Main {

  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private ComboBox<ShowType> editType;
  private Show editingShow;

  final TextField name;
  final Button createBtn;
  final Grid<Show> showGrid;

  public ShowListView(ShowService showService, ShowTypeService showTypeService, Clock clock) {
    this.showService = showService;
    this.showTypeService = showTypeService;

    name = new TextField();
    name.setPlaceholder("What do you want the show name to be?");
    name.setAriaLabel("Show Name");
    name.setMaxLength(255);
    name.setMinWidth("20em");

    createBtn = new Button("Create", event -> createShow());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout formLayout = new HorizontalLayout(name, createBtn);
    formLayout.setSpacing(true);

    showGrid = new Grid<>(Show.class, false);
    showGrid.addColumn(Show::getName).setHeader("Name").setSortable(true);
    showGrid
        .addColumn(show -> show.getType() != null ? show.getType().getName() : "")
        .setHeader("Type")
        .setSortable(true);
    showGrid.addColumn(Show::getDescription).setHeader("Description");
    showGrid.setSizeFull();

    showGrid
        .addComponentColumn(
            show -> {
              Button editBtn = new Button("Edit", e -> openEditDialog(show));
              editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
              return editBtn;
            })
        .setHeader("Actions");

    // Editor setup (optional, as in your previous code)
    Editor<Show> editor = showGrid.getEditor();
    Binder<Show> binder = new Binder<>(Show.class);
    editor.setBinder(binder);

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    // Toolbar and form in a header row
    add(new ViewToolbar("Show List", ViewToolbar.group(formLayout)));
    // Grid fills the rest
    add(showGrid);

    refreshGrid();
  }

  private void createShow() {
    String showName = name.getValue().trim();
    if (showName.isEmpty()) {
      Notification.show("Show name cannot be empty.", 3_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }
    if (showService.findByName(showName).isPresent()) {
      Notification.show("Show with this name already exists.", 3_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }
    Show show = new Show();
    show.setName(showName);
    showService.save(show);
    name.clear();
    refreshGrid();
    Notification.show("Show created.", 3_000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void refreshGrid() {
    List<Show> shows = showService.findAll();
    showGrid.setItems(shows);
  }

  private void setupEditDialog() {
    editDialog = new Dialog();
    editDialog.setHeaderTitle("Edit Show");
    editDialog.setWidth("500px");
    editDialog.setMaxWidth("90vw");

    editName = new TextField("Name");
    editName.setWidthFull();
    editDescription = new TextArea("Description");
    editDescription.setWidthFull();
    editType = new ComboBox<>("Type", showTypeService.findAll());
    editType.setItemLabelGenerator(ShowType::getName);
    editType.setWidthFull();

    Button saveBtn = new Button("Save", e -> saveEdit());
    Button cancelBtn = new Button("Cancel", e -> editDialog.close());

    VerticalLayout formLayout = new VerticalLayout(editName, editDescription, editType);
    formLayout.setWidthFull();
    formLayout.setSpacing(true);

    HorizontalLayout buttonLayout = new HorizontalLayout(saveBtn, cancelBtn);
    buttonLayout.setWidthFull();
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    editDialog.add(formLayout, buttonLayout);
  }

  private void openEditDialog(Show show) {
    if (editDialog == null) setupEditDialog();
    editingShow = show;
    editName.setValue(show.getName() != null ? show.getName() : "");
    editDescription.setValue(show.getDescription() != null ? show.getDescription() : "");
    editType.setValue(show.getType());
    editDialog.open();
  }

  private void saveEdit() {
    editingShow.setName(editName.getValue());
    editingShow.setDescription(editDescription.getValue());
    editingShow.setType(editType.getValue());
    showService.save(editingShow);
    editDialog.close();
    refreshGrid();
    Notification.show("Show updated.", 3_000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
