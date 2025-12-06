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
package com.github.javydreamercsw.management.service.show.planning.dto;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.PromoType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.FactionDTO;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningChampionship;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningContext;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningPle;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ShowPlanningDtoMapper {

  public ShowPlanningContextDTO toDto(@NonNull ShowPlanningContext context) {
    ShowPlanningContextDTO dto = new ShowPlanningContextDTO();
    dto.setRecentSegments(
        context.getRecentSegments().stream().map(this::toDto).collect(Collectors.toList()));
    dto.setCurrentRivalries(
        context.getCurrentRivalries().stream().map(this::toDto).collect(Collectors.toList()));
    dto.setRecentPromos(
        context.getRecentPromos().stream().map(this::toDto).collect(Collectors.toList()));
    dto.setShowTemplate(context.getShowTemplate());
    if (context.getChampionships() != null) {
      dto.setChampionships(
          context.getChampionships().stream().map(this::toDto).collect(Collectors.toList()));
    }
    if (context.getNextPle() != null) {
      dto.setNextPle(toDto(context.getNextPle()));
    }
    if (context.getFullRoster() != null) {
      dto.setFullRoster(
          context.getFullRoster().stream()
              .map(com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO::new)
              .peek(wrestlerDto -> wrestlerDto.setMoveSet(null))
              .collect(Collectors.toList()));
    }
    if (context.getFactions() != null) {
      dto.setFactions(context.getFactions().stream().map(this::toDto).collect(Collectors.toList()));
    }
    return dto;
  }

  public ShowPlanningSegmentDTO toDto(@NonNull Segment segment) {
    ShowPlanningSegmentDTO dto = new ShowPlanningSegmentDTO();
    dto.setId(segment.getId());
    dto.setSegmentDate(segment.getSegmentDate());
    dto.setShowName(segment.getShow().getName());
    dto.setShowDate(
        segment.getShow().getShowDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
    dto.setParticipants(
        segment.getParticipants().stream()
            .map(p -> p.getWrestler().getName())
            .collect(Collectors.toList()));
    if (segment.getSegmentType() != null && "Promo".equals(segment.getSegmentType().getName())) {
      // Find the first segment rule that matches a PromoType and use its display name
      segment.getSegmentRules().stream()
          .map(SegmentRule::getName)
          .filter(
              name ->
                  Arrays.stream(PromoType.values())
                      .anyMatch(pt -> pt.getDisplayName().equals(name)))
          .findFirst()
          .ifPresentOrElse(
              dto::setName,
              () -> dto.setName("Promo") // Fallback if no specific promo rule name is found
              );
      dto.setWinners(
          segment.getParticipants().stream()
              .filter(SegmentParticipant::getIsWinner)
              .map(p -> p.getWrestler().getName())
              .collect(Collectors.toList()));
    } else {
      dto.setName(segment.getSegmentRulesAsString());
      dto.setWinners(
          segment.getParticipants().stream()
              .filter(SegmentParticipant::getIsWinner)
              .map(p -> p.getWrestler().getName())
              .collect(Collectors.toList()));
    }
    dto.setSummary(segment.getSummary());
    return dto;
  }

  public ShowPlanningShowDTO toDto(@NonNull Show show) {
    ShowPlanningShowDTO dto = new ShowPlanningShowDTO();
    dto.setId(show.getId());
    dto.setName(show.getName());
    return dto;
  }

  public ShowPlanningRivalryDTO toDto(@NonNull Rivalry rivalry) {
    ShowPlanningRivalryDTO dto = new ShowPlanningRivalryDTO();
    dto.setId(rivalry.getId());
    dto.setName(rivalry.getDisplayName());
    dto.setParticipants(
        Arrays.asList(rivalry.getWrestler1().getName(), rivalry.getWrestler2().getName()));
    dto.setHeat(rivalry.getHeat());
    return dto;
  }

  public ShowPlanningChampionshipDTO toDto(@NonNull ShowPlanningChampionship championship) {
    ShowPlanningChampionshipDTO dto = new ShowPlanningChampionshipDTO();
    dto.setChampionshipName(championship.getTitle().getName());
    if (!championship.getChampions().isEmpty()) {
      dto.setChampionName(
          championship.getChampions().stream()
              .map(Wrestler::getName)
              .collect(Collectors.joining(" & ")));
    }
    if (!championship.getContenders().isEmpty()) {
      dto.setContenderName(
          championship.getContenders().stream()
              .map(Wrestler::getName)
              .collect(Collectors.joining(" & ")));
    }
    return dto;
  }

  public ShowPlanningPleDTO toDto(@NonNull ShowPlanningPle ple) {
    ShowPlanningPleDTO dto = new ShowPlanningPleDTO();
    dto.setPleName(ple.getPle().getName());
    dto.setPleDate(ple.getPle().getShowDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
    dto.setSummary(ple.getPle().getDescription());
    return dto;
  }

  public FactionDTO toDto(@NonNull Faction faction) {
    FactionDTO dto = new FactionDTO();
    dto.setName(faction.getName());
    dto.setDescription(faction.getDescription());
    if (faction.getLeader() != null) {
      dto.setLeader(faction.getLeader().getName());
    }
    dto.setMembers(
        faction.getMembers().stream().map(Wrestler::getName).collect(Collectors.toList()));
    dto.setIsActive(faction.getIsActive());
    if (faction.getFormedDate() != null) {
      dto.setFormedDate(faction.getFormedDate().toString());
    }
    if (faction.getDisbandedDate() != null) {
      dto.setDisbandedDate(faction.getDisbandedDate().toString());
    }
    dto.setExternalId(faction.getExternalId());
    return dto;
  }
}
