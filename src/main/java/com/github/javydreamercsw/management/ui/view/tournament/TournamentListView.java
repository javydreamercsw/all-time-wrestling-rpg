/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.tournament;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamMember;
import com.github.javydreamercsw.management.domain.team.TeamOrigin;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRecurrence;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.tournament.QualifierGroupsFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.component.TeamRowsEditor;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "tournament-list", layout = MainLayout.class)
@PageTitle("Tournaments | ATW RPG")
@Menu(order = 7, icon = "vaadin:sitemap", title = "Tournaments")
@RolesAllowed({"ADMIN", "BOOKER", "PLAYER", "VIEWER"})
@Slf4j
public class TournamentListView extends VerticalLayout {

  private final TournamentService tournamentService;
  private final WrestlerFacade wrestlerFacade;
  private final SegmentRuleService segmentRuleService;
  private final SegmentTypeService segmentTypeService;
  private final UniverseContextService universeContextService;
  private final SecurityUtils securityUtils;
  private final ShowFacade showFacade;

  /** Unpacked from {@link WrestlerFacade} — the wizard's team-entrant sources. */
  private final TeamService teamService;

  private final Grid<Tournament> grid;
  private Checkbox showPastEditionsCheckbox;

  @Autowired
  public TournamentListView(
      TournamentService tournamentService,
      WrestlerFacade wrestlerFacade,
      ShowFacade showFacade,
      ViewContext viewContext) {
    this.tournamentService = tournamentService;
    this.wrestlerFacade = wrestlerFacade;
    this.showFacade = showFacade;
    this.teamService = wrestlerFacade.getTeamService();
    this.segmentRuleService = showFacade.getSegmentRuleService();
    this.segmentTypeService = showFacade.getSegmentTypeService();
    this.universeContextService = viewContext.getUniverseContextService();
    this.securityUtils = viewContext.getSecurityUtils();

    setSizeFull();
    setPadding(false);

    grid = buildGrid();
    Div gridWrapper = new Div(grid);
    gridWrapper.addClassName("grid-scroll-container");
    add(buildToolbar(), gridWrapper);
    refresh();
    if (visibleTournaments().isEmpty()) {
      add(buildFirstTimeBanner());
    }
  }

  /**
   * Stateless cross-link to the tutorial page's Quick Tour — shown once, when the list is empty.
   */
  private Div buildFirstTimeBanner() {
    Div banner = new Div();
    banner.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Margin.Horizontal.LARGE,
        LumoUtility.Margin.Vertical.SMALL);

    HorizontalLayout row = new HorizontalLayout();
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setWidthFull();
    Span text =
        new Span(
            "First time here? Run a tournament in a few clicks — see the Quick Tour on the"
                + " Tutorial page.");
    text.addClassNames(LumoUtility.FontSize.SMALL);
    Button tutorialLink =
        new Button("Open Tutorial", e -> getUI().ifPresent(ui -> ui.navigate("tutorial")));
    tutorialLink.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

