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
package com.github.javydreamercsw.management.service.show.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ShowTemplateServiceTest {
  @Autowired private ShowTemplateRepository repository;
  @Autowired private ShowTemplateService service;
  @Autowired private ShowTypeService showTypeService;
  @MockitoBean private DataInitializer dataInitializer; // Exclude DataInitializer

  @BeforeEach
  void setUp() {
    // Manually create the "Weekly" ShowType for tests, but only if it doesn't already exist
    Optional<ShowType> existingType = showTypeService.findByName("Weekly");
    if (existingType.isEmpty()) {
      ShowType weeklyShowType = new ShowType();
      weeklyShowType.setName("Weekly");
      weeklyShowType.setDescription("Weekly Show Type");
      showTypeService.save(weeklyShowType); // Save it using the service
    }
  }

  /** Test of list method, of class ShowTemplateService. */
  @Test
  void testList() {
    Pageable pageable = Pageable.ofSize(10);
    List<ShowTemplate> result = service.list(pageable);
    assertNotNull(result);
  }

  /** Test of save method, of class ShowTemplateService. */
  @Test
  void testSave() {
    ShowTemplate st = new ShowTemplate();
    Optional<ShowType> type = showTypeService.findByName("Weekly");
    assertTrue(type.isPresent());
    st.setName("Test Show");
    st.setShowType(type.get());
    ShowTemplate result = service.save(st);
    assertNotNull(result);
    assertEquals(st.getName(), result.getName());
    repository.delete(result);
  }
}
