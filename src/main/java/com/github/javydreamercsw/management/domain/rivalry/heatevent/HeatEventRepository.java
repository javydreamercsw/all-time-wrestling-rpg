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
package com.github.javydreamercsw.management.domain.rivalry.heatevent;

import com.github.javydreamercsw.management.domain.rivalry.HeatEvent;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface HeatEventRepository extends JpaRepository<HeatEvent, Long> {

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM HeatEvent he WHERE he.rivalry IN :rivalries")
  int deleteByRivalryIn(@Param("rivalries") List<Rivalry> rivalries);
}
