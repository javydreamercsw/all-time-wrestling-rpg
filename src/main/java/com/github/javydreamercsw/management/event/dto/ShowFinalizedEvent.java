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
package com.github.javydreamercsw.management.event.dto;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Fired after a show is fully adjudicated and quality/attendance computed. */
@Getter
public class ShowFinalizedEvent extends ApplicationEvent {

  private final Show show;
  private final List<Segment> segments;

  public ShowFinalizedEvent(final Object source, final Show show, final List<Segment> segments) {
    super(source);
    this.show = show;
    this.segments = List.copyOf(segments);
  }
}
