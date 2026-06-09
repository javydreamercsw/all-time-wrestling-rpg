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
package com.github.javydreamercsw.management.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameSettingRepository extends JpaRepository<GameSetting, Long> {

  @Query("SELECT s FROM GameSetting s WHERE s.settingKey = :key AND s.universeId IS NULL")
  Optional<GameSetting> findGlobal(@Param("key") String key);

  Optional<GameSetting> findBySettingKeyAndUniverseId(String settingKey, Long universeId);

  @Query("SELECT s FROM GameSetting s WHERE s.universeId IS NULL ORDER BY s.settingKey")
  List<GameSetting> findAllGlobal();

  @Query("SELECT s FROM GameSetting s WHERE s.universeId = :universeId ORDER BY s.settingKey")
  List<GameSetting> findAllByUniverseId(@Param("universeId") Long universeId);
}
