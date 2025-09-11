package com.github.javydreamercsw.management.service.show.planning.dto;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningContext;
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
    return dto;
  }

  public ShowPlanningSegmentDTO toDto(Segment segment) {
    ShowPlanningSegmentDTO dto = new ShowPlanningSegmentDTO();
    dto.setId(segment.getId());
    dto.setName(segment.getSegmentRulesAsString());
    dto.setSegmentDate(segment.getSegmentDate());
    dto.setShow(toDto(segment.getShow()));
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
    return dto;
  }
}
