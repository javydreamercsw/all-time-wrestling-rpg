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
package com.github.javydreamercsw.management.service.show;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class ShowTemplateServiceIT extends ManagementIntegrationTest {

  @Autowired private ShowTemplateService showTemplateService;

  @Autowired private ShowTypeService showTypeService;

  @MockitoSpyBean private ShowTemplateRepository showTemplateRepository;

  @Test
  @DisplayName("Test that createShowTemplate evicts cache")
  void testCreateShowTemplateEvictsCache() {
    // First call, should hit the repository
    showTemplateService.findAll();
    verify(showTemplateRepository, times(1)).findAllWithShowType();

    // Create a new show type to use for the template
    ShowType showType =
        showTypeService.createOrUpdateShowType(
            "Weekly Test Show Template", "Weekly Template Show", 5, 2);

    // Create a new show template, should evict the cache
    showTemplateService.createOrUpdateTemplate(
        "Test Show Template", "Test Description", showType.getName(), "");

    // Second call, should hit the repository again
    showTemplateService.findAll();
    verify(showTemplateRepository, times(2)).findAllWithShowType();
  }
}
