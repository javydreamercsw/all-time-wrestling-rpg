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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Minimal editable team layout: one {@link MultiSelectComboBox} per team with add/remove rows and
 * automatic renumbering (team numbers are 1-based row positions, matching {@code
 * Segment.syncParticipants(Map)}).
 *
 * <p>Deliberately a small standalone component — {@code EditSegmentDialog} carries a much richer
 * team editor (gender filters, intergender enforcement, roster-team quick-pick, health fields) that
 * is welded to its dialog logic; a real shared extraction is tracked separately.
 *
 * <p>The extended constructor adds the tournament wizard's needs: a per-row candidate filter
 * (re-evaluated on every refresh, force-including the row's own current value so existing
 * selections never silently vanish — the EditSegmentDialog rule), an exact per-row team size (a duo
 * row must hold exactly 2), and row-count bounds.
 */
public class TeamRowsEditor extends VerticalLayout {

  private final List<MultiSelectComboBox<Wrestler>> teamCombos = new ArrayList<>();
  private final VerticalLayout rowsContainer;
  private final List<Wrestler> candidates;
  private final Runnable changeListener;
  private Predicate<Wrestler> rowFilter;
  private final Integer requiredTeamSize;
  private final int minRows;
  private final int maxRows;
  private boolean refreshingRowItems;

  /**
   * @param candidates every wrestler selectable into any team
   * @param changeListener invoked after any team membership change (may be null)
   */
  public TeamRowsEditor(List<Wrestler> candidates, Runnable changeListener) {
    this(candidates, changeListener, null, null, 0, Integer.MAX_VALUE);
  }

  /**
   * Extended construction: tournament-style team assembly.
   *
   * @param rowFilter per-row candidate filter, re-evaluated on every refresh (may be null =
   *     unfiltered); a row's own current value always stays selectable
   * @param requiredTeamSize exact member count every non-empty row must hold (null = any size)
   * @param minRows empty rows seeded when the editor starts out empty (the wizard wants one visible
   *     row immediately; {@code 0} keeps the legacy empty-start behavior)
   * @param maxRows upper bound on rows
   */
  public TeamRowsEditor(
      List<Wrestler> candidates,
      Runnable changeListener,
      Predicate<Wrestler> rowFilter,
      Integer requiredTeamSize,
      int minRows,
      int maxRows) {
    this.candidates = candidates != null ? new ArrayList<>(candidates) : List.of();
    this.changeListener = changeListener;
    this.rowFilter = rowFilter;
    this.requiredTeamSize = requiredTeamSize;
    this.minRows = minRows;
    this.maxRows = maxRows;

    setPadding(false);
    setSpacing(false);
    setWidthFull();

    rowsContainer = new VerticalLayout();
    rowsContainer.setPadding(false);
    rowsContainer.setSpacing(false);
    rowsContainer.setWidthFull();

    Button addTeamButton = new Button("+ Add Team", e -> addTeamRow());
    addTeamButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

    add(rowsContainer, addTeamButton);
    while (teamCombos.size() < minRows) {
      addTeamRow();
    }
  }

