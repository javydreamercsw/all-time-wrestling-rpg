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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.HasValueUtilsKt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/** Unit tests for the story-arc card shared by the rivalry detail and story-arcs list views. */
class FeudScriptCardTest extends AbstractViewTest {

  @Mock private FeudScriptService feudScriptService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private FeudBeatAssistantService feudBeatAssistantService;
  @Mock private SecurityUtils securityUtils;
  @Mock private WrestlerService wrestlerService;
  @Mock private ShowService showService;
  @Mock private TitleService titleService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private FeudScript script;

  @BeforeEach
  void setup() {
    wrestler1 = wrestler(1L, "Adam Axe");
    wrestler2 = wrestler(2L, "Bob Boulder");

    script = new FeudScript();
    script.setName("Test Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setMaxPleAppearances(2);
    script.setRivalry(rivalry());
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private Rivalry rivalry() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    return rivalry;
  }

  private FeudScriptCard.EditorServices services(boolean canCreate) {
    Mockito.when(securityUtils.canCreate()).thenReturn(canCreate);
    Mockito.when(wrestlerService.findAllFiltered(any(), any(), any(), any(), any()))
        .thenReturn(List.of());
    Mockito.when(showService.getUpcomingShows(any(Integer.class))).thenReturn(List.of());
    Mockito.when(titleService.getActiveTitles()).thenReturn(List.of());
    return new FeudScriptCard.EditorServices(
        feudScriptService,
        segmentTypeService,
        segmentRuleService,
        feudBeatAssistantService,
        securityUtils,
        wrestlerService,
        showService,
        titleService);
  }

  private FeudScriptCard newCard(Runnable reload) {
    return newCard(true, reload);
  }

  private FeudScriptCard newCard(boolean canCreate, Runnable reload) {
    FeudScriptCard card =
        new FeudScriptCard(
            script, FeudScriptCard.participantsOf(script), services(canCreate), reload);
    UI.getCurrent().add(card);
    return card;
  }

  /** All button texts anywhere in the card tree (dialogs included). */
  private static List<String> buttonTexts(Component root) {
    List<String> texts = new ArrayList<>();
    collectButtons(root, texts);
    return texts;
  }

  private static void collectButtons(Component c, List<String> texts) {
    if (c instanceof Button button && button.getText() != null && !button.getText().isBlank()) {
      texts.add(button.getText());
    }
    c.getChildren().forEach(child -> collectButtons(child, texts));
  }

  private static List<String> spanTexts(Component root) {
    List<String> texts = new ArrayList<>();
    collectSpans(root, texts);
    return texts;
  }

  private static void collectSpans(Component c, List<String> texts) {
    if (c instanceof Span span && span.getText() != null) {
      texts.add(span.getText());
    }
    c.getChildren().forEach(child -> collectSpans(child, texts));
  }

  @Test
  @DisplayName("participantsOf returns the rivalry pair")
  void participantsOf_rivalryPair() {
    List<Wrestler> participants = FeudScriptCard.participantsOf(script);
    assertEquals(2, participants.size());
    assertTrue(participants.containsAll(List.of(wrestler1, wrestler2)));
  }

  @Test
  @DisplayName("participantsOf falls back to active feud members")
  void participantsOf_activeFeudMembers() {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    Wrestler third = wrestler(3L, "Chip Cutter");
    feud.addParticipant(wrestler1, FeudRole.PROTAGONIST);
    feud.addParticipant(wrestler2, FeudRole.ANTAGONIST);
    feud.addParticipant(third, FeudRole.NEUTRAL);
    script.setRivalry(null);
    script.setFeud(feud);

    List<Wrestler> participants = FeudScriptCard.participantsOf(script);
    assertEquals(3, participants.size());
    assertTrue(participants.contains(third));
  }

  @Test
  @DisplayName("Editable card renders the action buttons; readonly card does not")
  void editableGating_byCanCreate() {
    FeudScriptCard editable = newCard(() -> {});
    assertTrue(buttonTexts(editable).contains("+ Add Beat"));
    assertTrue(buttonTexts(editable).contains("Edit"));
    assertTrue(buttonTexts(editable).contains("Cancel Arc"));

    FeudScriptCard readonly = newCard(false, () -> {});
    assertFalse(buttonTexts(readonly).contains("+ Add Beat"));
    assertFalse(buttonTexts(readonly).contains("Edit"));
    assertFalse(buttonTexts(readonly).contains("Cancel Arc"));
  }

  @Test
  @DisplayName("Completed arc renders without edit actions even for a booker")
  void completedArc_notEditable() {
    script.setStatus(FeudScriptStatus.COMPLETED);
    FeudScriptCard card = newCard(() -> {});

    assertFalse(buttonTexts(card).contains("+ Add Beat"));
    assertFalse(buttonTexts(card).contains("Edit"));
    assertFalse(buttonTexts(card).contains("Cancel Arc"));
  }

  @Test
  @DisplayName("Progress header shows resolved-beat and PLE counts")
  void progressHeader_counts() {
    FeudScriptBeat done = new FeudScriptBeat();
    done.setBeatOrder(1);
    done.setSegmentType("Singles Match");
    done.setWinnerControl(FeudScriptWinnerControl.AI_PICKS);
    done.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    FeudScriptBeat pending = new FeudScriptBeat();
    pending.setBeatOrder(2);
    pending.setSegmentType("Singles Match");
    pending.setWinnerControl(FeudScriptWinnerControl.AI_PICKS);
    script.getBeats().addAll(List.of(done, pending));

    FeudScriptCard card = newCard(() -> {});

    String progress =
        spanTexts(card).stream().filter(t -> t.contains("beats · ")).findFirst().orElse("");
    assertTrue(progress.contains("1/2 beats"));
    assertTrue(progress.contains("/2 PLEs"));
  }

  @Test
  @DisplayName("Edit dialog save persists a renamed arc and triggers reload")
  void editDialog_save_renamesArc() {
    AtomicInteger reloads = new AtomicInteger();
    FeudScriptCard card = newCard(reloads::incrementAndGet);

    _click(_get(card, Button.class, spec -> spec.withText("Edit")));
    Dialog dialog = _get(Dialog.class);
    TextField nameField = _get(dialog, TextField.class);
    HasValueUtilsKt._setValue(nameField, "Renamed Arc", true);
    _click(_get(dialog, Button.class, spec -> spec.withText("Save")));

    verify(feudScriptService).updateScript(same(script), same("Renamed Arc"), any(Integer.class));
    assertEquals(1, reloads.get());
  }

  @Test
  @DisplayName("Blank arc name in the edit dialog is rejected")
  void editDialog_blankName_rejected() {
    FeudScriptCard card = newCard(() -> {});

    _click(_get(card, Button.class, spec -> spec.withText("Edit")));
    Dialog dialog = _get(Dialog.class);
    com.vaadin.flow.component.textfield.TextField nameField =
        _get(dialog, com.vaadin.flow.component.textfield.TextField.class);
    HasValueUtilsKt._setValue(nameField, " ", true);
    _click(_get(dialog, Button.class, spec -> spec.withText("Save")));

    verify(feudScriptService, never()).updateScript(any(), any(), any(Integer.class));
  }

  @Test
  @DisplayName("Cancel-arc confirmation calls cancelScript and reloads")
  void cancelArcConfirmation_callsService() {
    AtomicInteger reloads = new AtomicInteger();
    FeudScriptCard card = newCard(reloads::incrementAndGet);

    _click(_get(card, Button.class, spec -> spec.withText("Cancel Arc")));
    _click(_get(_get(Dialog.class), Button.class, spec -> spec.withText("Cancel Arc")));

    verify(feudScriptService).cancelScript(same(script));
    assertEquals(1, reloads.get());
  }

  @Test
  @DisplayName("Keep Arc on the cancel confirmation does nothing")
  void cancelArcConfirmation_keepArc_noServiceCall() {
    FeudScriptCard card = newCard(() -> {});

    _click(_get(card, Button.class, spec -> spec.withText("Cancel Arc")));
    _click(_get(_get(Dialog.class), Button.class, spec -> spec.withText("Keep Arc")));

    verify(feudScriptService, never()).cancelScript(any());
  }

  @Test
  @DisplayName("Remove-beat confirmation calls removeBeat and reloads")
  void removeBeatConfirmation_callsService() {
    FeudScriptBeat pending = beat(1, "Singles Match");
    script.getBeats().add(pending);

    AtomicInteger reloads = new AtomicInteger();
    FeudScriptCard card = newCard(reloads::incrementAndGet);

    Button removeBtn = _get(cellButton(card, 0), Button.class, spec -> spec.withText("✕"));
    _click(removeBtn);
    _click(_get(_get(Dialog.class), Button.class, spec -> spec.withText("Remove")));

    verify(feudScriptService).removeBeat(same(script), same(pending));
    assertEquals(1, reloads.get());
  }

  @Test
  @DisplayName("Skip-beat confirmation calls skipBeat")
  void skipBeatConfirmation_callsService() {
    FeudScriptBeat pending = beat(1, "Singles Match");
    script.getBeats().add(pending);

    FeudScriptCard card = newCard(() -> {});

    Button skipBtn = _get(cellButton(card, 0), Button.class, spec -> spec.withText("Skip"));
    _click(skipBtn);
    _click(_get(_get(Dialog.class), Button.class, spec -> spec.withText("Skip Beat")));

    verify(feudScriptService).skipBeat(same(script), same(pending));
  }

  @Test
  @DisplayName("Beat grid lists beats in order")
  void beatGrid_rendersBeats() {
    script.getBeats().add(beat(1, "Singles Match"));
    FeudScriptCard card = newCard(() -> {});

    Grid<?> grid = _get(card, Grid.class);
    assertEquals(1, grid.getListDataView().getItemCount());
  }

  /**
   * Materializes grid row 0 and returns its action cell. Karibu renders grid cells lazily;
   * _getCellComponent forces the row to render so the per-beat buttons become locatable.
   */
  private static Component cellButton(FeudScriptCard card, int row) {
    Grid<?> grid = _get(card, Grid.class);
    return com.github.mvysny.kaributesting.v10.GridKt._getCellComponent(grid, 0, "actions");
  }

  private static FeudScriptBeat beat(int order, String segmentType) {
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(order);
    beat.setSegmentType(segmentType);
    beat.setWinnerControl(FeudScriptWinnerControl.AI_PICKS);
    return beat;
  }
}
