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

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * "Arrange Match-ups" dialog (ATW-hw6m): hand-pick the round-1 opponents of a single-elimination
 * tournament before the bracket is generated. Saving maps the chosen pairings onto a seed order and
 * hands it to the caller — seed order remains the single source of truth for bracket generation
 * (the mapping is the exact inverse of {@code SingleEliminationFormat.generateBracket}'s round-1
 * contract, so Start Tournament generates precisely the arranged match-ups).
 */
public class ArrangeMatchupsDialog extends Dialog {

  /**
   * Map desired round-1 pairings onto a seed order. Let bySeed = entrants sorted by current seed
   * (index 0 = seed 1), n = entrant count; pairings[j-1] lands at seed positions j and n+1-j
   * (1-based); an odd-sized bracket keeps its middle seed (bySeed[n/2]) at the bye position.
   *
   * <p>Throws when pairings.size() != n/2, any slot is null, the same entrant is picked twice, or
   * the bye entrant is picked into a match.
   */
  static List<Long> toSeedOrder(List<TournamentEntry> bySeed, List<TournamentEntry[]> pairings) {
    int n = bySeed.size();
    int expectedMatches = n / 2;
    if (pairings.size() != expectedMatches) {
      throw new IllegalArgumentException(
          "Expected " + expectedMatches + " round-1 pairings, got " + pairings.size());
    }

    Long[] order = new Long[n];
    Set<Long> placed = new HashSet<>();
    for (int j = 0; j < expectedMatches; j++) {
      TournamentEntry[] pairing = pairings.get(j);
      if (pairing == null || pairing.length != 2 || pairing[0] == null || pairing[1] == null) {
        throw new IllegalArgumentException("Match " + (j + 1) + " is missing a wrestler");
      }
      place(order, placed, j, pairing[0]);
      place(order, placed, n - 1 - j, pairing[1]);
    }
    // Odd-sized bracket: the middle seed keeps its bye — it must not appear in any pairing.
    if (n % 2 != 0) {
      TournamentEntry bye = bySeed.get(n / 2);
      if (placed.contains(bye.getId())) {
        throw new IllegalArgumentException(
            bye.entrantName() + " has the bye and cannot be picked into a match");
      }
      order[n / 2] = bye.getId();
    }

    for (int i = 0; i < n; i++) {
      if (order[i] == null) {
        throw new IllegalArgumentException("Seed position " + (i + 1) + " has no wrestler");
      }
    }
    return List.of(order);
  }

  private static void place(Long[] order, Set<Long> placed, int position, TournamentEntry entry) {
    if (!placed.add(entry.getId())) {
      throw new IllegalArgumentException(entry.entrantName() + " is picked more than once");
    }
    order[position] = entry.getId();
  }

  private final List<ComboBox<TournamentEntry>> slotPickers = new ArrayList<>();
  private List<TournamentEntry> bySeed;
  private final Span errorLabel = new Span();
  private final Button saveBtn = new Button("Save Pairings");
  private final Consumer<List<Long>> onSave;

  /**
   * @param tournament a SCHEDULED single-elimination tournament (caller gates visibility)
   * @param onSave receives the new seed order (entry ids, seed 1 first) on save
   */
  public ArrangeMatchupsDialog(Tournament tournament, Consumer<List<Long>> onSave) {
    this.onSave = onSave;
    setHeaderTitle("Arrange Match-ups");

    List<TournamentEntry> bySeed =
        tournament.getEntries().stream()
            .sorted(Comparator.comparingInt(TournamentEntry::getSeed))
            .toList();
    this.bySeed = bySeed;
    int n = bySeed.size();

    VerticalLayout body = new VerticalLayout();
    body.setPadding(false);
    body.setSpacing(true);
    body.add(new Span("Round 1 match-ups — each match lands at the outer seed positions:"));
    for (int j = 0; j < n / 2; j++) {
      body.add(buildMatchRow(j + 1, bySeed, bySeed.get(j), bySeed.get(n - 1 - j)));
    }
    if (n % 2 != 0) {
      TournamentEntry bye = bySeed.get(n / 2);
      Span byeLabel = new Span("Bye: " + bye.entrantName() + " — sits out round 1");
      byeLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
      body.add(byeLabel);
    }
    body.add(
        new Span(
            "Saving reshapes the seed order so Start Tournament generates exactly these"
                + " match-ups. Drag rows on the grid behind this dialog to change who gets the"
                + " bye (odd-sized brackets)."));
    add(body);

    errorLabel.getStyle().set("color", "var(--lumo-error-text-color)");
    errorLabel.setVisible(false);
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.setEnabled(false);
    saveBtn.addClickListener(e -> save());
    getFooter().add(new Button("Cancel", e -> close()));
    getFooter().add(saveBtn);

    revalidate();
  }

  private HorizontalLayout buildMatchRow(
      int matchNumber, List<TournamentEntry> bySeed, TournamentEntry slotA, TournamentEntry slotB) {
    HorizontalLayout row = new HorizontalLayout();
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.add(new Span("Match " + matchNumber + ":"));
    row.add(picker(bySeed, slotA));
    row.add(new Span("vs"));
    row.add(picker(bySeed, slotB));
    return row;
  }

  private ComboBox<TournamentEntry> picker(
      List<TournamentEntry> bySeed, TournamentEntry defaultEntry) {
    ComboBox<TournamentEntry> picker = new ComboBox<>();
    picker.setWidth("220px");
    picker.setItems(bySeed);
    picker.setItemLabelGenerator(TournamentEntry::entrantName);
    picker.setValue(defaultEntry);
    picker.addValueChangeListener(e -> revalidate());
    slotPickers.add(picker);
    return picker;
  }

  private void revalidate() {
    try {
      List<TournamentEntry[]> pairings = new ArrayList<>();
      for (int i = 0; i < slotPickers.size(); i += 2) {
        pairings.add(
            new TournamentEntry[] {
              slotPickers.get(i).getValue(), slotPickers.get(i + 1).getValue()
            });
      }
      toSeedOrder(currentBySeed(), pairings);
      errorLabel.setVisible(false);
      saveBtn.setEnabled(true);
    } catch (IllegalArgumentException ex) {
      errorLabel.setText(ex.getMessage());
      errorLabel.setVisible(true);
      saveBtn.setEnabled(false);
    }
  }

  private List<TournamentEntry> currentBySeed() {
    return bySeed;
  }

  private void save() {
    try {
      List<TournamentEntry[]> pairings = new ArrayList<>();
      for (int i = 0; i < slotPickers.size(); i += 2) {
        pairings.add(
            new TournamentEntry[] {
              slotPickers.get(i).getValue(), slotPickers.get(i + 1).getValue()
            });
      }
      onSave.accept(toSeedOrder(currentBySeed(), pairings));
      close();
    } catch (IllegalArgumentException ex) {
      errorLabel.setText(ex.getMessage());
      errorLabel.setVisible(true);
    }
  }
}
