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
package com.github.javydreamercsw.management.service;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSettingService {

  public static final String CURRENT_GAME_DATE_KEY = "current_game_date";
  private final GameSettingRepository repository;

  @PreAuthorize("permitAll()")
  public LocalDate getCurrentGameDate() {
    return repository
        .findById(CURRENT_GAME_DATE_KEY)
        .map(GameSetting::getValue)
        .map(LocalDate::parse)
        .orElse(LocalDate.now()); // Fallback to real date if not set
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @Transactional
  public void saveCurrentGameDate(LocalDate date) {
    GameSetting setting = repository.findById(CURRENT_GAME_DATE_KEY).orElseGet(GameSetting::new);
    setting.setId(CURRENT_GAME_DATE_KEY);
    setting.setValue(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    repository.save(setting);
  }

  @PreAuthorize("permitAll()")
  public Optional<GameSetting> findById(String key) {
    return repository.findById(key);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @Transactional
  public GameSetting save(GameSetting gameSetting) {
    log.debug(
        "Saving game setting: {} = {}",
        gameSetting.getId(),
        gameSetting.getId().contains("KEY") ? "********" : gameSetting.getValue());
    return repository.save(gameSetting);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @Transactional
  public void save(String key, String value) {
    GameSetting setting = repository.findById(key).orElseGet(GameSetting::new);
    setting.setId(key);
    setting.setValue(value);
    save(setting);
  }

  @PreAuthorize("permitAll()")
  public List<GameSetting> findAll() {
    return repository.findAll();
  }
}
