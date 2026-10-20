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
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    // Payoff mode (ATW-ewrp / ATW-8p72): classic title-on-the-line, contender-deciding, or
    // briefcase-deciding. Briefcase clears and disables the title combo (no linked title).
    RadioButtonGroup<String> payoffMode = new RadioButtonGroup<>("Payoff");
    payoffMode.setItems(
        "Title on the line", "Winner becomes #1 contender", "Winner earns the briefcase");
    payoffMode.setValue(
        managed.isBriefcaseDeciding()
            ? "Winner earns the briefcase"
            : managed.isContenderDeciding() ? "Winner becomes #1 contender" : "Title on the line");
    payoffMode.setHelperText(
        "Contender: needs a linked championship with a reigning champion — the winner becomes its"
            + " #1 contender. Briefcase: no linked championship — the winner earns a cashable"
            + " title shot against ANY reigning champion.");
    titleCombo.addValueChangeListener(
        e -> {
          if (e.getValue() != null && "Winner earns the briefcase".equals(payoffMode.getValue())) {
            payoffMode.setValue("Title on the line");
          }
        });
    payoffMode.addValueChangeListener(
        e -> {
          boolean briefcase = "Winner earns the briefcase".equals(e.getValue());
          if (briefcase) {
            titleCombo.clear();
          }
          titleCombo.setEnabled(!briefcase);
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
                tournamentService.updateTournament(
                    tournament.getId(),
                    nameField.getValue(),
                    formatCombo.getValue() != null ? formatCombo.getValue().getFormatId() : null,
                    briefcaseSelected ? null : titleCombo.getValue(),
                    managed.getStartDate(),
                    new ArrayList<>(rulesPicker.getSelectedItems()),
                    hostShowCombo.getValue(),
                    payoffTypeCombo.getValue(),
                    payoffRuleCombo.getValue(),
                    true,
                    contenderSelected,
                    briefcaseSelected);
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
    Optional<Universe> universe = universeContextService.getCurrentUniverse();
    List<Tournament> items =
        universe.map(tournamentService::findByUniverse).orElseGet(tournamentService::findAll);
    if (!showPastEditionsCheckbox.getValue()) {
      // Recurring chains show the latest edition only (ATW-o4ad) — past editions surface via
      // the toggle, and one-shot tournaments (no ordinal) always list.
      items = latestEditionPerChain(items);
    }
    grid.setItems(items);
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

    // Payoff mode (ATW-ewrp / ATW-8p72): classic title-on-the-line, contender-deciding, or
    // briefcase-deciding. Briefcase clears and disables the title combo (no linked title). Team
    // tournaments are inaugural-champion only, so contender/briefcase are disabled there.
    RadioButtonGroup<String> payoffMode = new RadioButtonGroup<>("Payoff");
    payoffMode.setItems(
        "Title on the line", "Winner becomes #1 contender", "Winner earns the briefcase");
    payoffMode.setValue("Title on the line");
    payoffMode.setHelperText(
        "Contender: needs a linked championship with a reigning champion — the winner becomes its"
            + " #1 contender. Briefcase: no linked championship — the winner earns a cashable"
            + " title shot against ANY reigning champion."
            + " Team tournaments always put the title on the line (the winner becomes the"
            + " inaugural team champions).");
    payoffMode.setItemEnabledProvider(
        item -> teamModeOf(entrantType.getValue()) ? "Title on the line".equals(item) : true);
    titleCombo.addValueChangeListener(
        e -> {
          if (e.getValue() != null && "Winner earns the briefcase".equals(payoffMode.getValue())) {
            payoffMode.setValue("Title on the line");
          }
        });
    payoffMode.addValueChangeListener(
        e -> {
          boolean teamMode = teamModeOf(entrantType.getValue());
          boolean briefcase = "Winner earns the briefcase".equals(e.getValue());
          if (briefcase && !teamMode) {
            titleCombo.clear();
          }
          titleCombo.setEnabled(!briefcase);
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

    // One-off assembly: members not already entered; the assembled team persists as AD_HOC
    // unless "save to registry" (default off) flips it to REGISTRY after the tournament takes it.
    MultiSelectComboBox<Wrestler> teamMemberPicker =
        new MultiSelectComboBox<>("Assemble a One-Off Team (optional)");
    teamMemberPicker.setId("wizard-one-off-members");
    teamMemberPicker.setItemLabelGenerator(Wrestler::getName);
    teamMemberPicker.setWidthFull();
    teamMemberPicker.setVisible(false);
    teamMemberPicker.setHelperText("Pick exactly the number of wrestlers the format requires.");

    Checkbox saveToRegistryCheckbox = new Checkbox("Save this team to the team registry");
    saveToRegistryCheckbox.setId("wizard-save-to-registry");
    saveToRegistryCheckbox.setValue(false);
    saveToRegistryCheckbox.setVisible(false);

    Span oneOffNamePreview = new Span();
    oneOffNamePreview.getStyle().set("color", "var(--lumo-secondary-text-color)");
    oneOffNamePreview.setVisible(false);
    teamMemberPicker.addValueChangeListener(
        e -> {
          Set<Wrestler> members = e.getValue();
          saveToRegistryCheckbox.setVisible(!members.isEmpty());
          oneOffNamePreview.setVisible(!members.isEmpty());
          if (!members.isEmpty()) {
            // Preview only — the persisted name is uniquified via generateUniqueName at create.
            // Same alphabetical order the create handler uses, so the preview always matches
            // the stored member order.
            oneOffNamePreview.setText(
                "Team name: "
                    + members.stream()
                        .map(Wrestler::getName)
                        .sorted()
                        .collect(Collectors.joining(" & "))
                    + " (uniquified on create)");
          }
        });

    VerticalLayout teamEntrantsPanel =
        new VerticalLayout(teamMemberPicker, saveToRegistryCheckbox, oneOffNamePreview);
    teamEntrantsPanel.setPadding(false);
    teamEntrantsPanel.setVisible(false);

    VerticalLayout tab1Content =
        new VerticalLayout(
            nameField,
            formatCombo,
            payoffMode,
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

    // Registry picker + member picker follow the entrant type and gender filter: the picker
    // re-queries the eligible REGISTRY teams, the one-off picker re-filters to wrestlers not
    // already committed to a picked registry team (a wrestler may appear in only one entry) and
    // to the tournament's eligibility rules (active, gender-matching — the same predicate the
    // picker's items apply through the service).
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
          teamMemberPicker.setItems(
              wrestlerFacade.getWrestlerService().getAllWrestlers().stream()
                  .filter(
                      w ->
                          Boolean.TRUE.equals(w.getActive())
                              && (gender == null || w.getGender() == gender)
                              && !committedMemberIds.contains(w.getId()))
                  .toList());
        };
    // Payoff normalization across the entrant-type switch (ATW-01g2 review): team mode forces
    // Title-on-the-line (contender/briefcase are grayed out there); leaving team mode re-arms
    // them and clears a stale briefcase selection, which the briefcase listener would otherwise
    // apply silently against a linked TEAM title. Runs ONLY on an actual team→singles
    // transition — the gender filter also re-runs refreshTeamPickers and must not clobber a
    // briefcase the booker picked in singles mode.
    Runnable resetPayoffOnLeavingTeamMode =
        () -> {
          if (!teamModeOf(entrantType.getValue())
              && "Winner earns the briefcase".equals(payoffMode.getValue())) {
            payoffMode.setValue("Title on the line");
          }
        };
    Runnable refreshTeamPickers =
        () -> {
          boolean teamMode = teamModeOf(entrantType.getValue());
          teamEntrantsPanel.setVisible(teamMode);
          teamMemberPicker.setVisible(teamMode);
          // Seeding-tab visibility mirrors the singles wrestler picker: only Manual mode shows
          // the pickers. Initial wizard state is Auto, so everything starts hidden either way.
          boolean manualSeeding = "Manual (pick wrestlers)".equals(seedingMode.getValue());
          teamRegistryPicker.setVisible(teamMode && manualSeeding);
          teamMemberPicker.setVisible(teamMode);
          teamEntrantsPanel.setVisible(teamMode && manualSeeding);
          // Entering team mode forces Title-on-the-line (the payoff normalization above only
          // touches the radio when LEAVING team mode).
          if (teamMode && !"Title on the line".equals(payoffMode.getValue())) {
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
            // Leaving team mode clears any half-assembled one-off team.
            teamMemberPicker.clear();
            teamRegistryPicker.clear();
            saveToRegistryCheckbox.setVisible(false);
            oneOffNamePreview.setVisible(false);
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
          boolean panelVisible = manual && teamMode;
          saveToRegistryCheckbox.setVisible(panelVisible && !teamMemberPicker.isEmpty());
          oneOffNamePreview.setVisible(panelVisible && !teamMemberPicker.isEmpty());
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
                TeamSize teamSize = teamSizeOf(entrantType.getValue());
                Tournament t =
                    tournamentService.createTournament(
                        nameField.getValue(),
                        formatCombo.getValue().getFormatId(),
                        universe.orElse(null),
                        briefcaseSelected ? null : titleCombo.getValue(),
                        LocalDate.now(),
                        new ArrayList<>(rulesPicker.getSelectedItems()),
                        hostShowCombo.getValue(),
                        payoffTypeCombo.getValue(),
                        payoffRuleCombo.getValue(),
                        genderCombo.getValue(),
                        contenderSelected,
                        briefcaseSelected,
                        teamSize);
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
                    if (!teamMemberPicker.isEmpty()) {
                      // Deterministic order: same sort the name preview uses, so the stored
                      // member order always matches what the booker saw.
                      List<Wrestler> members =
                          teamMemberPicker.getSelectedItems().stream()
                              .sorted(Comparator.comparing(Wrestler::getName))
                              .toList();
                      // Re-check at create time: a registry team picked after assembly may have
                      // claimed one of these members (a wrestler appears in one entry only).
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
                      Team oneOff =
                          teamService
                              .createTeam(
                                  teamService.generateUniqueName(members),
                                  null,
                                  members.stream().map(Wrestler::getId).toList(),
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

  /** Ordered member display names joined with " & " — the team's lineup as a single string. */
  private static String memberNamesOf(@NonNull Team team) {
    return team.getMembers().stream()
        .map(TeamMember::getWrestler)
        .map(Wrestler::getName)
        .collect(Collectors.joining(" & "));
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
