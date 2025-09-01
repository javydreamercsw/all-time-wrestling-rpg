package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.NonNull;
import org.vaadin.stefan.fullcalendar.Entry;
import org.vaadin.stefan.fullcalendar.FullCalendar;
import org.vaadin.stefan.fullcalendar.FullCalendarBuilder;

/**
 * Calendar view for displaying scheduled shows in the ATW RPG system. Provides a monthly calendar
 * interface with show details and navigation.
 */
@Route("show-calendar")
@PageTitle("Show Calendar")
@Menu(order = 4, icon = "vaadin:calendar", title = "Show Calendar")
@PermitAll
public class ShowCalendarView extends Main implements BeforeEnterObserver {

  private final ShowService showService;
  private final DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");

  private FullCalendar calendar;
  private VerticalLayout upcomingShowsPanel;
  private YearMonth currentYearMonth;
  private Select<Integer> yearSelect;
  private Select<String> monthSelect;
  private H5 currentDateLabel;

  public FullCalendar getCalendar() {
    return calendar;
  }

  public VerticalLayout getUpcomingShowsPanel() {
    return upcomingShowsPanel;
  }

  public ShowCalendarView(@NonNull ShowService showService) {
    this.showService = showService;
    this.currentYearMonth = YearMonth.now();

    initializeComponents();
    setupLayout();
    loadShowsIntoCalendar();
    refreshUpcomingShows();
  }

  @Override
  public void beforeEnter(@NonNull BeforeEnterEvent event) {
    // Check for date parameter in URL
    String dateParam =
        event
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .getOrDefault("date", java.util.List.of())
            .stream()
            .findFirst()
            .orElse(null);

    if (dateParam != null) {
      try {
        LocalDate targetDate = LocalDate.parse(dateParam);
        YearMonth targetYearMonth = YearMonth.from(targetDate);
        currentYearMonth = targetYearMonth;

        // Schedule update after component is fully attached and rendered
        updateCalendarAndControls();
      } catch (Exception e) {
        // Invalid date parameter, ignore and use current date
      }
    }
  }

  private void initializeComponents() {
    // Create navigation controls
    createNavigationControls();

    // Create FullCalendar
    calendar = FullCalendarBuilder.create().withAutoBrowserLocale().build();
    calendar.setSizeFull();
    calendar.setHeight("600px");

    // Add click listener for calendar entries
    calendar.addEntryClickedListener(
        event -> {
          Entry entry = event.getEntry();
          if (entry.getCustomProperty("showId") != null) {
            Long showId = Long.valueOf(entry.getCustomProperty("showId").toString());

            // Navigate to the show's date first
            if (entry.getStart() != null) {
              LocalDate showDate = entry.getStart().toLocalDate();
              YearMonth showYearMonth = YearMonth.from(showDate);
              currentYearMonth = showYearMonth;
              updateCalendarAndControls();
            }

            // Then navigate to show detail
            getUI().ifPresent(ui -> ui.navigate("show-detail/" + showId + "?ref=calendar"));
          }
        });

    // Upcoming shows panel
    upcomingShowsPanel = new VerticalLayout();
    upcomingShowsPanel.addClassNames(
        LumoUtility.Padding.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM);
    upcomingShowsPanel.setWidth("300px");

    // Main layout
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    // Create navigation toolbar
    HorizontalLayout navigationLayout = createNavigationToolbar();
    add(new ViewToolbar("Show Calendar", ViewToolbar.group(navigationLayout)));
  }

