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
package com.github.javydreamercsw.management.controller.outcome;

import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixResult;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/outcome-matrices")
@RequiredArgsConstructor
@Tag(
    name = "Outcome Matrices",
    description = "Dice-roll lookup chart definitions and roll resolution")
public class OutcomeMatrixController {

  private final OutcomeMatrixService outcomeMatrixService;

  @GetMapping
  @Operation(summary = "List all outcome matrices, optionally filtered by category")
  public List<OutcomeMatrix> listAll(
      @RequestParam(required = false) final OutcomeMatrixCategory category) {
    if (category != null) {
      return outcomeMatrixService.getByCategory(category);
    }
    return outcomeMatrixService.getAll();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a single outcome matrix by ID")
  public ResponseEntity<OutcomeMatrix> getById(@PathVariable final Long id) {
    return outcomeMatrixService
        .getById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @Operation(summary = "Create a new outcome matrix")
  public ResponseEntity<OutcomeMatrix> create(@RequestBody final OutcomeMatrix matrix) {
    return ResponseEntity.status(201).body(outcomeMatrixService.createMatrix(matrix));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update an outcome matrix")
  public ResponseEntity<OutcomeMatrix> update(
      @PathVariable final Long id, @RequestBody final OutcomeMatrix matrix) {
    if (!outcomeMatrixService.getById(id).isPresent()) {
      return ResponseEntity.notFound().build();
    }
    matrix.setId(id);
    return ResponseEntity.ok(outcomeMatrixService.updateMatrix(matrix));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete an outcome matrix")
  public ResponseEntity<Void> delete(@PathVariable final Long id) {
    if (!outcomeMatrixService.getById(id).isPresent()) {
      return ResponseEntity.notFound().build();
    }
    outcomeMatrixService.deleteMatrix(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/entries")
  @Operation(summary = "List all entries for a matrix, sorted by dice roll")
  public ResponseEntity<List<OutcomeMatrixEntry>> getEntries(@PathVariable final Long id) {
    return ResponseEntity.ok(outcomeMatrixService.getEntries(id));
  }

  @PostMapping("/{id}/entries")
  @Operation(summary = "Add an entry to a matrix")
  public ResponseEntity<OutcomeMatrixEntry> addEntry(
      @PathVariable final Long id, @RequestBody final OutcomeMatrixEntry entry) {
    return ResponseEntity.status(201).body(outcomeMatrixService.addEntry(id, entry));
  }

  @PostMapping("/{id}/roll/{diceValue}")
  @Operation(summary = "Resolve a dice roll against a matrix (no side effects)")
  public ResponseEntity<OutcomeMatrixResult> roll(
      @PathVariable final Long id,
      @PathVariable final int diceValue,
      @RequestBody(required = false) final Map<String, String> variables) {
    Optional<OutcomeMatrixResult> result =
        outcomeMatrixService.resolveRoll(id, diceValue, variables != null ? variables : Map.of());
    return result.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/categories")
  @Operation(summary = "List available outcome matrix categories")
  public OutcomeMatrixCategory[] getCategories() {
    return OutcomeMatrixCategory.values();
  }
}
