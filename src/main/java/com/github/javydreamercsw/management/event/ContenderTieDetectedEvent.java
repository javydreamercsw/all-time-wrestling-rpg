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
package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when contender auto-selection finds the top-ranked wrestlers too close to call. Carries
 * the tied wrestlers so a tie-breaker match can be suggested.
 */
@Getter
public class ContenderTieDetectedEvent extends ApplicationEvent {

  private final Title title;
  private final List<RankedWrestlerDTO> tiedWrestlers;

  public ContenderTieDetectedEvent(
      final Object source, final Title title, final List<RankedWrestlerDTO> tiedWrestlers) {
    super(source);
    this.title = title;
    this.tiedWrestlers = List.copyOf(tiedWrestlers);
  }
}