  private void createNavigationControls() {
    // Initialize year select
    yearSelect = new Select<>();
    yearSelect.setItems(2020, 2021, 2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029, 2030);
    yearSelect.setValue(currentYearMonth.getYear());
    yearSelect.addValueChangeListener(
        e -> navigateToYearMonth(e.getValue(), currentYearMonth.getMonthValue()));

    // Initialize month select
    monthSelect = new Select<>();
    monthSelect.setItems(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December");
    monthSelect.setValue(
        currentYearMonth.getMonth().name().substring(0, 1).toUpperCase()
            + currentYearMonth.getMonth().name().substring(1).toLowerCase());
    monthSelect.addValueChangeListener(
        e -> {
          int monthNumber = getMonthNumber(e.getValue());
          navigateToYearMonth(currentYearMonth.getYear(), monthNumber);
        });

    // Current date label
    currentDateLabel = new H5(currentYearMonth.format(monthYearFormatter));
    currentDateLabel.addClassNames(LumoUtility.Margin.NONE);
  }

  private HorizontalLayout createNavigationToolbar() {
    // Previous month button
    Button prevBtn = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    prevBtn.addClickListener(e -> navigateToPreviousMonth());

    // Next month button
    Button nextBtn = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    nextBtn.addClickListener(e -> navigateToNextMonth());

    // Today button
    Button todayBtn = new Button("Today");
    todayBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    todayBtn.addClickListener(e -> navigateToToday());

    HorizontalLayout navigation = new HorizontalLayout();
    navigation.setAlignItems(FlexComponent.Alignment.CENTER);
    navigation.setSpacing(true);
    navigation.add(prevBtn, currentDateLabel, nextBtn, yearSelect, monthSelect, todayBtn);

    return navigation;
  }

  private void navigateToPreviousMonth() {
    currentYearMonth = currentYearMonth.minusMonths(1);
    updateCalendarAndControls();
  }

  private void navigateToNextMonth() {
    currentYearMonth = currentYearMonth.plusMonths(1);
    updateCalendarAndControls();
  }

  private void navigateToToday() {
    currentYearMonth = YearMonth.now();
    updateCalendarAndControls();
  }

  private void navigateToYearMonth(int year, int month) {
    currentYearMonth = YearMonth.of(year, month);
    updateCalendarAndControls();
  }

  private void updateCalendarAndControls() {
    // Update the label
    currentDateLabel.setText(currentYearMonth.format(monthYearFormatter));

    // Update the select components
    yearSelect.setValue(currentYearMonth.getYear());
    monthSelect.setValue(
        currentYearMonth.getMonth().name().substring(0, 1).toUpperCase()
            + currentYearMonth.getMonth().name().substring(1).toLowerCase());

    // Navigate the calendar to the specific date
    LocalDate targetDate = currentYearMonth.atDay(1);
    calendar.gotoDate(targetDate);

    // Refresh the calendar to reload shows for the new date range
    loadShowsIntoCalendar();
  }

  private int getMonthNumber(String monthName) {
    return switch (monthName) {
      case "January" -> 1;
      case "February" -> 2;
      case "March" -> 3;
      case "April" -> 4;
      case "May" -> 5;
      case "June" -> 6;
      case "July" -> 7;
      case "August" -> 8;
      case "September" -> 9;
      case "October" -> 10;
      case "November" -> 11;
      case "December" -> 12;
      default -> 1;
    };
  }

  private void loadShowsIntoCalendar() {
    // Get all shows with dates
    List<Show> allShows =
        showService.findAllWithRelationships().stream()
            .filter(show -> show.getShowDate() != null)
            .toList();

    // Clear existing entries
    calendar.getEntryProvider().asInMemory().removeAllEntries();

    // Add shows as calendar entries
    for (Show show : allShows) {
      Entry entry = new Entry();
      entry.setTitle(show.getName());
      entry.setStart(show.getShowDate().atStartOfDay());
      entry.setEnd(show.getShowDate().atTime(23, 59, 59));
      entry.setAllDay(true);

      // Set color based on show type
      if (show.isPremiumLiveEvent()) {
        entry.setColor("#dc2626"); // Red for PLEs
      } else if (show.isWeeklyShow()) {
        entry.setColor("#2563eb"); // Blue for Weekly shows
      } else {
        entry.setColor("#8A2BE2"); // Purple for other shows
      }

      // Store show ID for navigation
      entry.setCustomProperty("showId", show.getId().toString());

      calendar.getEntryProvider().asInMemory().addEntries(entry);
    }

    // Refresh the calendar to show the entries
    calendar.getEntryProvider().refreshAll();
  }

  private void setupLayout() {
    setSizeFull(); // Make the main view use full screen
    addClassNames(LumoUtility.Padding.MEDIUM);

    HorizontalLayout mainContent = new HorizontalLayout();
    mainContent.setSizeFull();
    mainContent.setSpacing(true);
    mainContent.addClassNames(LumoUtility.Width.FULL);

    VerticalLayout calendarContainer = new VerticalLayout();
    calendarContainer.setSizeFull();
    calendarContainer.setSpacing(false);
    calendarContainer.setPadding(false);
    calendarContainer.addClassNames(LumoUtility.Width.FULL);
    calendarContainer.add(calendar);

    // Make calendar take up most of the space
    mainContent.add(calendarContainer, upcomingShowsPanel);
    mainContent.setFlexGrow(3, calendarContainer); // Give calendar more space
    mainContent.setFlexGrow(0, upcomingShowsPanel); // Keep sidebar fixed

    // Ensure sidebar has a reasonable width
    upcomingShowsPanel.setWidth("350px");

    add(mainContent);
  }

  private void refreshUpcomingShows() {
    upcomingShowsPanel.removeAll();

    H4 upcomingTitle = new H4("Upcoming Shows");
    upcomingTitle.addClassNames(LumoUtility.Margin.NONE);
    upcomingShowsPanel.add(upcomingTitle);

    List<Show> upcomingShows = showService.getUpcomingShowsWithRelationships(10);

    if (upcomingShows.isEmpty()) {
      Span noShows = new Span("No upcoming shows scheduled");
      noShows.addClassNames(LumoUtility.TextColor.SECONDARY);
      upcomingShowsPanel.add(noShows);
    } else {
      for (Show show : upcomingShows) {
        Div showItem = createUpcomingShowItem(show);
        upcomingShowsPanel.add(showItem);
      }
    }
  }

  private Div createUpcomingShowItem(@NonNull Show show) {
    Div showItem = new Div();
    showItem.addClassNames(
        LumoUtility.Padding.SMALL,
        LumoUtility.Margin.Bottom.SMALL,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.SMALL);
    showItem.getStyle().set("cursor", "pointer");
    showItem.addClickListener(
        e -> {
          // Navigate to the show's date first if it has a date
          if (show.getShowDate() != null) {
            YearMonth showYearMonth = YearMonth.from(show.getShowDate());
            currentYearMonth = showYearMonth;
            updateCalendarAndControls();
          }

          // Then navigate to show detail
          getUI().ifPresent(ui -> ui.navigate("show-detail/" + show.getId() + "?ref=calendar"));
        });

    if (!show.isPremiumLiveEvent() && !show.isWeeklyShow()) {
      showItem.getStyle().set("border-color", "#8A2BE2");
      showItem.getStyle().set("border-width", "2px");
    }

    Span showName = new Span(show.getName());
    showName.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

    Span showDate =
        new Span(
            show.getShowDate() != null
                ? show.getShowDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                : "Date TBD");
    showDate.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

    Span showType = new Span(show.getType().getName());
    showType.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.XSMALL);

    VerticalLayout content = new VerticalLayout(showName, showDate, showType);
    content.setSpacing(false);
    content.setPadding(false);

    showItem.add(content);
    return showItem;
  }
}
