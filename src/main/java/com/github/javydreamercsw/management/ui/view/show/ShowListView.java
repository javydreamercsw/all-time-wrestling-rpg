package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Route("show-list")
@PageTitle("Show List")
@Menu(order = 3, icon = "vaadin:calendar-o", title = "Show List")
@PermitAll
public class ShowListView extends Main {

  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;
  private final Clock clock; // Add this field

  private ComboBox<Season> newSeason; // New field
  private ComboBox<ShowTemplate> newTemplate; // New field

  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private ComboBox<ShowType> editType;
  private ComboBox<Season> editSeason;
  private ComboBox<ShowTemplate> editTemplate;
  private DatePicker editShowDate;
  private Show editingShow;

  final TextField name;
  private ComboBox<ShowType> newShowType;
  private DatePicker newShowDate;
  final Button createBtn;
  final Grid<Show> showGrid;

  public ShowListView(
      ShowService showService,
      ShowTypeService showTypeService,
      SeasonService seasonService,
      ShowTemplateService showTemplateService,
      Clock clock) {
    this.showService = showService;
    this.showTypeService = showTypeService;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.clock =
        (clock != null) ? clock : Clock.systemDefaultZone(); // Assign clock here, with fallback

    name = new TextField();
    name.setPlaceholder("What do you want the show name to be?");
    name.setAriaLabel("Show Name");
    name.setMaxLength(255);
    name.setMinWidth("20em");

    newShowType = new ComboBox<>("Show Type");
    newShowType.setItems(showTypeService.findAll());
    newShowType.setItemLabelGenerator(ShowType::getName);
    newShowType.setRequired(true);
    newShowType.setPlaceholder("Select a type");

    newSeason = new ComboBox<>("Season");
    Page<Season> seasonsPage = seasonService.getAllSeasons(Pageable.unpaged());
    if (seasonsPage != null) {
      newSeason.setItems(seasonsPage.getContent());
    }
    newSeason.setItemLabelGenerator(Season::getName);
    newSeason.setClearButtonVisible(true);
    newSeason.setPlaceholder("Select a season (optional)");

    newTemplate = new ComboBox<>("Template");
    newTemplate.setItems(showTemplateService.findAll());
    newTemplate.setItemLabelGenerator(ShowTemplate::getName);
    newTemplate.setClearButtonVisible(true);
    newTemplate.setPlaceholder("Select a template (optional)");

    newShowDate = new DatePicker("Show Date");
    newShowDate.setPlaceholder("Select date (optional)");
    newShowDate.setClearButtonVisible(true);

    createBtn = new Button("Create", event -> createShow());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout formLayout =
        new HorizontalLayout(name, newShowType, newSeason, newTemplate, newShowDate, createBtn);
    formLayout.setSpacing(true);
    formLayout.setAlignItems(FlexComponent.Alignment.BASELINE); // Align items nicely

    showGrid = new Grid<>(Show.class, false);
    showGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

    showGrid.setPartNameGenerator(
        show -> {
          if (!show.isPremiumLiveEvent() && !show.isWeeklyShow()) {
            return "ple-show";
          } else {
            return null;
          }
        });

    // Name column with link to detail view
    showGrid
        .addComponentColumn(
            show -> {
              Button nameButton = new Button(show.getName());
              nameButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
              nameButton.addClickListener(
                  e ->
                      getUI()
                          .ifPresent(
                              ui -> ui.navigate("show-detail/" + show.getId() + "?ref=shows")));
              return nameButton;
            })
        .setHeader("Name")
        .setSortable(true)
        .setFlexGrow(2);

    // Show date column
    showGrid
        .addColumn(
            show ->
                show.getShowDate() != null
                    ? show.getShowDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    : "Not scheduled")
        .setHeader("Date")
        .setSortable(true)
        .setFlexGrow(1);

    // Type column with color coding
    showGrid
        .addComponentColumn(
            show -> {
              if (show.getType() == null) return new Span("No Type");

              Span typeSpan = new Span(show.getType().getName());
              typeSpan.addClassNames(
                  LumoUtility.Padding.Horizontal.SMALL,
                  LumoUtility.Padding.Vertical.XSMALL,
                  LumoUtility.BorderRadius.SMALL,
                  LumoUtility.FontSize.SMALL,
                  LumoUtility.FontWeight.SEMIBOLD);

              if (show.isPremiumLiveEvent()) {
                typeSpan.addClassNames(
                    LumoUtility.Background.ERROR, LumoUtility.TextColor.ERROR_CONTRAST);
              } else if (show.isWeeklyShow()) {
                typeSpan.addClassNames(
                    LumoUtility.Background.PRIMARY, LumoUtility.TextColor.PRIMARY_CONTRAST);
              } else {
                typeSpan.getStyle().set("background-color", "#8A2BE2");
                typeSpan.getStyle().set("color", "white");
              }

              return typeSpan;
            })
        .setHeader("Type")
        .setSortable(true)
        .setFlexGrow(1);

    // Season column
    showGrid
        .addColumn(show -> show.getSeason() != null ? show.getSeason().getName() : "No Season")
        .setHeader("Season")
        .setSortable(true)
        .setFlexGrow(1);

    // Template column
    showGrid
        .addColumn(
            show -> show.getTemplate() != null ? show.getTemplate().getName() : "No Template")
        .setHeader("Template")
        .setSortable(true)
        .setFlexGrow(1);

    showGrid.setSizeFull();

    showGrid
        .addComponentColumn(
            show -> {
              HorizontalLayout actions = new HorizontalLayout();
              actions.setSpacing(true);

              // View details button
              Button viewBtn = new Button(new Icon(VaadinIcon.EYE));
              viewBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
              viewBtn.setTooltipText("View Details");
              viewBtn.addClickListener(
                  e ->
                      getUI()
                          .ifPresent(
                              ui -> ui.navigate("show-detail/" + show.getId() + "?ref=shows")));

              // Edit button
              Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
              editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
              editBtn.setTooltipText("Edit Show");
              editBtn.addClickListener(e -> openEditDialog(show));

              // Calendar button (if show has date)
              if (show.getShowDate() != null) {
                Button calendarBtn = new Button(new Icon(VaadinIcon.CALENDAR));
                calendarBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                calendarBtn.setTooltipText("View in Calendar");
                calendarBtn.addClickListener(
                    e -> {
                      // Navigate to calendar with date parameter
                      String dateParam =
                          show.getShowDate()
                              .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                      getUI().ifPresent(ui -> ui.navigate("show-calendar?date=" + dateParam));
                    });
                actions.add(calendarBtn);
              }

              actions.add(viewBtn, editBtn);
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(0);

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
    if (newShowType.getValue() == null) {
      Notification.show("Please select a Show Type.", 3_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }
    LocalDate selectedDate = newShowDate.getValue();
    if (showService.existsByNameAndShowDate(showName, selectedDate)) {
      Notification.show(
              "Show with this name and date already exists.", 3_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    showService.createShow(
        showName,
        "", // No description input in this form
        newShowType.getValue().getId(),
        selectedDate,
        newSeason.getValue() != null ? newSeason.getValue().getId() : null,
        newTemplate.getValue() != null ? newTemplate.getValue().getId() : null);
    name.clear();
    newShowType.clear();
    newSeason.clear();
    newTemplate.clear();
    newShowDate.setValue(LocalDate.now(this.clock)); // Reset to today
    refreshGrid();
    Notification.show("Show created.", 3_000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void refreshGrid() {
    List<Show> shows = showService.findAllWithRelationships();
    showGrid.setItems(shows);
  }

  private void setupEditDialog() {
    editDialog = new Dialog();
    editDialog.setHeaderTitle("Edit Show");
    editDialog.setWidth("600px");
    editDialog.setMaxWidth("90vw");
    editDialog.setMaxHeight("80vh");

    editName = new TextField("Name");
    editName.setWidthFull();

    editDescription = new TextArea("Description");
    editDescription.setWidthFull();
    editDescription.setHeight("100px");

    editType = new ComboBox<>("Type");
    editType.setItems(showTypeService.findAll());
    editType.setItemLabelGenerator(ShowType::getName);
    editType.setWidthFull();

    editSeason = new ComboBox<>("Season");
    editSeason.setItems(seasonService.getAllSeasons(Pageable.unpaged()).getContent());
    editSeason.setItemLabelGenerator(Season::getName);
    editSeason.setWidthFull();
    editSeason.setClearButtonVisible(true);

    editTemplate = new ComboBox<>("Template");
    editTemplate.setItems(showTemplateService.findAll());
    editTemplate.setItemLabelGenerator(ShowTemplate::getName);
    editTemplate.setWidthFull();
    editTemplate.setClearButtonVisible(true);

    editShowDate = new DatePicker("Show Date");
    editShowDate.setWidthFull();
    editShowDate.setClearButtonVisible(true);

    Button saveBtn = new Button("Save", e -> saveEdit());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancelBtn = new Button("Cancel", e -> editDialog.close());

    VerticalLayout formLayout =
        new VerticalLayout(
            editName, editDescription, editType, editSeason, editTemplate, editShowDate);
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
    editSeason.setValue(show.getSeason());
    editTemplate.setValue(show.getTemplate());
    editShowDate.setValue(show.getShowDate());
    editDialog.open();
  }

  private void saveEdit() {
    editingShow.setName(editName.getValue());
    editingShow.setDescription(editDescription.getValue());
    editingShow.setType(editType.getValue());
    editingShow.setSeason(editSeason.getValue());
    editingShow.setTemplate(editTemplate.getValue());
    editingShow.setShowDate(editShowDate.getValue());
    showService.save(editingShow);
    editDialog.close();
    refreshGrid();
    Notification.show("Show updated successfully.", 3_000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
