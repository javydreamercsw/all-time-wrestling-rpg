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
package com.github.javydreamercsw.management.domain.team;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link TeamMember} rows — the ordered membership of a {@link Team}. */
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

  /** All members of a team in slot order. */
  List<TeamMember> findByTeamIdOrderBySlotAsc(Long teamId);

  /** All memberships of a wrestler across every team. */
  List<TeamMember> findByWrestler(Wrestler wrestler);
}
