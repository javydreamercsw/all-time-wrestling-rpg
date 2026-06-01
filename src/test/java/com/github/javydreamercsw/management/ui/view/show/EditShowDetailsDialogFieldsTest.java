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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class EditShowDetailsDialogFieldsTest extends AbstractViewTest {

  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private UniverseRepository universeRepository;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private ArenaService arenaService;
  @Mock private LeagueRepository leagueRepository;

  private EditShowDetailsDialog dialog;

  @BeforeEach
  void setup() {
    when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    when(seasonService.getAllSeasons(any(Pageable.class)))
        .thenReturn(new PageImpl<>(Collections.emptyList()));
    when(showTemplateService.findAll()).thenReturn(Collections.emptyList());
    when(universeRepository.findAll()).thenReturn(Collections.emptyList());
    when(commentaryTeamRepository.findAll()).thenReturn(Collections.emptyList());
    when(arenaService.findAll()).thenReturn(Collections.emptyList());
    when(leagueRepository.findAll()).thenReturn(Collections.emptyList());

    ShowType showType = new ShowType();
    showType.setName("Weekly");

    Show show = new Show();
    show.setType(showType);
    show.setShowDate(LocalDate.now());

    dialog =
        new EditShowDetailsDialog(
            showService,
            showTypeService,
            seasonService,
            showTemplateService,
            universeRepository,
            commentaryTeamRepository,
            arenaService,
            leagueRepository,
            show);
  }

  @Test
  @DisplayName("leagueField, attendanceField and gateRevenueField should exist in the dialog")
  void leagueAttendanceAndGateRevenueFieldsExist() {
    ComboBox<?> leagueField = (ComboBox<?>) ReflectionTestUtils.getField(dialog, "leagueField");
    assertNotNull(leagueField, "leagueField should not be null");

    IntegerField attendanceField =
        (IntegerField) ReflectionTestUtils.getField(dialog, "attendanceField");
    assertNotNull(attendanceField, "attendanceField should not be null");

    BigDecimalField gateRevenueField =
        (BigDecimalField) ReflectionTestUtils.getField(dialog, "gateRevenueField");
    assertNotNull(gateRevenueField, "gateRevenueField should not be null");
  }
}
