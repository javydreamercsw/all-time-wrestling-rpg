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
package com.github.javydreamercsw.management.ui.view.feud;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudBeatParticipantRole;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.feud.AiSuggestedOpponentDTO;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.github.javydreamercsw.management.ui.component.TeamRowsEditor;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/** Unit tests for the shared beat editor (wizard + add/edit dialogs). */
class BeatEditorTest extends AbstractViewTest {

  @Mock private FeudBeatAssistantService assistant;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler externalWrestler;
  private Title title;
  private FeudScript script;

  @BeforeEach
  void setup() {
    wrestler1 = wrestler(1L, "Adam Axe");
    wrestler2 = wrestler(2L, "Bob Boulder");
    externalWrestler = wrestler(30L, "Randy Orton");

    title = new Title();
    title.setId(5L);
    title.setName("World Title");

    script = new FeudScript();
    script.setName("Test Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setMaxPleAppearances(2);
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private static Show show(Long id, String name, boolean ple) {
    Show show = new Show();
    show.setId(id);
    show.setName(name);
    show.setShowDate(LocalDate.of(2026, 10, 1));
    if (ple) {
      ShowType pleType = new ShowType();
      pleType.setName("Premium Live Event");
      pleType.setCategory(ShowCategory.PLE);
      ShowTemplate template = new ShowTemplate();
      template.setShowType(pleType);
      show.setTemplate(template);
    }
    return show;
  }

  private BeatEditor newEditor() {
    BeatEditor editor = new BeatEditor(fullContext());
    UI.getCurrent().add(editor);
    return editor;
  }

  private BeatEditor.BeatEditorContext fullContext() {
    return new BeatEditor.BeatEditorContext(
        List.of(wrestler1, wrestler2),
        List.of(externalWrestler),
        List.of(show(100L, "Weekly Show", false), show(101L, "Big Event", true)),
        List.of(title),
        List.of("Singles Match", "Tag Team Match"),
        List.of("No Disqualification"),
        true,
        editor -> {},
        assistant);
  }

  @Test
  @DisplayName("Empty editor is invalid; picking a match type alone makes it valid")
  void isValid_requiresMatchType() {
    BeatEditor editor = newEditor();
    assertFalse(editor.isValid());

    matchType(editor).setValue("Singles Match");
    assertTrue(editor.isValid());
  }

  @Test
  @DisplayName("Winner control toggles the planned-winner picker and value lands on the beat")
  void bookerPicks_setsPlannedWinnerOnBeat() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Singles Match");

    winnerControl(editor).setValue("Booker Picks");
    plannedWinner(editor).setValue(wrestler2);

    FeudScriptBeat beat = editor.toBeat();
    assertEquals(FeudScriptWinnerControl.BOOKER_PICKS, beat.getWinnerControl());
    assertEquals(wrestler2, beat.getPlannedWinner());
    assertEquals(FeudScriptBeatStatus.PENDING, beat.getBeatStatus());
  }

  @Test
  @DisplayName("System Roll leaves the planned winner unset")
  void systemRoll_plannedWinnerNotSet() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Singles Match");
    winnerControl(editor).setValue("System Roll");

    FeudScriptBeat beat = editor.toBeat();
    assertEquals(FeudScriptWinnerControl.SYSTEM_ROLL, beat.getWinnerControl());
    assertNull(beat.getPlannedWinner());
  }

  @Test
  @DisplayName("toBeat carries stipulation, culmination and notes")
  void toBeat_carriesOptionalFields() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Singles Match");
    stipulation(editor).setValue("No Disqualification");
    culmination(editor).setValue(true);
    notes(editor).setValue("Blowoff angle");

