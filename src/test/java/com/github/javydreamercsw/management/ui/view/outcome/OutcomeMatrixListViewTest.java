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
package com.github.javydreamercsw.management.ui.view.outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.Query;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class OutcomeMatrixListViewTest extends AbstractViewTest {

  @Mock private OutcomeMatrixService outcomeMatrixService;
  @Mock private SecurityUtils securityUtils;

  private OutcomeMatrixListView view;
  private OutcomeMatrix testMatrix;

  @BeforeEach
  public void setUp() {
    testMatrix = new OutcomeMatrix();
    testMatrix.setId(1L);
    testMatrix.setName("Highlight Reel O");
    testMatrix.setCategory(OutcomeMatrixCategory.HIGHLIGHT_REEL);

    when(outcomeMatrixService.getAll()).thenReturn(List.of(testMatrix));
    when(outcomeMatrixService.getByCategory(any())).thenReturn(List.of(testMatrix));
    when(outcomeMatrixService.getEntries(anyLong())).thenReturn(List.of());
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);

    view = new OutcomeMatrixListView(outcomeMatrixService, securityUtils);
  }

  @Test
  void grid_rendersWithData() {
    @SuppressWarnings("unchecked")
    Grid<OutcomeMatrix> grid = (Grid<OutcomeMatrix>) ReflectionTestUtils.getField(view, "grid");
    assertThat(grid).isNotNull();

    long count = grid.getDataProvider().size(new Query<>());
    assertThat(count).isEqualTo(1);
  }

  @Test
  void grid_showsMatrixName() {
    @SuppressWarnings("unchecked")
    Grid<OutcomeMatrix> grid = (Grid<OutcomeMatrix>) ReflectionTestUtils.getField(view, "grid");
    assertThat(grid).isNotNull();

    List<OutcomeMatrix> items = grid.getDataProvider().fetch(new Query<>()).toList();
    assertThat(items).hasSize(1);
    assertThat(items.getFirst().getName()).isEqualTo("Highlight Reel O");
  }

  @Test
  void addButton_isVisibleWhenCanCreate() {
    when(securityUtils.canCreate()).thenReturn(true);
    OutcomeMatrixListView freshView =
        new OutcomeMatrixListView(outcomeMatrixService, securityUtils);

    assertThat(freshView.createButton.isVisible()).isTrue();
  }

  @Test
  void addButton_isHiddenWhenCannotCreate() {
    when(securityUtils.canCreate()).thenReturn(false);
    OutcomeMatrixListView restrictedView =
        new OutcomeMatrixListView(outcomeMatrixService, securityUtils);

    assertThat(restrictedView.createButton.isVisible()).isFalse();
  }

  @Test
  void refreshGrid_callsGetAll() {
    view.refreshGrid();
    verify(outcomeMatrixService, atLeast(1)).getAll();
  }

  @Test
  void refreshGrid_withCategory_callsGetByCategory() {
    view.refreshGrid();
    // Trigger category filter selection
    when(outcomeMatrixService.getByCategory(OutcomeMatrixCategory.HIGHLIGHT_REEL))
        .thenReturn(List.of(testMatrix));
    view.categoryFilter.setValue(OutcomeMatrixCategory.HIGHLIGHT_REEL);

    verify(outcomeMatrixService).getByCategory(OutcomeMatrixCategory.HIGHLIGHT_REEL);
  }
}
