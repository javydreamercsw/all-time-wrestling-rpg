package com.github.javydreamercsw.management.service.show.planning.dto;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningChampionship;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningContext;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningPle;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ShowPlanningDtoMapper {

  public ShowPlanningContextDTO toDto(ShowPlanningContext context) {
    ShowPlanningContextDTO dto = new ShowPlanningContextDTO();
    dto.setLastMonthSegments(
        context.getLastMonthSegments().stream().map(this::toDto).collect(Collectors.toList()));
    dto.setCurrentRivalries(
        context.getCurrentRivalries().stream().map(this::toDto).collect(Collectors.toList()));
    dto.setLastMonthPromos(
        context.getLastMonthPromos().stream().map(this::toDto).collect(Collectors.toList()));
    dto.setShowTemplate(context.getShowTemplate());
    if (context.getChampionships() != null) {
      dto.setChampionships(
          context.getChampionships().stream().map(this::toDto).collect(Collectors.toList()));
    }
    if (context.getNextPle() != null) {
      dto.setNextPle(toDto(context.getNextPle()));
    }
    return dto;
  }

  public ShowPlanningSegmentDTO toDto(Segment segment) {
    ShowPlanningSegmentDTO dto = new ShowPlanningSegmentDTO();
    dto.setId(segment.getId());
    if (segment.getSegmentType() != null && "Promo".equals(segment.getSegmentType().getName())) {
      dto.setName("Promo");
    } else {
      dto.setName(segment.getSegmentRulesAsString());
    }
    dto.setSegmentDate(segment.getSegmentDate());
    dto.setShow(toDto(segment.getShow()));
    dto.setParticipants(
        segment.getParticipants().stream()
            .map(p -> p.getWrestler().getName())
            .collect(Collectors.toList()));
    dto.setWinners(
        segment.getParticipants().stream()
            .filter(p -> p.getIsWinner())
            .map(p -> p.getWrestler().getName())
            .collect(Collectors.toList()));
    dto.setSummary(segment.getSummary());
    return dto;
  }

  public ShowPlanningShowDTO toDto(Show show) {
    ShowPlanningShowDTO dto = new ShowPlanningShowDTO();
    dto.setId(show.getId());
    dto.setName(show.getName());
    return dto;
  }

  public ShowPlanningRivalryDTO toDto(Rivalry rivalry) {
    ShowPlanningRivalryDTO dto = new ShowPlanningRivalryDTO();
    dto.setId(rivalry.getId());
    dto.setName(rivalry.getDisplayName());
    dto.setParticipants(
        Arrays.asList(rivalry.getWrestler1().getName(), rivalry.getWrestler2().getName()));
    dto.setHeat(rivalry.getHeat());
    return dto;
  }

  public ShowPlanningChampionshipDTO toDto(ShowPlanningChampionship championship) {
    ShowPlanningChampionshipDTO dto = new ShowPlanningChampionshipDTO();
    dto.setChampionshipName(championship.getTitle().getName());
    if (championship.getChampion() != null) {
      dto.setChampionName(championship.getChampion().getName());
    }
    if (championship.getContender() != null) {
      dto.setContenderName(championship.getContender().getName());
    }
    return dto;
  }

  public ShowPlanningPleDTO toDto(ShowPlanningPle ple) {
    ShowPlanningPleDTO dto = new ShowPlanningPleDTO();
    dto.setPleName(ple.getPle().getName());
    dto.setPleDate(ple.getPle().getShowDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
    dto.setSummary(ple.getPle().getDescription());
    return dto;
  }
}
