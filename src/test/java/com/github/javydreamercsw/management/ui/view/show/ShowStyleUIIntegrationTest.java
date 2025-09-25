package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinSession;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.vaadin.stefan.fullcalendar.Entry;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryQuery;

class ShowStyleUIIntegrationTest extends AbstractIntegrationTest {

  private Show pleShow;
  private Show weeklyShow;
  private Show otherShow;

  @BeforeEach
  void setUp() {
    showRepository.deleteAllInBatch();
    showTemplateRepository.deleteAllInBatch();
    showTypeRepository.deleteAllInBatch();

    ShowType pleType = new ShowType();
    pleType.setName("Premium Live Event (PLE)");
    pleType.setDescription("A major event");
    pleType = showTypeRepository.save(pleType);

    ShowType weeklyType = new ShowType();
    weeklyType.setName("Weekly");
    weeklyType.setDescription("A weekly show");
    weeklyType = showTypeRepository.save(weeklyType);

    ShowType otherType = new ShowType();
    otherType.setName("Special Event");
    otherType.setDescription("A special event");
    otherType = showTypeRepository.save(otherType);

    ShowTemplate pleTemplate = new ShowTemplate();
    pleTemplate.setName("PLE Template");
    pleTemplate.setShowType(pleType);
    pleTemplate = showTemplateRepository.save(pleTemplate);

    ShowTemplate weeklyTemplate = new ShowTemplate();
    weeklyTemplate.setName("Weekly Template");
    weeklyTemplate.setShowType(weeklyType);
    weeklyTemplate = showTemplateRepository.save(weeklyTemplate);

    ShowTemplate otherTemplate = new ShowTemplate();
    otherTemplate.setName("Other Template");
    otherTemplate.setShowType(otherType);
    otherTemplate = showTemplateRepository.save(otherTemplate);

    pleShow = new Show();
    pleShow.setName("My PLE Show");
    pleShow.setTemplate(pleTemplate);
    pleShow.setType(pleType);
    pleShow.setDescription("PLE Show Description");
    pleShow.setShowDate(LocalDate.now().plusDays(1));
    showRepository.save(pleShow);

    weeklyShow = new Show();
    weeklyShow.setName("My Weekly Show");
    weeklyShow.setTemplate(weeklyTemplate);
    weeklyShow.setType(weeklyType);
    weeklyShow.setDescription("Weekly Show Description");
    weeklyShow.setShowDate(LocalDate.now().plusDays(2));
    showRepository.save(weeklyShow);

    otherShow = new Show();
    otherShow.setName("My Other Show");
    otherShow.setTemplate(otherTemplate);
    otherShow.setType(otherType);
    otherShow.setDescription("Other Show Description");
    otherShow.setShowDate(LocalDate.now().plusDays(3));
    showRepository.save(otherShow);
  }

  @Test
  @DisplayName("Should apply correct styles in ShowListView")
  void shouldApplyCorrectStylesInShowListView() {
    SeasonService seasonService = mock(SeasonService.class);
    Clock clock = mock(Clock.class);
    ShowListView showListView =
        new ShowListView(showService, showTypeService, seasonService, showTemplateService, clock);
    Grid<Show> grid = showListView.showGrid;

    // Test the part name generator
    assertEquals("ple-show", grid.getPartNameGenerator().apply(otherShow));
    assertNull(grid.getPartNameGenerator().apply(pleShow));
    assertNull(grid.getPartNameGenerator().apply(weeklyShow));

    // Test the type column renderer
    Grid.Column<Show> typeColumn =
        grid.getColumns().stream()
            .filter(c -> "Type".equals(c.getHeaderText()))
            .findFirst()
            .orElse(null);
    assertNotNull(typeColumn);

    ComponentRenderer<Span, Show> renderer =
        (ComponentRenderer<Span, Show>) typeColumn.getRenderer();

    Span otherSpan = renderer.createComponent(otherShow);
    assertEquals(
        "background-color:#8A2BE2;color:white", otherSpan.getElement().getAttribute("style"));
  }

  @Test
  @DisplayName("Should apply correct colors in ShowCalendarView")
  void shouldApplyCorrectColorsInShowCalendarView() {
    // Mock UI and session for the calendar view
    UI ui = new UI();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.getLocale()).thenReturn(Locale.US);
    ui.getInternals().setSession(session);

    ShowCalendarView showCalendarView = new ShowCalendarView(showService);

    // The calendar is populated in the constructor, so we can get the entries right away.
    Instant start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
    Instant end = LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
    EntryQuery query = new EntryQuery(start, end);
    List<Entry> entries = showCalendarView.getCalendar().getEntryProvider().fetch(query).toList();

    Entry pleEntry =
        entries.stream().filter(e -> e.getTitle().equals("My PLE Show")).findFirst().orElse(null);
    assertNotNull(pleEntry);
    assertEquals("#dc2626", pleEntry.getColor());

    Entry weeklyEntry =
        entries.stream()
            .filter(e -> e.getTitle().equals("My Weekly Show"))
            .findFirst()
            .orElse(null);
    assertNotNull(weeklyEntry);
    assertEquals("#2563eb", weeklyEntry.getColor());

    Entry otherEntry =
        entries.stream().filter(e -> e.getTitle().equals("My Other Show")).findFirst().orElse(null);
    assertNotNull(otherEntry);
    assertEquals("#8A2BE2", otherEntry.getColor());
  }
}