    row.add(text, tutorialLink);
    banner.add(row);
    return banner;
  }

  private ViewToolbar buildToolbar() {
    Button newBtn = new Button("New Tournament", e -> openCreationWizard());
    newBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    // ATW-o4ad: recurring chains list their latest edition by default; this toggle reveals
    // the completed earlier editions of each chain.
    showPastEditionsCheckbox = new Checkbox("Show past editions");
    showPastEditionsCheckbox.setValue(false);
    showPastEditionsCheckbox.addValueChangeListener(e -> refresh());
    return new ViewToolbar(
        "Tournaments", ViewToolbar.group(newBtn), ViewToolbar.group(showPastEditionsCheckbox));
  }

  private Grid<Tournament> buildGrid() {
    Grid<Tournament> g = new Grid<>(Tournament.class, false);
    g.setSizeFull();

    g.addColumn(Tournament::getName).setHeader("Name").setSortable(true).setFlexGrow(2);
    g.addColumn(t -> t.getFormatId().replace('_', ' ')).setHeader("Format").setSortable(true);
    // Edition badge (ATW-o4ad): roman-numeral ordinal for recurring chains, blank for one-shots.
    g.addColumn(t -> editionBadgeOf(t)).setHeader("Edition").setSortable(true);
    // Entries are lazy and rows render outside a transaction — count through the service
    // instead of touching the collection (LazyInitializationException otherwise).
    g.addColumn(tournamentService::countEntries).setHeader("Entrants");
    g.addColumn(t -> t.getStatus().name()).setHeader("Status").setSortable(true);
    // EAGER mapping — safe to read on detached rows (ATW-xbn4).
    g.addColumn(
            t ->
                t.getPayoffShow() != null
                    ? t.getPayoffShow().getName()
                        + (t.getPayoffShow().getShowDate() != null
                            ? " — " + t.getPayoffShow().getShowDate()
                            : "")
                    : "")
        .setHeader("Host Show");

    // AutoWidth so both buttons fit: a fixed narrow width clips the Delete button once the
    // Host Show column takes its share of the row (user-reported). The key lets tests target
    // the cell (Karibu _getCellComponent).
    g.addComponentColumn(this::buildRowActions)
        .setKey("actions")
        .setHeader("Actions")
        .setAutoWidth(true)
        .setFlexGrow(0);

    g.addItemClickListener(
        e -> {
          // Clicks inside the Actions cell belong to the Edit/Delete buttons — the buttons
          // open their dialogs; navigating away here would instantly destroy the just-opened
          // dialog (user-reported: Delete appeared dead). Anywhere else opens the detail view.
          if (e.getColumn() != null && "actions".equals(e.getColumn().getKey())) {
            return;
          }
          UI.getCurrent().navigate("tournament-detail/" + e.getItem().getId());
        });
    return g;
  }

  /** Edit / Delete controls, mirroring TitleListView's action column. */
  private HorizontalLayout buildRowActions(@NonNull Tournament tournament) {
    HorizontalLayout actions = new HorizontalLayout();
    actions.setSpacing(true);

    Button editBtn = new Button("Edit", new Icon(VaadinIcon.EDIT));
    editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
    editBtn.addClickListener(e -> openEditDialog(tournament));
    editBtn.setVisible(securityUtils.canEdit());

    Button deleteBtn = new Button("Delete", new Icon(VaadinIcon.TRASH));
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
    deleteBtn.addClickListener(e -> confirmDelete(tournament));
    deleteBtn.setVisible(securityUtils.canDelete());

    actions.add(editBtn, deleteBtn);
    return actions;
  }

  private void openEditDialog(@NonNull Tournament tournament) {
    // The grid row is detached — its linkedTitle is an uninitialized proxy. Re-read with the
    // graph initialized before binding lazy values to the dialog fields.
    Tournament managed =
        tournamentService.findByIdWithDetails(tournament.getId()).orElse(tournament);
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Edit Tournament");
    dialog.setWidth("min(950px, 95vw)");

    TextField nameField = new TextField("Tournament Name");
    nameField.setValue(managed.getName());
    nameField.setRequired(true);
    nameField.setWidthFull();

    ComboBox<TournamentFormat> formatCombo = new ComboBox<>("Format");
    formatCombo.setItems(tournamentService.getAvailableFormats());
    formatCombo.setItemLabelGenerator(TournamentFormat::getDisplayName);
    formatCombo.setWidthFull();
    tournamentService.findFormat(managed.getFormatId()).ifPresent(formatCombo::setValue);

    ComboBox<Title> titleCombo = new ComboBox<>("Linked Championship (optional)");
    titleCombo.setItems(wrestlerFacade.getTitleService().findAll());
    titleCombo.setItemLabelGenerator(Title::getName);
    titleCombo.setValue(managed.getLinkedTitle());
    titleCombo.setWidthFull();
    titleCombo.setClearButtonVisible(true);

    // Payoff mode (ATW-ewrp / ATW-8p72): classic title-on-the-line, contender-deciding,
    // briefcase-deciding, named trophy, or bragging rights. Briefcase/trophy/no-payoff all clear
    // and disable the title combo (no linked title).
    RadioButtonGroup<String> payoffMode = new RadioButtonGroup<>("Payoff");
    payoffMode.setItems(
        "Title on the line",
        "Winner becomes #1 contender",
        "Winner earns the briefcase",
        "Winner earns a trophy",
        "No payoff — bragging rights");
    payoffMode.setValue(
        managed.isBriefcaseDeciding()
            ? "Winner earns the briefcase"
            : managed.isContenderDeciding()
                ? "Winner becomes #1 contender"
                : managed.getTrophyName() != null ? "Winner earns a trophy" : "Title on the line");
    payoffMode.setHelperText(
        "Contender: needs a linked championship with a reigning champion — the winner becomes its"
            + " #1 contender. Briefcase: no linked championship — the winner earns a cashable"
            + " title shot against ANY reigning champion. Trophy: the winner lifts the named"
            + " trophy (e.g. the 2026 Dusty Cup) — recorded for the AI's past-glory references."
            + " Bragging rights: the win is all the prize.");
    TextField trophyNameField = new TextField("Trophy Name");
    trophyNameField.setId("trophy-name-field");
    trophyNameField.setPlaceholder("Dusty Cup");
    trophyNameField.setWidthFull();
    trophyNameField.setValue(managed.getTrophyName() != null ? managed.getTrophyName() : "");
    trophyNameField.setVisible("Winner earns a trophy".equals(payoffMode.getValue()));
    titleCombo.addValueChangeListener(
        e -> {
          if (e.getValue() != null
              && ("Winner earns the briefcase".equals(payoffMode.getValue())
                  || "Winner earns a trophy".equals(payoffMode.getValue())
                  || "No payoff — bragging rights".equals(payoffMode.getValue()))) {
            payoffMode.setValue("Title on the line");
          }
        });
    payoffMode.addValueChangeListener(
        e -> {
          boolean noTitle =
              "Winner earns the briefcase".equals(e.getValue())
                  || "Winner earns a trophy".equals(e.getValue())
                  || "No payoff — bragging rights".equals(e.getValue());
          if (noTitle) {
            titleCombo.clear();
          }
          titleCombo.setEnabled(!noTitle);
          trophyNameField.setVisible("Winner earns a trophy".equals(e.getValue()));
        });

    // One-time host-show binding (ATW-xbn4).
    ComboBox<Show> hostShowCombo = new ComboBox<>("Host Show (optional)");
    hostShowCombo.setItems(showFacade.getShowService().getUpcomingShows(50));
    hostShowCombo.setItemLabelGenerator(
        s -> s.getName() + (s.getShowDate() != null ? " — " + s.getShowDate() : ""));
    hostShowCombo.setValue(managed.getPayoffShow());
    hostShowCombo.setWidthFull();
    hostShowCombo.setClearButtonVisible(true);
    hostShowCombo.setAllowCustomValue(false);

    ComboBox<SegmentType> payoffTypeCombo = new ComboBox<>("Payoff Match Type (optional)");
    payoffTypeCombo.setItems(segmentTypeService.findAll());
    payoffTypeCombo.setItemLabelGenerator(SegmentType::getName);
    payoffTypeCombo.setValue(managed.getPayoffSegmentType());
    payoffTypeCombo.setWidthFull();
    payoffTypeCombo.setClearButtonVisible(true);
    payoffTypeCombo.setHelperText("Defaults to One on One.");

    ComboBox<SegmentRule> payoffRuleCombo = new ComboBox<>("Payoff Rule (optional)");
    payoffRuleCombo.setItems(segmentRuleService.findAll());
    payoffRuleCombo.setItemLabelGenerator(SegmentRule::getName);
    payoffRuleCombo.setValue(managed.getPayoffSegmentRule());
    payoffRuleCombo.setWidthFull();
    payoffRuleCombo.setClearButtonVisible(true);
    payoffTypeCombo.setEnabled(managed.getPayoffShow() != null);
    payoffRuleCombo.setEnabled(managed.getPayoffShow() != null);
    hostShowCombo.addValueChangeListener(
        e -> {
          boolean hasHost = e.getValue() != null;
          payoffTypeCombo.setEnabled(hasHost);
          payoffRuleCombo.setEnabled(hasHost);
          if (!hasHost) {
            payoffTypeCombo.clear();
            payoffRuleCombo.clear();
          }
        });

    MultiSelectComboBox<SegmentRule> rulesPicker =
        new MultiSelectComboBox<>("Allowed Segment Rules (optional)");
    rulesPicker.setItems(segmentRuleService.findAll());
    rulesPicker.setItemLabelGenerator(SegmentRule::getName);
    rulesPicker.setValue(new HashSet<>(managed.getAllowedRules()));
    rulesPicker.setWidthFull();

    Button cancel = new Button("Cancel", e -> dialog.close());
    Button save =
        new Button(
            "Save",
            e -> {
              if (nameField.isEmpty()) {
                Notification.show("Name is required.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
              }
              try {
                boolean briefcaseSelected =
                    "Winner earns the briefcase".equals(payoffMode.getValue());
                boolean contenderSelected =
                    "Winner becomes #1 contender".equals(payoffMode.getValue());
                boolean trophySelected = "Winner earns a trophy".equals(payoffMode.getValue());
                if (trophySelected && trophyNameField.isEmpty()) {
                  Notification.show("Trophy name is required.", 3000, Notification.Position.MIDDLE)
                      .addThemeVariants(NotificationVariant.LUMO_ERROR);
                  return;
                }
                // The Boolean-flag pattern: a value arms/clears ("" clears), null leaves the
                // existing name untouched. Switching away from trophy mode clears it.
                String trophyName;
                if (trophySelected) {
                  trophyName = trophyNameField.getValue();
                } else if (managed.getTrophyName() != null) {
                  trophyName = "";
                } else {
                  trophyName = null;
                }
                tournamentService.updateTournament(
                    tournament.getId(),
                    nameField.getValue(),
                    formatCombo.getValue() != null ? formatCombo.getValue().getFormatId() : null,
                    briefcaseSelected
                            || trophySelected
                            || "No payoff — bragging rights".equals(payoffMode.getValue())
                        ? null
                        : titleCombo.getValue(),
                    managed.getStartDate(),
                    new ArrayList<>(rulesPicker.getSelectedItems()),
                    hostShowCombo.getValue(),
                    payoffTypeCombo.getValue(),
                    payoffRuleCombo.getValue(),
                    true,
                    contenderSelected,
                    briefcaseSelected,
                    trophyName);
                dialog.close();
                refresh();
                Notification.show("Tournament updated!", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              } catch (Exception ex) {
                log.error("Error updating tournament", ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    dialog.getFooter().add(cancel, save);
    dialog.add(
        new VerticalLayout(
            nameField,
            formatCombo,
            payoffMode,
            trophyNameField,
            titleCombo,
            hostShowCombo,
            payoffTypeCombo,
            payoffRuleCombo,
            rulesPicker));
    dialog.open();
  }

  private void confirmDelete(@NonNull Tournament tournament) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setHeader("Delete Tournament");
    confirmDialog.setText(
        "Are you sure you want to delete the tournament '"
            + tournament.getName()
            + "'?"
            + " Only scheduled (not started) tournaments can be deleted.");
    confirmDialog.setCancelable(true);
    confirmDialog.setConfirmText("Delete");
    confirmDialog.setConfirmButtonTheme("error primary");

    confirmDialog.addConfirmListener(
        e -> {
          try {
            assert tournament.getId() != null;
            boolean deleted = tournamentService.deleteTournament(tournament.getId());
            if (deleted) {
              refresh();
              Notification.show("Tournament deleted", 3000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
              Notification.show("Tournament not found.", 5000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
          } catch (Exception ex) {
            log.error("Error deleting tournament", ex);
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });
    confirmDialog.open();
  }

  private void refresh() {
    grid.setItems(visibleTournaments());
  }

  /**
   * The listing the booker sees: the universe's tournaments (or all of them without a universe
   * context), latest editions only by default, with pristine catalog rows filtered out — the seeded
   * templates ("Deadly Combat" et al.) exist to be paired with show templates, and on a fresh
   * install they read as an empty duplicate tournament. A catalog row that has been seeded
   * (entries) or started leaves the pristine state and lists normally.
   */
  private List<Tournament> visibleTournaments() {
    Optional<Universe> universe = universeContextService.getCurrentUniverse();
    List<Tournament> items =
        universe.map(tournamentService::findByUniverse).orElseGet(tournamentService::findAll);
    items =
        items.stream()
            .filter(
                t ->
                    !(t.getCode() != null
                        && t.getStatus() == TournamentStatus.SCHEDULED
                        && tournamentService.countEntries(t) == 0))
            .toList();
    if (!showPastEditionsCheckbox.getValue()) {
      // Recurring chains show the latest edition only (ATW-o4ad) — past editions surface via
      // the toggle, and one-shot tournaments (no ordinal) always list.
      items = latestEditionPerChain(items);
    }
    return items;
  }

  /**
   * Keeps only the highest-ordinal edition of each recurring chain; one-shots (no ordinal) pass
   * through. Chains are walked via {@code parent} (EAGER), so a completed edition with a successor
   * is hidden from the default view.
   */
  private static List<Tournament> latestEditionPerChain(@NonNull List<Tournament> items) {
    Map<Long, Tournament> latestByChainRoot = new HashMap<>();
    List<Tournament> oneShots = new ArrayList<>();
    for (Tournament t : items) {
      if (t.getEditionOrdinal() == null) {
        oneShots.add(t);
      }
    }
    // Root each edition at its chain's first edition id (walk parents), keep the max ordinal.
    for (Tournament t : items) {
      if (t.getEditionOrdinal() == null) {
        continue;
      }
      Tournament walk = t;
      while (walk.getParent() != null && walk.getParent().getId() != null) {
        walk = walk.getParent();
      }
      Long root = walk.getId();
      Tournament current = latestByChainRoot.get(root);
      if (current == null
          || t.getEditionOrdinal() > current.getEditionOrdinal()
          || (t.getEditionOrdinal().equals(current.getEditionOrdinal())
              && t.getId() != null
              && t.getId() > current.getId())) {
        latestByChainRoot.put(root, t);
      }
    }
    List<Tournament> result = new ArrayList<>(oneShots);
    result.addAll(latestByChainRoot.values());
    return result;
  }

  /** "II" / "III" — the roman-numeral edition ordinal, blank for one-shot tournaments. */
  private static String editionBadgeOf(@NonNull Tournament t) {
    return t.getEditionOrdinal() != null && t.getEditionOrdinal() > 1
        ? TournamentService.editionName("", t.getEditionOrdinal()).trim()
        : "";
  }

  // ── Creation wizard ───────────────────────────────────────────────────────

  private void openCreationWizard() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("New Tournament");
    dialog.setWidth("min(950px, 95vw)");

    TabSheet tabs = new TabSheet();
    Tab tab1 = new Tab("1. Details");
    Tab tab2 = new Tab("2. Seeding");

    // Tab 1: Name, format, title link, start date
    TextField nameField = new TextField("Tournament Name");
    nameField.setId("wizard-tournament-name");
    nameField.setRequired(true);
    nameField.setWidthFull();

    ComboBox<TournamentFormat> formatCombo = new ComboBox<>("Format");
    formatCombo.setId("wizard-format");
    formatCombo.setItems(tournamentService.getAvailableFormats());
    formatCombo.setItemLabelGenerator(TournamentFormat::getDisplayName);
    formatCombo.setRequired(true);
    formatCombo.setWidthFull();

    ComboBox<Title> titleCombo = new ComboBox<>("Linked Championship (optional)");
    titleCombo.setId("wizard-linked-title");
    titleCombo.setItems(wrestlerFacade.getTitleService().findAll());
    titleCombo.setItemLabelGenerator(Title::getName);
    titleCombo.setWidthFull();

    // One-time host-show binding (ATW-xbn4): the payoff books on this show exactly once; the
    // non-final rounds pace automatically onto the weekly shows before it. Template pairing
    // stays reserved for recurring tournaments.
    ComboBox<Show> hostShowCombo = new ComboBox<>("Host Show (optional)");
    hostShowCombo.setItems(
        showFacade.getShowService().getUpcomingShows(50).stream()
            .filter(
                s ->
                    universeContextService.getCurrentUniverse().isEmpty()
                        || s.getUniverse() == null
                        || universeContextService
                            .getCurrentUniverse()
                            .map(u -> u.getId().equals(s.getUniverse().getId()))
                            .orElse(false))
            .toList());
    hostShowCombo.setItemLabelGenerator(
        s -> s.getName() + (s.getShowDate() != null ? " — " + s.getShowDate() : ""));
    hostShowCombo.setWidthFull();
    hostShowCombo.setClearButtonVisible(true);
    hostShowCombo.setAllowCustomValue(false);
    hostShowCombo.setHelperText(
        "Payoff books on this show exactly once; rounds pace onto the weekly shows before"
            + " it automatically.");

    ComboBox<SegmentType> payoffTypeCombo = new ComboBox<>("Payoff Match Type (optional)");
    payoffTypeCombo.setItems(segmentTypeService.findAll());
    payoffTypeCombo.setItemLabelGenerator(SegmentType::getName);
    payoffTypeCombo.setWidthFull();
    payoffTypeCombo.setClearButtonVisible(true);
    payoffTypeCombo.setHelperText("Defaults to One on One.");

    ComboBox<SegmentRule> payoffRuleCombo = new ComboBox<>("Payoff Rule (optional)");
    payoffRuleCombo.setItems(segmentRuleService.findAll());
    payoffRuleCombo.setItemLabelGenerator(SegmentRule::getName);
    payoffRuleCombo.setWidthFull();
    payoffRuleCombo.setClearButtonVisible(true);
    // The payoff only exists on a show — keep the payoff pickers inert until a host is chosen.
    payoffTypeCombo.setEnabled(false);
    payoffRuleCombo.setEnabled(false);
    hostShowCombo.addValueChangeListener(
        e -> {
          boolean hasHost = e.getValue() != null;
          payoffTypeCombo.setEnabled(hasHost);
          payoffRuleCombo.setEnabled(hasHost);
          if (!hasHost) {
            payoffTypeCombo.clear();
            payoffRuleCombo.clear();
          }
        });

    // Edition cadence (ATW-o4ad): recurring editions re-arm the PLE template pairing each cycle
    // — the next edition auto-creates when the payoff books, instead of consuming the pairing.
    ComboBox<TournamentRecurrence> recurrenceCombo =
        new ComboBox<>("Edition Cadence (recurring tournaments)");
    recurrenceCombo.setItems(TournamentRecurrence.values());
    recurrenceCombo.setItemLabelGenerator(
        r ->
            switch (r) {
              case NONE -> "One-time";
              case ANNUAL -> "Annual";
            });
    recurrenceCombo.setValue(TournamentRecurrence.NONE);
    recurrenceCombo.setWidthFull();
    recurrenceCombo.setHelperText(
        "Annual editions pair with a PLE show template: when a payoff books, the next edition"
            + " is created automatically and the template pairing re-points to it — no manual"
            + " re-arming each year.");

    MultiSelectComboBox<SegmentRule> rulesPicker =
        new MultiSelectComboBox<>("Allowed Segment Rules (optional)");
    rulesPicker.setItems(segmentRuleService.findAll());
    rulesPicker.setItemLabelGenerator(SegmentRule::getName);
    rulesPicker.setWidthFull();
    rulesPicker.setHelperText(
        "Rules randomly applied to matches. A fixed rule can be set per round later.");

    ComboBox<Gender> genderCombo = new ComboBox<>("Gender Filter (optional)");
    genderCombo.setItems(Gender.values());
    genderCombo.setItemLabelGenerator(
        g ->
            switch (g) {
              case MALE -> "Men's division";
              case FEMALE -> "Women's division";
            });
    genderCombo.setWidthFull();
    genderCombo.setClearButtonVisible(true);
    genderCombo.setHelperText(
        "Restricts entrants to one gender. The linked championship's own constraint also applies.");

    // Entrant type (ATW-01g2): singles (default) or team tournaments (Tag Team / Trios). Team
    // tournaments are inaugural-champion only — the payoff-mode radio is disabled for them.
    RadioButtonGroup<String> entrantType = new RadioButtonGroup<>("Entrants");
    entrantType.setId("wizard-entrant-type");
    entrantType.setItems(SINGLES_LABEL, TAG_TEAM_LABEL, TRIOS_LABEL);
    entrantType.setValue(SINGLES_LABEL);
    entrantType.setWidthFull();

    // Payoff mode (ATW-ewrp / ATW-8p72): classic title-on-the-line, contender-deciding,
    // briefcase-deciding, named trophy, or bragging rights. Briefcase/trophy/no-payoff all clear
    // and disable the title combo (no linked title). Team tournaments keep contender/briefcase
    // disabled (the winner is a team) — trophy and bragging rights are legal for teams.
    RadioButtonGroup<String> payoffMode = new RadioButtonGroup<>("Payoff");
    payoffMode.setItems(
        "Title on the line",
        "Winner becomes #1 contender",
        "Winner earns the briefcase",
        "Winner earns a trophy",
        "No payoff — bragging rights");
    payoffMode.setValue("Title on the line");
    payoffMode.setHelperText(
        "Contender: needs a linked championship with a reigning champion — the winner becomes its"
            + " #1 contender. Briefcase: no linked championship — the winner earns a cashable"
            + " title shot against ANY reigning champion. Trophy: the winner lifts the named"
            + " trophy (e.g. the Dusty Cup) — the win is recorded for the AI's past-glory"
            + " references. Bragging rights: the win is all the prize."
            + " Team tournaments put a TEAM title on the line, take a trophy, or settle for"
            + " bragging rights.");
    payoffMode.setItemEnabledProvider(
        item ->
            teamModeOf(entrantType.getValue())
                ? !"Winner becomes #1 contender".equals(item)
                    && !"Winner earns the briefcase".equals(item)
                : true);
    TextField wizardTrophyNameField = new TextField("Trophy Name");
    wizardTrophyNameField.setId("wizard-trophy-name");
    wizardTrophyNameField.setPlaceholder("Dusty Cup");
    wizardTrophyNameField.setWidthFull();
    wizardTrophyNameField.setVisible(false);
    titleCombo.addValueChangeListener(
        e -> {
          if (e.getValue() != null
              && ("Winner earns the briefcase".equals(payoffMode.getValue())
                  || "Winner earns a trophy".equals(payoffMode.getValue())
                  || "No payoff — bragging rights".equals(payoffMode.getValue()))) {
            payoffMode.setValue("Title on the line");
          }
        });
    payoffMode.addValueChangeListener(
        e -> {
          boolean noTitle =
              "Winner earns the briefcase".equals(e.getValue())
                  || "Winner earns a trophy".equals(e.getValue())
                  || "No payoff — bragging rights".equals(e.getValue());
          if (noTitle) {
            titleCombo.clear();
          }
          titleCombo.setEnabled(!noTitle);
          wizardTrophyNameField.setVisible("Winner earns a trophy".equals(e.getValue()));
        });

    // ── Team entrants (ATW-01g2): registry picker + one-off assembly ──────────
    // Registry picker mirrors the singles flow's wrestlerPicker: a multi-select combo on the
    // seeding tab ("Select Teams"), visible only in Manual mode. Picked teams + an optional
    // one-off become entries with sequential seeds in one Create pass. The query is the same
    // pool seedAuto draws from: active REGISTRY teams of the declared size whose every member
    // satisfies the gender filter, sorted by team fans.
    MultiSelectComboBox<Team> teamRegistryPicker = new MultiSelectComboBox<>("Select Teams");
    teamRegistryPicker.setId("team-registry-picker");
    teamRegistryPicker.setItemLabelGenerator(Team::getName);
    teamRegistryPicker.setWidthFull();
    teamRegistryPicker.setVisible(false);
    teamRegistryPicker.setHelperText("Only teams eligible for this tournament are listed.");

    // One-off assembly: add-team rows like the segment editor — one picker per team,
    // each row accepting exactly the declared member count, excluding wrestlers committed to
    // picked registry teams or other rows (the editor handles cross-row exclusion; the filter
    // closure handles registry picks, gender, and activity). Each assembled team persists as
    // AD_HOC unless "save to registry" (default off) flips them all to REGISTRY.
    // The editor is rebuilt whenever the declared size changes (entrant-type switch).
    Runnable[] rebuildOneOffEditorHolder = new Runnable[1];
    TeamRowsEditor[] oneOffEditorHolder = new TeamRowsEditor[1];
    Integer[] oneOffEditorSizeHolder = new Integer[1];
    Supplier<TeamSize> oneOffSizeSupplier = () -> teamSizeOf(entrantType.getValue());
    Checkbox saveToRegistryCheckbox = new Checkbox("Save these teams to the team registry");
    saveToRegistryCheckbox.setId("wizard-save-to-registry");
    saveToRegistryCheckbox.setValue(false);
    saveToRegistryCheckbox.setVisible(false);
    // Row edits only refresh the save-to-registry checkbox — the editor itself is rebuilt
    // exclusively by refreshTeamPickers when the DECLARED size changes (entrant-type switch);
    // wiring the rebuild here would wipe the rows on every member pick.
    Runnable oneOffChanged =
        () -> saveToRegistryCheckbox.setVisible(oneOffEditorHolder[0].hasAnySelection());
    TeamRowsEditor oneOffTeamsEditor =
        new TeamRowsEditor(
            wrestlerFacade.getWrestlerService().getAllWrestlers().stream()
                .filter(w -> Boolean.TRUE.equals(w.getActive()))
                .toList(),
            oneOffChanged,
            null,
            oneOffSizeSupplier.get() != null ? oneOffSizeSupplier.get().getMemberCount() : null,
            1,
            16);
    oneOffEditorHolder[0] = oneOffTeamsEditor;
    oneOffTeamsEditor.setId("wizard-one-off-teams");
    oneOffTeamsEditor.setVisible(false);

    VerticalLayout teamEntrantsPanel =
        new VerticalLayout(oneOffTeamsEditor, saveToRegistryCheckbox);
    teamEntrantsPanel.setPadding(false);
    teamEntrantsPanel.setVisible(false);

    rebuildOneOffEditorHolder[0] =
        () -> {
          TeamSize size = oneOffSizeSupplier.get();
          TeamRowsEditor editor =
              new TeamRowsEditor(
                  wrestlerFacade.getWrestlerService().getAllWrestlers().stream()
                      .filter(w -> Boolean.TRUE.equals(w.getActive()))
                      .toList(),
                  oneOffChanged,
                  null,
                  size != null ? size.getMemberCount() : null,
                  1,
                  16);
          editor.setId("wizard-one-off-teams");
          // Visibility follows the panel (Manual + team mode), NOT the outgoing editor — a
          // rebuild while hidden would leave the new editor explicitly invisible even after
          // the panel itself becomes visible.
          editor.setVisible(teamEntrantsPanel.isVisible());
          teamEntrantsPanel.replace(oneOffEditorHolder[0], editor);
          oneOffEditorHolder[0] = editor;
          saveToRegistryCheckbox.setVisible(editor.hasAnySelection());
        };

    VerticalLayout tab1Content =
        new VerticalLayout(
            nameField,
            formatCombo,
            payoffMode,
            wizardTrophyNameField,
            titleCombo,
            genderCombo,
            entrantType,
            teamEntrantsPanel,
            hostShowCombo,
            payoffTypeCombo,
            payoffRuleCombo,
            recurrenceCombo,
            rulesPicker);
    tab1Content.setPadding(false);

    // Tab 2: Seeding
    ComboBox<String> seedingMode = new ComboBox<>("Seeding Method");
    seedingMode.setId("wizard-seeding-mode");
    seedingMode.setItems(
        "Auto (by fan count)", "Manual (pick wrestlers)", "Don't seed now (seed later)");
    seedingMode.setValue("Auto (by fan count)");
    seedingMode.setWidthFull();
    seedingMode.setHelperText(
        "Auto seeds the top entrants by fan count (teams for team tournaments). A tournament"
            + " paired with a show template can also be seeded automatically when the paired"
            + " show is approved.");

    IntegerField countField = new IntegerField("Number of Entrants");
    countField.setValue(8);
    countField.setMin(3);
    countField.setMax(64);
    countField.setWidthFull();
    countField.setHelperText(
        "Capped at the number of eligible entrants (active wrestlers, or registry teams for"
            + " team tournaments).");

    // QUALIFIER_GROUPS group size: wrestlers per qualifier Free-for-All. Part of the entrant
    // validation — the bracket needs at least two groups, so entrants must cover 2 × group size.
    IntegerField groupSizeField = new IntegerField("Wrestlers per Qualifier Group (optional)");
    groupSizeField.setMin(2);
    groupSizeField.setMax(QualifierGroupsFormat.MAX_GROUP_SIZE);
    groupSizeField.setWidthFull();
    groupSizeField.setVisible(false);
    groupSizeField.setHelperText("Leave empty for auto-sized groups (≈3 wrestlers each).");
    formatCombo.addValueChangeListener(
        e -> {
          boolean qualifierGroups =
              e.getValue() != null
                  && QualifierGroupsFormat.FORMAT_ID.equals(e.getValue().getFormatId());
          groupSizeField.setVisible(qualifierGroups);
          if (!qualifierGroups) {
            groupSizeField.clear();
          }
        });

    // Entrant-count validation across the pair: with a group size G the bracket needs at least
    // two groups (entrants ≥ 2G). Re-checks whenever either field changes.
    Runnable validateGroupSplit =
        () -> {
          Integer groupSize = groupSizeField.getValue();
          Integer entrants = countField.getValue();
          boolean invalid =
              qualifierGroupsSelected(formatCombo)
                  && groupSize != null
                  && entrants != null
                  && entrants
                      < 2 * Math.max(2, Math.min(groupSize, QualifierGroupsFormat.MAX_GROUP_SIZE));
          groupSizeField.setInvalid(invalid);
          groupSizeField.setErrorMessage(
              "At least "
                  + (groupSize == null ? 2 : 2 * groupSize)
                  + " entrants are needed for"
                  + " groups of "
                  + (groupSize == null ? 3 : groupSize)
                  + " — two groups minimum.");
        };
    groupSizeField.addValueChangeListener(e -> validateGroupSplit.run());
    countField.addValueChangeListener(e -> validateGroupSplit.run());
    formatCombo.addValueChangeListener(e -> validateGroupSplit.run());

    // Cap the entrant count at the eligible roster (narrowed by the linked championship's
    // gender constraint; team tournaments count eligible REGISTRY teams) — the format's 64
    // max means nothing to a 12-wrestler universe.
    Runnable refreshEntrantCap =
        () -> {
          TeamSize size = teamSizeOf(entrantType.getValue());
          Long universeId =
              universeContextService.getCurrentUniverse().map(Universe::getId).orElse(null);
          int eligible =
              size != null
                  ? tournamentService.countEligibleTeamEntrants(
                      size, genderCombo.getValue(), titleCombo.getValue(), universeId)
                  : tournamentService.countEligibleEntrants(
                      titleCombo.getValue(), genderCombo.getValue());
          int cap = Math.clamp(eligible, 3, 64);
          countField.setMax(cap);
          if (countField.getValue() == null || countField.getValue() > cap) {
            countField.setValue(cap);
          }
        };
    titleCombo.addValueChangeListener(e -> refreshEntrantCap.run());
    genderCombo.addValueChangeListener(e -> refreshEntrantCap.run());
    entrantType.addValueChangeListener(e -> refreshEntrantCap.run());
    refreshEntrantCap.run();

    // Registry picker + one-off rows follow the entrant type and gender filter: the picker
    // re-queries the eligible REGISTRY teams, the one-off rows' candidate filter excludes
    // wrestlers already committed to a picked registry team (a wrestler may appear in only one
    // entry) and anyone failing the tournament's eligibility rules (active, gender-matching).
    Runnable refreshMemberPickerExclusions =
        () -> {
          Set<Long> committedMemberIds = new HashSet<>();
          for (Team picked : teamRegistryPicker.getSelectedItems()) {
            picked.getMembers().stream()
                .map(TeamMember::getWrestler)
                .map(Wrestler::getId)
                .forEach(committedMemberIds::add);
          }
          Gender gender = genderCombo.getValue();
          oneOffEditorHolder[0].refreshRowItemsWithFilter(
              w ->
                  Boolean.TRUE.equals(w.getActive())
                      && (gender == null || w.getGender() == gender)
                      && !committedMemberIds.contains(w.getId()));
        };
    // Payoff normalization across the entrant-type switch (ATW-01g2 review): team mode forces
    // Title-on-the-line only from the contender/briefcase modes that are illegal there — a
    // trophy or bragging-rights pick is legal in team mode and must survive the switch
    // Leaving team mode re-arms contender/briefcase and clears a stale briefcase
    // selection, which the briefcase listener would otherwise apply silently against a linked
    // TEAM title.
    Runnable resetPayoffOnLeavingTeamMode =
        () -> {
          if (!teamModeOf(entrantType.getValue())
              && ("Winner earns the briefcase".equals(payoffMode.getValue()))) {
            payoffMode.setValue("Title on the line");
          }
        };
    Runnable refreshTeamPickers =
        () -> {
          boolean teamMode = teamModeOf(entrantType.getValue());
          // Seeding-tab visibility mirrors the singles wrestler picker: only Manual mode shows
          // the pickers. Initial wizard state is Auto, so everything starts hidden either way.
          boolean manualSeeding = "Manual (pick wrestlers)".equals(seedingMode.getValue());
          teamRegistryPicker.setVisible(teamMode && manualSeeding);
          teamEntrantsPanel.setVisible(teamMode && manualSeeding);
          // The editor's own visibility must be pinned too — an explicitly-invisible child is
          // not shown even when its parent panel becomes visible.
          oneOffEditorHolder[0].setVisible(teamMode && manualSeeding);
          TeamSize pickerSize = teamSizeOf(entrantType.getValue());
          int pickerMemberCount = pickerSize != null ? pickerSize.getMemberCount() : 2;
          // Rebuild the editor only when the declared size actually changed (entrant-type
          // switch); gender/registry exclusions refresh through the row filter instead.
          Integer editorSize = oneOffEditorSizeHolder[0];
          if (editorSize == null || editorSize.intValue() != pickerMemberCount) {
            rebuildOneOffEditorHolder[0].run();
            oneOffEditorSizeHolder[0] = pickerMemberCount;
            oneOffEditorHolder[0].setVisible(teamMode && manualSeeding);
          }
          // Entering team mode normalizes an illegal contender/briefcase pick to the default —
          // trophy/no-payoff picks carry over (legal in team mode now).
          if (teamMode
              && ("Winner becomes #1 contender".equals(payoffMode.getValue())
                  || "Winner earns the briefcase".equals(payoffMode.getValue()))) {
            payoffMode.setValue("Title on the line");
          }
          if (teamMode) {
            Tournament probe =
                teamProbe(teamSizeOf(entrantType.getValue()), genderCombo.getValue());
            universeContextService.getCurrentUniverse().ifPresent(probe::setUniverse);
            probe.setLinkedTitle(titleCombo.getValue());
            teamRegistryPicker.setItems(tournamentService.findEligibleTeamsSortedByFans(probe));
            refreshMemberPickerExclusions.run();
          } else {
            // Leaving team mode clears any half-assembled one-off teams.
            oneOffEditorHolder[0].clear();
            oneOffEditorHolder[0].addTeamRow();
            teamRegistryPicker.clear();
            saveToRegistryCheckbox.setVisible(false);
          }
        };
    entrantType.addValueChangeListener(
        e -> {
          // Leaving team mode normalizes the payoff BEFORE the pickers refresh (a briefcase
          // selected in team mode is stale there — contender/briefcase are team-mode-disabled).
          resetPayoffOnLeavingTeamMode.run();
          refreshTeamPickers.run();
        });
    genderCombo.addValueChangeListener(e -> refreshTeamPickers.run());
    titleCombo.addValueChangeListener(e -> refreshTeamPickers.run());
    teamRegistryPicker.addValueChangeListener(e -> refreshMemberPickerExclusions.run());
    refreshTeamPickers.run();

    MultiSelectComboBox<Wrestler> wrestlerPicker = new MultiSelectComboBox<>("Select Wrestlers");
    wrestlerPicker.setItems(wrestlerFacade.getWrestlerService().getAllWrestlers());
    wrestlerPicker.setItemLabelGenerator(Wrestler::getName);
    wrestlerPicker.setWidthFull();
    wrestlerPicker.setVisible(false);

    seedingMode.addValueChangeListener(
        e -> {
          boolean manual = "Manual (pick wrestlers)".equals(e.getValue());
          boolean skip = "Don't seed now (seed later)".equals(e.getValue());
          boolean teamMode = teamModeOf(entrantType.getValue());
          countField.setVisible(!manual && !skip);
          wrestlerPicker.setVisible(manual && !teamMode);
          // Team mode mirrors the singles flow: the registry picker replaces the wrestler
          // picker on the seeding tab; the one-off assembly panel rides along with it.
          teamRegistryPicker.setVisible(manual && teamMode);
          teamEntrantsPanel.setVisible(manual && teamMode);
          // Pin the editor's own visibility — an explicitly-invisible child is not shown even
          // when its parent panel becomes visible.
          oneOffEditorHolder[0].setVisible(manual && teamMode);
          boolean panelVisible = manual && teamMode;
          saveToRegistryCheckbox.setVisible(
              panelVisible && oneOffEditorHolder[0].hasAnySelection());
        });

    // Match-up preview for Auto seeding: what Create will build, before it commits.
    Span matchupPreview = new Span();
    matchupPreview.getStyle().set("color", "var(--lumo-secondary-text-color)");
    matchupPreview.setVisible(false);
    Runnable refreshMatchups =
        () -> {
          boolean autoMode = "Auto (by fan count)".equals(seedingMode.getValue());
          Integer entrants = countField.getValue();
          if (!autoMode || entrants == null || entrants < 2) {
            matchupPreview.setVisible(false);
            return;
          }
          // Fans walk the lazy wrestlerStates collection — rank inside the service's
          // transaction instead of touching detached entities here.
          List<Wrestler> pool =
              new ArrayList<>(
                  tournamentService.findEligibleWrestlersSortedByFans(
                      titleCombo.getValue(),
                      genderCombo.getValue(),
                      universeContextService.getCurrentUniverse().map(Universe::getId).orElse(1L)));
          int take = Math.min(entrants, pool.size());
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < take / 2; i++) {
            if (!sb.isEmpty()) {
              sb.append(" · ");
            }
            sb.append(pool.get(i).getName())
                .append(" vs ")
                .append(pool.get(take - 1 - i).getName());
          }
          matchupPreview.setText(sb.isEmpty() ? "No eligible wrestlers yet." : sb.toString());
          matchupPreview.setVisible(true);
        };
    countField.addValueChangeListener(e -> refreshMatchups.run());
    titleCombo.addValueChangeListener(e -> refreshMatchups.run());
    genderCombo.addValueChangeListener(e -> refreshMatchups.run());
    seedingMode.addValueChangeListener(e -> refreshMatchups.run());

    VerticalLayout tab2Content =
        new VerticalLayout(
            seedingMode,
            countField,
            groupSizeField,
            wrestlerPicker,
            teamRegistryPicker,
            teamEntrantsPanel,
            matchupPreview);
    tab2Content.setPadding(false);

    tabs.add(tab1, tab1Content);
    tabs.add(tab2, tab2Content);
    tabs.setSizeFull();

    // Wizard gating: only the Details tab is selectable until name + format are filled — the
    // Create button lives on the last step, so nothing can be created with an invalid step 1.
    Button back = new Button("Back", e -> tabs.setSelectedIndex(0));
    back.setEnabled(false);

    Button next =
        new Button(
            "Next",
            e -> {
              if (nameField.isEmpty() || formatCombo.isEmpty()) {
                Notification.show(
                        "Name and format are required before seeding.",
                        3000,
                        Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
              }
              if (groupSizeField.isInvalid()) {
                Notification.show(
                        "Fix the qualifier group size before continuing.",
                        3000,
                        Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
              }
              tabs.setSelectedIndex(1);
            });
    next.setId("wizard-next");

    Button cancel = new Button("Cancel", e -> dialog.close());
    Button create =
        new Button(
            "Create Tournament",
            e -> {
              if (nameField.isEmpty() || formatCombo.isEmpty()) {
                Notification.show(
                        "Name and format are required.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
              }
              try {
                Optional<Universe> universe = universeContextService.getCurrentUniverse();
                boolean briefcaseSelected =
                    "Winner earns the briefcase".equals(payoffMode.getValue());
                boolean contenderSelected =
                    "Winner becomes #1 contender".equals(payoffMode.getValue());
                boolean trophySelected = "Winner earns a trophy".equals(payoffMode.getValue());
                if (trophySelected && wizardTrophyNameField.isEmpty()) {
                  Notification.show("Trophy name is required.", 3000, Notification.Position.MIDDLE)
                      .addThemeVariants(NotificationVariant.LUMO_ERROR);
                  return;
                }
                boolean noPayoff = "No payoff — bragging rights".equals(payoffMode.getValue());
                TeamSize teamSize = teamSizeOf(entrantType.getValue());
                Tournament t =
                    tournamentService.createTournament(
                        nameField.getValue(),
                        formatCombo.getValue().getFormatId(),
                        universe.orElse(null),
                        briefcaseSelected || trophySelected || noPayoff
                            ? null
                            : titleCombo.getValue(),
                        LocalDate.now(),
                        new ArrayList<>(rulesPicker.getSelectedItems()),
                        hostShowCombo.getValue(),
                        payoffTypeCombo.getValue(),
                        payoffRuleCombo.getValue(),
                        genderCombo.getValue(),
                        contenderSelected,
                        briefcaseSelected,
                        teamSize,
                        trophySelected ? wizardTrophyNameField.getValue() : null);
                t.setRecurrence(recurrenceCombo.getValue());
                t.setQualifierGroupSize(groupSizeField.getValue());
                tournamentService.save(t);

                boolean auto = "Auto (by fan count)".equals(seedingMode.getValue());
                boolean manual = "Manual (pick wrestlers)".equals(seedingMode.getValue());
                if (teamSize != null) {
                  // Team tournaments follow the same seeding tab: Auto seeds eligible REGISTRY
                  // teams by fans (the service validates the format range); Manual seeds the
                  // wizard's explicit picks — the picked registry team(s) first, then a one-off
                  // assembled team when one was put together.
                  if (auto) {
                    tournamentService.seedAuto(
                        t, countField.getValue(), universe.map(Universe::getId).orElse(1L));
                  } else if (manual) {
                    List<Team> picked = new ArrayList<>(teamRegistryPicker.getSelectedItems());
                    int seed = 1;
                    for (Team team : picked) {
                      tournamentService.addEntry(t, team, seed++);
                    }
                    if (oneOffEditorHolder[0].hasAnySelection()) {
                      Map<Integer, List<Wrestler>> rowTeams = oneOffEditorHolder[0].getTeams();
                      // Rows hold exactly the declared size by construction; the re-check keeps
                      // a stale UI state (rows edited between validation and create) honest.
                      int memberCount = teamSize.getMemberCount();
                      for (List<Wrestler> members : rowTeams.values()) {
                        if (members.size() != memberCount) {
                          throw new IllegalStateException(
                              "Every one-off team needs exactly %d wrestlers — one row holds %d."
                                  .formatted(memberCount, members.size()));
                        }
                        // Re-check at create time: a registry team picked after assembly may
                        // have claimed one of these members (a wrestler appears in one entry
                        // only).
                        for (Team team : picked) {
                          if (members.stream().anyMatch(team::hasMember)) {
                            throw new IllegalStateException(
                                "Wrestler '"
                                    + members.stream()
                                        .filter(team::hasMember)
                                        .map(Wrestler::getName)
                                        .findFirst()
                                        .orElse("")
                                    + "' is already in the picked registry team '"
                                    + team.getName()
                                    + "' — clear the one-off team or the registry pick.");
                          }
                        }
                      }
                      for (List<Wrestler> group : rowTeams.values()) {
                        Team oneOff =
                            teamService
                                .createTeam(
                                    teamService.generateUniqueName(group),
                                    null,
                                    group.stream().map(Wrestler::getId).toList(),
                                    teamSize,
                                    null,
                                    null,
                                    TeamOrigin.AD_HOC)
                                .orElseThrow(
                                    () ->
                                        new IllegalStateException(
                                            "Could not create the one-off team — check that the"
                                                + " members are still eligible"));
                        if (saveToRegistryCheckbox.getValue()) {
                          oneOff = teamService.saveToRegistry(oneOff);
                        }
                        tournamentService.addEntry(t, oneOff, seed++);
                      }
                    }
                  } // "Don't seed now": seed later from the detail view or via a paired show.
                } else if (auto) {
                  tournamentService.seedAuto(
                      t, countField.getValue(), universe.map(Universe::getId).orElse(1L));
                } else if (manual) {
                  List<Wrestler> selected = new ArrayList<>(wrestlerPicker.getSelectedItems());
                  for (int i = 0; i < selected.size(); i++) {
                    tournamentService.addEntry(t, selected.get(i), i + 1);
                  }
                } // "Don't seed now": seed later from the detail view or via a paired show.

                dialog.close();
                refresh();
                Notification.show("Tournament created!", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate("tournament-detail/" + t.getId());
              } catch (Exception ex) {
                log.error("Error creating tournament", ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    // Wizard button visibility follows the selected tab: on Details only Next is offered, so
    // Create is unreachable until the seeding step.
    Runnable updateWizardButtons =
        () -> {
          boolean onDetails = Objects.equals(tabs.getSelectedIndex(), 0);
          back.setEnabled(!onDetails);
          next.setVisible(onDetails);
          create.setVisible(!onDetails);
        };
    tabs.addSelectedChangeListener(e -> updateWizardButtons.run());
    updateWizardButtons.run();

    dialog.getFooter().add(cancel, back, next, create);
    dialog.add(tabs);
    dialog.open();
  }

  /** Test hooks: drive the dialogs directly (Karibu tests can't traverse grid cell components). */
  void openCreationWizardForTest() {
    openCreationWizard();
  }

  /** Whether the wizard's current format selection is the qualifier-groups format. */
  private static boolean qualifierGroupsSelected(ComboBox<TournamentFormat> formatCombo) {
    return formatCombo.getValue() != null
        && QualifierGroupsFormat.FORMAT_ID.equals(formatCombo.getValue().getFormatId());
  }

  // ── Team entrant helpers (ATW-01g2) ───────────────────────────────────────

  /** Wizard label for the singles entrant mode (individual wrestlers). */
  static final String SINGLES_LABEL = "Singles";

  /** Wizard label for the DUO team entrant mode (classic tag team tournaments). */
  static final String TAG_TEAM_LABEL = "Tag Team";

  /** Wizard label for the TRIO team entrant mode (trios tournaments). */
  static final String TRIOS_LABEL = "Trios";

  /**
   * Maps the wizard's entrant-type radio to the tournament's declared {@link TeamSize} — null on
   * the singles mode (the tournament stays individual-wrestler only).
   */
  static TeamSize teamSizeOf(String entrantTypeValue) {
    if (TAG_TEAM_LABEL.equals(entrantTypeValue)) {
      return TeamSize.DUO;
    }
    if (TRIOS_LABEL.equals(entrantTypeValue)) {
      return TeamSize.TRIO;
    }
    return null;
  }

  /** Whether the entrant-type radio selects a team mode (Tag Team or Trios). */
  static boolean teamModeOf(String entrantTypeValue) {
    return teamSizeOf(entrantTypeValue) != null;
  }

  /**
   * An eligibility probe carrying just the wizard's declared size + gender — the shape {@code
   * findEligibleTeamsSortedByFans} reads.
   */
  private static Tournament teamProbe(TeamSize size, Gender gender) {
    Tournament probe = new Tournament();
    probe.setTeamSize(size);
    probe.setGender(gender);
    return probe;
  }

  void refreshGridForTest() {
    refresh();
  }

  void openEditDialogForTest(final Tournament tournament) {
    openEditDialog(tournament);
  }

  void confirmDeleteForTest(final Tournament tournament) {
    confirmDelete(tournament);
  }
}
