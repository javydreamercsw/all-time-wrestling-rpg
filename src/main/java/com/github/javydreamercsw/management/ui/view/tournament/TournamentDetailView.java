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

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.tournament.QualifierGroupsFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.component.TournamentBracketComponent;
import com.github.javydreamercsw.management.ui.component.TournamentBracketPreviewModel;
import com.github.javydreamercsw.management.ui.component.TournamentEntityAdapter;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@Route(value = "tournament-detail/:tournamentId", layout = MainLayout.class)
@PageTitle("Tournament | ATW RPG")
@RolesAllowed({"ADMIN", "BOOKER", "PLAYER", "VIEWER"})
@Slf4j
public class TournamentDetailView extends VerticalLayout implements BeforeEnterObserver {

  private final TournamentService tournamentService;
  private final SegmentRuleService segmentRuleService;
  private final UniverseContextService universeContextService;
  private final ShowRepository showRepository;

  private Tournament tournament;

  private final VerticalLayout content = new VerticalLayout();

  @Autowired
  public TournamentDetailView(
      TournamentService tournamentService,
      ShowRepository showRepository,
      ShowFacade showFacade,
      ViewContext viewContext) {
    this.tournamentService = tournamentService;
    this.segmentRuleService = showFacade.getSegmentRuleService();
    this.showRepository = showRepository;
    this.universeContextService = viewContext.getUniverseContextService();

    setSizeFull();
    setPadding(false);
    content.setSizeFull();
    add(content);
  }

  /** Test hooks: bypass route-parameter resolution and render directly (Karibu tests). */
  void setTournamentForTest(Tournament tournament) {
    this.tournament = tournament;
  }

  void buildContentForTest() {
    buildContent();
  }

  void openReplaceDialogForTest(TournamentEntry entry) {
    openReplaceDialog(entry);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String idStr = event.getRouteParameters().get("tournamentId").orElse(null);
    if (idStr == null) {
      event.forwardTo("tournament-list");
      return;
    }
    try {
      long id = Long.parseLong(idStr);
      tournament = tournamentService.findByIdWithDetails(id).orElse(null);
      if (tournament == null) {
        event.forwardTo("tournament-list");
        return;
      }
      buildContent();
    } catch (NumberFormatException e) {
      event.forwardTo("tournament-list");
    }
  }

