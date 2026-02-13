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
package com.github.javydreamercsw.management.service.show.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowTemplateServiceTest {

  @Mock private ShowTemplateRepository showTemplateRepository;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private Clock clock;

  @InjectMocks private ShowTemplateService showTemplateService;

  @BeforeEach
  void setUp() {
    lenient().when(clock.instant()).thenReturn(java.time.Instant.now());
  }

  @Test
  void testCreateOrUpdateWithImageUrl() {
    ShowType showType = new ShowType();
    showType.setName("Weekly");
    when(showTypeRepository.findByName("Weekly")).thenReturn(Optional.of(showType));
    when(showTemplateRepository.findByName("Test")).thenReturn(Optional.empty());
    when(showTemplateRepository.save(any(ShowTemplate.class))).thenAnswer(i -> i.getArguments()[0]);

    ShowTemplate result =
        showTemplateService.createOrUpdateTemplate(
            "Test",
            "Desc",
            "Weekly",
            "http://notion",
            "http://image",
            null,
            null,
            null,
            1,
            com.github.javydreamercsw.management.domain.show.template.RecurrenceType.NONE,
            null,
            null,
            null,
            null);

    assertNotNull(result);
    assertEquals("http://image", result.getImageUrl());
  }

  @Test
  void testUpdateTemplateWithImageUrl() {
    ShowType showType = new ShowType();
    showType.setName("Weekly");
    ShowTemplate template = new ShowTemplate();
    template.setId(1L);
    template.setName("Test");

    when(showTemplateRepository.findById(1L)).thenReturn(Optional.of(template));
    when(showTypeRepository.findByName("Weekly")).thenReturn(Optional.of(showType));
    when(showTemplateRepository.save(any(ShowTemplate.class))).thenAnswer(i -> i.getArguments()[0]);

    Optional<ShowTemplate> result =
        showTemplateService.updateTemplate(
            1L,
            "Test New",
            "Desc New",
            "Weekly",
            "http://notion",
            "http://image",
            null,
            null,
            null,
            1,
            com.github.javydreamercsw.management.domain.show.template.RecurrenceType.NONE,
            null,
            null,
            null,
            null);

    assertEquals(true, result.isPresent());
    assertEquals("http://image", result.get().getImageUrl());
    assertEquals("Test New", result.get().getName());
  }
}
