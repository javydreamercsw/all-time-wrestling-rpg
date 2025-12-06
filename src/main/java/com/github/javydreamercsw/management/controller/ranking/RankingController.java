/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.controller.ranking;

import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@Tag(name = "Ranking", description = "Ranking management endpoints")
public class RankingController {

  private final RankingService rankingService;

  @GetMapping("/championships")
  @Operation(summary = "Get all championships")
  public List<ChampionshipDTO> getChampionships() {
    return rankingService.getChampionships();
  }

  @GetMapping("/championships/{id}/contenders")
  @Operation(summary = "Get ranked contenders for a championship")
  public List<RankedWrestlerDTO> getRankedContenders(@PathVariable Long id) {
    return rankingService.getRankedContenders(id);
  }

  @GetMapping("/championships/{id}/champion")
  @Operation(summary = "Get current champion for a championship")
  public ResponseEntity<List<ChampionDTO>> getCurrentChampions(@PathVariable Long id) {
    List<ChampionDTO> champions = rankingService.getCurrentChampions(id);
    return champions.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(champions);
  }
}
