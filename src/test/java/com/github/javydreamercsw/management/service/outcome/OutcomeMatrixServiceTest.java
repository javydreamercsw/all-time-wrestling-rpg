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
package com.github.javydreamercsw.management.service.outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntryRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixResult;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutcomeMatrixServiceTest {

  @Mock private OutcomeMatrixRepository matrixRepository;
  @Mock private OutcomeMatrixEntryRepository entryRepository;

  @InjectMocks private OutcomeMatrixService service;

  private OutcomeMatrix matrix;

  @BeforeEach
  void setUp() {
    matrix = new OutcomeMatrix();
    matrix.setId(1L);
    matrix.setName("Highlight Reel O");
    matrix.setCategory(OutcomeMatrixCategory.HIGHLIGHT_REEL);
  }

  @Test
  void resolveRoll_substitutesVariables() {
    OutcomeMatrixEntry entry = new OutcomeMatrixEntry();
    entry.setId(10L);
    entry.setMatrix(matrix);
    entry.setDiceRoll(12);
    entry.setTemplateText(
        "{WRESTLER_1} wrestler talks trash about {WRESTLER_2}, increase Grudge Grade 1 point.");
    entry.setGrudgeGradeDelta(1);

    when(matrixRepository.findById(1L)).thenReturn(Optional.of(matrix));
    when(entryRepository.findByMatrixAndDiceRoll(matrix, 12)).thenReturn(Optional.of(entry));

    Optional<OutcomeMatrixResult> result =
        service.resolveRoll(
            1L, 12, Map.of("{WRESTLER_1}", "El Fuego", "{WRESTLER_2}", "The Ghost"));

    assertThat(result).isPresent();
    assertThat(result.get().renderedText())
        .isEqualTo("El Fuego wrestler talks trash about The Ghost, increase Grudge Grade 1 point.");
    assertThat(result.get().isRedirect()).isFalse();
    assertThat(result.get().entry().getGrudgeGradeDelta()).isEqualTo(1);
  }

  @Test
  void resolveRoll_followsRedirectToMatrix() {
    OutcomeMatrix targetMatrix = new OutcomeMatrix();
    targetMatrix.setId(2L);
    targetMatrix.setName("Highlight Reel P");
    targetMatrix.setCategory(OutcomeMatrixCategory.HIGHLIGHT_REEL);

    OutcomeMatrixEntry redirectEntry = new OutcomeMatrixEntry();
    redirectEntry.setId(11L);
    redirectEntry.setMatrix(matrix);
    redirectEntry.setDiceRoll(11);
    redirectEntry.setTemplateText("Go to HIGHLIGHT REEL P.");
    redirectEntry.setRedirectToMatrix(targetMatrix);

    when(matrixRepository.findById(1L)).thenReturn(Optional.of(matrix));
    when(entryRepository.findByMatrixAndDiceRoll(matrix, 11))
        .thenReturn(Optional.of(redirectEntry));

    Optional<OutcomeMatrixResult> result = service.resolveRoll(1L, 11, Map.of());

    assertThat(result).isPresent();
    assertThat(result.get().isRedirect()).isTrue();
    assertThat(result.get().redirectMatrix().getName()).isEqualTo("Highlight Reel P");
    assertThat(result.get().renderedText()).isEqualTo("Go to HIGHLIGHT REEL P.");
  }

  @Test
  void resolveRoll_returnsEmptyForUnknownDiceRoll() {
    when(matrixRepository.findById(1L)).thenReturn(Optional.of(matrix));
    when(entryRepository.findByMatrixAndDiceRoll(matrix, 99)).thenReturn(Optional.empty());

    Optional<OutcomeMatrixResult> result = service.resolveRoll(1L, 99, Map.of());

    assertThat(result).isEmpty();
  }

  @Test
  void resolveRoll_emptyVariablesLeavesTemplateUnchanged() {
    OutcomeMatrixEntry entry = new OutcomeMatrixEntry();
    entry.setId(12L);
    entry.setMatrix(matrix);
    entry.setDiceRoll(23);
    entry.setTemplateText("{WRESTLER_2} wrestler livid after opponent makes belittling comments.");

    when(matrixRepository.findById(1L)).thenReturn(Optional.of(matrix));
    when(entryRepository.findByMatrixAndDiceRoll(matrix, 23)).thenReturn(Optional.of(entry));

    Optional<OutcomeMatrixResult> result = service.resolveRoll(1L, 23, Map.of());

    assertThat(result).isPresent();
    assertThat(result.get().renderedText())
        .isEqualTo("{WRESTLER_2} wrestler livid after opponent makes belittling comments.");
  }
}
