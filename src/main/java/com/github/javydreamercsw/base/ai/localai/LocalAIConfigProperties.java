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
package com.github.javydreamercsw.base.ai.localai;

import com.github.javydreamercsw.base.ai.AiBaseProperties;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocalAIConfigProperties extends AiBaseProperties {
  private final AiSettingsService aiSettingsService;

  @Autowired
  public LocalAIConfigProperties(AiSettingsService aiSettingsService) {
    super(aiSettingsService);
    this.aiSettingsService = aiSettingsService;
  }

  public boolean isEnabled() {
    return aiSettingsService.isLocalAIEnabled();
  }

  public String getBaseUrl() {
    return aiSettingsService.getLocalAIBaseUrl();
  }

  public String getModel() {
    return aiSettingsService.getLocalAIModel();
  }

  public String getModelUrl() {
    return aiSettingsService.getLocalAIModelUrl();
  }
}