    FeudScriptBeat beat = editor.toBeat();
    assertEquals("No Disqualification", beat.getSegmentRule());
    assertTrue(beat.isCulmination());
    assertEquals("Blowoff angle", beat.getNotes());
  }

  @Test
  @DisplayName(
      "External opponent lands on the beat as OPPONENT; picking them as extra too is invalid")
  void toBeat_externals_becomeOpponentAndExtras() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Singles Match");
    opponent(editor).setValue(externalWrestler);

    FeudScriptBeat beat = editor.toBeat();
    assertEquals(1, beat.getExternalParticipants().size());
    assertEquals(externalWrestler.getName(), beat.getExternalOpponents().get(0).getName());

    // Selecting the same wrestler as an extra is invalid.
    extras(editor).setValue(Set.of(externalWrestler));
    assertFalse(editor.isValid());
  }

  @Test
  @DisplayName("Custom team layout maps arc wrestlers to FEUD_MEMBER and others to OPPONENT")
  void customTeams_rolesMappedByArcMembership() {
    BeatEditor editor = newEditor();
    editor.bindScriptContext(script, List.of(wrestler1, wrestler2));
    matchType(editor).setValue("Tag Team Match");
    customTeams(editor).setValue(true);

    // Pre-seeded: team 1 = arc wrestlers. Team 2 gets the external (OPPONENT).
    teamRowCombo(editor, 0).setValue(Set.of(wrestler1, wrestler2));
    teamRows(editor).addTeamRow(List.of(externalWrestler));

    assertTrue(editor.isValid());
    FeudScriptBeat beat = editor.toBeat();

    List<Wrestler> feudMembers =
        beat.getExternalParticipants().stream()
            .filter(p -> p.getRole() == FeudBeatParticipantRole.FEUD_MEMBER)
            .map(p -> p.getWrestler())
            .toList();
    assertEquals(2, feudMembers.size());
    assertTrue(feudMembers.containsAll(List.of(wrestler1, wrestler2)));
    assertEquals(externalWrestler.getName(), beat.getExternalOpponents().get(0).getName());
  }

  @Test
  @DisplayName("Invalid custom layout (one team empty) is rejected and reported")
  void customTeams_oneEmptyTeam_invalid() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Tag Team Match");
    customTeams(editor).setValue(true);
    teamRowCombo(editor, 0).setValue(Set.of(wrestler1));

    assertFalse(editor.isValid());
    editor.showValidationError(); // must not throw
  }

  @Test
  @DisplayName(
      "Title stakes: title checked without a title selected is invalid, then valid once picked")
  void titleStakes_requiresTitle() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Singles Match");
    titleOnTheLine(editor).setValue(true);

    assertFalse(editor.isValid());
    editor.showValidationError();

    titlesMulti(editor).setValue(Set.of(title));
    assertTrue(editor.isValid());
    FeudScriptBeat beat = editor.toBeat();
    assertTrue(beat.isTitleStakes());
    assertTrue(beat.getTitles().contains(title));
  }

  @Test
  @DisplayName("#1 contender without a title is invalid; picking one sets contenderTitle")
  void contenderTitle_requiresSelection() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Singles Match");
    contenderCheck(editor).setValue(true);
    assertFalse(editor.isValid());

    contenderTitle(editor).setValue(title);
    assertTrue(editor.isValid());
    assertEquals(title, editor.toBeat().getContenderTitle());
  }

  @Test
  @DisplayName("Target show lands on the beat")
  void targetShow_landsOnBeat() {
    BeatEditor editor = newEditor();
    matchType(editor).setValue("Singles Match");
    editor.bindScriptContext(script, List.of(wrestler1, wrestler2));

    targetShow(editor).setValue(show(101L, "Big Event", true));
    assertEquals("Big Event", editor.toBeat().getTargetShow().getName());
  }

  @Test
  @DisplayName("setBeat pre-fills every field from the pending beat")
  void setBeat_prefillsAllFields() {
    BeatEditor editor = newEditor();

    FeudScriptBeat existing = new FeudScriptBeat();
    existing.setSegmentType("Tag Team Match");
    existing.setSegmentRule("No Disqualification");
    existing.setWinnerControl(FeudScriptWinnerControl.BOOKER_PICKS);
    existing.setPlannedWinner(wrestler1);
    existing.setCulmination(true);
    existing.setNotes("Old notes");
    existing.addExternalParticipant(externalWrestler, FeudBeatParticipantRole.OPPONENT);

    editor.setBeat(existing, List.of());

    assertEquals("Tag Team Match", matchType(editor).getValue());
    assertEquals("No Disqualification", stipulation(editor).getValue());
    assertEquals("Booker Picks", winnerControl(editor).getValue());
    assertEquals(wrestler1, plannedWinner(editor).getValue());
    assertTrue(culmination(editor).getValue());
    assertEquals("Old notes", notes(editor).getValue());
    assertEquals(externalWrestler, opponent(editor).getValue());
  }

  @Test
  @DisplayName("setBeat re-adds persisted participants that dropped off the candidate list")
  void setBeat_persistedParticipantMissingFromCandidates_stillSelectable() {
    BeatEditor editor = newEditor();

    FeudScriptBeat existing = new FeudScriptBeat();
    existing.setSegmentType("Singles Match");
    existing.setWinnerControl(FeudScriptWinnerControl.AI_PICKS);
    existing.addExternalParticipant(externalWrestler, FeudBeatParticipantRole.EXTRA);

    editor.setBeat(existing, List.of());
    assertTrue(editor.getSelectedExternals().contains(externalWrestler));
  }

  @Test
  @DisplayName("AI suggestion fills the opponent combo and rationale (no UI: synchronous path)")
  void aiSuggestion_fillsOpponentAndRationale() throws Exception {
    BeatEditor editor = newEditor();
    editor.bindScriptContext(script, List.of(wrestler1, wrestler2));
    matchType(editor).setValue("Singles Match");

    when(assistant.suggestOpponent(same(script), any(), any(), any(), any()))
        .thenReturn(
            AiSuggestedOpponentDTO.builder()
                .wrestlerId(30L)
                .name("Randy Orton")
                .rationale("Fresh challenger")
                .build());

    _click(aiSuggest(editor));
    waitForUiUpdates();

    assertEquals(externalWrestler, opponent(editor).getValue());
  }

  /**
   * The suggestion may complete on a ForkJoinPool thread and hand control back via {@code
   * ui.access(...)}; give the mocked UI a moment to run the pending access task.
   */
  private static void waitForUiUpdates() throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      MockVaadin.clientRoundtrip();
      Thread.sleep(50);
    }
  }

  @Test
  @DisplayName("AI failure keeps the manual selection and restores the button")
  void aiSuggest_failure_keepsManualSelection() throws Exception {
    BeatEditor editor = newEditor();
    editor.bindScriptContext(script, List.of(wrestler1, wrestler2));
    matchType(editor).setValue("Singles Match");
    opponent(editor).setValue(externalWrestler);

    when(assistant.suggestOpponent(same(script), any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("No AI providers available"));

    _click(aiSuggest(editor));

    assertEquals(externalWrestler, opponent(editor).getValue());
  }

  @Test
  @DisplayName("Remove button invokes the context remove handler")
  void removeButton_invokesRemoveHandler() {
    AtomicBoolean removed = new AtomicBoolean(false);
    BeatEditor.BeatEditorContext context =
        new BeatEditor.BeatEditorContext(
            List.of(wrestler1, wrestler2),
            List.of(),
            List.of(),
            List.of(),
            List.of("Singles Match"),
            List.of(),
            true,
            invoked -> removed.set(true),
            null);
    BeatEditor editor = new BeatEditor(context);
    _click(removeButton(editor));
    assertTrue(removed.get());
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private static ComboBox<String> matchType(BeatEditor editor) {
    return _get(editor, ComboBox.class, spec -> spec.withLabel("Match Type"));
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<String> stipulation(BeatEditor editor) {
    return _get(editor, ComboBox.class, spec -> spec.withLabel("Stipulation"));
  }

  @SuppressWarnings("unchecked")
  private static RadioButtonGroup<String> winnerControl(BeatEditor editor) {
    return _get(editor, RadioButtonGroup.class, spec -> spec.withLabel("Winner"));
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<Wrestler> plannedWinner(BeatEditor editor) {
    return _get(editor, ComboBox.class, spec -> spec.withLabel("Planned Winner"));
  }

  private static Checkbox culmination(BeatEditor editor) {
    return _get(editor, Checkbox.class, spec -> spec.withLabel("Culmination / Blowoff"));
  }

  private static TextArea notes(BeatEditor editor) {
    return _get(editor, TextArea.class);
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<Wrestler> opponent(BeatEditor editor) {
    return _get(editor, ComboBox.class, spec -> spec.withLabel("External Opponent"));
  }

  @SuppressWarnings("unchecked")
  private static MultiSelectComboBox<Wrestler> extras(BeatEditor editor) {
    return _get(editor, MultiSelectComboBox.class, spec -> spec.withLabel("External Extras"));
  }

  private static Checkbox customTeams(BeatEditor editor) {
    return _get(editor, Checkbox.class, spec -> spec.withLabel("Custom team layout"));
  }

  private static Checkbox titleOnTheLine(BeatEditor editor) {
    return _get(editor, Checkbox.class, spec -> spec.withLabel("Title on the line"));
  }

  @SuppressWarnings("unchecked")
  private static MultiSelectComboBox<Title> titlesMulti(BeatEditor editor) {
    return _get(editor, MultiSelectComboBox.class, spec -> spec.withLabel("Titles"));
  }

  private static Checkbox contenderCheck(BeatEditor editor) {
    return _get(editor, Checkbox.class, spec -> spec.withLabel("Winner becomes #1 contender for…"));
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<Title> contenderTitle(BeatEditor editor) {
    return _get(editor, ComboBox.class, spec -> spec.withLabel("Contender Title"));
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<Show> targetShow(BeatEditor editor) {
    return _get(editor, ComboBox.class, spec -> spec.withLabel("Target Show (optional)"));
  }

  private static Button aiSuggest(BeatEditor editor) {
    return _get(editor, Button.class, spec -> spec.withText("✨ AI Suggest Opponent"));
  }

  private static Button removeButton(BeatEditor editor) {
    return _get(editor, Button.class, spec -> spec.withText("Remove"));
  }

  private static TeamRowsEditor teamRows(BeatEditor editor) {
    return _get(editor, TeamRowsEditor.class);
  }

  /** The multi-select combo of the i-th team row inside the custom-layout area. */
  @SuppressWarnings("unchecked")
  private static MultiSelectComboBox<Wrestler> teamRowCombo(BeatEditor editor, int row) {
    TeamRowsEditor rows = teamRows(editor);
    VerticalLayout rowsContainer = (VerticalLayout) rows.getComponentAt(0);
    return (MultiSelectComboBox<Wrestler>)
        rowsContainer.getComponentAt(row).getChildren().findFirst().orElseThrow();
  }
}
