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
package com.github.javydreamercsw.management.sync;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(20)
public class GameDateSync implements DataSyncContributor {

  private final GameSettingRepository gameSettingRepository;

  @Autowired
  public GameDateSync(final GameSettingRepository gameSettingRepository) {
    this.gameSettingRepository = gameSettingRepository;
  }

  @Override
  public void sync() {
    if (gameSettingRepository.findGlobal(GameSettingService.CURRENT_GAME_DATE_KEY).isEmpty()) {
      log.debug("In-game date not set. Initializing to current date.");
      GameSetting setting = new GameSetting();
      setting.setSettingKey(GameSettingService.CURRENT_GAME_DATE_KEY);
      setting.setValue(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
      gameSettingRepository.save(setting);
    }
  }
}
