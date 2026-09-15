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
package com.github.javydreamercsw.management.ui.view.match;

import static com.github.mvysny.kaributesting.v10.ComboBoxKt.getSuggestionItems;
import static com.github.mvysny.kaributesting.v10.HasValueUtilsKt._setValue;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;

/** Karibu tests for the root-menu QR segment picker dialog (ATW-pflh). */
class SegmentQrPickerDialogTest extends AbstractViewTest {

  @Mock private ShowFacade showFacade;
  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;

  private Show show;

  @BeforeEach
  void setup() {
    when(showFacade.getShowService()).thenReturn(showService);
    when(showFacade.getSegmentService()).thenReturn(segmentService);

    show = new Show();
    show.setId(1L);
    show.setName("Monday Night Clash");
    show.setShowDate(LocalDate.of(2026, 9, 1));
  }

  private Segment segment(long id, SegmentStatus status, int order) {
    SegmentType type = new SegmentType();
    type.setName("Singles Match");
    type.setCode("one_on_one");

    Segment segment = new Segment();
    segment.setId(id);
    segment.setShow(show);
    segment.setSegmentType(type);
    segment.setStatus(status);
    segment.setSegmentOrder(order);
    return segment;
  }

  @Test
  @DisplayName("Zero shows shows an explanatory message and disables the show picker")
  void noShows_showsEmptyMessage() {
    when(showService.getAllShows(any())).thenReturn(new PageImpl<>(List.of()));

    SegmentQrPickerDialog dialog = new SegmentQrPickerDialog(showFacade);
    dialog.open();

    Span emptyMessage =
        _get(dialog, Span.class, spec -> spec.withId(SegmentQrPickerDialog.EMPTY_MESSAGE_ID));
    assertThat(emptyMessage.getText()).contains("No shows available");
    assertThat(emptyMessage.isVisible()).isTrue();

    ComboBox<Show> showPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SHOW_PICKER_ID));
    assertThat(showPicker.isEnabled()).isFalse();
  }

  @Test
  @DisplayName("Opening the dialog lists recent shows in the show picker")
  void withShows_listsRecentShows() {
    when(showService.getAllShows(any())).thenReturn(new PageImpl<>(List.of(show)));

    SegmentQrPickerDialog dialog = new SegmentQrPickerDialog(showFacade);
    dialog.open();

    ComboBox<Show> showPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SHOW_PICKER_ID));
    List<Show> items = getSuggestionItems(showPicker);
    assertThat(items).hasSize(1);
    assertThat(items.get(0).getName()).isEqualTo("Monday Night Clash");
  }

  @Test
  @DisplayName("Selecting a show loads only its completed segments")
  void selectShow_loadsCompletedSegments() {
    when(showService.getAllShows(any())).thenReturn(new PageImpl<>(List.of(show)));
    Segment completed = segment(10L, SegmentStatus.COMPLETED, 1);
    Segment booked = segment(11L, SegmentStatus.BOOKED, 2);
    when(segmentService.getSegmentsByShow(show)).thenReturn(List.of(completed, booked));

    SegmentQrPickerDialog dialog = new SegmentQrPickerDialog(showFacade);
    dialog.open();

    ComboBox<Show> showPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SHOW_PICKER_ID));
    _setValue(showPicker, show);

    ComboBox<Segment> segmentPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SEGMENT_PICKER_ID));
    assertThat(segmentPicker.isEnabled()).isTrue();
    List<Segment> items = getSuggestionItems(segmentPicker);
    assertThat(items).containsExactly(completed);
  }

  @Test
  @DisplayName("Selecting a segment opens the existing QrCodeDialog for it")
  void selectSegment_opensQrCodeDialog() {
    when(showService.getAllShows(any())).thenReturn(new PageImpl<>(List.of(show)));
    Segment completed = segment(10L, SegmentStatus.COMPLETED, 1);
    when(segmentService.getSegmentsByShow(show)).thenReturn(List.of(completed));

    SegmentQrPickerDialog dialog = new SegmentQrPickerDialog(showFacade);
    dialog.open();

    ComboBox<Show> showPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SHOW_PICKER_ID));
    _setValue(showPicker, show);

    ComboBox<Segment> segmentPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SEGMENT_PICKER_ID));
    _setValue(segmentPicker, completed);

    List<Dialog> dialogs = _find(Dialog.class);
    assertThat(dialogs).anyMatch(qrDialog -> qrDialog instanceof QrCodeDialog);
  }

  @Test
  @DisplayName("Show without completed segments disables the segment picker")
  void selectShow_noCompletedSegments_disablesSegmentPicker() {
    when(showService.getAllShows(any())).thenReturn(new PageImpl<>(List.of(show)));
    when(segmentService.getSegmentsByShow(show))
        .thenReturn(List.of(segment(12L, SegmentStatus.BOOKED, 1)));

    SegmentQrPickerDialog dialog = new SegmentQrPickerDialog(showFacade);
    dialog.open();

    ComboBox<Show> showPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SHOW_PICKER_ID));
    _setValue(showPicker, show);

    ComboBox<Segment> segmentPicker =
        _get(dialog, ComboBox.class, spec -> spec.withId(SegmentQrPickerDialog.SEGMENT_PICKER_ID));
    assertThat(segmentPicker.isEnabled()).isFalse();
  }
}