  private void buildContent() {
    content.removeAll();
    content.add(buildToolbar());
    content.add(buildInfo());
    content.add(buildEntrantsGrid());

    if (!tournament.getRounds().isEmpty()) {
      content.add(buildBracketSection());
      content.add(buildRoundRulesSection());
      if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
        buildActiveMatchControls().ifPresent(content::add);
      }
    }
  }

  private ViewToolbar buildToolbar() {
    Button backBtn =
        new Button("← Back", e -> getUI().ifPresent(ui -> ui.navigate("tournament-list")));

    HorizontalLayout actions = new HorizontalLayout();

    if (tournament.getStatus() == TournamentStatus.SCHEDULED
        && !tournament.getEntries().isEmpty()) {
      Button previewBtn = new Button("Preview Bracket", e -> showBracketPreview());
      previewBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
      previewBtn.setTooltipText("Show the match-ups 'Start Tournament' will generate");
      actions.add(previewBtn);

      Button startBtn = new Button("Start Tournament", e -> startTournament());
      startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      startBtn.setTooltipText(
          "Generate the first-round bracket from the seeded entrants (1 vs last, 2 vs"
              + " second-to-last, ...). The tournament switches to IN_PROGRESS and its rounds"
              + " can then be booked onto shows — or fed into a paired PLE template"
              + " automatically.");
      actions.add(startBtn);
    }

    if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
      Optional<TournamentRound> pendingRound =
          tournament.getRounds().stream()
              .filter(r -> r.getStatus() == TournamentRoundStatus.PENDING)
              .findFirst();
      pendingRound.ifPresent(
          round -> {
            Button bookBtn = new Button("Book Round on Show", e -> openBookRoundDialog(round));
            bookBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            actions.add(bookBtn);
          });

      Optional<TournamentRound> completeRound =
          tournament.getRounds().stream()
              .filter(r -> r.getStatus() == TournamentRoundStatus.COMPLETE)
              .reduce((a, b) -> b);
      if (completeRound.isPresent()
          && tournament.getRounds().stream()
              .noneMatch(r -> r.getStatus() == TournamentRoundStatus.PENDING)) {
        Button advanceBtn = new Button("Generate Next Round", e -> advanceRound());
        advanceBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        actions.add(advanceBtn);
      }
    }

    return new ViewToolbar(
        tournament.getName(), ViewToolbar.group(backBtn), ViewToolbar.group(actions));
  }

  private VerticalLayout buildInfo() {
    VerticalLayout info = new VerticalLayout();
    info.setPadding(false);
    info.setSpacing(false);

    Span status = new Span("Status: " + tournament.getStatus().name());
    Span format = new Span("Format: " + tournament.getFormatId().replace('_', ' '));
    Span entrants = new Span("Entrants: " + tournament.getEntries().size());

    info.add(status, format, entrants);
    if (QualifierGroupsFormat.FORMAT_ID.equals(tournament.getFormatId())) {
      info.add(
          new Span(
              "Qualifier group size: "
                  + (tournament.getQualifierGroupSize() != null
                      ? tournament.getQualifierGroupSize()
                      : "auto (≈3 per group)")));
    }
    if (tournament.getEditionOrdinal() != null) {
      // Recurring edition (ATW-o4ad): chain name/ordinal plus the cadence.
      info.add(
          new Span(
              "Edition: "
                  + (tournament.getEditionOrdinal() > 1
                      ? TournamentService.editionName("", tournament.getEditionOrdinal()).trim()
                      : "I (first)")
                  + " — "
                  + tournament.getRecurrence().name().toLowerCase()
                  + " recurring"));
      if (tournament.getParent() != null) {
        info.add(new Span("Previous edition: " + tournament.getParent().getName()));
      }
    }
    if (tournament.getLinkedTitle() != null) {
      info.add(new Span("Championship: " + tournament.getLinkedTitle().getName()));
    }
    if (tournament.getPayoffShow() != null) {
      // One-time tournament (ATW-xbn4): the payoff books on this show.
      Show host = tournament.getPayoffShow();
      info.add(
          new Span(
              "Host Show: "
                  + host.getName()
                  + (host.getShowDate() != null ? " (" + host.getShowDate() + ")" : "")));
    }

    List<SegmentRule> rules = tournament.getAllowedRules();
    if (!rules.isEmpty()) {
      FlexLayout chips = new FlexLayout();
      chips.getStyle().set("flex-wrap", "wrap").set("gap", "4px").set("margin-top", "4px");
      rules.forEach(
          r -> {
            Span chip = new Span(r.getName());
            chip.getElement()
                .setAttribute(
                    "style",
                    "background:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m);padding:2px"
                        + " 8px;font-size:var(--lumo-font-size-s)");
            chips.add(chip);
          });
      info.add(new Span("Allowed Rules:"));
      info.add(chips);
    }
    return info;
  }

  private VerticalLayout buildEntrantsGrid() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.add(new H4("Entrants"));

    Grid<TournamentEntry> grid = new Grid<>(TournamentEntry.class, false);
    grid.setAllRowsVisible(true);
    grid.addColumn(TournamentEntry::getSeed).setHeader("Seed").setWidth("80px").setFlexGrow(0);
    grid.addColumn(e -> e.getWrestler().getName()).setHeader("Wrestler").setFlexGrow(1);
    grid.addColumn(e -> e.getStatus().name()).setHeader("Status");
    grid.setItems(tournament.getEntries());

    // Seeds are editable until the bracket is generated (ATW-hw6m): move an entrant up/down to
    // change its seed, or swap in a different wrestler. The round-1 pairing is 1 vs last,
    // 2 vs second-to-last, ... so reordering changes the match-ups Start will generate.
    if (tournament.getStatus() == TournamentStatus.SCHEDULED
        && !tournament.getEntries().isEmpty()) {
      grid.addComponentColumn(this::buildSeedControls)
          .setHeader("Reorder")
          .setWidth("140px")
          .setKey("reorder");
      grid.addComponentColumn(
              entry -> {
                Button replaceBtn = new Button("Replace");
                replaceBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                replaceBtn.addClickListener(e -> openReplaceDialog(entry));
                return replaceBtn;
              })
          .setHeader("Swap")
          .setWidth("100px")
          .setKey("swap");
    }

    section.add(grid);
    return section;
  }

  /** Up/down buttons moving one entry a seed at a time; writes the new order to the service. */
  private HorizontalLayout buildSeedControls(TournamentEntry entry) {
    HorizontalLayout controls = new HorizontalLayout();
    controls.setSpacing(false);
    controls.setPadding(false);

    Button up = new Button(VaadinIcon.ARROW_UP.create());
    up.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    up.setTooltipText("Move up one seed");
    up.setEnabled(entry.getSeed() > 1);
    up.addClickListener(e -> moveSeed(entry, entry.getSeed() - 1));

    Button down = new Button(VaadinIcon.ARROW_DOWN.create());
    down.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    down.setTooltipText("Move down one seed");
    down.setEnabled(entry.getSeed() < tournament.getEntries().size());
    down.addClickListener(e -> moveSeed(entry, entry.getSeed() + 1));

    controls.add(up, down);
    return controls;
  }

  /** Swap the entry with whichever entry currently holds {@code targetSeed} and persist. */
  private void moveSeed(TournamentEntry entry, int targetSeed) {
    try {
      List<Long> reordered = new ArrayList<>();
      List<TournamentEntry> entries = tournament.getEntries();
      for (int seed = 1; seed < entries.size() + 1; seed++) {
        reordered.add(seed == targetSeed ? entry.getId() : entries.get(seed - 1).getId());
      }
      // The swapped-out entry takes the moved entry's original position.
      int original = entry.getSeed();
      reordered.set(original - 1, entries.get(targetSeed - 1).getId());
      tournamentService.reorderSeeds(tournament.getId(), reordered);
      refreshAfterSeedingEdit();
      Notification.show("Seed order updated", 2000, Notification.Position.BOTTOM_CENTER)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    } catch (Exception ex) {
      log.error("Error reordering seeds", ex);
      Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  /** Dialog replacing one entrant with another wrestler (same seed). */
  private void openReplaceDialog(TournamentEntry entry) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Replace seed " + entry.getSeed() + ": " + entry.getWrestler().getName());

    ComboBox<Wrestler> picker = new ComboBox<>("New wrestler");
    // Offer the eligible pool minus anyone already entered — the service rejects duplicates,
    // but showing them as choices would guarantee an error after selection.
    Set<Long> enteredIds = new HashSet<>();
    tournament.getEntries().forEach(e -> enteredIds.add(e.getWrestler().getId()));
    picker.setItems(
        tournamentService
            .findEligibleWrestlersSortedByFans(
                tournament.getLinkedTitle(), universeContextService.getCurrentUniverseId())
            .stream()
            .filter(w -> !enteredIds.contains(w.getId()))
            .toList());
    picker.setItemLabelGenerator(Wrestler::getName);
    picker.setWidth("320px");
    picker.setPlaceholder("Pick a replacement");
    if (picker.getListDataView().getItemCount() == 0) {
      picker.setHelperText("No eligible wrestlers available outside the current entrants.");
    }
    Button replaceBtn = new Button("Replace", e -> {});
    replaceBtn.setEnabled(false);
    picker.addValueChangeListener(
        e -> replaceBtn.setEnabled(e.getValue() != null && e.getValue() != entry.getWrestler()));

    replaceBtn.addClickListener(
        e -> {
          try {
            tournamentService.replaceEntrant(
                tournament.getId(), entry.getId(), picker.getValue().getId());
            dialog.close();
            refreshAfterSeedingEdit();
            Notification.show("Entrant replaced", 2000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    HorizontalLayout footer = new HorizontalLayout(replaceBtn);
    dialog.add(picker);
    dialog.getFooter().add(footer);
    dialog.getFooter().add(new Button("Cancel", ev -> dialog.close()));
    dialog.open();
  }

  /** Re-read the tournament graph and rebuild the view after a seeding edit. */
  private void refreshAfterSeedingEdit() {
    tournament = tournamentService.findByIdWithDetails(tournament.getId()).orElse(tournament);
    buildContent();
  }

  private VerticalLayout buildBracketSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.add(new H4("Bracket"));

    TournamentEntityAdapter model =
        new TournamentEntityAdapter(tournament, tournamentService.getAvailableFormats());
    section.add(new TournamentBracketComponent(model));
    return section;
  }

  /** Per-round fixed-rule assignment panel. Visible to ADMIN/BOOKER only via role check. */
  private VerticalLayout buildRoundRulesSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.add(new H4("Round Rules"));

    List<SegmentRule> allRules = segmentRuleService.findAll();

    for (TournamentRound round : tournament.getRounds()) {
      HorizontalLayout row = new HorizontalLayout();
      row.setAlignItems(Alignment.CENTER);

      Span label = new Span(round.getRoundName() + ":");
      label.getStyle().set("min-width", "120px");

      ComboBox<SegmentRule> ruleCombo = new ComboBox<>();
      ruleCombo.setItems(allRules);
      ruleCombo.setItemLabelGenerator(SegmentRule::getName);
      ruleCombo.setPlaceholder("From pool (random)");
      ruleCombo.setClearButtonVisible(true);
      ruleCombo.setValue(round.getFixedRule());
      ruleCombo.setWidth("260px");

      ruleCombo.addValueChangeListener(
          e -> {
            try {
              tournamentService.setRoundFixedRule(round, e.getValue());
              Notification.show(
                      round.getRoundName()
                          + " rule set to: "
                          + (e.getValue() != null ? e.getValue().getName() : "pool random"),
                      2000,
                      Notification.Position.BOTTOM_CENTER)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
              log.error("Error setting round rule", ex);
              Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                  .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
          });

      row.add(label, ruleCombo);
      section.add(row);
    }
    return section;
  }

  /**
   * Returns the winner-recording controls for any in-progress round that has unresolved matches.
   * Returns empty when there is nothing for the booker to act on.
   */
  private Optional<VerticalLayout> buildActiveMatchControls() {
    Optional<TournamentRound> inProgressRound =
        tournament.getRounds().stream()
            .filter(r -> r.getStatus() == TournamentRoundStatus.IN_PROGRESS)
            .findFirst();

    if (inProgressRound.isEmpty()) {
      return Optional.empty();
    }

    TournamentRound round = inProgressRound.get();
    List<TournamentMatch> pending =
        round.getMatches().stream().filter(m -> m.getWinner() == null).toList();

    if (pending.isEmpty()) {
      return Optional.empty();
    }

    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.add(new H4("Record Results — " + round.getRoundName()));

    for (TournamentMatch match : pending) {
      section.add(buildRecordMatchRow(match));
    }
    return Optional.of(section);
  }

  private HorizontalLayout buildRecordMatchRow(TournamentMatch match) {
    HorizontalLayout row = new HorizontalLayout();
    row.setAlignItems(Alignment.CENTER);

    List<TournamentEntry> entrants = match.entrants();
    String label =
        entrants.stream().map(e -> e.getWrestler().getName()).collect(Collectors.joining(" vs "));
    row.add(new Span(label));

    ComboBox<TournamentEntry> winnerPicker = new ComboBox<>("Pick winner");
    winnerPicker.setItems(entrants);
    winnerPicker.setItemLabelGenerator(e -> e.getWrestler().getName());

    Button recordBtn =
        new Button(
            "Record",
            e -> {
              TournamentEntry selected = winnerPicker.getValue();
              if (selected == null) {
                Notification.show("Select a winner first.", 2000, Notification.Position.MIDDLE);
                return;
              }
              try {
                tournamentService.recordMatchResult(match, selected);
                tournament =
                    tournamentService.findByIdWithDetails(tournament.getId()).orElse(tournament);
                buildContent();
              } catch (Exception ex) {
                log.error("Error recording match result", ex);
                Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });

    row.add(winnerPicker, recordBtn);
    return row;
  }

  private void startTournament() {
    try {
      tournament = tournamentService.startTournament(tournament);
      tournament = tournamentService.findByIdWithDetails(tournament.getId()).orElse(tournament);
      buildContent();
      Notification.show("Tournament started!", 3000, Notification.Position.BOTTOM_CENTER)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    } catch (Exception e) {
      log.error("Error starting tournament", e);
      Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  /**
   * Show the match-ups Start Tournament would generate, without committing — rendered with the same
   * bracket viewer the in-progress tournament uses, fed an in-memory preview model.
   */
  private void showBracketPreview() {
    Dialog preview = new Dialog();
    preview.setHeaderTitle("Bracket Preview (not started yet)");
    preview.setWidth("min(900px, 95vw)");

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    if (tournament.getEntries().size() < 2) {
      content.add(new Span("Not enough entrants for a bracket."));
    } else {
      content.add(
          new TournamentBracketComponent(
              new TournamentBracketPreviewModel(tournament, resolveRenderMode())));
      content.add(
          new Span("Starting commits this bracket and switches the tournament to IN_PROGRESS."));
    }
    preview.add(content);
    preview.getFooter().add(new Button("Close", e -> preview.close()));
    preview.open();
  }

  private TournamentFormat.RenderMode resolveRenderMode() {
    return tournamentService
        .findFormat(tournament.getFormatId())
        .map(TournamentFormat::renderMode)
        .orElse(TournamentFormat.RenderMode.TREE);
  }

  private void advanceRound() {
    try {
      tournamentService.advanceToNextRound(tournament);
      tournament = tournamentService.findByIdWithDetails(tournament.getId()).orElse(tournament);
      buildContent();
      Notification.show("Next round generated!", 3000, Notification.Position.BOTTOM_CENTER)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    } catch (Exception e) {
      log.error("Error advancing round", e);
      Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void openBookRoundDialog(TournamentRound round) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Book Round on Show");

    List<Show> upcoming =
        showRepository.findByShowDateGreaterThanEqualOrderByShowDate(
            LocalDate.now(), PageRequest.of(0, 20));

    ComboBox<Show> showCombo = new ComboBox<>("Show");
    showCombo.setItems(upcoming);
    showCombo.setItemLabelGenerator(
        s -> s.getName() + (s.getShowDate() != null ? " (" + s.getShowDate() + ")" : ""));
    showCombo.setWidthFull();

    if (!upcoming.isEmpty()) {
      showCombo.setValue(upcoming.get(0));
    }

    Button cancel = new Button("Cancel", e -> dialog.close());
    Button book =
        new Button(
            "Book",
            e -> {
              Show selected = showCombo.getValue();
              if (selected == null) {
                Notification.show("Select a show.", 2000, Notification.Position.MIDDLE);
                return;
              }
              try {
                tournamentService.bookRoundOnShow(tournament, round, selected);
                dialog.close();
                tournament =
                    tournamentService.findByIdWithDetails(tournament.getId()).orElse(tournament);
                buildContent();
                Notification.show(
                        "Round booked on " + selected.getName() + "!",
                        3000,
                        Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              } catch (Exception ex) {
                log.error("Error booking round", ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    book.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    dialog.add(new VerticalLayout(showCombo));
    dialog.getFooter().add(cancel, book);
    dialog.open();
  }
}
