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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.ui.ViewContext;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                    t.getCode() == null
                        || t.getStatus() != TournamentStatus.SCHEDULED
                        || tournamentService.countEntries(t) != 0)
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
    TournamentCreationWizardDialog wizard =
        new TournamentCreationWizardDialog(
            tournamentService,
            wrestlerFacade,
            showFacade,
            universeContextService,
            wrestlerFacade.getTeamService(),
            created -> UI.getCurrent().navigate("tournament-detail/" + created.getId()));
    wizard.open();
  }

  /** Test hooks: drive the dialogs directly (Karibu tests can't traverse grid cell components). */
  void openCreationWizardForTest() {
    openCreationWizard();
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
