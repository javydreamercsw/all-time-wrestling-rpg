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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal editable team layout: one {@link MultiSelectComboBox} per team with add/remove rows and
 * automatic renumbering (team numbers are 1-based row positions, matching {@code
 * Segment.syncParticipants(Map)}).
 *
 * <p>Deliberately a small standalone component — {@code EditSegmentDialog} carries a much richer
 * team editor (gender filters, intergender enforcement, roster-team quick-pick, health fields) that
 * is welded to its dialog logic; a real shared extraction is tracked separately.
 */
public class TeamRowsEditor extends VerticalLayout {

  private final List<MultiSelectComboBox<Wrestler>> teamCombos = new ArrayList<>();
  private final VerticalLayout rowsContainer;
  private final List<Wrestler> candidates;
  private final Runnable changeListener;

  /**
   * @param candidates every wrestler selectable into any team
   * @param changeListener invoked after any team membership change (may be null)
   */
  public TeamRowsEditor(List<Wrestler> candidates, Runnable changeListener) {
    this.candidates = candidates != null ? new ArrayList<>(candidates) : List.of();
    this.changeListener = changeListener;

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
  }

  /** Adds an empty team row; pre-seed the arc's own wrestlers on team 1 from the caller. */
  public void addTeamRow() {
    MultiSelectComboBox<Wrestler> combo =
        new MultiSelectComboBox<>("Team " + (teamCombos.size() + 1));
    combo.setItems(candidates);
    combo.setItemLabelGenerator(Wrestler::getName);
    combo.setWidthFull();
    combo.addThemeVariants(
        com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant.LUMO_SMALL);
    combo.addValueChangeListener(e -> notifyChanged());
    teamCombos.add(combo);

    HorizontalLayout row = new HorizontalLayout();
    row.setWidthFull();
    row.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
    Button removeButton = new Button("−", ev -> removeTeamRow(combo));
    removeButton.addThemeVariants(
        com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR,
        com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY,
        com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
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
    if (index < 0 || teamCombos.size() <= 1) {
      return; // keep at least one team row
    }
    teamCombos.remove(index);
    rowsContainer.remove(rowsContainer.getComponentAt(index));
    renumberTeams();
    notifyChanged();
  }

  private void renumberTeams() {
    for (int i = 0; i < teamCombos.size(); i++) {
      teamCombos.get(i).setLabel("Team " + (i + 1));
    }
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

  private void notifyChanged() {
    if (changeListener != null) {
      changeListener.run();
    }
  }
}
