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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudLength;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.HasValueUtilsKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/** Unit tests for the three-step story arc creation wizard. */
class FeudScriptWizardDialogTest extends AbstractViewTest {

  @Mock private FeudScriptService feudScriptService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;

  @BeforeEach
  void setup() {
    wrestler1 = wrestler(1L, "Adam Axe");
    wrestler2 = wrestler(2L, "Bob Boulder");
    wrestler3 = wrestler(3L, "Chip Cutter");
    when(feudScriptService.createScriptWithBeats(anyString(), anyList(), anyInt(), anyList()))
        .thenAnswer(
            inv -> {
              FeudScript script = new FeudScript();
              script.setName(inv.getArgument(0));
              return script;
            });
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private FeudScriptWizardDialog openWizard() {
    FeudScriptWizardDialog wizard =
        new FeudScriptWizardDialog(
            List.of(wrestler1, wrestler2, wrestler3),
            List.of("Singles Match", "Tag Team Match"),
            List.of("No Disqualification"),
            2,
            feudScriptService,
            () -> {});
    wizard.open();
    UI.getCurrent().add(wizard);
    return wizard;
  }

  private static H3 stepTitle(FeudScriptWizardDialog wizard) {
    return _get(wizard, H3.class);
  }

  private static void next(FeudScriptWizardDialog wizard) {
    _click(_get(wizard, Button.class, spec -> spec.withText("Next")));
  }

  private static void back(FeudScriptWizardDialog wizard) {
    _click(_get(wizard, Button.class, spec -> spec.withText("Back")));
  }

  @Test
  @DisplayName("Step 1 blocks advancing with fewer than two wrestlers")
  void step1_fewerThanTwoWrestlers_blocked() {
    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1), true);

    next(wizard);
    assertEquals("Step 1 of 3 — Select Wrestlers", stepTitle(wizard).getText());
    verify(feudScriptService, never())
        .createScriptWithBeats(anyString(), anyList(), anyInt(), anyList());
  }

  @Test
  @DisplayName("Step 2 defaults the arc name from the selected wrestlers")
  void step2_defaultNameFromWrestlers() {
    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1, wrestler2), true);
    next(wizard);

    assertEquals("Step 2 of 3 — Arc Details", stepTitle(wizard).getText());
    TextField nameField = _get(wizard, TextField.class, spec -> spec.withLabel("Arc Name"));
    // Set iteration order is unspecified — assert on structure, not ordering.
    assertTrue(nameField.getValue().contains("Adam Axe"));
    assertTrue(nameField.getValue().contains("Bob Boulder"));
    assertTrue(nameField.getValue().endsWith(" Arc"));
  }

  @Test
  @DisplayName("Back navigation preserves a custom arc name")
  void backNavigation_preservesCustomArcName() {
    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1, wrestler2), true);
    next(wizard);

    TextField nameField = _get(wizard, TextField.class, spec -> spec.withLabel("Arc Name"));
    HasValueUtilsKt._setValue(nameField, "My Custom Arc", true);
    next(wizard);
    assertEquals("Step 3 of 3 — Beats", stepTitle(wizard).getText());

    back(wizard);
    assertEquals("Step 2 of 3 — Arc Details", stepTitle(wizard).getText());
    TextField nameAgain = _get(wizard, TextField.class, spec -> spec.withLabel("Arc Name"));
    assertEquals("My Custom Arc", nameAgain.getValue());
  }

  @Test
  @DisplayName("Step 2 blocks advancing with a blank arc name")
  void step2_blankName_blocked() {
    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1, wrestler2), true);
    next(wizard);

    TextField nameField = _get(wizard, TextField.class, spec -> spec.withLabel("Arc Name"));
    HasValueUtilsKt._setValue(nameField, "", true);
    next(wizard);

    // An empty arc name blocks advancing (the field is required).
    assertEquals("Step 2 of 3 — Arc Details", stepTitle(wizard).getText());
  }

  @Test
  @DisplayName("Finish with a valid beat persists the arc in a single service call")
  void finish_validBeat_createsScriptWithBeats() {
    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1, wrestler2), true);
    next(wizard);
    next(wizard); // default name is fine

    // Step 3 starts with one beat row; give it a match type.
    BeatEditor beatRow = _get(wizard, BeatEditor.class);
    _get(beatRow, ComboBox.class, spec -> spec.withLabel("Match Type")).setValue("Singles Match");
    _click(_get(wizard, Button.class, spec -> spec.withText("Finish")));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<FeudScriptBeat>> beatsCaptor = ArgumentCaptor.forClass(List.class);
    verify(feudScriptService)
        .createScriptWithBeats(anyString(), anyList(), anyInt(), beatsCaptor.capture());
    assertEquals(1, beatsCaptor.getValue().size());
    assertEquals("Singles Match", beatsCaptor.getValue().get(0).getSegmentType());
    assertFalse(wizard.isOpened());
  }

  @Test
  @DisplayName("Finish with an invalid beat row is blocked and nothing is persisted")
  void finish_invalidBeatRow_blocked() {
    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1, wrestler2), true);
    next(wizard);
    next(wizard);

    // Leave the beat row without a match type.
    _click(_get(wizard, Button.class, spec -> spec.withText("Finish")));

    verify(feudScriptService, never())
        .createScriptWithBeats(anyString(), anyList(), anyInt(), anyList());
  }

  @Test
  @DisplayName("Service failure on finish surfaces an error and keeps the wizard open")
  void finish_serviceFailure_showsErrorAndStaysOpen() {
    when(feudScriptService.createScriptWithBeats(anyString(), anyList(), anyInt(), anyList()))
        .thenThrow(new IllegalStateException("Beat validation failed"));

    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1, wrestler2), true);
    next(wizard);
    next(wizard);

    BeatEditor beatRow = _get(wizard, BeatEditor.class);
    _get(beatRow, ComboBox.class, spec -> spec.withLabel("Match Type")).setValue("Singles Match");
    _click(_get(wizard, Button.class, spec -> spec.withText("Finish")));

    assertTrue(wizard.isOpened());
  }

  @Test
  @DisplayName("Length selection maps to the PLE cap passed to the service")
  void lengthSelection_mapsToPleCap() {
    FeudScriptWizardDialog wizard = openWizard();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    HasValueUtilsKt._setValue(picker, Set.of(wrestler1, wrestler2), true);
    next(wizard);

    @SuppressWarnings("unchecked")
    RadioButtonGroup<FeudLength> length =
        _get(wizard, RadioButtonGroup.class, spec -> spec.withLabel("Feud Length"));
    HasValueUtilsKt._setValue(length, FeudLength.LONG, true);
    next(wizard);

    BeatEditor beatRow = _get(wizard, BeatEditor.class);
    _get(beatRow, ComboBox.class, spec -> spec.withLabel("Match Type")).setValue("Singles Match");
    _click(_get(wizard, Button.class, spec -> spec.withText("Finish")));

    verify(feudScriptService).createScriptWithBeats(anyString(), anyList(), same(3), anyList());
  }

  @Test
  @DisplayName("preSelectWrestlers seeds the step-1 picker")
  void preSelectWrestlers_seedsPicker() {
    FeudScriptWizardDialog wizard = openWizard();
    wizard.preSelectWrestlers(List.of(wrestler1, wrestler2));

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> picker =
        _get(
            wizard,
            MultiSelectComboBox.class,
            spec -> spec.withLabel("Wrestlers (2 for rivalry, 3+ for multi-feud)"));
    assertEquals(2, picker.getValue().size());
  }
}
