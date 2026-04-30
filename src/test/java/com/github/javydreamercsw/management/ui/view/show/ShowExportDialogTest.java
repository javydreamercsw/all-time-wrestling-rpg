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
package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.export.ShowExportService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowExportDialogTest {

  private ShowExportService exportService;
  private NotificationService notificationService;
  private Show show;

  @BeforeEach
  void setUp() {
    exportService = mock(ShowExportService.class);
    notificationService = mock(NotificationService.class);
    show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");

    List<String> formats = Arrays.asList("Markdown", "Facebook");
    when(exportService.getAvailableFormats()).thenReturn(formats);
    when(exportService.export(show, "Markdown", true, true)).thenReturn("Markdown Content");
    when(exportService.export(show, "Facebook", true, true)).thenReturn("Facebook Content");
    when(exportService.export(show, "Markdown", false, false)).thenReturn("Minimal Content");
  }

  @Test
  void testDialogInitialization() {
    ShowExportDialog dialog = new ShowExportDialog(exportService, notificationService, show);

    assertNotNull(dialog);
    assertEquals("Markdown", dialog.getFormatSelector().getValue());
    assertEquals("Markdown Content", dialog.getPreviewArea().getValue());
    assertTrue(dialog.getIncludeResults().getValue());
    assertTrue(dialog.getIncludeSummary().getValue());
    assertTrue(dialog.getCopyButton().isEnabled());
  }

  @Test
  void testFormatSelectionChange() {
    ShowExportDialog dialog = new ShowExportDialog(exportService, notificationService, show);

    dialog.getFormatSelector().setValue("Facebook");
    assertEquals("Facebook Content", dialog.getPreviewArea().getValue());
  }

  @Test
  void testOptionToggle() {
    ShowExportDialog dialog = new ShowExportDialog(exportService, notificationService, show);

    dialog.getIncludeResults().setValue(false);
    dialog.getIncludeSummary().setValue(false);

    assertEquals("Minimal Content", dialog.getPreviewArea().getValue());
  }
}
