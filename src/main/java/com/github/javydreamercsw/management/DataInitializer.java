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
package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.Initializable;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.sync.DataSyncContributor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements Initializable {

  private final boolean enabled;
  private final List<DataSyncContributor> contributors;

  @Autowired
  public DataInitializer(
      @Value("${data.initializer.enabled:true}") final boolean enabled,
      final List<DataSyncContributor> contributors) {
    this.enabled = enabled;
    this.contributors = contributors;
  }

  public void init() {
    log.debug("DataInitializer.init() called. enabled={}", enabled);
    if (enabled) {
      GeneralSecurityUtils.runAsAdmin(this::performInit);
    }
  }

  private void performInit() {
    contributors.forEach(DataSyncContributor::sync);
    log.debug("Data initialization complete.");
  }
}
