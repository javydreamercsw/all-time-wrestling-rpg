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

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Root-menu entry point for sharing a match QR code (ATW-pflh).
 *
 * <p>Lets the user pick one of the most recent shows and then one of that show's completed
 * segments. On selection it opens the existing {@link QrCodeDialog} for the chosen segment so the
 * QR generation logic is not duplicated. The dialog is route-independent: it can be opened from
 * anywhere the main layout is present.
 */
public class SegmentQrPickerDialog extends Dialog {

  public static final String SHOW_PICKER_ID = "qr-show-picker";
  public static final String SEGMENT_PICKER_ID = "qr-segment-picker";
  public static final String EMPTY_MESSAGE_ID = "qr-picker-empty";

  static final int RECENT_SHOW_LIMIT = 20;

  private final ShowFacade showFacade;
  private final ComboBox<Show> showPicker = new ComboBox<>("Show");
  private final ComboBox<Segment> segmentPicker = new ComboBox<>("Match");

  public SegmentQrPickerDialog(final ShowFacade showFacade) {
    this.showFacade = showFacade;
    setHeaderTitle("Share Match QR Code");
    setWidth("360px");

    showPicker.setId(SHOW_PICKER_ID);
    showPicker.setWidthFull();
    showPicker.setAllowCustomValue(false);
    showPicker.setClearButtonVisible(true);
    showPicker.setItemLabelGenerator(this::formatShowLabel);
    showPicker.addValueChangeListener(event -> loadSegments(event.getValue()));

    segmentPicker.setId(SEGMENT_PICKER_ID);
    segmentPicker.setWidthFull();
    segmentPicker.setAllowCustomValue(false);
    segmentPicker.setEnabled(false);
    segmentPicker.setPlaceholder("Select a show first");
    segmentPicker.setItemLabelGenerator(Segment::getName);
    segmentPicker.addValueChangeListener(
        event -> {
          Segment segment = event.getValue();
          if (segment != null) {
            close();
            new QrCodeDialog(segment.getId()).open();
          }
        });

    VerticalLayout layout = new VerticalLayout(showPicker, segmentPicker);
    layout.setPadding(false);
    layout.setSpacing(true);
    layout.setAlignItems(FlexComponent.Alignment.STRETCH);
    add(layout);
    getFooter().add(new Button("Cancel", event -> close()));

    loadRecentShows(layout);
  }

  private String formatShowLabel(final Show show) {
    if (show.getShowDate() != null) {
      return show.getName() + " (" + show.getShowDate() + ")";
    }
    return show.getName();
  }

  private void loadRecentShows(final VerticalLayout layout) {
    Pageable pageable =
        PageRequest.of(0, RECENT_SHOW_LIMIT, Sort.by(Sort.Direction.DESC, "showDate"));
    List<Show> recentShows = showFacade.getShowService().getAllShows(pageable).getContent();
    if (recentShows.isEmpty()) {
      showPicker.setEnabled(false);
      Span emptyMessage = new Span("No shows available yet. Create a show first.");
      emptyMessage.setId(EMPTY_MESSAGE_ID);
      layout.addComponentAsFirst(emptyMessage);
      return;
    }
    showPicker.setItems(recentShows);
  }

  private void loadSegments(final Show show) {
    segmentPicker.setItems(List.of());
    segmentPicker.setValue(null);
    if (show == null) {
      segmentPicker.setEnabled(false);
      segmentPicker.setPlaceholder("Select a show first");
      return;
    }
    List<Segment> completedSegments =
        showFacade.getSegmentService().getSegmentsByShow(show).stream()
            .filter(segment -> segment.getStatus() == SegmentStatus.COMPLETED)
            .sorted(Comparator.comparingInt(Segment::getSegmentOrder))
            .toList();
    segmentPicker.setItems(completedSegments);
    segmentPicker.setEnabled(!completedSegments.isEmpty());
    segmentPicker.setPlaceholder(
        completedSegments.isEmpty() ? "No completed matches" : "Select a match");
  }
}
