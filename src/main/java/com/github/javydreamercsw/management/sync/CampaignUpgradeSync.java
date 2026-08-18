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

import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(200)
public class CampaignUpgradeSync implements DataSyncContributor {

  private final CampaignUpgradeService campaignUpgradeService;

  @Autowired
  public CampaignUpgradeSync(final CampaignUpgradeService campaignUpgradeService) {
    this.campaignUpgradeService = campaignUpgradeService;
  }

  @Override
  public void sync() {
    log.debug("Loading campaign upgrades.");
    campaignUpgradeService.loadUpgrades();
    log.debug("Campaign upgrades loaded.");
  }
}