  /** Adds an empty team row; pre-seed the arc's own wrestlers on team 1 from the caller. */
  public void addTeamRow() {
    MultiSelectComboBox<Wrestler> combo =
        new MultiSelectComboBox<>("Team " + (teamCombos.size() + 1));
    combo.setItems(filteredCandidatesFor(null));
    combo.setItemLabelGenerator(Wrestler::getName);
    combo.setWidthFull();
    combo.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
    combo.addValueChangeListener(
        e -> {
          enforceTeamSize(combo);
          refreshRowItems();
          // The refresh re-pins values (setItems resets a Vaadin combobox's selection); the
          // synthetic re-fire must not count as a booker change.
          if (!refreshingRowItems) {
            notifyChanged();
          }
        });
    teamCombos.add(combo);

    HorizontalLayout row = new HorizontalLayout();
    row.setWidthFull();
    row.setAlignItems(FlexComponent.Alignment.END);
    Button removeButton = new Button("−", ev -> removeTeamRow(combo));
    removeButton.addThemeVariants(
        ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    removeButton.getElement().setAttribute("aria-label", "Remove team");
    row.add(combo, removeButton);
    row.setFlexGrow(1, combo);
    rowsContainer.add(row);
  }

  /** Adds a team row pre-filled with the given members. */
  public void addTeamRow(Collection<Wrestler> members) {
    addTeamRow();
    MultiSelectComboBox<Wrestler> combo = teamCombos.get(teamCombos.size() - 1);
    combo.setValue(new HashSet<>(members));
  }

  private void removeTeamRow(MultiSelectComboBox<Wrestler> combo) {
    int index = teamCombos.indexOf(combo);
    if (index < 0 || teamCombos.size() <= 1 || teamCombos.size() <= minRows) {
      return; // keep the row floor
    }
    teamCombos.remove(index);
    rowsContainer.remove(rowsContainer.getComponentAt(index));
    renumberTeams();
    refreshRowItems();
    notifyChanged();
  }

  private void renumberTeams() {
    for (int i = 0; i < teamCombos.size(); i++) {
      teamCombos.get(i).setLabel("Team " + (i + 1));
    }
  }

  /**
   * Re-filters every row's items: the base filter (recalculated — its closure may capture registry
   * picks or filters that changed) minus wrestlers taken by other rows, plus the row's own current
   * value, which never drops out of its own picker. {@code setItems} resets a Vaadin combobox's
   * selection, so a row's items are only replaced when the visible candidate set actually changed —
   * and the whole refresh is re-entrancy-guarded (the listener below triggers it on every value
   * change).
   */
  public void refreshRowItems() {
    if (refreshingRowItems) {
      return;
    }
    refreshingRowItems = true;
    try {
      for (MultiSelectComboBox<Wrestler> combo : teamCombos) {
        Set<Wrestler> own = new LinkedHashSet<>(combo.getValue());
        List<Wrestler> items = new ArrayList<>(filteredCandidatesFor(own));
        for (MultiSelectComboBox<Wrestler> other : teamCombos) {
          if (other == combo) {
            continue;
          }
          items.removeAll(other.getValue());
        }
        items.removeAll(own);
        if (!items.equals(getItemsOf(combo))) {
          combo.setItems(items);
          combo.setValue(own); // re-pin after setItems: setItems resets the selection
        }
      }
    } finally {
      refreshingRowItems = false;
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Wrestler> getItemsOf(MultiSelectComboBox<Wrestler> combo) {
    return ((MultiSelectComboBox<Wrestler>) combo).getListDataView().getItems().toList();
  }

  /** Whether every non-empty row holds exactly the required size (any size when unset). */
  public boolean rowsMatchRequiredSize() {
    if (requiredTeamSize == null) {
      return true;
    }
    return teamCombos.stream()
        .filter(combo -> !combo.getValue().isEmpty())
        .allMatch(combo -> combo.getValue().size() == requiredTeamSize);
  }

  /**
   * Replaces the per-row filter and immediately re-filters — the wizard calls this when the gender
   * filter or the picked registry teams change (its closure recomputes the exclusions). The row's
   * own current value never drops out of its own picker.
   */
  public void refreshRowItemsWithFilter(Predicate<Wrestler> newFilter) {
    this.rowFilter = newFilter;
    refreshRowItems();
  }

  private void enforceTeamSize(MultiSelectComboBox<Wrestler> combo) {
    if (requiredTeamSize == null) {
      return;
    }
    Set<Wrestler> value = combo.getValue();
    if (value.size() > requiredTeamSize) {
      combo.setValue(new LinkedHashSet<>(value.stream().limit(requiredTeamSize).toList()));
    }
  }

  /** True when there are at least two non-empty teams and no wrestler sits on two teams. */
  public boolean isValid() {
    List<Wrestler> assigned = new ArrayList<>();
    int nonEmpty = 0;
    for (MultiSelectComboBox<Wrestler> combo : teamCombos) {
      if (combo.getValue().isEmpty()) {
        continue;
      }
      nonEmpty++;
      for (Wrestler wrestler : combo.getValue()) {
        if (assigned.stream().anyMatch(w -> w.getId().equals(wrestler.getId()))) {
          return false;
        }
        assigned.add(wrestler);
      }
    }
    return nonEmpty >= 2;
  }

  private List<Wrestler> filteredCandidatesFor(Collection<Wrestler> ownValue) {
    if (rowFilter == null) {
      return candidates;
    }
    List<Wrestler> items = new ArrayList<>();
    for (Wrestler candidate : candidates) {
      if (rowFilter.test(candidate) || (ownValue != null && ownValue.contains(candidate))) {
        items.add(candidate);
      }
    }
    return items;
  }

  /** Current layout: 1-based team number → selected wrestlers (empty teams skipped). */
  public Map<Integer, List<Wrestler>> getTeams() {
    Map<Integer, List<Wrestler>> teams = new LinkedHashMap<>();
    int teamNumber = 1;
    for (MultiSelectComboBox<Wrestler> combo : teamCombos) {
      if (!combo.getValue().isEmpty()) {
        teams.put(teamNumber, new ArrayList<>(combo.getValue()));
      }
      teamNumber++;
    }
    return teams;
  }

  /** Replaces the layout with the given teams (in map order, keys ignored). */
  public void setTeams(Map<Integer, ? extends Collection<Wrestler>> teams) {
    clear();
    if (teams == null || teams.isEmpty()) {
      addTeamRow();
      return;
    }
    for (Collection<Wrestler> members : teams.values()) {
      addTeamRow(members);
    }
  }

  public void clear() {
    teamCombos.clear();
    rowsContainer.removeAll();
  }

  /** True when any row carries a selection. */
  public boolean hasAnySelection() {
    return teamCombos.stream().anyMatch(combo -> !combo.getValue().isEmpty());
  }

  /** How many rows the editor currently shows. */
  public int rowCount() {
    return teamCombos.size();
  }

  private void notifyChanged() {
    if (changeListener != null) {
      changeListener.run();
    }
  }
}
